package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "paper_orders")
data class PaperOrderEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val side: String,
    val orderType: String,
    val product: String,
    val quantity: Int,
    val price: Double,
    val triggerPrice: Double,
    val status: String,
    val timestamp: Long,
    val executionPrice: Double,
    val brokerage: Double
)

@Entity(tableName = "paper_positions")
data class PaperPositionEntity(
    @PrimaryKey val symbol: String,
    val product: String,
    val quantity: Int,
    val buyAvgPrice: Double,
    val currentPrice: Double,
    val realizedPnl: Double
)

@Entity(tableName = "trade_journal")
data class TradeJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnl: Double,
    val strategyTag: String,
    val emotionTag: String,
    val notes: String,
    val lessonLearned: String,
    val timestamp: Long
)
