package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiMode
import com.example.data.ai.AiResponse
import com.example.data.ai.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.local.PaperOrderEntity
import com.example.data.local.PaperPositionEntity
import com.example.data.local.TradeJournalEntity
import com.example.data.local.WatchlistEntity
import com.example.data.repository.MarketRepository
import com.example.model.Candle
import com.example.model.ChartType
import com.example.model.DrawingToolType
import com.example.model.IndicatorType
import com.example.model.MarketIndex
import com.example.model.MarketNews
import com.example.model.OrderSide
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.PaperHolding
import com.example.model.PaperOrder
import com.example.model.PaperPosition
import com.example.model.ProductType
import com.example.model.RiskCalculation
import com.example.model.Stock
import com.example.model.TimeFrame
import com.example.model.TradeJournal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    CHART("Charts"),
    PAPER_TRADING("Paper Trade"),
    SCREENER("Screener"),
    AI_ASSISTANT("AI Assistant"),
    JOURNAL_BROKER("Journal & Broker")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val imageBase64: String? = null,
    val searchSources: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val mode: AiMode? = null
)

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.tradeDao()
    private val marketRepo = MarketRepository(viewModelScope)
    private val aiService = GeminiAiService()

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Market data
    val indices: StateFlow<List<MarketIndex>> = marketRepo.indices
    val stocks: StateFlow<List<Stock>> = marketRepo.stocks
    val news: StateFlow<List<MarketNews>> = marketRepo.news
    val isMarketOpen: StateFlow<Boolean> = marketRepo.isMarketOpen

    // Selected Stock for Charts & Trading
    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    // Chart Workspace State
    private val _selectedTimeframe = MutableStateFlow(TimeFrame.M15)
    val selectedTimeframe: StateFlow<TimeFrame> = _selectedTimeframe.asStateFlow()

    private val _chartType = MutableStateFlow(ChartType.CANDLESTICK)
    val chartType: StateFlow<ChartType> = _chartType.asStateFlow()

    private val _selectedIndicators = MutableStateFlow(setOf(IndicatorType.SMA20, IndicatorType.VWAP))
    val selectedIndicators: StateFlow<Set<IndicatorType>> = _selectedIndicators.asStateFlow()

    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.NONE)
    val activeDrawingTool: StateFlow<DrawingToolType> = _activeDrawingTool.asStateFlow()

    private val _chartCandles = MutableStateFlow<List<Candle>>(emptyList())
    val chartCandles: StateFlow<List<Candle>> = _chartCandles.asStateFlow()

    // Watchlist from Room
    val watchlistSymbols: StateFlow<List<String>> = dao.getWatchlist()
        .combine(MutableStateFlow(emptyList<String>())) { list, _ ->
            list.map { it.symbol }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Paper Trading Balance & Portfolio
    private val _demoBalance = MutableStateFlow(1000000.0) // ₹10,00,000 initial virtual cash
    val demoBalance: StateFlow<Double> = _demoBalance.asStateFlow()

    // Orders & Positions from Room
    val paperOrders: StateFlow<List<PaperOrder>> = dao.getAllOrders()
        .combine(MutableStateFlow(emptyList<PaperOrder>())) { list, _ ->
            list.map { e ->
                PaperOrder(
                    id = e.id,
                    symbol = e.symbol,
                    side = OrderSide.valueOf(e.side),
                    orderType = OrderType.valueOf(e.orderType),
                    product = ProductType.valueOf(e.product),
                    quantity = e.quantity,
                    price = e.price,
                    triggerPrice = e.triggerPrice,
                    status = OrderStatus.valueOf(e.status),
                    timestamp = e.timestamp,
                    executionPrice = e.executionPrice,
                    brokerage = e.brokerage
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paperPositions: StateFlow<List<PaperPosition>> = dao.getAllPositions()
        .combine(stocks) { posEntities, curStocks ->
            posEntities.map { pe ->
                val currentPrice = curStocks.find { it.symbol == pe.symbol }?.price ?: pe.buyAvgPrice
                PaperPosition(
                    symbol = pe.symbol,
                    product = ProductType.valueOf(pe.product),
                    quantity = pe.quantity,
                    buyAvgPrice = pe.buyAvgPrice,
                    currentPrice = currentPrice,
                    realizedPnl = pe.realizedPnl
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Holdings (calculated from delivery positions or sample investments)
    private val _holdings = MutableStateFlow(
        listOf(
            PaperHolding("RELIANCE", 50, 2850.00, 2985.40),
            PaperHolding("HDFCBANK", 100, 1580.00, 1665.25),
            PaperHolding("TATAMOTORS", 150, 890.00, 985.60),
            PaperHolding("ITC", 200, 480.00, 502.15)
        )
    )
    val holdings: StateFlow<List<PaperHolding>> = combine(_holdings, stocks) { list, curStocks ->
        list.map { h ->
            val cur = curStocks.find { it.symbol == h.symbol }?.price ?: h.avgPrice
            h.copy(currentPrice = cur)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Assistant State
    private val _aiMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Namaste! I am BharatTrade AI, your technical analysis & market research assistant.\n\n" +
                        "• Select **Fast Insight** for quick trade setups & glossary.\n" +
                        "• Select **Deep Technical Reasoning** (Thinking Mode) for full chart structure & strategy backtesting.\n" +
                        "• Select **Live Market Search** for search-grounded Indian corporate news and earnings.\n" +
                        "• Select **Chart Blueprint** to generate technical breakout visuals with aspect ratio & resolution controls.",
                mode = AiMode.FAST_LITE
            )
        )
    )
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiMode = MutableStateFlow(AiMode.FAST_LITE)
    val aiMode: StateFlow<AiMode> = _aiMode.asStateFlow()

    private val _aiAspectRatio = MutableStateFlow("1:1")
    val aiAspectRatio: StateFlow<String> = _aiAspectRatio.asStateFlow()

    private val _aiImageSize = MutableStateFlow("1K")
    val aiImageSize: StateFlow<String> = _aiImageSize.asStateFlow()

    // Trade Journal from Room
    val journalEntries: StateFlow<List<TradeJournal>> = dao.getJournalEntries()
        .combine(MutableStateFlow(emptyList<TradeJournal>())) { list, _ ->
            list.map { e ->
                TradeJournal(
                    id = e.id,
                    symbol = e.symbol,
                    side = OrderSide.valueOf(e.side),
                    quantity = e.quantity,
                    entryPrice = e.entryPrice,
                    exitPrice = e.exitPrice,
                    pnl = e.pnl,
                    strategyTag = e.strategyTag,
                    emotionTag = e.emotionTag,
                    notes = e.notes,
                    lessonLearned = e.lessonLearned,
                    timestamp = e.timestamp
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Risk Calculator state
    private val _riskCalculation = MutableStateFlow(
        calculateRisk(
            capital = 500000.0,
            riskPercent = 1.5,
            entry = 2985.0,
            sl = 2940.0,
            target = 3075.0
        )
    )
    val riskCalculation: StateFlow<RiskCalculation> = _riskCalculation.asStateFlow()

    // Order Placement Sheet State
    private val _showOrderSheet = MutableStateFlow(false)
    val showOrderSheet: StateFlow<Boolean> = _showOrderSheet.asStateFlow()

    private val _orderSide = MutableStateFlow(OrderSide.BUY)
    val orderSide: StateFlow<OrderSide> = _orderSide.asStateFlow()

    init {
        // Initialize default stock
        viewModelScope.launch {
            val initialList = stocks.value
            if (initialList.isNotEmpty()) {
                selectStock(initialList.first())
            }
            // Add default watchlists if empty
            val currentWatch = dao.getWatchlist()
            dao.addToWatchlist(WatchlistEntity("RELIANCE"))
            dao.addToWatchlist(WatchlistEntity("HDFCBANK"))
            dao.addToWatchlist(WatchlistEntity("TATAMOTORS"))
            dao.addToWatchlist(WatchlistEntity("ZOMATO"))
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectStock(stock: Stock) {
        _selectedStock.value = stock
        loadCandles(stock.symbol, _selectedTimeframe.value)
    }

    fun setTimeframe(timeframe: TimeFrame) {
        _selectedTimeframe.value = timeframe
        _selectedStock.value?.let { loadCandles(it.symbol, timeframe) }
    }

    fun setChartType(type: ChartType) {
        _chartType.value = type
    }

    fun toggleIndicator(indicator: IndicatorType) {
        val cur = _selectedIndicators.value.toMutableSet()
        if (cur.contains(indicator)) {
            cur.remove(indicator)
        } else {
            cur.add(indicator)
        }
        _selectedIndicators.value = cur
    }

    fun setDrawingTool(tool: DrawingToolType) {
        _activeDrawingTool.value = tool
    }

    private fun loadCandles(symbol: String, timeframe: TimeFrame) {
        val candles = marketRepo.getCandlesForStock(symbol, timeframe)
        _chartCandles.value = candles
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = watchlistSymbols.value.contains(symbol)
            if (exists) {
                dao.removeFromWatchlist(symbol)
            } else {
                dao.addToWatchlist(WatchlistEntity(symbol))
            }
        }
    }

    fun openOrderSheet(stock: Stock, side: OrderSide) {
        _selectedStock.value = stock
        _orderSide.value = side
        _showOrderSheet.value = true
    }

    fun closeOrderSheet() {
        _showOrderSheet.value = false
    }

    fun placePaperOrder(
        symbol: String,
        side: OrderSide,
        orderType: OrderType,
        product: ProductType,
        quantity: Int,
        price: Double,
        triggerPrice: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val stock = stocks.value.find { it.symbol == symbol } ?: return@launch
            val executionPrice = if (orderType == OrderType.MARKET) stock.price else price
            val brokerage = 20.0 // Standard Indian discount broker fee
            val totalValue = executionPrice * quantity
            val marginRequired = if (product == ProductType.MIS) totalValue / 5.0 else totalValue

            if (_demoBalance.value < marginRequired) {
                // Not enough margin
                return@launch
            }

            val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
            val orderEntity = PaperOrderEntity(
                id = orderId,
                symbol = symbol,
                side = side.name,
                orderType = orderType.name,
                product = product.name,
                quantity = quantity,
                price = price,
                triggerPrice = triggerPrice,
                status = OrderStatus.EXECUTED.name,
                timestamp = System.currentTimeMillis(),
                executionPrice = executionPrice,
                brokerage = brokerage
            )
            dao.insertOrder(orderEntity)

            // Update balance
            _demoBalance.value -= (brokerage + if (side == OrderSide.BUY) marginRequired else 0.0)

            // Update positions
            val existingPositions = dao.getAllPositions()
            val existingPos = dao.getAllPositions() // check open positions
            // Simplified position update:
            val posEntity = PaperPositionEntity(
                symbol = symbol,
                product = product.name,
                quantity = if (side == OrderSide.BUY) quantity else -quantity,
                buyAvgPrice = executionPrice,
                currentPrice = stock.price,
                realizedPnl = 0.0
            )
            dao.insertPosition(posEntity)
            _showOrderSheet.value = false
        }
    }

    fun closePosition(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pos = paperPositions.value.find { it.symbol == symbol } ?: return@launch
            val pnl = pos.unrealizedPnl
            // Return margin + pnl
            _demoBalance.value += (pos.buyAvgPrice * abs(pos.quantity)) + pnl - 20.0
            dao.deletePosition(symbol)

            // Auto-log to trading journal
            val journal = TradeJournalEntity(
                symbol = symbol,
                side = if (pos.quantity > 0) "BUY" else "SELL",
                quantity = abs(pos.quantity),
                entryPrice = pos.buyAvgPrice,
                exitPrice = pos.currentPrice,
                pnl = pnl,
                strategyTag = "Paper Intraday",
                emotionTag = if (pnl >= 0) "Disciplined" else "Patient",
                notes = "Position squared off via Paper Trade dashboard.",
                lessonLearned = "Followed profit target & stop-loss rules.",
                timestamp = System.currentTimeMillis()
            )
            dao.insertJournal(journal)
        }
    }

    fun emergencyKillSwitch() {
        viewModelScope.launch(Dispatchers.IO) {
            val allPos = paperPositions.value
            for (p in allPos) {
                closePosition(p.symbol)
            }
            dao.clearPositions()
        }
    }

    fun resetDemoAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            _demoBalance.value = 1000000.0
            dao.clearOrders()
            dao.clearPositions()
        }
    }

    // AI Assistant actions
    fun setAiMode(mode: AiMode) {
        _aiMode.value = mode
    }

    fun setAiAspectRatio(ratio: String) {
        _aiAspectRatio.value = ratio
    }

    fun setAiImageSize(size: String) {
        _aiImageSize.value = size
    }

    fun sendAiMessage(prompt: String, chartBitmap: Bitmap? = null) {
        if (prompt.isBlank()) return
        val currentMode = _aiMode.value
        val userMsg = ChatMessage(isUser = true, text = prompt, mode = currentMode)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val response: AiResponse = aiService.queryAi(
                prompt = prompt,
                mode = currentMode,
                aspectRatio = _aiAspectRatio.value,
                imageSize = _aiImageSize.value,
                chartBitmap = chartBitmap
            )
            _isAiLoading.value = false
            val botMsg = ChatMessage(
                isUser = false,
                text = response.text,
                imageBase64 = response.imageBase64,
                searchSources = response.searchSources,
                mode = currentMode
            )
            _aiMessages.value = _aiMessages.value + botMsg
        }
    }

    // Risk Calculator
    fun updateRiskCalculation(capital: Double, riskPct: Double, entry: Double, sl: Double, target: Double) {
        _riskCalculation.value = calculateRisk(capital, riskPct, entry, sl, target)
    }

    private fun calculateRisk(capital: Double, riskPercent: Double, entry: Double, sl: Double, target: Double): RiskCalculation {
        val maxRiskAmt = capital * (riskPercent / 100.0)
        val perShareRisk = abs(entry - sl)
        val qty = if (perShareRisk > 0) (maxRiskAmt / perShareRisk).toInt() else 1
        val safeQty = max(1, qty)
        val positionCapital = safeQty * entry
        val profitPerShare = abs(target - entry)
        val targetProfit = safeQty * profitPerShare
        val rrRatio = if (perShareRisk > 0) ((profitPerShare / perShareRisk) * 100.0).roundToInt() / 100.0 else 0.0
        val charges = 40.0 + (positionCapital * 0.0003) // Estimated STT + Exchange charges

        return RiskCalculation(
            capital = capital,
            riskPercent = riskPercent,
            entryPrice = entry,
            stopLossPrice = sl,
            targetPrice = target,
            recommendedQuantity = safeQty,
            positionCapitalRequired = positionCapital,
            maxRiskAmount = maxRiskAmt,
            targetProfitAmount = targetProfit,
            riskRewardRatio = rrRatio,
            estimatedCharges = charges
        )
    }

    // Trade Journal
    fun addJournalEntry(
        symbol: String,
        side: OrderSide,
        qty: Int,
        entry: Double,
        exit: Double,
        strategyTag: String,
        emotionTag: String,
        notes: String,
        lesson: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val pnl = (exit - entry) * (if (side == OrderSide.BUY) qty else -qty)
            val entryObj = TradeJournalEntity(
                symbol = symbol,
                side = side.name,
                quantity = qty,
                entryPrice = entry,
                exitPrice = exit,
                pnl = pnl,
                strategyTag = strategyTag,
                emotionTag = emotionTag,
                notes = notes,
                lessonLearned = lesson,
                timestamp = System.currentTimeMillis()
            )
            dao.insertJournal(entryObj)
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteJournal(id)
        }
    }
}
