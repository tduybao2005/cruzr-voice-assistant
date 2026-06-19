package com.baxailab.cadebot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baxailab.cadebot.ui.theme.*

@Composable
fun Mộc LamPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Mộc LamEspresso,
            contentColor = Mộc LamOnDark
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Mộc LamSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Mộc LamEspresso),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Mộc LamEspresso)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Mộc LamTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Mộc LamEspresso)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quay lại",
                    tint = Mộc LamOnDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Mộc LamOnDark,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), content = actions)
    }
}

@Composable
fun Mộc LamTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Mộc LamLatte)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = Mộc LamCoffee)
    }
}

@Composable
fun PriceText(amount: Int, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    Text(
        text = "${String.format("%,d", amount)}đ",
        style = style,
        color = Mộc LamEspresso,
        modifier = modifier
    )
}

@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onDecrease,
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Mộc LamEspresso, CircleShape)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = Mộc LamEspresso, modifier = Modifier.size(18.dp))
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = Mộc LamEspresso,
            modifier = Modifier.widthIn(min = 28.dp),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onIncrease,
            modifier = Modifier
                .size(40.dp)
                .background(Mộc LamEspresso, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tăng", tint = Mộc LamOnDark, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Mộc LamEspresso else Mộc LamLightGray)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Mộc LamLatte,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Mộc LamOnDark else Mộc LamCoffee
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Mộc LamEspresso,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun Mộc LamGradientHeader(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Mộc LamEspresso, Mộc LamCoffee)
                )
            )
            .padding(24.dp),
        content = content
    )
}
