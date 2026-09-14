package com.example.data.repository

import com.example.model.Candle
import com.example.model.DepthItem
import com.example.model.MarketIndex
import com.example.model.MarketNews
import com.example.model.Stock
import com.example.model.TimeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class MarketRepository(private val scope: CoroutineScope) {

    private val _indices = MutableStateFlow<List<MarketIndex>>(getInitialIndices())
    val indices: StateFlow<List<MarketIndex>> = _indices.asStateFlow()

    private val _stocks = MutableStateFlow<List<Stock>>(getInitialStocks())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _news = MutableStateFlow<List<MarketNews>>(getInitialNews())
    val news: StateFlow<List<MarketNews>> = _news.asStateFlow()

    private val _isMarketOpen = MutableStateFlow(true)
    val isMarketOpen: StateFlow<Boolean> = _isMarketOpen.asStateFlow()

    init {
        startTickSimulation()
    }

    private fun startTickSimulation() {
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(1200) // 1.2s tick update
                updateMarketTicks()
            }
        }
    }

    private fun updateMarketTicks() {
        // Update indices
        _indices.value = _indices.value.map { idx ->
            val deltaPct = (Random.nextDouble(-0.15, 0.20)) / 100.0
            val newValue = (idx.value * (1 + deltaPct) * 100.0).roundToInt() / 100.0
            val newChange = (idx.change + (newValue - idx.value) * 100.0).roundToInt() / 100.0
            val newChangePct = ((newChange / (idx.value - idx.change)) * 10000.0).roundToInt() / 100.0
            idx.copy(
                value = newValue,
                change = newChange,
                changePercent = newChangePct,
                high = max(idx.high, newValue),
                low = min(idx.low, newValue)
            )
        }

        // Randomly pick 3-6 stocks to tick
        val currentStocks = _stocks.value.toMutableList()
        val numToUpdate = Random.nextInt(3, 7)
        val indicesToUpdate = (currentStocks.indices).shuffled().take(numToUpdate)

        for (i in indicesToUpdate) {
            val stock = currentStocks[i]
            val tickPct = (Random.nextDouble(-0.25, 0.30)) / 100.0
            val newPrice = (stock.price * (1 + tickPct) * 20.0).roundToInt() / 20.0 // 0.05 tick size (NSE)
            val diff = newPrice - (stock.price - stock.change)
            val newChange = (diff * 100.0).roundToInt() / 100.0
            val basePrice = stock.price - stock.change
            val newChangePct = if (basePrice > 0) ((newChange / basePrice) * 10000.0).roundToInt() / 100.0 else 0.0
            val newVolume = stock.volume + Random.nextInt(100, 5000)

            // Dynamic 5-level market depth
            val spread = 0.05
            val bids = (1..5).map { step ->
                DepthItem(
                    orders = Random.nextInt(5, 50),
                    quantity = Random.nextInt(100, 3000),
                    price = ((newPrice - step * spread) * 100.0).roundToInt() / 100.0
                )
            }
            val asks = (1..5).map { step ->
                DepthItem(
                    orders = Random.nextInt(5, 50),
                    quantity = Random.nextInt(100, 3000),
                    price = ((newPrice + step * spread) * 100.0).roundToInt() / 100.0
                )
            }

            currentStocks[i] = stock.copy(
                price = newPrice,
                change = newChange,
                changePercent = newChangePct,
                volume = newVolume,
                dayHigh = max(stock.dayHigh, newPrice),
                dayLow = min(stock.dayLow, newPrice),
                bids = bids,
                asks = asks
            )
        }
        _stocks.value = currentStocks
    }

    fun getCandlesForStock(symbol: String, timeframe: TimeFrame): List<Candle> {
        val stock = _stocks.value.find { it.symbol == symbol } ?: _stocks.value.first()
        val count = when (timeframe) {
            TimeFrame.M1 -> 50
            TimeFrame.M5 -> 60
            TimeFrame.M15 -> 50
            TimeFrame.H1 -> 40
            TimeFrame.D1 -> 60
            TimeFrame.W1 -> 52
        }

        val seed = symbol.hashCode() + timeframe.ordinal * 1000
        val rnd = Random(seed)
        val basePrice = stock.price * 0.85
        val candles = mutableListOf<Candle>()
        var curClose = basePrice.toFloat()
        val now = System.currentTimeMillis()
        val intervalMs = when (timeframe) {
            TimeFrame.M1 -> 60_000L
            TimeFrame.M5 -> 300_000L
            TimeFrame.M15 -> 900_000L
            TimeFrame.H1 -> 3600_000L
            TimeFrame.D1 -> 86400_000L
            TimeFrame.W1 -> 7 * 86400_000L
        }

        for (i in count downTo 1) {
            val ts = now - (i * intervalMs)
            val stepPct = rnd.nextDouble(-0.025, 0.028).toFloat()
            val curOpen = curClose
            curClose = curOpen * (1f + stepPct)
            val high = max(curOpen, curClose) + (curOpen * rnd.nextDouble(0.001, 0.015).toFloat())
            val low = min(curOpen, curClose) - (curOpen * rnd.nextDouble(0.001, 0.015).toFloat())
            val vol = rnd.nextDouble(10000.0, 500000.0).toFloat()
            candles.add(Candle(ts, curOpen, high, low, curClose, vol))
        }

        // Align last candle close with current stock price
        if (candles.isNotEmpty()) {
            val last = candles.last()
            val finalPrice = stock.price.toFloat()
            candles[candles.size - 1] = last.copy(
                close = finalPrice,
                high = max(last.high, finalPrice),
                low = min(last.low, finalPrice)
            )
        }
        return candles
    }

    private fun getInitialIndices(): List<MarketIndex> {
        return listOf(
            MarketIndex("NIFTY 50", "NIFTY 50", 25124.65, 168.40, 0.67, 25185.00, 24960.30),
            MarketIndex("SENSEX", "BSE SENSEX", 82345.20, 512.75, 0.63, 82510.10, 81850.00),
            MarketIndex("BANK NIFTY", "NIFTY BANK", 52410.80, 395.20, 0.76, 52580.40, 52020.10),
            MarketIndex("NIFTY IT", "NIFTY IT", 42150.30, -145.10, -0.34, 42400.00, 41980.50),
            MarketIndex("INDIA VIX", "India VIX", 13.15, -0.55, -4.01, 14.10, 12.90)
        )
    }

    private fun getInitialStocks(): List<Stock> {
        return listOf(
            Stock(
                symbol = "RELIANCE",
                name = "Reliance Industries Ltd",
                sector = "Energy",
                price = 2985.40,
                change = 38.50,
                changePercent = 1.31,
                volume = 4850000,
                dayHigh = 2998.00,
                dayLow = 2942.10,
                fiftyTwoWeekHigh = 3217.90,
                fiftyTwoWeekLow = 2220.30,
                vwap = 2972.15,
                peRatio = 27.8,
                roe = 10.4,
                marketCap = "₹20.2 Lakh Cr",
                rsi = 58.4,
                ema20 = 2940.20,
                ema50 = 2905.10
            ),
            Stock(
                symbol = "TCS",
                name = "Tata Consultancy Services",
                sector = "IT",
                price = 4210.80,
                change = -22.40,
                changePercent = -0.53,
                volume = 1820000,
                dayHigh = 4260.00,
                dayLow = 4195.00,
                fiftyTwoWeekHigh = 4585.00,
                fiftyTwoWeekLow = 3313.00,
                vwap = 4225.40,
                peRatio = 31.4,
                roe = 49.8,
                marketCap = "₹15.2 Lakh Cr",
                rsi = 46.2,
                ema20 = 4235.00,
                ema50 = 4180.00
            ),
            Stock(
                symbol = "HDFCBANK",
                name = "HDFC Bank Limited",
                sector = "Banking",
                price = 1665.25,
                change = 18.75,
                changePercent = 1.14,
                volume = 12450000,
                dayHigh = 1672.00,
                dayLow = 1640.50,
                fiftyTwoWeekHigh = 1794.00,
                fiftyTwoWeekLow = 1363.55,
                vwap = 1658.10,
                peRatio = 18.9,
                roe = 16.8,
                marketCap = "₹12.6 Lakh Cr",
                rsi = 62.1,
                ema20 = 1642.00,
                ema50 = 1620.00
            ),
            Stock(
                symbol = "INFY",
                name = "Infosys Limited",
                sector = "IT",
                price = 1895.50,
                change = -14.20,
                changePercent = -0.74,
                volume = 3980000,
                dayHigh = 1918.00,
                dayLow = 1888.00,
                fiftyTwoWeekHigh = 1991.45,
                fiftyTwoWeekLow = 1358.35,
                vwap = 1902.30,
                peRatio = 29.1,
                roe = 31.2,
                marketCap = "₹7.8 Lakh Cr",
                rsi = 49.5,
                ema20 = 1910.00,
                ema50 = 1870.00
            ),
            Stock(
                symbol = "ICICIBANK",
                name = "ICICI Bank Limited",
                sector = "Banking",
                price = 1245.80,
                change = 21.30,
                changePercent = 1.74,
                volume = 8720000,
                dayHigh = 1252.00,
                dayLow = 1222.00,
                fiftyTwoWeekHigh = 1300.00,
                fiftyTwoWeekLow = 912.00,
                vwap = 1238.40,
                peRatio = 17.5,
                roe = 18.5,
                marketCap = "₹8.7 Lakh Cr",
                rsi = 65.8,
                ema20 = 1220.00,
                ema50 = 1195.00
            ),
            Stock(
                symbol = "TATAMOTORS",
                name = "Tata Motors Limited",
                sector = "Auto",
                price = 985.60,
                change = 28.40,
                changePercent = 2.97,
                volume = 9430000,
                dayHigh = 992.50,
                dayLow = 952.00,
                fiftyTwoWeekHigh = 1179.00,
                fiftyTwoWeekLow = 600.50,
                vwap = 976.20,
                peRatio = 11.2,
                roe = 38.2,
                marketCap = "₹3.6 Lakh Cr",
                rsi = 68.2,
                ema20 = 960.00,
                ema50 = 945.00
            ),
            Stock(
                symbol = "SBIN",
                name = "State Bank of India",
                sector = "Banking",
                price = 812.30,
                change = 7.60,
                changePercent = 0.94,
                volume = 7120000,
                dayHigh = 818.00,
                dayLow = 801.50,
                fiftyTwoWeekHigh = 912.00,
                fiftyTwoWeekLow = 555.00,
                vwap = 809.50,
                peRatio = 10.4,
                roe = 17.9,
                marketCap = "₹7.2 Lakh Cr",
                rsi = 54.1,
                ema20 = 805.00,
                ema50 = 790.00
            ),
            Stock(
                symbol = "ITC",
                name = "ITC Limited",
                sector = "FMCG",
                price = 502.15,
                change = 4.20,
                changePercent = 0.84,
                volume = 5420000,
                dayHigh = 506.00,
                dayLow = 496.00,
                fiftyTwoWeekHigh = 528.00,
                fiftyTwoWeekLow = 399.30,
                vwap = 500.40,
                peRatio = 28.5,
                roe = 29.1,
                marketCap = "₹6.3 Lakh Cr",
                rsi = 52.8,
                ema20 = 498.00,
                ema50 = 485.00
            ),
            Stock(
                symbol = "BHARTIARTL",
                name = "Bharti Airtel Limited",
                sector = "Telecom",
                price = 1580.40,
                change = 32.10,
                changePercent = 2.07,
                volume = 4310000,
                dayHigh = 1592.00,
                dayLow = 1545.00,
                fiftyTwoWeekHigh = 1680.00,
                fiftyTwoWeekLow = 890.00,
                vwap = 1572.00,
                peRatio = 54.2,
                roe = 15.6,
                marketCap = "₹9.1 Lakh Cr",
                rsi = 71.4,
                ema20 = 1540.00,
                ema50 = 1490.00
            ),
            Stock(
                symbol = "LT",
                name = "Larsen & Toubro Ltd",
                sector = "Infra",
                price = 3620.00,
                change = -18.50,
                changePercent = -0.51,
                volume = 1250000,
                dayHigh = 3660.00,
                dayLow = 3595.00,
                fiftyTwoWeekHigh = 3919.00,
                fiftyTwoWeekLow = 2860.00,
                vwap = 3630.00,
                peRatio = 37.1,
                roe = 14.8,
                marketCap = "₹4.9 Lakh Cr",
                rsi = 44.5,
                ema20 = 3650.00,
                ema50 = 3610.00
            ),
            Stock(
                symbol = "SUNPHARMA",
                name = "Sun Pharmaceutical Inds",
                sector = "Pharma",
                price = 1845.00,
                change = 19.50,
                changePercent = 1.07,
                volume = 2100000,
                dayHigh = 1860.00,
                dayLow = 1820.00,
                fiftyTwoWeekHigh = 1950.00,
                fiftyTwoWeekLow = 1110.00,
                vwap = 1838.00,
                peRatio = 39.8,
                roe = 16.2,
                marketCap = "₹4.4 Lakh Cr",
                rsi = 61.2,
                ema20 = 1820.00,
                ema50 = 1780.00
            ),
            Stock(
                symbol = "MARUTI",
                name = "Maruti Suzuki India",
                sector = "Auto",
                price = 12450.00,
                change = -95.00,
                changePercent = -0.76,
                volume = 430000,
                dayHigh = 12620.00,
                dayLow = 12380.00,
                fiftyTwoWeekHigh = 13680.00,
                fiftyTwoWeekLow = 9737.00,
                vwap = 12490.00,
                peRatio = 26.4,
                roe = 17.5,
                marketCap = "₹3.9 Lakh Cr",
                rsi = 42.1,
                ema20 = 12550.00,
                ema50 = 12400.00
            ),
            Stock(
                symbol = "BAJFINANCE",
                name = "Bajaj Finance Limited",
                sector = "Finance",
                price = 7240.00,
                change = 142.00,
                changePercent = 2.00,
                volume = 1100000,
                dayHigh = 7290.00,
                dayLow = 7080.00,
                fiftyTwoWeekHigh = 8192.00,
                fiftyTwoWeekLow = 6360.00,
                vwap = 7180.00,
                peRatio = 29.8,
                roe = 22.4,
                marketCap = "₹4.5 Lakh Cr",
                rsi = 64.0,
                ema20 = 7100.00,
                ema50 = 6980.00
            ),
            Stock(
                symbol = "TITAN",
                name = "Titan Company Limited",
                sector = "Consumer",
                price = 3715.00,
                change = 12.00,
                changePercent = 0.32,
                volume = 890000,
                dayHigh = 3740.00,
                dayLow = 3690.00,
                fiftyTwoWeekHigh = 3886.00,
                fiftyTwoWeekLow = 3055.00,
                vwap = 3710.00,
                peRatio = 84.1,
                roe = 28.5,
                marketCap = "₹3.3 Lakh Cr",
                rsi = 51.2,
                ema20 = 3700.00,
                ema50 = 3640.00
            ),
            Stock(
                symbol = "ADANIENT",
                name = "Adani Enterprises Ltd",
                sector = "Conglomerate",
                price = 3050.00,
                change = 78.00,
                changePercent = 2.62,
                volume = 2850000,
                dayHigh = 3090.00,
                dayLow = 2960.00,
                fiftyTwoWeekHigh = 3743.00,
                fiftyTwoWeekLow = 2142.00,
                vwap = 3020.00,
                peRatio = 98.4,
                roe = 9.8,
                marketCap = "₹3.5 Lakh Cr",
                rsi = 59.8,
                ema20 = 2990.00,
                ema50 = 2950.00
            ),
            Stock(
                symbol = "ZOMATO",
                name = "Zomato Limited",
                sector = "Tech/Internet",
                price = 275.40,
                change = 11.20,
                changePercent = 4.24,
                volume = 24500000,
                dayHigh = 278.50,
                dayLow = 262.00,
                fiftyTwoWeekHigh = 298.00,
                fiftyTwoWeekLow = 98.50,
                vwap = 271.00,
                peRatio = 112.0,
                roe = 4.5,
                marketCap = "₹2.4 Lakh Cr",
                rsi = 74.5,
                ema20 = 258.00,
                ema50 = 235.00
            )
        )
    }

    private fun getInitialNews(): List<MarketNews> {
        return listOf(
            MarketNews(
                id = "n1",
                title = "RBI MPC Policy: Repo rate held steady at 6.50%; growth outlook raised to 7.2%",
                source = "Economic Times",
                timeAgo = "18m ago",
                sentiment = "Bullish",
                relatedSymbols = listOf("HDFCBANK", "ICICIBANK", "SBIN")
            ),
            MarketNews(
                id = "n2",
                title = "Tata Motors reports 14% surge in JLR retail sales; EV rollout on track",
                source = "Moneycontrol",
                timeAgo = "45m ago",
                sentiment = "Bullish",
                relatedSymbols = listOf("TATAMOTORS")
            ),
            MarketNews(
                id = "n3",
                title = "FIIs turn net buyers in Indian equities, pump ₹2,840 Cr in cash segment today",
                source = "Livemint",
                timeAgo = "1h ago",
                sentiment = "Bullish",
                relatedSymbols = listOf("RELIANCE", "INFY", "TCS")
            ),
            MarketNews(
                id = "n4",
                title = "Crude oil slips below \$72/bbl; major relief for Indian OMCs and specialty chemicals",
                source = "CNBC TV18",
                timeAgo = "2h ago",
                sentiment = "Bullish",
                relatedSymbols = listOf("RELIANCE")
            ),
            MarketNews(
                id = "n5",
                title = "SEBI introduces new framework for index derivatives eligibility to curb retail F&O excess",
                source = "Bloomberg Quint",
                timeAgo = "3h ago",
                sentiment = "Neutral",
                relatedSymbols = listOf("BAJFINANCE")
            )
        )
    }
}
