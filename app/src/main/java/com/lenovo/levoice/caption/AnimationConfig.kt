package com.lenovo.levoice.caption

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 动效配置数据类
 * 用于定义悬浮窗动效的持续时间、背景颜色、高度和粒子速度等参数
 */
@Parcelize
data class AnimationConfig(
    val durationMillis: Long = 3000L,
    val backgroundColor: Int = 0x80000000.toInt(), // 默认半透明黑色
    val heightDp: Int = 110, // 悬浮窗高度（dp），默认110dp
    val particleSpeed: Float = 6.5f // 粒子下落速度（像素/帧），默认6.5，范围4-9
) : Parcelable {

    companion object {
        // 广播 Intent Extra 键名常量
        const val EXTRA_DURATION = "duration"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_HEIGHT_DP = "height_dp"
        const val EXTRA_PARTICLE_SPEED = "particle_speed"
    }
}