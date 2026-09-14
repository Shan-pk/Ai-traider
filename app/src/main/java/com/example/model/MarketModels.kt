package com.example.model

data class MarketIndex(
    val symbol: String,
    val name: String,
    val value: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double
)

data class DepthItem(
    val orders: Int,
    val quantity: Int,
    val price: Double
)

data class Stock(
    val symbol: String,
    val name: String,
    val sector: String,
    val exchange: String = "NSE",
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val dayHigh: Double,
    val dayLow: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val vwap: Double,
    val peRatio: Double,
    val roe: Double,
    val marketCap: String, // e.g. "₹20.1 Lakh Cr"
    val rsi: Double,
    val ema20: Double,
    val ema50: Double,
    val bids: List<DepthItem> = emptyList(),
    val asks: List<DepthItem> = emptyList()
)

data class Candle(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
)

enum class TimeFrame(val label: String) {
    M1("1m"),
    M5("5m"),
    M15("15m"),
    H1("1h"),
    D1("1D"),
    W1("1W")
}

enum class ChartType(val label: String) {
    CANDLESTICK("Candles"),
    LINE("Line"),
    AREA("Area")
}

enum class IndicatorType(val label: String) {
    SMA20("SMA 20"),
    EMA50("EMA 50"),
    BOLLINGER("Bollinger Bands"),
    VWAP("VWAP"),
    RSI("RSI (14)"),
    MACD("MACD")
}

enum class DrawingToolType(val label: String) {
    NONE("Cursor"),
    TRENDLINE("Trendline"),
    HORIZONTAL("Support/Res"),
    FIBONACCI("Fib Retracement")
}

enum class OrderSide {
    BUY, SELL
}

enum class OrderType(val label: String) {
    MARKET("Market"),
    LIMIT("Limit"),
    SL_LIMIT("SL-Limit")
}

enum class ProductType(val label: String, val leverage: Int) {
    MIS("Intraday MIS (5x)", 5),
    CNC("Delivery CNC (1x)", 1)
}

enum class OrderStatus {
    EXECUTED,
    PENDING,
    CANCELLED,
    REJECTED
}

data class PaperOrder(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val orderType: OrderType,
    val product: ProductType,
    val quantity: Int,
    val price: Double,
    val triggerPrice: Double = 0.0,
    val status: OrderStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val executionPrice: Double = price,
    val brokerage: Double = 20.0
)

data class PaperPosition(
    val symbol: String,
    val product: ProductType,
    val quantity: Int,
    val buyAvgPrice: Double,
    val currentPrice: Double,
    val realizedPnl: Double = 0.0
) {
    val unrealizedPnl: Double get() = (currentPrice - buyAvgPrice) * quantity
    val pnlPercent: Double get() = if (buyAvgPrice > 0) ((currentPrice - buyAvgPrice) / buyAvgPrice) * 100.0 else 0.0
}

data class PaperHolding(
    val symbol: String,
    val quantity: Int,
    val avgPrice: Double,
    val currentPrice: Double
) {
    val investedValue: Double get() = quantity * avgPrice
    val currentValue: Double get() = quantity * currentPrice
    val totalPnl: Double get() = currentValue - investedValue
    val totalPnlPercent: Double get() = if (investedValue > 0) (totalPnl / investedValue) * 100.0 else 0.0
}

data class TradeJournal(
    val id: Long = 0,
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnl: Double,
    val strategyTag: String,
    val emotionTag: String,
    val notes: String,
    val lessonLearned: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RiskCalculation(
    val capital: Double,
    val riskPercent: Double,
    val entryPrice: Double,
    val stopLossPrice: Double,
    val targetPrice: Double,
    val recommendedQuantity: Int,
    val positionCapitalRequired: Double,
    val maxRiskAmount: Double,
    val targetProfitAmount: Double,
    val riskRewardRatio: Double,
    val estimatedCharges: Double
)

data class MarketNews(
    val id: String,
    val title: String,
    val source: String,
    val timeAgo: String,
    val sentiment: String, // "Bullish", "Bearish", "Neutral"
    val relatedSymbols: List<String>
)

data class BrokerConnection(
    val brokerName: String,
    val iconUrl: String = "",
    val isConnected: Boolean,
    val isSandbox: Boolean,
    val clientId: String,
    val lastSyncTime: String
)
