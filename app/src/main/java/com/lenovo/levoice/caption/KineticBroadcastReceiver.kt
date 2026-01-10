package com.lenovo.levoice.caption

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 接收 com.zui.action.SHOW_KINETIC 广播
 * 解析广播参数并触发悬浮窗动效
 */
class KineticBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KineticReceiver"
        const val ACTION_SHOW_KINETIC = "com.zui.action.SHOW_KINETIC"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_KINETIC) {
            return
        }

        Log.d(TAG, "Received SHOW_KINETIC broadcast")

        // 解析广播携带的参数
        val config = parseAnimationConfig(intent)

        Log.d(TAG, "Animation config: duration=${config.durationMillis}ms, height=${config.heightDp}dp")

        // 通知 OverlayService 显示动效
        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_ANIMATION
            putExtra(OverlayService.EXTRA_ANIMATION_CONFIG, config)
        }

        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
        }
    }

    /**
     * 从 Intent 中解析动效配置
     */
    private fun parseAnimationConfig(intent: Intent): AnimationConfig {
        val duration = intent.getLongExtra(AnimationConfig.EXTRA_DURATION, 3000L)
        val backgroundColor = intent.getIntExtra(
            AnimationConfig.EXTRA_BACKGROUND_COLOR,
            0x80000000.toInt()
        )
        val heightDp = intent.getIntExtra(AnimationConfig.EXTRA_HEIGHT_DP, 110) // 默认110dp
        val particleSpeed = intent.getFloatExtra(AnimationConfig.EXTRA_PARTICLE_SPEED, 6.5f) // 默认6.5

        return AnimationConfig(
            durationMillis = duration,
            backgroundColor = backgroundColor,
            heightDp = heightDp,
            particleSpeed = particleSpeed
        )
    }
}