package com.example.ruwia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun ProductDetailScreen(
    productName: String,
    transactions: List<SaleEntry> = emptyList(),
    onBack: () -> Unit,
) {

    var isDescending by remember { mutableStateOf(true) }
    val sortedTransactions = remember(transactions, isDescending) {
        if (isDescending) transactions.sortedByDescending { it.date }
        else transactions.sortedBy { it.date }
    }

    val totalQty      = transactions.sumOf { it.qty }
    val totalPurchase = transactions.sumOf { it.purchasePricePerUnit * it.qty }
    val totalSelling  = transactions.sumOf { it.totalSelling }
    val totalMargin   = transactions.sumOf { it.totalMargin }

    Scaffold(
        containerColor = NTColors.Background,
        topBar = { ProductDetailTopBar(productName = productName, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Summary strip ──────────────────────────────────
            item {
                ProductSummaryStrip(
                    totalQty      = totalQty,
                    totalPurchase = totalPurchase,
                    totalSelling  = totalSelling,
                    totalMargin   = totalMargin,
                    modifier      = Modifier.padding(NTDp.screenPad),
                )
            }

            // ── Column header ──────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    TransactionTableHeader(isDescending = isDescending, onToggle = { isDescending = !isDescending })
                    HorizontalDivider(color = NTColors.Divider)
                }
            }

            // ── Transactions ───────────────────────────────────
            if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Inbox, null, tint = NTColors.TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No transactions yet for $productName", color = NTColors.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(sortedTransactions) { tx ->
                    Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                        TransactionRow(tx = tx)
                        HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                    }
                }
            }

            // ── Total footer ───────────────────────────────────
            if (transactions.isNotEmpty()) {
                item {
                    TransactionTotalRow(
                        totalQty      = totalQty,
                        totalPurchase = totalPurchase,
                        totalSelling  = totalSelling,
                        totalMargin   = totalMargin,
                        modifier      = Modifier.padding(horizontal = NTDp.screenPad).padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun ProductDetailTopBar(productName: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(NTColors.Surface, RoundedCornerShape(10.dp))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack).align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }

            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(productName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                val monthLabel = remember(now) {
                    "${com.example.ruwia.util.monthName(now.monthNumber).take(3)} ${now.year}"
                }
                Text(monthLabel, fontSize = 11.sp, color = NTColors.TextTertiary)
            }

            Box(
                modifier = Modifier.size(36.dp).background(NTColors.PrimaryLight, RoundedCornerShape(10.dp))
                    .align(Alignment.CenterEnd).clickable {},
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Share, null, tint = NTColors.Primary, modifier = Modifier.size(16.dp)) }
        }
    }
}

// ── Summary strip ─────────────────────────────────────────────────────────────

@Composable
private fun ProductSummaryStrip(
    totalQty: Int, totalPurchase: Double, totalSelling: Double, totalMargin: Double,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryChip("Qty",       "$totalQty units",         NTColors.PrimaryLight, NTColors.Primary)
        SummaryChip("Purchase",  "₹${totalPurchase.toInt()}", NTColors.WarningLight, NTColors.Warning)
        SummaryChip("Selling",   "₹${totalSelling.toInt()}", NTColors.InfoLight,    NTColors.Info)
        SummaryChip("Margin",    "₹${totalMargin.toInt()}",  NTColors.SuccessLight, NTColors.Success)
    }
}

@Composable
private fun SummaryChip(label: String, value: String, bg: Color, fg: Color) {
    Column(
        modifier = Modifier.background(bg, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 9.sp, color = fg.copy(alpha = 0.65f), fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

// ── Table ─────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionTableHeader(isDescending: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1.1f)
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.4.sp, textAlign = TextAlign.End)
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = if (isDescending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = NTColors.TextTertiary,
                modifier = Modifier.size(10.dp)
            )
        }
        
        listOf(
            "Customer"     to 2.0f,
            "Qty"          to 0.6f,
            "Buy/u"        to 0.8f,
            "Sell/u"       to 0.8f,
            "Margin"       to 0.9f,
        ).forEach { (h, w) ->
            Text(h, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.4.sp, textAlign = if (w < 1.5f) TextAlign.End else TextAlign.Start,
                modifier = Modifier.weight(w))
        }
    }
}

@Composable
private fun TransactionRow(tx: SaleEntry) {
    val isSpecialMargin = tx.purchasePricePerUnit == 0.0  // Soda company type
    val marginColor = when {
        tx.totalMargin > 100 -> NTColors.Success
        tx.totalMargin > 0   -> NTColors.Primary
        else                 -> NTColors.Warning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatDateString(tx.date), fontSize = 11.sp, color = NTColors.TextTertiary, modifier = Modifier.weight(1.1f))
        Text(tx.customerName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2.0f))
        Text("${tx.qty}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary,
            textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))

        if (isSpecialMargin) {
            Text("—", fontSize = 12.sp, color = NTColors.TextDisabled, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
            Text("—", fontSize = 12.sp, color = NTColors.TextDisabled, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
        } else {
            Text("₹${tx.purchasePricePerUnit.toInt()}", fontSize = 11.sp, color = NTColors.TextSecondary,
                textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
            Text("₹${tx.sellingPricePerUnit.toInt()}", fontSize = 11.sp, color = NTColors.TextSecondary,
                textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
        }

        Text("₹${tx.totalMargin.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = marginColor,
            textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
    }
}

@Composable
private fun TransactionTotalRow(
    totalQty: Int, totalPurchase: Double, totalSelling: Double, totalMargin: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.PrimaryDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            letterSpacing = 0.8.sp, modifier = Modifier.weight(1.1f))
        Text("", fontSize = 12.sp, modifier = Modifier.weight(2.0f))
        Text("$totalQty", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
            textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
        Text("₹${totalPurchase.toInt()}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
        Text("₹${totalSelling.toInt()}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
        Text("₹${totalMargin.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7),
            textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
    }
}

private fun formatDateString(dateStr: String): String =
    com.example.ruwia.util.dbToDisplayDate(dateStr)
