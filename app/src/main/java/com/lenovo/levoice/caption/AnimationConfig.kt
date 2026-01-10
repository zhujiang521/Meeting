package com.lenovo.levoice.caption

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 动效配置数据类
 * 用于定义悬浮窗动效的类型、持续时间等参数
 */
@Parcelize
data class AnimationConfig(
    val animationType: AnimationType = AnimationType.FADE,
    val durationMillis: Long = 3000L,
    val backgroundColor: Int = 0x80000000.toInt(), // 默认半透明黑色
    val message: String? = null,
    val heightDp: Int = 110 // 悬浮窗高度（dp），默认110dp
) : Parcelable {

    companion object {
        // 广播 Intent Extra 键名常量
        const val EXTRA_ANIMATION_TYPE = "animation_type"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_HEIGHT_DP = "height_dp"

        // 动效类型字符串常量
        const val TYPE_FADE = "fade"
        const val TYPE_SLIDE = "slide"
        const val TYPE_RIPPLE = "ripple"

        /**
         * 从字符串解析动效类型
         */
        fun parseAnimationType(typeString: String?): AnimationType {
            return when (typeString?.lowercase()) {
                TYPE_FADE -> AnimationType.FADE
                TYPE_SLIDE -> AnimationType.SLIDE
                TYPE_RIPPLE -> AnimationType.RIPPLE
                else -> AnimationType.FADE // 默认
            }
        }
    }
}

/**
 * 动效类型枚举
 */
enum class AnimationType {
    FADE,       // 淡入淡出
    SLIDE,      // 从顶部滑入
    RIPPLE      // 波纹扩散
}