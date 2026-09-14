package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Candle
import com.example.model.ChartType
import com.example.model.DrawingToolType
import com.example.model.IndicatorType
import com.example.model.OrderSide
import com.example.model.Stock
import com.example.model.TimeFrame
import com.example.ui.components.formatInr
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ChartWorkspaceScreen(
    stock: Stock?,
    candles: List<Candle>,
    selectedTimeframe: TimeFrame,
    chartType: ChartType,
    selectedIndicators: Set<IndicatorType>,
    activeDrawingTool: DrawingToolType,
    onTimeframeSelect: (TimeFrame) -> Unit,
    onChartTypeSelect: (ChartType) -> Unit,
    onIndicatorToggle: (IndicatorType) -> Unit,
    onDrawingToolSelect: (DrawingToolType) -> Unit,
    onOpenOrderSheet: (Stock, OrderSide) -> Unit
) {
    if (stock == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an instrument from Dashboard or Screener to view charts.")
        }
        return
    }

    var crosshairX by remember { mutableFloatStateOf(-1f) }
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Stock Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stock.exchange,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${formatInr(stock.price)} (${if (stock.change >= 0) "+" else ""}${String.format(Locale.US, "%.2f", stock.changePercent)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (stock.change >= 0) BullishGreen else BearishRed
                    )
                }

                // Chart type switcher
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onChartTypeSelect(ChartType.CANDLESTICK) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CandlestickChart,
                            contentDescription = "Candles",
                            tint = if (chartType == ChartType.CANDLESTICK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onChartTypeSelect(ChartType.LINE) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Line",
                            tint = if (chartType == ChartType.LINE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Timeframe selector bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TimeFrame.values()) { tf ->
                FilterChip(
                    selected = selectedTimeframe == tf,
                    onClick = { onTimeframeSelect(tf) },
                    label = { Text(tf.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Technical Indicators & Drawing Tools Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(IndicatorType.values()) { ind ->
                val active = selectedIndicators.contains(ind)
                Surface(
                    onClick = { onIndicatorToggle(ind) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = ind.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.width(4.dp))
            }
            items(DrawingToolType.values()) { tool ->
                val active = activeDrawingTool == tool
                Surface(
                    onClick = { onDrawingToolSelect(tool) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "✏ ${tool.label}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // HUD overlay for selected candle
        val activeCandle = selectedCandleIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()
        if (activeCandle != null) {
            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(activeCandle.timestamp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$dateStr | O: ${String.format(Locale.US, "%.2f", activeCandle.open)}  H: ${String.format(Locale.US, "%.2f", activeCandle.high)}  L: ${String.format(Locale.US, "%.2f", activeCandle.low)}  C: ${String.format(Locale.US, "%.2f", activeCandle.close)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (activeCandle.close >= activeCandle.open) BullishGreen else BearishRed
                )
            }
        }

        // Chart Interactive Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            TradingCanvas(
                candles = candles,
                chartType = chartType,
                selectedIndicators = selectedIndicators,
                activeDrawingTool = activeDrawingTool,
                crosshairX = crosshairX,
                onCrosshairScrub = { x, index ->
                    crosshairX = x
                    selectedCandleIndex = index
                }
            )
        }

        // Bottom Quick Order Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onOpenOrderSheet(stock, OrderSide.BUY) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullishGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BUY / LONG", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onOpenOrderSheet(stock, OrderSide.SELL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BearishRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SELL / SHORT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TradingCanvas(
    candles: List<Candle>,
    chartType: ChartType,
    selectedIndicators: Set<IndicatorType>,
    activeDrawingTool: DrawingToolType,
    crosshairX: Float,
    onCrosshairScrub: (Float, Int?) -> Unit
) {
    if (candles.isEmpty()) return

    val minPrice = remember(candles) { candles.minOf { it.low } * 0.998f }
    val maxPrice = remember(candles) { candles.maxOf { it.high } * 1.002f }
    val priceRange = max(1f, maxPrice - minPrice)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(candles) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val idx = calculateIndexAtX(offset.x, size.width.toFloat(), candles.size)
                        onCrosshairScrub(offset.x, idx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val idx = calculateIndexAtX(change.position.x, size.width.toFloat(), candles.size)
                        onCrosshairScrub(change.position.x, idx)
                    },
                    onDragEnd = {
                        onCrosshairScrub(-1f, null)
                    },
                    onDragCancel = {
                        onCrosshairScrub(-1f, null)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val isRsiEnabled = selectedIndicators.contains(IndicatorType.RSI)
        val mainChartHeight = if (isRsiEnabled) height * 0.75f else height
        val rsiHeight = if (isRsiEnabled) height * 0.25f else 0f

        // Draw horizontal grid lines & price labels
        val gridSteps = 5
        for (i in 0..gridSteps) {
            val y = (mainChartHeight / gridSteps) * i
            drawLine(
                color = Color(0x1A808080),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        val count = candles.size
        val candleWidth = width / count.toFloat()

        // 1. Draw Candlesticks or Line/Area
        if (chartType == ChartType.CANDLESTICK) {
            candles.forEachIndexed { i, candle ->
                val x = (i * candleWidth) + (candleWidth / 2f)
                val openY = mainChartHeight - ((candle.open - minPrice) / priceRange) * mainChartHeight
                val closeY = mainChartHeight - ((candle.close - minPrice) / priceRange) * mainChartHeight
                val highY = mainChartHeight - ((candle.high - minPrice) / priceRange) * mainChartHeight
                val lowY = mainChartHeight - ((candle.low - minPrice) / priceRange) * mainChartHeight

                val isGreen = candle.close >= candle.open
                val candleColor = if (isGreen) BullishGreen else BearishRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyHeight = max(2f, bodyBottom - bodyTop)
                val bodyWidth = max(2f, candleWidth * 0.7f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x - (bodyWidth / 2f), bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    style = Fill
                )
            }
        } else {
            // Line / Area chart
            val path = Path()
            val fillPath = Path()
            candles.forEachIndexed { i, candle ->
                val x = (i * candleWidth) + (candleWidth / 2f)
                val y = mainChartHeight - ((candle.close - minPrice) / priceRange) * mainChartHeight
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, mainChartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(width, mainChartHeight)
            fillPath.close()

            if (chartType == ChartType.AREA) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BullishGreen.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = mainChartHeight
                    )
                )
            }
            drawPath(
                path = path,
                color = BullishGreen,
                style = Stroke(width = 2.5f)
            )
        }

        // 2. Technical Indicators Overlay
        if (selectedIndicators.contains(IndicatorType.SMA20)) {
            drawMovingAverage(candles, 20, minPrice, priceRange, mainChartHeight, candleWidth, AccentGold)
        }
        if (selectedIndicators.contains(IndicatorType.EMA50)) {
            drawMovingAverage(candles, 50, minPrice, priceRange, mainChartHeight, candleWidth, AccentCyan)
        }
        if (selectedIndicators.contains(IndicatorType.VWAP)) {
            drawVwap(candles, minPrice, priceRange, mainChartHeight, candleWidth, AccentPurple)
        }
        if (selectedIndicators.contains(IndicatorType.BOLLINGER)) {
            drawBollingerBands(candles, 20, 2f, minPrice, priceRange, mainChartHeight, candleWidth)
        }

        // 3. Drawing Tools Overlay
        when (activeDrawingTool) {
            DrawingToolType.TRENDLINE -> {
                // Draw trendline from bottom left swing low to upper right
                drawLine(
                    color = AccentGold,
                    start = Offset(width * 0.15f, mainChartHeight * 0.75f),
                    end = Offset(width * 0.90f, mainChartHeight * 0.25f),
                    strokeWidth = 2.5f
                )
            }
            DrawingToolType.HORIZONTAL -> {
                // Key resistance & support lines
                val rY = mainChartHeight * 0.30f
                val sY = mainChartHeight * 0.70f
                drawLine(
                    color = BearishRed.copy(alpha = 0.8f),
                    start = Offset(0f, rY),
                    end = Offset(width, rY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
                drawLine(
                    color = BullishGreen.copy(alpha = 0.8f),
                    start = Offset(0f, sY),
                    end = Offset(width, sY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            }
            DrawingToolType.FIBONACCI -> {
                // Fibonacci Retracement horizontal bands
                val fibLevels = listOf(0.0f, 0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1.0f)
                val topY = mainChartHeight * 0.2f
                val botY = mainChartHeight * 0.8f
                fibLevels.forEach { lvl ->
                    val y = topY + (botY - topY) * lvl
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }
            }
            DrawingToolType.NONE -> { /* No active tool */ }
        }

        // 4. RSI Subpanel if enabled
        if (isRsiEnabled) {
            val rsiTop = mainChartHeight
            drawLine(
                color = Color.Gray,
                start = Offset(0f, rsiTop),
                end = Offset(width, rsiTop),
                strokeWidth = 2f
            )
            // 70 & 30 lines
            val y70 = rsiTop + rsiHeight * 0.3f
            val y30 = rsiTop + rsiHeight * 0.7f
            drawLine(
                color = BearishRed.copy(alpha = 0.5f),
                start = Offset(0f, y70),
                end = Offset(width, y70),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            drawLine(
                color = BullishGreen.copy(alpha = 0.5f),
                start = Offset(0f, y30),
                end = Offset(width, y30),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Draw RSI curve
            val rsiPath = Path()
            candles.forEachIndexed { i, candle ->
                val x = (i * candleWidth) + (candleWidth / 2f)
                val rsiVal = 30f + ((candle.close - minPrice) / priceRange) * 40f
                val y = rsiTop + rsiHeight * (1f - (rsiVal / 100f))
                if (i == 0) rsiPath.moveTo(x, y) else rsiPath.lineTo(x, y)
            }
            drawPath(path = rsiPath, color = AccentPurple, style = Stroke(width = 2f))
        }

        // 5. Crosshair Line if active
        if (crosshairX >= 0f) {
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(crosshairX, 0f),
                end = Offset(crosshairX, height),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }
    }
}

private fun calculateIndexAtX(x: Float, totalWidth: Float, count: Int): Int {
    if (count == 0 || totalWidth <= 0f) return 0
    val candleWidth = totalWidth / count.toFloat()
    val index = (x / candleWidth).toInt()
    return index.coerceIn(0, count - 1)
}

private fun DrawScope.drawMovingAverage(
    candles: List<Candle>,
    period: Int,
    minPrice: Float,
    priceRange: Float,
    height: Float,
    candleWidth: Float,
    color: Color
) {
    if (candles.size < period) return
    val path = Path()
    var hasMoved = false

    for (i in (period - 1) until candles.size) {
        var sum = 0f
        for (j in 0 until period) {
            sum += candles[i - j].close
        }
        val ma = sum / period.toFloat()
        val x = (i * candleWidth) + (candleWidth / 2f)
        val y = height - ((ma - minPrice) / priceRange) * height

        if (!hasMoved) {
            path.moveTo(x, y)
            hasMoved = true
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(path = path, color = color, style = Stroke(width = 2f))
}

private fun DrawScope.drawVwap(
    candles: List<Candle>,
    minPrice: Float,
    priceRange: Float,
    height: Float,
    candleWidth: Float,
    color: Color
) {
    var cumVol = 0f
    var cumTpVol = 0f
    val path = Path()

    candles.forEachIndexed { i, candle ->
        val tp = (candle.high + candle.low + candle.close) / 3f
        cumVol += candle.volume
        cumTpVol += (tp * candle.volume)
        val vwap = if (cumVol > 0) cumTpVol / cumVol else candle.close

        val x = (i * candleWidth) + (candleWidth / 2f)
        val y = height - ((vwap - minPrice) / priceRange) * height
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2f))
}

private fun DrawScope.drawBollingerBands(
    candles: List<Candle>,
    period: Int,
    multiplier: Float,
    minPrice: Float,
    priceRange: Float,
    height: Float,
    candleWidth: Float
) {
    if (candles.size < period) return
    val upperPath = Path()
    val lowerPath = Path()
    var started = false

    for (i in (period - 1) until candles.size) {
        var sum = 0f
        for (j in 0 until period) {
            sum += candles[i - j].close
        }
        val ma = sum / period.toFloat()

        var sumSq = 0f
        for (j in 0 until period) {
            val diff = candles[i - j].close - ma
            sumSq += diff * diff
        }
        val stdDev = sqrt(sumSq / period.toFloat())
        val upper = ma + (multiplier * stdDev)
        val lower = ma - (multiplier * stdDev)

        val x = (i * candleWidth) + (candleWidth / 2f)
        val uY = height - ((upper - minPrice) / priceRange) * height
        val lY = height - ((lower - minPrice) / priceRange) * height

        if (!started) {
            upperPath.moveTo(x, uY)
            lowerPath.moveTo(x, lY)
            started = true
        } else {
            upperPath.lineTo(x, uY)
            lowerPath.lineTo(x, lY)
        }
    }

    drawPath(path = upperPath, color = Color(0xFF38BDF8).copy(alpha = 0.5f), style = Stroke(width = 1.5f))
    drawPath(path = lowerPath, color = Color(0xFF38BDF8).copy(alpha = 0.5f), style = Stroke(width = 1.5f))
}
