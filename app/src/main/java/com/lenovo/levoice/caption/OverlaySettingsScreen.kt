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
    var isServiceRunning by remember { mutableStateOf(false) }

    // 刷新权限状态
    LaunchedEffect(Unit) {
        hasOverlayPermission = OverlayService.checkOverlayPermission(context)
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
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = "启用后，本应用将在后台运行服务，监听广播消息。当接收到 com.zui.action.SHOW_KINETIC 广播时，会在屏幕顶部显示动效。",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持的动效类型：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "• 淡入淡出 (fade)", fontSize = 13.sp)
                    Text(text = "• 从顶部滑入 (slide)", fontSize = 13.sp)
                    Text(text = "• 波纹扩散 (ripple)", fontSize = 13.sp)
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
                        text = "点击下方按钮测试不同的动效类型",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Button(
                        onClick = {
                            sendTestBroadcast(context, AnimationType.FADE)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning
                    ) {
                        Text("测试：淡入淡出")
                    }

                    Button(
                        onClick = {
                            sendTestBroadcast(context, AnimationType.SLIDE)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning
                    ) {
                        Text("测试：从顶部滑入")
                    }

                    Button(
                        onClick = {
                            sendTestBroadcast(context, AnimationType.RIPPLE)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasOverlayPermission && isServiceRunning
                    ) {
                        Text("测试：波纹扩散")
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
                        text = "• animation_type (String): fade/slide/ripple",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
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
                        text = "• message (String): 消息内容（可选）",
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
private fun sendTestBroadcast(context: android.content.Context, type: AnimationType) {
    val intent = Intent(KineticBroadcastReceiver.ACTION_SHOW_KINETIC).apply {
        // 重要：Android 8.0+ 需要显式指定接收器组件
        setPackage(context.packageName)
        setClassName(context.packageName, "com.lenovo.levoice.caption.KineticBroadcastReceiver")

        putExtra(AnimationConfig.EXTRA_ANIMATION_TYPE, when (type) {
            AnimationType.FADE -> AnimationConfig.TYPE_FADE
            AnimationType.SLIDE -> AnimationConfig.TYPE_SLIDE
            AnimationType.RIPPLE -> AnimationConfig.TYPE_RIPPLE
        })
        putExtra(AnimationConfig.EXTRA_DURATION, 3000L)
        putExtra(AnimationConfig.EXTRA_BACKGROUND_COLOR, 0x80FF5722.toInt()) // 半透明橙色
    }
    context.sendBroadcast(intent)
}