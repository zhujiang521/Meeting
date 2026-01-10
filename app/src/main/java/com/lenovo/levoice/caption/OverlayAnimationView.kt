package com.lenovo.levoice.caption

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 悬浮窗动效视图
 * 支持淡入淡出、滑入、波纹扩散和粒子动效
 */
class OverlayAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentAnimator: ValueAnimator? = null
    private var animationProgress = 0f
    private var config: AnimationConfig = AnimationConfig()

    // 粒子系统
    private val particles = mutableListOf<Particle>()
    private var particleAnimator: ValueAnimator? = null
    private var particleTime = 0f

    var onDismissListener: (() -> Unit)? = null

    // 性能优化：缓存 Bitmap 和 Paint 对象
    private var cachedGlowBitmap: android.graphics.Bitmap? = null
    private var cachedMaskBitmap: android.graphics.Bitmap? = null
    private var cachedBlurredMaskBitmap: android.graphics.Bitmap? = null
    private var lastBackgroundColor: Int = 0
    private var lastAlpha: Int = 0
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0

    // 复用的 Paint 对象
    private val particleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val particleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    }

    // 粒子模糊滤镜复用（分档位）
    private val blurFilters = mapOf(
        15f to BlurMaskFilter(15f * 0.15f, BlurMaskFilter.Blur.NORMAL),
        20f to BlurMaskFilter(20f * 0.15f, BlurMaskFilter.Blur.NORMAL),
        25f to BlurMaskFilter(25f * 0.15f, BlurMaskFilter.Blur.NORMAL),
        30f to BlurMaskFilter(30f * 0.15f, BlurMaskFilter.Blur.NORMAL),
        35f to BlurMaskFilter(35f * 0.15f, BlurMaskFilter.Blur.NORMAL)
    )

    // 粒子数据类
    private data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val angle: Float,
        val rotationSpeed: Float,
        var rotation: Float,
        val color: Int,
        val alpha: Float,
        val shape: ParticleShape,
        val initialX: Float,
        val initialY: Float,
        val orbitRadius: Float,
        val orbitSpeed: Float
    )

    private enum class ParticleShape {
        CIRCLE, SQUARE
    }

    init {
        setOnClickListener {
            // 用户点击关闭
            dismissWithAnimation()
        }
        // 启用硬件加速图层
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * 显示动效
     */
    fun showAnimation(config: AnimationConfig) {
        this.config = config
        paint.color = config.backgroundColor

        visibility = VISIBLE
        animationProgress = 0f

        // 清除缓存（背景色可能改变）
        if (lastBackgroundColor != config.backgroundColor) {
            clearBitmapCache()
            lastBackgroundColor = config.backgroundColor
        }

        currentAnimator?.cancel()

        // 标记粒子是否已初始化
        var particlesInitialized = false
        // 标记缓存是否已预创建
        var cachePreCreated = false

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500L // 入场动画固定 500ms
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float

                // 在第一帧动画时初始化粒子和缓存（此时 View 已经完成布局）
                if (!particlesInitialized && width > 0 && height > 0) {
                    initParticles()
                    startParticleAnimation()
                    particlesInitialized = true
                }

                // 预创建缓存，避免第一次绘制时创建导致动画卡顿
                if (!cachePreCreated && width > 0 && height > 0) {
                    // 在后台线程预创建缓存（不阻塞动画）
                    post {
                        if (cachedGlowBitmap == null) {
                            createCachedBitmaps(255)
                        }
                    }
                    cachePreCreated = true
                }

                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 入场动画结束后，等待指定时长，然后自动消失
                    postDelayed({
                        dismissWithAnimation()
                    }, config.durationMillis)
                }
            })
            start()
        }
    }

    /**
     * 初始化粒子系统
     */
    private fun initParticles() {
        particles.clear()
        val particleCount = 80 // 优化：减少粒子数量到80（视觉效果基本不变）

        // 提取传入背景色的RGB值
        val baseColor = config.backgroundColor
        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)

        repeat(particleCount) { index ->
            // 确保X坐标均匀分布：将屏幕宽度分成若干段，加上随机偏移
            val segmentWidth = width.toFloat() / particleCount
            val baseX = index * segmentWidth
            val randomOffset = Random.nextFloat() * segmentWidth
            val x = baseX + randomOffset

            // 计算该X位置的弧线高度
            val normalizedX = (x / width.toFloat() - 0.5f) * 2f
            val arcHeightAtX = height * (1f - normalizedX * normalizedX)

            // 让粒子在该X位置的弧线高度范围内随机分布
            val y = -Random.nextFloat() * arcHeightAtX * 1.5f

            // 粒子大小：5-25dp（不受悬浮窗高度限制）
            val size = Random.nextFloat() * 20 + 15f

            val speed = Random.nextFloat() * 5 + 4f // 下落速度 4-9（加快）
            val angle = 0f // 不需要角度
            val rotationSpeed = 0f // 正方形不旋转

            // 使用传入的背景色，只改变透明度（0.3-0.9）
            val particleAlpha = Random.nextFloat() * 0.6f + 0.3f
            val color = Color.argb((particleAlpha * 255).toInt(), red, green, blue)

            val alpha = 1.0f // 粒子本身的alpha已经在color中设置，这里使用1.0
            val shape = ParticleShape.SQUARE // 全部是正方形
            val orbitRadius = 0f // 不需要轨道
            val orbitSpeed = 0f

            particles.add(
                Particle(
                    x = x,
                    y = y,
                    size = size,
                    speed = speed,
                    angle = angle,
                    rotationSpeed = rotationSpeed,
                    rotation = 0f, // 不旋转
                    color = color,
                    alpha = alpha,
                    shape = shape,
                    initialX = x,
                    initialY = y,
                    orbitRadius = orbitRadius,
                    orbitSpeed = orbitSpeed
                )
            )
        }
    }

    /**
     * 启动粒子动画
     */
    private fun startParticleAnimation() {
        particleAnimator?.cancel()
        particleTime = 0f

        particleAnimator = ValueAnimator.ofFloat(0f, Float.MAX_VALUE).apply {
            duration = Long.MAX_VALUE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                particleTime += 0.016f // 约60fps
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    /**
     * 更新粒子位置
     */
    private fun updateParticles() {
        particles.forEachIndexed { index, particle ->
            // 向下滑落运动
            particle.y += particle.speed

            // 轻微的左右摆动
            particle.x += sin(particleTime * 2f) * 0.5f

            // 正方形不旋转，保持水平
            // particle.rotation 保持为 0

            // 计算当前X位置对应的弧线高度
            val normalizedX = (particle.x / width.toFloat() - 0.5f) * 2f
            val arcHeight = height * (1f - normalizedX * normalizedX)

            // 粒子超出弧线边界后从顶部重新出现
            if (particle.y > arcHeight + particle.size) {
                particle.y = -particle.size
                // 重新计算X坐标，保持均匀分布
                val segmentWidth = width.toFloat() / particles.size
                val baseX = index * segmentWidth
                val randomOffset = Random.nextFloat() * segmentWidth
                particle.x = baseX + randomOffset
            }
        }
    }

    /**
     * 带动画的消失
     */
    private fun dismissWithAnimation() {
        currentAnimator?.cancel()
        particleAnimator?.cancel()

        currentAnimator = ValueAnimator.ofFloat(animationProgress, 0f).apply {
            duration = 300L // 退场动画固定 300ms
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    particles.clear()
                    onDismissListener?.invoke()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (config.animationType) {
            AnimationType.FADE -> drawFadeAnimation(canvas)
            AnimationType.SLIDE -> drawSlideAnimation(canvas)
            AnimationType.RIPPLE -> drawRippleAnimation(canvas)
        }

        // 绘制粒子效果（在所有动画类型上都显示）
        drawParticles(canvas)
    }

    /**
     * 绘制粒子系统（优化版本）
     */
    private fun drawParticles(canvas: Canvas) {
        if (particles.isEmpty()) return

        val globalAlpha = (animationProgress * 255).toInt()

        particles.forEachIndexed { index, particle ->
            // 50%实心，50%空心
            val isFilled = index % 3 != 0

            // 复用 Paint 对象
            val particlePaint = if (isFilled) particleFillPaint else particleStrokePaint
            particlePaint.color = particle.color
            particlePaint.alpha = ((particle.alpha * globalAlpha).toInt()).coerceIn(0, 255)

            // 优化：使用分档的模糊滤镜，减少对象创建
            val sizeKey = ((particle.size / 5).toInt() * 5).toFloat().coerceIn(15f, 35f)
            particlePaint.maskFilter = blurFilters[sizeKey]

            canvas.save()
            canvas.translate(particle.x, particle.y)

            // 全部绘制为正方形
            val halfSize = particle.size / 2
            canvas.drawRect(-halfSize, -halfSize, halfSize, halfSize, particlePaint)

            canvas.restore()
        }
    }

    /**
     * 淡入淡出动效（带渐变弧形背景）
     */
    private fun drawFadeAnimation(canvas: Canvas) {
        val alpha = (animationProgress * 255).toInt()

        // 绘制弧形背景（中间最高，两边为0）
        drawArcBackground(canvas, alpha)
    }

    /**
     * 绘制炫光效果背景（优化版本：使用缓存）
     */
    private fun drawArcBackground(canvas: Canvas, alpha: Int) {
        // 检查缓存是否有效
        val needsRecreate = cachedGlowBitmap == null ||
                cachedMaskBitmap == null ||
                cachedBlurredMaskBitmap == null ||
                lastWidth != width ||
                lastHeight != height ||
                lastBackgroundColor != config.backgroundColor

        if (needsRecreate) {
            // 使用完全不透明（255）创建缓存，后续通过 Paint.alpha 控制透明度
            createCachedBitmaps(255)
            lastWidth = width
            lastHeight = height
            lastBackgroundColor = config.backgroundColor
            lastAlpha = 255
        }

        // 使用缓存的 Bitmap 绘制
        cachedGlowBitmap?.let { glowBitmap ->
            cachedBlurredMaskBitmap?.let { blurredMaskBitmap ->
                // 根据当前 alpha 调整绘制的透明度
                if (alpha < 255) {
                    paint.alpha = alpha
                    canvas.drawBitmap(glowBitmap, 0f, 0f, paint)
                    finalPaint.alpha = alpha
                    canvas.drawBitmap(blurredMaskBitmap, 0f, 0f, finalPaint)
                    paint.alpha = 255
                    finalPaint.alpha = 255
                } else {
                    canvas.drawBitmap(glowBitmap, 0f, 0f, null)
                    canvas.drawBitmap(blurredMaskBitmap, 0f, 0f, finalPaint)
                }
            }
        }
    }

    /**
     * 创建并缓存背景 Bitmap
     */
    private fun createCachedBitmaps(alpha: Int) {
        if (width <= 0 || height <= 0) return

        // 清理旧缓存
        clearBitmapCache()

        val centerX = width / 2f
        val w = width.toFloat()
        val h = height.toFloat()

        // 提取传入背景色的RGB值
        val baseColor = config.backgroundColor
        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)

        // 创建光晕 Bitmap
        cachedGlowBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val glowCanvas = Canvas(cachedGlowBitmap!!)

        // 第一层：从顶部向下的主光源渐变
        val topGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f, -h * 0.3f, 0f, h * 1.2f,
                intArrayOf(
                    Color.argb((alpha * 0.95f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.75f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.45f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.20f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.08f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.02f).toInt(), red, green, blue),
                    Color.argb(0, red, green, blue)
                ),
                floatArrayOf(0f, 0.15f, 0.35f, 0.55f, 0.75f, 0.9f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
        }
        glowCanvas.drawRect(0f, 0f, w, h * 2f, topGlowPaint)

        // 第二层：顶部增强光晕
        val topIntensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f, -h * 0.2f, 0f, h * 0.6f,
                intArrayOf(
                    Color.argb((alpha * 0.85f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.55f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.25f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.08f).toInt(), red, green, blue),
                    Color.argb(0, red, green, blue)
                ),
                floatArrayOf(0f, 0.25f, 0.5f, 0.8f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(120f, BlurMaskFilter.Blur.NORMAL)
        }
        glowCanvas.drawRect(0f, 0f, w, h, topIntensePaint)

        // 第三层：径向辅助
        val radialAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                centerX, h * 0.1f, h * 1.5f,
                intArrayOf(
                    Color.argb((alpha * 0.65f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.35f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.15f).toInt(), red, green, blue),
                    Color.argb((alpha * 0.04f).toInt(), red, green, blue),
                    Color.argb(0, red, green, blue)
                ),
                floatArrayOf(0f, 0.3f, 0.6f, 0.85f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            maskFilter = BlurMaskFilter(100f, BlurMaskFilter.Blur.NORMAL)
        }
        glowCanvas.drawRect(0f, 0f, w, h * 2f, radialAccentPaint)

        // 创建弧形遮罩
        cachedMaskBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(cachedMaskBitmap!!)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until height) {
            val progress = y / h
            val normalizedHeight = 1f - progress
            val arcWidth = if (normalizedHeight > 0) {
                kotlin.math.sqrt(normalizedHeight) * w / 2f
            } else {
                0f
            }

            val baseAlpha = (1f - progress * progress).coerceIn(0f, 1f)
            val left = centerX - arcWidth
            val right = centerX + arcWidth

            if (arcWidth > 0) {
                val rowGradient = android.graphics.LinearGradient(
                    left, y.toFloat(), right, y.toFloat(),
                    intArrayOf(
                        Color.argb((baseAlpha * 255).toInt(), 255, 255, 255),
                        Color.argb((baseAlpha * 0.95f * 255).toInt(), 255, 255, 255),
                        Color.argb((baseAlpha * 0.75f * 255).toInt(), 255, 255, 255),
                        Color.argb((baseAlpha * 0.45f * 255).toInt(), 255, 255, 255),
                        Color.argb((baseAlpha * 0.20f * 255).toInt(), 255, 255, 255),
                        Color.argb((baseAlpha * 0.05f * 255).toInt(), 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                    ),
                    floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 0.85f, 0.95f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                maskPaint.shader = rowGradient
                maskCanvas.drawRect(left, y.toFloat(), right, (y + 1).toFloat(), maskPaint)
            }
        }

        // 创建模糊后的遮罩
        cachedBlurredMaskBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ALPHA_8)
        val blurredMaskCanvas = Canvas(cachedBlurredMaskBitmap!!)
        blurPaint.maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
        blurredMaskCanvas.drawBitmap(cachedMaskBitmap!!, 0f, 0f, blurPaint)
    }

    /**
     * 清理 Bitmap 缓存
     */
    private fun clearBitmapCache() {
        cachedGlowBitmap?.recycle()
        cachedGlowBitmap = null
        cachedMaskBitmap?.recycle()
        cachedMaskBitmap = null
        cachedBlurredMaskBitmap?.recycle()
        cachedBlurredMaskBitmap = null
    }

    /**
     * 从顶部滑入动效（带渐变弧形背景）
     */
    private fun drawSlideAnimation(canvas: Canvas) {
        val translateY = -height * (1 - animationProgress)

        canvas.save()
        canvas.translate(0f, translateY)

        // 绘制弧形背景
        drawArcBackground(canvas, 255)

        canvas.restore()
    }

    /**
     * 波纹扩散动效（带渐变弧形背景）
     */
    private fun drawRippleAnimation(canvas: Canvas) {
        val alpha = (animationProgress * 255).toInt()

        val centerX = width / 2f
        val centerY = 0f // 波纹中心在顶部中间
        val maxRadius = sqrt((centerX * centerX + height * height).toDouble()).toFloat()
        val currentRadius = maxRadius * animationProgress

        // 绘制扩散的圆形，但限制在视图范围内
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())

        // 先填充弧形背景
        drawArcBackground(canvas, alpha)

        // 再绘制波纹效果（从顶部中间扩散）
        // 防止 radius 为 0 导致 IllegalArgumentException
        val rippleRadius = (currentRadius * 0.8f).coerceAtLeast(1f)

        if (rippleRadius > 1f && animationProgress > 0.01f) {
            val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    centerX, centerY, rippleRadius,
                    intArrayOf(
                        Color.argb((animationProgress * 150).toInt(), 100, 200, 255),
                        Color.argb((animationProgress * 50).toInt(), 60, 140, 200)
                    ),
                    floatArrayOf(0.7f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawCircle(centerX, centerY, rippleRadius, ripplePaint)
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimator?.cancel()
        currentAnimator = null
        particleAnimator?.cancel()
        particleAnimator = null
        particles.clear()
        clearBitmapCache()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 确保点击事件能被处理
        return true
    }
}