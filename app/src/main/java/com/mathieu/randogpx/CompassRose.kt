package com.mathieu.randogpx

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * Boussole de visée (V1.13) :
 * - l'aiguille rouge/bleue est FIXE : la pointe rouge marque la direction
 *   de marche (haut de l'écran)
 * - la couronne graduée (N E S O, graduations 15°) tourne sous l'aiguille
 * - le cap se lit sous l'index orange fixe, en haut
 * - si un objectif est défini (targetBearingDeg), un pin orange apparaît sur
 *   la couronne : alignez-le sous l'aiguille rouge pour vous diriger vers lui.
 */
class CompassRose : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: android.util.AttributeSet?) : super(context, attrs)

    private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 30, 26); style = Paint.Style.FILL
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(230, 225, 213); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 175, 160); strokeWidth = 2f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(242, 234, 216); textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(211, 47, 47); style = Paint.Style.FILL
    }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 96, 160); style = Paint.Style.FILL
    }
    private val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val lubber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 176, 0); style = Paint.Style.FILL
    }
    private val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 152, 0); style = Paint.Style.FILL
    }

    /** Orientation de la couronne en degrés (−heading pour suivre le cap). */
    var rotationDeg = 0f

    /** Relèvement (°) de l'objectif à afficher sur la couronne, ou null. */
    var targetBearingDeg: Float? = null

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = Math.min(w, h) / 2f - 4f

        // ---- couronne tournante : fond, graduations, cardinales, pin objectif ----
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        canvas.drawCircle(cx, cy, r, circle)
        canvas.drawCircle(cx, cy, r, ring)
        for (a in 0 until 360 step 15) {
            val rad = Math.toRadians(a.toDouble())
            val long = a % 90 == 0
            val inner = r * (if (long) 0.78f else 0.90f)
            canvas.drawLine(
                cx + (inner * Math.sin(rad)).toFloat(),
                cy - (inner * Math.cos(rad)).toFloat(),
                cx + (r * 0.97f * Math.sin(rad)).toFloat(),
                cy - (r * 0.97f * Math.cos(rad)).toFloat(),
                if (long) ring else tick
            )
        }
        text.textSize = r * 0.17f
        val labels = listOf("N" to 0f, "E" to 90f, "S" to 180f, "O" to 270f)
        for ((lbl, ang) in labels) {
            val rad = Math.toRadians(ang.toDouble())
            val tx = cx + (r * 0.66f * Math.sin(rad)).toFloat()
            val ty = cy - (r * 0.66f * Math.cos(rad)).toFloat() + text.textSize / 3f
            text.color = if (lbl == "N") Color.rgb(255, 120, 120) else Color.rgb(242, 234, 216)
            canvas.drawText(lbl, tx, ty, text)
        }
        targetBearingDeg?.let { tb ->
            val rad = Math.toRadians(tb.toDouble())
            val px = cx + (r * 0.83f * Math.sin(rad)).toFloat()
            val py = cy - (r * 0.83f * Math.cos(rad)).toFloat()
            canvas.drawCircle(px, py, r * 0.055f, pin)
            canvas.drawCircle(px, py, r * 0.025f, hub)
        }
        canvas.restore()

        // ---- aiguille FIXE : rouge vers le haut = direction de marche ----
        val nR = r * 0.64f
        val nW = r * 0.10f
        val pathN = android.graphics.Path().apply {
            moveTo(cx, cy - nR); lineTo(cx - nW, cy); lineTo(cx + nW, cy); close()
        }
        canvas.drawPath(pathN, red)
        val pathS = android.graphics.Path().apply {
            moveTo(cx, cy + nR); lineTo(cx - nW, cy); lineTo(cx + nW, cy); close()
        }
        canvas.drawPath(pathS, blue)
        canvas.drawCircle(cx, cy, r * 0.05f, hub)

        // index fixe orange en haut : le cap se lit ici
        val s = r * 0.09f
        val p = android.graphics.Path().apply {
            moveTo(cx, cy - r + s); lineTo(cx - s, cy - r - s); lineTo(cx + s, cy - r - s); close()
        }
        canvas.drawPath(p, lubber)
    }
}
