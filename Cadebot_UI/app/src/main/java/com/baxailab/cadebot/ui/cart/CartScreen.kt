package com.baxailab.cadebot.ui.cart

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baxailab.cadebot.data.model.CartItem
import com.baxailab.cadebot.ui.components.PriceText
import com.baxailab.cadebot.ui.components.Mộc LamPrimaryButton
import com.baxailab.cadebot.ui.detail.iceLabel
import com.baxailab.cadebot.ui.detail.tempLabel
import com.baxailab.cadebot.ui.detail.toppingLabel
import com.baxailab.cadebot.ui.theme.*

@Composable
fun CartScreen(
    uiState: CartUiState,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onSelectTable: (String) -> Unit,
    onContinueShopping: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Mộc LamFoam)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Mộc LamEspresso, Mộc LamCoffee)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Mộc LamOnDark)
            }
            Text(
                text = "Giỏ hàng",
                style = MaterialTheme.typography.headlineSmall,
                color = Mộc LamOnDark,
                modifier = Modifier.align(Alignment.Center)
            )
            if (!uiState.isEmpty) {
                Text(
                    text = "${uiState.items.size} món",
                    style = MaterialTheme.typography.labelMedium,
                    color = Mộc LamCaramel,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        if (uiState.isEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Giỏ hàng đang trống", style = MaterialTheme.typography.titleLarge, color = Mộc LamEspresso)
                    Spacer(Modifier.height(8.dp))
                    Text("Hãy chọn món từ thực đơn nhé!", style = MaterialTheme.typography.bodyMedium, color = Mộc LamGray)
                    Spacer(Modifier.height(24.dp))
                    Mộc LamPrimaryButton("Xem thực đơn", onClick = onContinueShopping, modifier = Modifier.width(200.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Table selector
                item {
                    TableSelector(
                        tables = uiState.tables,
                        selectedTableId = uiState.selectedTableId,
                        onSelect = onSelectTable
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Cart items
                items(uiState.items) { cartItem ->
                    CartItemRow(item = cartItem, onRemove = { onRemoveItem(cartItem.id) })
                }

                // Divider
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Mộc LamLatte)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng cộng", style = MaterialTheme.typography.titleLarge, color = Mộc LamEspresso)
                        PriceText(amount = uiState.totalAmount, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            // Checkout bar
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (uiState.isPaymentLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Mộc LamEspresso)
                        }
                    } else {
                        Mộc LamPrimaryButton(
                            text = "Thanh toán QR  ${String.format("%,d", uiState.totalAmount)}đ",
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.QrCode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableSelector(
    tables: List<com.baxailab.cadebot.data.model.TableInfo>,
    selectedTableId: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Mộc LamSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📍 Bàn của bạn", style = MaterialTheme.typography.titleMedium, color = Mộc LamEspresso)
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                tables.forEach { table ->
                    val selected = table.tableId == selectedTableId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Mộc LamEspresso else Mộc LamLightGray)
                            .clickable { onSelect(table.tableId) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = table.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Mộc LamOnDark else Mộc LamCoffee
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Mộc LamSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.menuItem.name, style = MaterialTheme.typography.titleMedium, color = Mộc LamEspresso)
                Spacer(Modifier.height(4.dp))
                val opts = buildList {
                    if (item.selectedSize.isNotEmpty()) add(item.selectedSize)
                    if (item.selectedTemperature.isNotEmpty()) add(tempLabel(item.selectedTemperature))
                    if (item.selectedSweetness.isNotEmpty()) add(item.selectedSweetness)
                    if (item.selectedIce.isNotEmpty()) add(iceLabel(item.selectedIce))
                    addAll(item.selectedToppings.map { toppingLabel(it) })
                }.joinToString(" · ")
                if (opts.isNotEmpty()) {
                    Text(opts, style = MaterialTheme.typography.bodyMedium, color = Mộc LamGray,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "x${item.quantity}  ×  ${String.format("%,d", item.unitPrice)}đ",
                    style = MaterialTheme.typography.bodyMedium, color = Mộc LamCoffee
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = Mộc LamError, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(4.dp))
                PriceText(amount = item.totalPrice, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

