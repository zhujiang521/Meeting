package com.lenovo.levoice.caption

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 悬浮窗设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(OverlayService.checkOverlayPermission(context))
    }
    var isServiceRunning by remember { mutableStateOf(OverlayService.isServiceRunning(context)) }

    // 刷新权限状态和服务运行状态
    LaunchedEffect(Unit) {
        hasOverlayPermission = OverlayService.checkOverlayPermission(context)
        isServiceRunning = OverlayService.isServiceRunning(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("悬浮窗设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { _ ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 56.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "功能说明",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "启用后，本应用将在后台运行服务，监听广播消息。当接收到 com.zui.action.SHOW_KINETIC 广播时，会在屏幕顶部显示淡入淡出动效。",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "动效特性：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "• 淡入淡出效果", fontSize = 13.sp)
                    Text(text = "• 可自定义持续时间", fontSize = 13.sp)
                    Text(text = "• 可自定义背景颜色", fontSize = 13.sp)
                    Text(text = "• 可自定义悬浮窗高度", fontSize = 13.sp)
                }
            }

            // 权限状态卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "悬浮窗权限",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasOverlayPermission) "✓ 已授权" else "✗ 未授权",
                            fontSize = 14.sp,
                            color = if (hasOverlayPermission)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }

                    if (!hasOverlayPermission) {
                        Text(
                            text = "需要授予悬浮窗权限才能显示动效",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("前往授权")
                        }
                    }
                }
            }

            // 服务控制卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "服务控制",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "服务状态：",
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isServiceRunning) "运行中" else "已停止",
                            fontSize = 14.sp,
                            color = if (isServiceRunning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasOverlayPermission) {
                                    OverlayService.startService(context)
                                    isServiceRunning = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = hasOverlayPermission && !isServiceRunning
                        ) {
                            Text("启动服务")
                        }

                        OutlinedButton(
                            onClick = {
                                OverlayService.stopService(context)
                                isServiceRunning = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isServiceRunning
                        ) {
                            Text("停止服务")
                        }
                    }
                }
            }

            // 测试卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "测试动效",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "点击下方按钮测试不同配置的淡入淡出动效",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Button(
                        onClick = {
                            sendTestBroadcast(
                                context,
                                durationMillis = 1500L,
                                backgroundColor = 0xFFFF69B4.toInt(), // 粉红色
                                heightDp = 50,
                                particleSpeed = 4.5f
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFF69B4)
                        )
                    ) {
                        Text("测试：粉红色 (1500ms, 50dp, 慢速)")
                    }

                    Button(
                        onClick = {
                            sendTestBroadcast(
                                context,
                                durationMillis = 2000L,
                                backgroundColor = 0xFFADD8E6.toInt(), // 淡蓝色
                                heightDp = 80,
                                particleSpeed = 6.5f
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFADD8E6)
                        )
                    ) {
                        Text("测试：淡蓝色 (2000ms, 80dp, 中速)")
                    }

                    Button(
                        onClick = {
                            sendTestBroadcast(
                                context,
                                durationMillis = 3000L,
                                backgroundColor = 0xFF00CED1.toInt(), // 青色
                                heightDp = 100,
                                particleSpeed = 8.5f
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF00CED1)
                        )
                    ) {
                        Text("测试：青色 (3000ms, 100dp, 快速)")
                    }
                }
            }

            // 广播参数说明卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "广播参数说明",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Action: com.zui.action.SHOW_KINETIC",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Extra 参数：", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "• duration (Long): 持续时间（毫秒）",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "• background_color (Int): 背景色（ARGB）",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "• height_dp (Int): 悬浮窗高度（dp）",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "• particle_speed (Float): 粒子速度（4.0-9.0）",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 发送测试广播
 */
private fun sendTestBroadcast(
    context: android.content.Context,
    durationMillis: Long,
    backgroundColor: Int,
    heightDp: Int,
    particleSpeed: Float
) {
    val intent = Intent(KineticBroadcastReceiver.ACTION_SHOW_KINETIC).apply {
        // 重要：Android 8.0+ 需要显式指定接收器组件
        setPackage(context.packageName)
        setClassName(context.packageName, "com.lenovo.levoice.caption.KineticBroadcastReceiver")

        putExtra(AnimationConfig.EXTRA_DURATION, durationMillis)
        putExtra(AnimationConfig.EXTRA_BACKGROUND_COLOR, backgroundColor)
        putExtra(AnimationConfig.EXTRA_HEIGHT_DP, heightDp)
        putExtra(AnimationConfig.EXTRA_PARTICLE_SPEED, particleSpeed)
    }
    context.sendBroadcast(intent)
}