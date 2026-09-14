package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    // Watchlist
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    // Orders
    @Query("SELECT * FROM paper_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<PaperOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PaperOrderEntity)

    @Query("DELETE FROM paper_orders")
    suspend fun clearOrders()

    // Positions
    @Query("SELECT * FROM paper_positions")
    fun getAllPositions(): Flow<List<PaperPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PaperPositionEntity)

    @Query("DELETE FROM paper_positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)

    @Query("DELETE FROM paper_positions")
    suspend fun clearPositions()

    // Journal
    @Query("SELECT * FROM trade_journal ORDER BY timestamp DESC")
    fun getJournalEntries(): Flow<List<TradeJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(entry: TradeJournalEntity)

    @Query("DELETE FROM trade_journal WHERE id = :id")
    suspend fun deleteJournal(id: Long)
}
