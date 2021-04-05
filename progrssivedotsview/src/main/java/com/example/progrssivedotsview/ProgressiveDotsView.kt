package com.example.progrssivedotsview

import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.app.Activity
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Canvas

val colors : Array<Int> = arrayOf(
    "#f44336",
    "#673AB7",
    "#BF360C",
    "#00C853",
    "#C51162"
).map {
    Color.parseColor(it)
}.toTypedArray()
val balls : Int = 5
val parts : Int = 1 + balls
val wFactor : Float = 5.9f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawProgressiveDots(w : Float, h : Float, scale : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / wFactor
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val r : Float = size / (3 * balls + 1)
    save()
    translate(w / 2, h / 2)
    paint.style = Paint.Style.STROKE
    drawRect(RectF(-size * sf1, -2 * r * sf1, size * sf1, 2 * r * sf1), paint)
    for (j in 1..balls) {
        save()
        translate(-size + 2 * (j + 1) * r, 0f)
        drawCircle(0f, 0f, r * sf.divideScale(j, parts), paint)
        restore()
    }
    restore()
}

fun Canvas.drawPDNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawProgressiveDots(w, h, scale, paint)
}

class ProgressiveDotsView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class PDNode(var i : Int, val state : State = State()) {

        private var next : PDNode? = null
        private var prev : PDNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = PDNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawPDNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : PDNode {
            var curr : PDNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class ProgressiveDots(var i : Int) {

        private var curr : PDNode = PDNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : ProgressiveDotsView) {

        private val animator : Animator = Animator(view)
        private val pd : ProgressiveDots = ProgressiveDots(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            pd.draw(canvas, paint)
            animator.animate {
                pd.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            pd.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : ProgressiveDotsView {
            val view : ProgressiveDotsView = ProgressiveDotsView(activity)
            activity.setContentView(view)
            return view
        }
    }
}