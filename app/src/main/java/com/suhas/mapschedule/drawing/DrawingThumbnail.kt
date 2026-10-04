package com.suhas.mapschedule.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.suhas.mapschedule.model.DrawingStroke

/**
 * Renders a crisp vector thumbnail of normalized strokes directly inside Compose UI.
 */
@Composable
fun DrawingThumbnail(
    strokes: List<DrawingStroke>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    strokeColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (strokes.isEmpty()) return@Canvas

            val normalizedStrokes = DrawingNormalization.normalize(
                strokes = strokes,
                targetWidth = size.width,
                targetHeight = size.height,
                paddingFraction = 0.15f
            )

            for (stroke in normalizedStrokes) {
                val points = stroke.points
                if (points.isEmpty()) continue

                if (points.size == 1) {
                    drawCircle(
                        color = strokeColor,
                        radius = stroke.strokeWidth / 2f,
                        center = Offset(points[0].x, points[0].y)
                    )
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
                        path.quadraticTo(prev.x, prev.y, midX, midY)
                    }
                }
                path.lineTo(points.last().x, points.last().y)

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
