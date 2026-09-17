package com.baxailab.cadebot.ui.callstaff

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baxailab.cadebot.ui.components.MocLamPrimaryButton
import com.baxailab.cadebot.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CallStaffScreen(onBack: () -> Unit) {
    var called by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(called) {
        if (called) {
            repeat(3) {
                delay(1000)
                countdown--
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MocLamFoam),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MocLamEspresso)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = MocLamOnDark)
            }
            Text("Gọi nhân viên", style = MaterialTheme.typography.headlineSmall,
                color = MocLamOnDark, modifier = Modifier.align(Alignment.Center))
        }

        Spacer(Modifier.weight(1f))

        AnimatedContent(targetState = called, label = "callStaff") { isCalled ->
            if (!isCalled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MocLamCaramel.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MocLamCaramel,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Cần hỗ trợ?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MocLamEspresso
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nhấn nút bên dưới để gọi nhân viên\nMộc Lam đến hỗ trợ bạn",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MocLamGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))
                    MocLamPrimaryButton(
                        text = "🔔  Gọi nhân viên ngay",
                        onClick = { called = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MocLamSuccess.copy(alpha = 0.1f)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✅", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Đã gửi thông báo!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MocLamSuccess,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nhân viên Mộc Lam sẽ đến hỗ trợ bạn ngay. Vui lòng đợi trong giây lát.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MocLamGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    MocLamPrimaryButton(
                        text = "Về trang chủ",
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}
