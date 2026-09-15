package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OrderSide
import com.example.model.RiskCalculation
import com.example.model.Stock
import com.example.ui.components.RiskCalculatorView
import com.example.ui.components.formatInr
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import java.util.Locale

enum class ScreenerStrategy(val label: String) {
    ALL("All NSE"),
    RSI_BULLISH("RSI Momentum (>60)"),
    RSI_OVERSOLD("RSI Oversold (<48)"),
    NEAR_52W_HIGH("Near 52W High"),
    VALUE_HIGH_ROE("P/E < 25 & ROE > 15%")
}

@Composable
fun ScreenerRiskScreen(
    stocks: List<Stock>,
    riskCalculation: RiskCalculation,
    onCalculateRisk: (Double, Double, Double, Double, Double) -> Unit,
    onStockClick: (Stock) -> Unit,
    onOrderApply: ((quantity: Int, side: OrderSide, entry: Double, sl: Double, target: Double) -> Unit)? = null
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Screener, 1: Risk Calculator
    var selectedStrategy by remember { mutableStateOf(ScreenerStrategy.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Technical Screener", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Risk & Position Sizer", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        if (activeTab == 0) {
            StockScreenerView(
                stocks = stocks,
                selectedStrategy = selectedStrategy,
                onStrategySelect = { selectedStrategy = it },
                onStockClick = onStockClick
            )
        } else {
            RiskCalculatorView(
                currentRisk = riskCalculation,
                onCalculate = onCalculateRisk,
                onOrderApply = onOrderApply
            )
        }
    }
}

@Composable
fun StockScreenerView(
    stocks: List<Stock>,
    selectedStrategy: ScreenerStrategy,
    onStrategySelect: (ScreenerStrategy) -> Unit,
    onStockClick: (Stock) -> Unit
) {
    val filtered = remember(stocks, selectedStrategy) {
        when (selectedStrategy) {
            ScreenerStrategy.ALL -> stocks
            ScreenerStrategy.RSI_BULLISH -> stocks.filter { it.rsi >= 60.0 }
            ScreenerStrategy.RSI_OVERSOLD -> stocks.filter { it.rsi <= 48.0 }
            ScreenerStrategy.NEAR_52W_HIGH -> stocks.filter { (it.fiftyTwoWeekHigh - it.price) / it.fiftyTwoWeekHigh <= 0.08 }
            ScreenerStrategy.VALUE_HIGH_ROE -> stocks.filter { it.peRatio <= 25.0 && it.roe >= 15.0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "PRESET SCREENER FILTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ScreenerStrategy.values()) { strat ->
                    FilterChip(
                        selected = selectedStrategy == strat,
                        onClick = { onStrategySelect(strat) },
                        label = { Text(strat.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "MATCHED INSTRUMENTS (${filtered.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(filtered) { stock ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onStockClick(stock) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${stock.sector} • Cap: ${stock.marketCap}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatInr(stock.price), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            val isPos = stock.change >= 0
                            Text(
                                text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", stock.changePercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPos) BullishGreen else BearishRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fundamental & Technical Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge(label = "RSI (14)", value = String.format(Locale.US, "%.1f", stock.rsi))
                        MetricBadge(label = "P/E", value = String.format(Locale.US, "%.1f", stock.peRatio))
                        MetricBadge(label = "ROE", value = "${String.format(Locale.US, "%.1f", stock.roe)}%")
                        MetricBadge(label = "52W High", value = formatInr(stock.fiftyTwoWeekHigh))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

