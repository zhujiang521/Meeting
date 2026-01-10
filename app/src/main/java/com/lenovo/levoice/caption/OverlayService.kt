package com.lenovo.levoice.caption

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * 悬浮窗服务
 * 负责管理悬浮窗的生命周期、显示和隐藏
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "overlay_service_channel"

        const val ACTION_SHOW_ANIMATION = "action_show_animation"
        const val ACTION_STOP_SERVICE = "action_stop_service"
        const val EXTRA_ANIMATION_CONFIG = "extra_animation_config"

        /**
         * 启动服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        /**
         * 检查悬浮窗权限
         */
        fun checkOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayAnimationView? = null
    private var isOverlayShowing = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_ANIMATION -> {
                val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_ANIMATION_CONFIG, AnimationConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_ANIMATION_CONFIG)
                }

                if (config != null) {
                    showAnimation(config)
                } else {
                    Log.e(TAG, "Animation config is null")
                }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    /**
     * 显示悬浮窗动效
     */
    private fun showAnimation(config: AnimationConfig) {
        if (!checkOverlayPermission(this)) {
            Log.e(TAG, "Overlay permission not granted")
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 如果悬浮窗已存在，复用并更新配置
            if (isOverlayShowing && overlayView != null) {
                // 直接在现有视图上显示新动画
                overlayView?.showAnimation(config)
                Log.d(TAG, "Reusing existing overlay view with new animation")
                return
            }

            // 创建新的悬浮窗视图
            overlayView = OverlayAnimationView(this).apply {
                onDismissListener = {
                    removeOverlay()
                }
            }

            // 配置悬浮窗参数
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dpToPx(config.heightDp), // 使用配置中的高度
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            // 添加到窗口管理器
            windowManager?.addView(overlayView, params)
            isOverlayShowing = true

            // 开始动画
            overlayView?.showAnimation(config)

            Log.d(TAG, "Overlay animation shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            Toast.makeText(this, "显示悬浮窗失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 移除悬浮窗
     */
    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            isOverlayShowing = false
            Log.d(TAG, "Overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay", e)
        }
    }

    /**
     * dp 转 px
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示动效悬浮窗"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("动效悬浮窗服务")
            .setContentText("正在运行，可接收动效指令")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}