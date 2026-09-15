package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OrderSide
import com.example.model.ProductType
import com.example.model.RiskCalculation
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class LotPreset(val label: String, val size: Int) {
    EQUITY("Equity (1x)", 1),
    NIFTY_50("Nifty (25)", 25),
    BANKNIFTY("BankNifty (15)", 15),
    FINNIFTY("FinNifty (25)", 25),
    MIDCPNIFTY("Midcap (50)", 50)
}

/**
 * World-Class Risk & Position Sizing Composable for Indian Retail Traders.
 * Calculates position sizing based on user-defined stop-loss, take-profit levels,
 * account risk percentage, capital, product type (MIS vs CNC), and instrument lot size.
 */
@Composable
fun RiskCalculatorView(
    modifier: Modifier = Modifier,
    initialCapital: Double = 500000.0,
    initialRiskPercent: Double = 1.5,
    initialEntry: Double = 2985.0,
    initialStopLoss: Double = 2940.0,
    initialTakeProfit: Double = 3075.0,
    onOrderApply: ((quantity: Int, side: OrderSide, entry: Double, sl: Double, target: Double) -> Unit)? = null
) {
    var capitalInput by remember { mutableStateOf(initialCapital.toInt().toString()) }
    var riskPct by remember { mutableDoubleStateOf(initialRiskPercent) }
    var side by remember { mutableStateOf(OrderSide.BUY) }
    var entryInput by remember { mutableStateOf(initialEntry.toString()) }
    var slInput by remember { mutableStateOf(initialStopLoss.toString()) }
    var tpInput by remember { mutableStateOf(initialTakeProfit.toString()) }
    var productType by remember { mutableStateOf(ProductType.MIS) }
    var selectedLotPreset by remember { mutableStateOf(LotPreset.EQUITY) }
    var showTaxBreakdown by remember { mutableStateOf(false) }

    val capital = capitalInput.toDoubleOrNull() ?: 100000.0
    val entry = entryInput.toDoubleOrNull() ?: 100.0
    val stopLoss = slInput.toDoubleOrNull() ?: 95.0
    val takeProfit = tpInput.toDoubleOrNull() ?: 110.0

    // Calculations
    val isLong = side == OrderSide.BUY
    val isValidSetup = if (isLong) {
        stopLoss < entry && takeProfit > entry
    } else {
        stopLoss > entry && takeProfit < entry
    }

    val riskPerShare = abs(entry - stopLoss)
    val rewardPerShare = abs(takeProfit - entry)
    val slPercent = if (entry > 0) (riskPerShare / entry) * 100.0 else 0.0
    val tpPercent = if (entry > 0) (rewardPerShare / entry) * 100.0 else 0.0

    val maxAccountRisk = capital * (riskPct / 100.0)

    // Base quantity based strictly on account risk tolerance
    val rawQty = if (riskPerShare > 0) (maxAccountRisk / riskPerShare).toInt() else 0

    // Align with lot size (e.g. F&O lots or 1 for Equity)
    val lotSize = selectedLotPreset.size
    val lotsCount = if (lotSize > 1) rawQty / lotSize else rawQty
    val sizedQtyByRisk = if (lotSize > 1) lotsCount * lotSize else rawQty

    // Leverage & Margin limit check
    val marginPerShare = if (productType == ProductType.MIS) entry * 0.20 else entry // 5x intraday leverage
    val maxAffordableQty = if (marginPerShare > 0) (capital / marginPerShare).toInt() else 0
    val recommendedQty = max(0, min(sizedQtyByRisk, maxAffordableQty))
    val finalLots = if (lotSize > 1) recommendedQty / lotSize else recommendedQty

    val totalExposure = recommendedQty * entry
    val requiredMargin = recommendedQty * marginPerShare
    val actualCapitalAtRisk = recommendedQty * riskPerShare
    val actualPotentialProfit = recommendedQty * rewardPerShare
    val riskRewardRatio = if (riskPerShare > 0) (rewardPerShare / riskPerShare) else 0.0

    // Regulatory Charges & Taxes estimate (SEBI/NSE standard)
    val turnover = totalExposure * 2.0
    val brokerage = 40.0 // Flat ₹20 buy + ₹20 sell
    val stt = if (productType == ProductType.MIS) totalExposure * 0.00025 else totalExposure * 0.001 * 2.0
    val exchangeCharges = turnover * 0.0000345
    val sebiFees = turnover * 0.000001
    val gst = (brokerage + exchangeCharges) * 0.18
    val stampDuty = totalExposure * 0.00003
    val totalTaxes = brokerage + stt + exchangeCharges + sebiFees + gst + stampDuty

    val netProfit = actualPotentialProfit - totalTaxes
    val netLoss = actualCapitalAtRisk + totalTaxes

    val capitalPresets = listOf(50000.0, 100000.0, 250000.0, 500000.0, 1000000.0)
    val riskPresets = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "POSITION SIZING & CAPITAL DEFENSE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Size positions by risk, not by capital. Never exceed max drawdown threshold per setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Account Capital & Risk Percentage Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. ACCOUNT RISK PARAMETERS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = capitalInput,
                        onValueChange = { capitalInput = it },
                        label = { Text("Trading Capital (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(capitalPresets) { preset ->
                            val isSelected = capital == preset
                            Surface(
                                onClick = { capitalInput = preset.toInt().toString() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = formatInr(preset),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max Risk per Trade (%):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", riskPct)}% (${formatInr(maxAccountRisk)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (riskPct <= 2.0) BullishGreen else AccentGold
                        )
                    }

                    Slider(
                        value = riskPct.toFloat(),
                        onValueChange = { riskPct = ((it * 100).roundToInt() / 100.0) },
                        valueRange = 0.25f..5.0f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(riskPresets) { r ->
                            FilterChip(
                                selected = riskPct == r,
                                onClick = { riskPct = r },
                                label = { Text("$r%", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Trade Setup: Long/Short, Entry, Stop-Loss, Take-Profit
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. TRADE SETUP & TARGETS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Long / Short Toggle
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                .padding(2.dp)
                        ) {
                            Surface(
                                onClick = { side = OrderSide.BUY },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isLong) BullishGreen else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isLong) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LONG",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLong) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                onClick = { side = OrderSide.SELL },
                                shape = RoundedCornerShape(16.dp),
                                color = if (!isLong) BearishRed else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (!isLong) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SHORT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isLong) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Entry Price
                    OutlinedTextField(
                        value = entryInput,
                        onValueChange = { entryInput = it },
                        label = { Text("Planned Entry Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stop-Loss & Take-Profit row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = slInput,
                                onValueChange = { slInput = it },
                                label = { Text("Stop-Loss (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Risk: ${formatInr(riskPerShare)} (${String.format(Locale.US, "%.2f", slPercent)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = BearishRed
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = tpInput,
                                onValueChange = { tpInput = it },
                                label = { Text("Take-Profit (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gain: ${formatInr(rewardPerShare)} (${String.format(Locale.US, "%.2f", tpPercent)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = BullishGreen
                            )
                        }
                    }

                    // Validation warning if setup is inverted
                    if (!isValidSetup) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BearishRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLong) "For Long positions, Stop-Loss must be < Entry and Target > Entry" else "For Short positions, Stop-Loss must be > Entry and Target < Entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = BearishRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Instrument Lot Sizing & Product Type
                    Text("Instrument Lot Size:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LotPreset.values()) { lot ->
                            FilterChip(
                                selected = selectedLotPreset == lot,
                                onClick = { selectedLotPreset = lot },
                                label = { Text(lot.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Product Leverage:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProductType.values().forEach { prod ->
                                FilterChip(
                                    selected = productType == prod,
                                    onClick = { productType = prod },
                                    label = { Text(prod.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Primary Results: Position Sizing & Risk/Reward Gauge
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECOMMENDED POSITION SIZE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Favorable/Unfavorable Status Badge
                        val isFavorable = riskRewardRatio >= 1.8
                        val badgeColor = when {
                            riskRewardRatio >= 2.0 -> BullishGreen
                            riskRewardRatio >= 1.5 -> AccentGold
                            else -> BearishRed
                        }
                        val badgeText = when {
                            riskRewardRatio >= 2.0 -> "INSTITUTIONAL GRADE (≥ 1:2)"
                            riskRewardRatio >= 1.5 -> "ACCEPTABLE (≥ 1:1.5)"
                            else -> "UNFAVORABLE (< 1:1.5)"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Safe Quantity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (selectedLotPreset.size > 1) "$recommendedQty Shares (${finalLots} Lots)" else "$recommendedQty Shares",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Risk : Reward Ratio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "1 : ${String.format(Locale.US, "%.2f", riskRewardRatio)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (riskRewardRatio >= 1.5) BullishGreen else BearishRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Segmented Risk vs Reward Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Risk Zone (${String.format(Locale.US, "%.1f", slPercent)}%)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = BearishRed)
                            Text("Reward Zone (${String.format(Locale.US, "%.1f", tpPercent)}%)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = BullishGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalDistance = (riskPerShare + rewardPerShare).coerceAtLeast(1.0)
                        val riskWeight = (riskPerShare / totalDistance).toFloat().coerceIn(0.1f, 0.9f)
                        val rewardWeight = 1.0f - riskWeight

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(riskWeight)
                                    .fillMaxHeight()
                                    .background(BearishRed)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .weight(rewardWeight)
                                    .fillMaxHeight()
                                    .background(BullishGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Financial Outcomes Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Max Capital at Risk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "- ${formatInr(actualCapitalAtRisk)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = BearishRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Potential Target Reward", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "+ ${formatInr(actualPotentialProfit)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = BullishGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Required Margin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatInr(requiredMargin),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Position Exposure", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatInr(totalExposure),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 5. Taxes, Brokerage & SEBI Charges Breakdown (Expandable)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTaxBreakdown = !showTaxBreakdown },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estimated Charges & Taxes: ${formatInr(totalTaxes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (showTaxBreakdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showTaxBreakdown) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            TaxDetailRow("Brokerage (Roundtrip)", formatInr(brokerage))
                            TaxDetailRow("Securities Transaction Tax (STT)", formatInr(stt))
                            TaxDetailRow("Exchange & SEBI Turnover Charges", formatInr(exchangeCharges + sebiFees))
                            TaxDetailRow("GST (18%) & Stamp Duty", formatInr(gst + stampDuty))
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            TaxDetailRow("Net Expected Profit after Charges", formatInr(netProfit), isProfit = true)
                            TaxDetailRow("Net Max Loss after Charges", formatInr(netLoss), isLoss = true)
                        }
                    }
                }
            }
        }

        // 6. Action Button
        if (onOrderApply != null && recommendedQty > 0 && isValidSetup) {
            item {
                Button(
                    onClick = {
                        onOrderApply(recommendedQty, side, entry, stopLoss, takeProfit)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply ${recommendedQty} Qty to Order Sheet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Adapter Composable for compatibility with RiskCalculation state.
 */
@Composable
fun RiskCalculatorView(
    currentRisk: RiskCalculation,
    onCalculate: (Double, Double, Double, Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    onOrderApply: ((quantity: Int, side: OrderSide, entry: Double, sl: Double, target: Double) -> Unit)? = null
) {
    RiskCalculatorView(
        modifier = modifier,
        initialCapital = currentRisk.capital,
        initialRiskPercent = currentRisk.riskPercent,
        initialEntry = currentRisk.entryPrice,
        initialStopLoss = currentRisk.stopLossPrice,
        initialTakeProfit = currentRisk.targetPrice,
        onOrderApply = onOrderApply
    )
}

@Composable
private fun TaxDetailRow(
    label: String,
    value: String,
    isProfit: Boolean = false,
    isLoss: Boolean = false
) {
    val valueColor = when {
        isProfit -> BullishGreen
        isLoss -> BearishRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isProfit || isLoss) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}
