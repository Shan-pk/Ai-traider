package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OrderSheetModal
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ChartWorkspaceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JournalBrokerScreen
import com.example.ui.screens.PaperTradingScreen
import com.example.ui.screens.ScreenerRiskScreen
import com.example.ui.theme.BharatTradeTheme
import com.example.ui.theme.BullishGreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TradingViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TradingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BharatTradeTheme {
                BharatTradeApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatTradeApp(viewModel: TradingViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val indices by viewModel.indices.collectAsState()
    val stocks by viewModel.stocks.collectAsState()
    val watchlist by viewModel.watchlistSymbols.collectAsState()
    val news by viewModel.news.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    val candles by viewModel.chartCandles.collectAsState()
    val timeframe by viewModel.selectedTimeframe.collectAsState()
    val chartType by viewModel.chartType.collectAsState()
    val indicators by viewModel.selectedIndicators.collectAsState()
    val drawingTool by viewModel.activeDrawingTool.collectAsState()

    // Paper trade state
    val demoBalance by viewModel.demoBalance.collectAsState()
    val paperOrders by viewModel.paperOrders.collectAsState()
    val paperPositions by viewModel.paperPositions.collectAsState()
    val holdings by viewModel.holdings.collectAsState()

    // AI state
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiMode by viewModel.aiMode.collectAsState()
    val aiAspectRatio by viewModel.aiAspectRatio.collectAsState()
    val aiImageSize by viewModel.aiImageSize.collectAsState()

    // Screener & Risk state
    val riskCalculation by viewModel.riskCalculation.collectAsState()

    // Journal state
    val journalEntries by viewModel.journalEntries.collectAsState()

    // Order modal sheet state
    val showOrderSheet by viewModel.showOrderSheet.collectAsState()
    val orderSide by viewModel.orderSide.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BullishGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BharatTrade",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.setTab(AppTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                    label = { Text("Market", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.CHART,
                    onClick = { viewModel.setTab(AppTab.CHART) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "Charts") },
                    label = { Text("Charts", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.PAPER_TRADING,
                    onClick = { viewModel.setTab(AppTab.PAPER_TRADING) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Paper Trade") },
                    label = { Text("Paper", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SCREENER,
                    onClick = { viewModel.setTab(AppTab.SCREENER) },
                    icon = { Icon(Icons.Default.FilterList, contentDescription = "Screener") },
                    label = { Text("Screener", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.AI_ASSISTANT,
                    onClick = { viewModel.setTab(AppTab.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI Lab", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.JOURNAL_BROKER,
                    onClick = { viewModel.setTab(AppTab.JOURNAL_BROKER) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Journal & Broker") },
                    label = { Text("Broker", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    indices = indices,
                    stocks = stocks,
                    watchlist = watchlist,
                    news = news,
                    positions = paperPositions,
                    virtualBalance = demoBalance,
                    onStockClick = { stock ->
                        viewModel.selectStock(stock)
                        viewModel.setTab(AppTab.CHART)
                    },
                    onWatchlistToggle = { viewModel.toggleWatchlist(it) }
                )

                AppTab.CHART -> ChartWorkspaceScreen(
                    stock = selectedStock,
                    candles = candles,
                    selectedTimeframe = timeframe,
                    chartType = chartType,
                    selectedIndicators = indicators,
                    activeDrawingTool = drawingTool,
                    onTimeframeSelect = { viewModel.setTimeframe(it) },
                    onChartTypeSelect = { viewModel.setChartType(it) },
                    onIndicatorToggle = { viewModel.toggleIndicator(it) },
                    onDrawingToolSelect = { viewModel.setDrawingTool(it) },
                    onOpenOrderSheet = { stock, side ->
                        viewModel.openOrderSheet(stock, side)
                    }
                )

                AppTab.PAPER_TRADING -> PaperTradingScreen(
                    virtualBalance = demoBalance,
                    positions = paperPositions,
                    orders = paperOrders,
                    holdings = holdings,
                    onClosePosition = { viewModel.closePosition(it) },
                    onEmergencyKillSwitch = { viewModel.emergencyKillSwitch() },
                    onResetDemoAccount = { viewModel.resetDemoAccount() }
                )

                AppTab.SCREENER -> ScreenerRiskScreen(
                    stocks = stocks,
                    riskCalculation = riskCalculation,
                    onCalculateRisk = { cap, risk, ent, sl, tgt ->
                        viewModel.updateRiskCalculation(cap, risk, ent, sl, tgt)
                    },
                    onStockClick = { stock ->
                        viewModel.selectStock(stock)
                        viewModel.setTab(AppTab.CHART)
                    }
                )

                AppTab.AI_ASSISTANT -> AiAssistantScreen(
                    messages = aiMessages,
                    isLoading = isAiLoading,
                    currentMode = aiMode,
                    currentAspectRatio = aiAspectRatio,
                    currentImageSize = aiImageSize,
                    onModeSelect = { viewModel.setAiMode(it) },
                    onAspectRatioSelect = { viewModel.setAiAspectRatio(it) },
                    onImageSizeSelect = { viewModel.setAiImageSize(it) },
                    onSendMessage = { viewModel.sendAiMessage(it) }
                )

                AppTab.JOURNAL_BROKER -> JournalBrokerScreen(
                    journalEntries = journalEntries,
                    onAddJournal = { sym, side, qty, en, ex, strat, emo, notes, les ->
                        viewModel.addJournalEntry(sym, side, qty, en, ex, strat, emo, notes, les)
                    },
                    onDeleteJournal = { viewModel.deleteJournalEntry(it) }
                )
            }

            // Order Placement Bottom Sheet Modal
            if (showOrderSheet && selectedStock != null) {
                OrderSheetModal(
                    stock = selectedStock!!,
                    initialSide = orderSide,
                    availableBalance = demoBalance,
                    onDismiss = { viewModel.closeOrderSheet() },
                    onSubmitOrder = { sym, side, type, prod, qty, pr, trPr ->
                        viewModel.placePaperOrder(sym, side, type, prod, qty, pr, trPr)
                    }
                )
            }
        }
    }
}
