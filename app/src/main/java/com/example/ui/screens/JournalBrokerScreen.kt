package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrokerConnection
import com.example.model.OrderSide
import com.example.model.TradeJournal
import com.example.ui.components.formatInr
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalBrokerScreen(
    journalEntries: List<TradeJournal>,
    onAddJournal: (
        symbol: String,
        side: OrderSide,
        qty: Int,
        entry: Double,
        exit: Double,
        strategy: String,
        emotion: String,
        notes: String,
        lesson: String
    ) -> Unit,
    onDeleteJournal: (Long) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Journal, 1: Broker Integration
    var showAddDialog by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trade Journal (${journalEntries.size})", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broker APIs", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        if (activeTab == 0) {
            JournalTab(
                entries = journalEntries,
                onAddClick = { showAddDialog = true },
                onDeleteEntry = onDeleteJournal
            )
        } else {
            BrokerApiTab()
        }
    }

    if (showAddDialog) {
        AddJournalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { sym, side, q, en, ex, strat, emo, notes, les ->
                onAddJournal(sym, side, q, en, ex, strat, emo, notes, les)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun JournalTab(
    entries: List<TradeJournal>,
    onAddClick: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val totalTrades = entries.size
    val winningTrades = entries.count { it.pnl > 0 }
    val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades * 100.0) else 0.0
    val netPnl = entries.sumOf { it.pnl }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Analytics Summary
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PSYCHOLOGY & PERFORMANCE METRICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalTrades Trades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Win Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.1f", winRate)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (winRate >= 50) BullishGreen else BearishRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Realized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${if (netPnl >= 0) "+" else ""}${formatInr(netPnl)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (netPnl >= 0) BullishGreen else BearishRed)
                            }
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No journal entries logged yet.\nTap the '+' button to record trade psychology, strategies, and lessons learned.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(entries) { entry ->
                val isWin = entry.pnl >= 0
                val pnlColor = if (isWin) BullishGreen else BearishRed
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(entry.timestamp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (entry.side == OrderSide.BUY) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = entry.side.name,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.side == OrderSide.BUY) BullishGreen else BearishRed
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entry.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${if (isWin) "+" else ""}${formatInr(entry.pnl)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = pnlColor
                                )
                                IconButton(onClick = { onDeleteEntry(entry.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Qty: ${entry.quantity} • Entry: ${formatInr(entry.entryPrice)} • Exit: ${formatInr(entry.exitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(
                                    text = "Tag: ${entry.strategyTag}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(
                                    text = "Emotion: ${entry.emotionTag}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold
                                )
                            }
                        }

                        if (entry.notes.isNotBlank() || entry.lessonLearned.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (entry.notes.isNotBlank()) {
                                Text("Notes: ${entry.notes}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (entry.lessonLearned.isNotBlank()) {
                                Text("💡 Lesson: ${entry.lessonLearned}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Journal Entry")
        }
    }
}

@Composable
fun BrokerApiTab() {
    var isSandboxEnabled by remember { mutableStateOf(true) }
    var kiteConnected by remember { mutableStateOf(true) }
    var upstoxConnected by remember { mutableStateOf(false) }
    var angelConnected by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // SEBI Risk Disclosure
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("SEBI Compliance & OAuth Token Security", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Broker tokens are encrypted in hardware-backed Android Keystore. Intraday equity and F&O trading carries high financial risk. 9 out of 10 retail traders incur net losses in derivatives.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            // Sandbox Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("API Sandbox / Simulation Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Execute against simulated order-matching engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isSandboxEnabled,
                    onCheckedChange = { isSandboxEnabled = it }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Brokers List
        item {
            BrokerCard(
                name = "Zerodha Kite Connect",
                description = "NSE / BSE Equity & F&O execution with WebSocket tick stream",
                isConnected = kiteConnected,
                clientId = if (kiteConnected) "ZT8491" else "Not Linked",
                onToggle = { kiteConnected = !kiteConnected }
            )
        }

        item {
            BrokerCard(
                name = "Upstox API v2",
                description = "High-speed OAuth2 order routing & option chain streaming",
                isConnected = upstoxConnected,
                clientId = if (upstoxConnected) "UP3910" else "Not Linked",
                onToggle = { upstoxConnected = !upstoxConnected }
            )
        }

        item {
            BrokerCard(
                name = "Angel One SmartAPI",
                description = "Algo trading & historical candlestick market data feed",
                isConnected = angelConnected,
                clientId = if (angelConnected) "AG1024" else "Not Linked",
                onToggle = { angelConnected = !angelConnected }
            )
        }
    }
}

@Composable
fun BrokerCard(
    name: String,
    description: String,
    isConnected: Boolean,
    clientId: String,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) BullishGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isConnected) BullishGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) BullishGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Client ID: $clientId", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddJournalDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        symbol: String,
        side: OrderSide,
        qty: Int,
        entry: Double,
        exit: Double,
        strategy: String,
        emotion: String,
        notes: String,
        lesson: String
    ) -> Unit
) {
    var symbol by remember { mutableStateOf("RELIANCE") }
    var side by remember { mutableStateOf(OrderSide.BUY) }
    var qty by remember { mutableStateOf("25") }
    var entryPrice by remember { mutableStateOf("2950.0") }
    var exitPrice by remember { mutableStateOf("2985.0") }
    var strategy by remember { mutableStateOf("Breakout") }
    var emotion by remember { mutableStateOf("Disciplined") }
    var notes by remember { mutableStateOf("Waited for volume breakout confirmation above 15m VWAP.") }
    var lesson by remember { mutableStateOf("Trailing stop-loss locked in 80% of peak gains.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Trade to Journal") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("Symbol") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryPrice,
                        onValueChange = { entryPrice = it },
                        label = { Text("Entry Price (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = exitPrice,
                        onValueChange = { exitPrice = it },
                        label = { Text("Exit Price (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = strategy,
                        onValueChange = { strategy = it },
                        label = { Text("Strategy Tag") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = emotion,
                        onValueChange = { emotion = it },
                        label = { Text("Emotion") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trade Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lesson,
                    onValueChange = { lesson = it },
                    label = { Text("Lesson Learned") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        symbol,
                        side,
                        qty.toIntOrNull() ?: 1,
                        entryPrice.toDoubleOrNull() ?: 0.0,
                        exitPrice.toDoubleOrNull() ?: 0.0,
                        strategy,
                        emotion,
                        notes,
                        lesson
                    )
                }
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
