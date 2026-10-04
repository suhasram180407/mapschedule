package com.suhas.mapschedule.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import kotlin.math.sqrt

/**
 * Interactive Jetpack Compose Drawing Canvas.
 * Supports smooth finger and stylus input.
 * Strictly adheres to the rule: captures ONLY vector stroke geometry;
 * never creates or records any pen icon, cursor, indicator, or border.
 */
@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    onStrokesChanged: (List<DrawingStroke>) -> Unit,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.White,
    strokeWidth: Float = 14f,
    backgroundColor: Color = Color(0xFF161616),
    showBorder: Boolean = true,
    cornerRadius: Dp = 16.dp,
    onTap: (() -> Unit)? = null
) {
    // Current ongoing stroke while finger/stylus is touching screen
    var currentPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    val currentStrokes by rememberUpdatedState(strokes)
    val currentOnStrokesChanged by rememberUpdatedState(onStrokesChanged)
    val currentOnTap by rememberUpdatedState(onTap)

    var boxModifier = modifier
    if (cornerRadius > 0.dp) {
        boxModifier = boxModifier.clip(RoundedCornerShape(cornerRadius))
    }
    boxModifier = boxModifier.background(backgroundColor)
    if (showBorder) {
        boxModifier = boxModifier.border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(cornerRadius))
    }

    Box(
        modifier = boxModifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startPos = down.position
                    val startTime = System.currentTimeMillis()
                    var maxDistance = 0f
                    val points = mutableListOf(DrawingPoint(startPos.x, startPos.y))
                    currentPoints = points.toList()

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val pos = change.position
                        val dx = pos.x - startPos.x
                        val dy = pos.y - startPos.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist > maxDistance) {
                            maxDistance = dist
                        }
                        if (dist > 6f) {
                            change.consume()
                            points.add(DrawingPoint(pos.x, pos.y))
                            currentPoints = points.toList()
                        }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    // If it was a quick tap with minimal movement (< 18px and < 350ms)
                    if (maxDistance < 18f && duration < 350L && points.size <= 3) {
                        currentPoints = emptyList()
                        currentOnTap?.invoke()
                    } else if (points.isNotEmpty()) {
                        val newStroke = DrawingStroke(
                            points = points.toList(),
                            strokeWidth = strokeWidth
                        )
                        currentOnStrokesChanged(currentStrokes + newStroke)
                        currentPoints = emptyList()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeStyle = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Render all committed strokes
            for (stroke in strokes) {
                drawStrokePath(stroke.points, strokeColor, stroke.strokeWidth)
            }

            // Render current active stroke
            if (currentPoints.isNotEmpty()) {
                drawStrokePath(currentPoints, strokeColor, strokeWidth)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokePath(
    points: List<DrawingPoint>,
    color: Color,
    width: Float
) {
    if (points.isEmpty()) return

    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = width / 2f,
            center = androidx.compose.ui.geometry.Offset(points[0].x, points[0].y)
        )
        return
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
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
