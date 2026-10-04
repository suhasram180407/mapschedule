package com.suhas.mapschedule.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.suhas.mapschedule.model.DrawingStroke
import java.io.File
import java.io.FileOutputStream

/**
 * Renders normalized vector drawing strokes into high-quality anti-aliased Bitmaps.
 * Handles saving to local private storage for fast widget and preview loading.
 */
object DrawingRenderer {

    const val DEFAULT_BITMAP_SIZE = 512

    /**
     * Renders a list of [DrawingStroke] onto a transparent Bitmap of [width] x [height].
     * Normalizes and centers the strokes automatically.
     */
    fun renderToBitmap(
        strokes: List<DrawingStroke>,
        width: Int = DEFAULT_BITMAP_SIZE,
        height: Int = DEFAULT_BITMAP_SIZE,
        strokeColor: Int = Color.WHITE
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)

        if (strokes.isEmpty()) return bitmap

        val normalizedStrokes = DrawingNormalization.normalize(
            strokes = strokes,
            targetWidth = width.toFloat(),
            targetHeight = height.toFloat(),
            paddingFraction = 0.12f
        )

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = strokeColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in normalizedStrokes) {
            val points = stroke.points
            if (points.isEmpty()) continue

            paint.strokeWidth = stroke.strokeWidth

            if (points.size == 1) {
                // Single point (tap) -> draw small circle dot
                canvas.drawCircle(points[0].x, points[0].y, stroke.strokeWidth / 2f, paint)
                continue
            }

            val path = Path()
            path.moveTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f

                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(prev.x, prev.y, midX, midY)
                }
            }
            path.lineTo(points.last().x, points.last().y)
            canvas.drawPath(path, paint)
        }

        return bitmap
    }

    /**
     * Saves the pre-rendered bitmap to internal private storage for a given mapping ID.
     * Returns the absolute path of the saved PNG file.
     */
    fun savePreview(
        context: Context,
        mappingId: String,
        strokes: List<DrawingStroke>
    ): String? {
        return try {
            val previewsDir = File(context.filesDir, "previews").apply { if (!exists()) mkdirs() }
            val file = File(previewsDir, "$mappingId.png")
            val bitmap = renderToBitmap(strokes)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Loads a Bitmap from a file path. Returns null if missing or invalid.
     */
    fun loadPreview(path: String?): Bitmap? {
        if (path == null) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders a completely clean, solid black bitmap for the home screen widget box.
     * Contains no text, no app name, no border, no decorative UI.
     */
    fun renderWidgetPadBitmap(
        width: Int = DEFAULT_BITMAP_SIZE,
        height: Int = DEFAULT_BITMAP_SIZE
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        return bitmap
    }

    /**
     * Deletes a pre-rendered preview file.
     */
    fun deletePreview(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // Ignore deletion errors
        }
    }
}
