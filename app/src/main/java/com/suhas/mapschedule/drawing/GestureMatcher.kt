package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic multi-stroke 2D gesture matcher based on the Point-Cloud ($P) recognition principle.
 *
 * Characteristics:
 * - 100% offline, local, deterministic, and lightweight (<2ms execution).
 * - Zero OCR, zero cloud AI, zero external dependencies.
 * - Invariant to stroke count, stroke order, and stroke direction (treats drawing as a 2D point cloud).
 * - Invariant to scale (normalized to 100x100 bounding box) and position (centroid-centered at origin).
 * - Distinguishes disparate shapes (e.g. 'A' vs 'C' vs '★') with high precision.
 * - Safely handles ambiguity and unknown drawings by enforcing similarity thresholds.
 */
object GestureMatcher {

    const val RESAMPLE_POINT_COUNT = 32
    const val CANONICAL_SIZE = 100f
    const val MIN_SIMILARITY_SCORE = 0.58f
    const val AMBIGUITY_SCORE_MARGIN = 0.05f
    private const val MAX_NORMALIZATION_DISTANCE = 35.0f

    sealed class MatchResult {
        data class Matched(
            val mapping: AppMapping,
            val score: Float,
            val distance: Float
        ) : MatchResult()

        data class Ambiguous(
            val candidates: List<CandidateScore>
        ) : MatchResult()

        data class NoMatch(
            val bestScore: Float,
            val bestDistance: Float
        ) : MatchResult()

        object Empty : MatchResult()
    }

    data class CandidateScore(
        val mapping: AppMapping,
        val score: Float,
        val distance: Float
    )

    /**
     * Matches [candidateStrokes] against a list of saved [mappings].
     */
    fun match(
        candidateStrokes: List<DrawingStroke>,
        mappings: List<AppMapping>
    ): MatchResult {
        val totalPoints = candidateStrokes.sumOf { it.points.size }
        if (candidateStrokes.isEmpty() || totalPoints < 2) {
            return MatchResult.Empty
        }

        val validMappings = mappings.filter { it.drawingStrokes.isNotEmpty() }
        if (validMappings.isEmpty()) {
            return MatchResult.NoMatch(0f, Float.MAX_VALUE)
        }

        val candidateCloud = extractNormalizedPointCloud(candidateStrokes, RESAMPLE_POINT_COUNT)
        if (candidateCloud == null || candidateCloud.points.isEmpty()) {
            return MatchResult.Empty
        }

        val scores = mutableListOf<CandidateScore>()

        for (mapping in validMappings) {
            val refCloud = extractNormalizedPointCloud(mapping.drawingStrokes, RESAMPLE_POINT_COUNT)
            if (refCloud != null && refCloud.points.isNotEmpty()) {
                val distance = computePointCloudDistance(candidateCloud, refCloud)
                val score = distanceToScore(distance)
                scores.add(CandidateScore(mapping, score, distance))
            }
        }

        if (scores.isEmpty()) {
            return MatchResult.NoMatch(0f, Float.MAX_VALUE)
        }

        // Sort descending by score (ascending by distance)
        scores.sortByDescending { it.score }

        val best = scores[0]

        // 1. Check if best score meets minimum similarity threshold
        if (best.score < MIN_SIMILARITY_SCORE) {
            return MatchResult.NoMatch(best.score, best.distance)
        }

        // 2. Check for ambiguity between top candidate and runner-up
        if (scores.size > 1) {
            val runnerUp = scores[1]
            val scoreDiff = best.score - runnerUp.score
            // Ambiguous only if both are high scoring, close to each other, and map to DIFFERENT apps
            if (runnerUp.score >= MIN_SIMILARITY_SCORE &&
                scoreDiff < AMBIGUITY_SCORE_MARGIN &&
                best.mapping.packageName != runnerUp.mapping.packageName
            ) {
                return MatchResult.Ambiguous(listOf(best, runnerUp))
            }
        }

        return MatchResult.Matched(best.mapping, best.score, best.distance)
    }

    /**
     * Evaluates similarity score [0.0..1.0] and distance between candidate and reference strokes.
     */
    fun evaluate(
        candidateStrokes: List<DrawingStroke>,
        referenceStrokes: List<DrawingStroke>
    ): Pair<Float, Float> {
        val cloud1 = extractNormalizedPointCloud(candidateStrokes, RESAMPLE_POINT_COUNT)
            ?: return Pair(0f, Float.MAX_VALUE)
        val cloud2 = extractNormalizedPointCloud(referenceStrokes, RESAMPLE_POINT_COUNT)
            ?: return Pair(0f, Float.MAX_VALUE)

        val distance = computePointCloudDistance(cloud1, cloud2)
        val score = distanceToScore(distance)
        return Pair(score, distance)
    }

    private fun distanceToScore(distance: Float): Float {
        return max(0f, min(1f, 1f - (distance / MAX_NORMALIZATION_DISTANCE)))
    }

    internal data class PointCloud(
        val points: List<DrawingPoint>,
        val aspectRatio: Float
    )

    internal fun extractNormalizedPointCloud(
        strokes: List<DrawingStroke>,
        targetPointCount: Int
    ): PointCloud? {
        val resampled = resampleStrokes(strokes, targetPointCount)
        if (resampled.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (pt in resampled) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }

        val width = max(maxX - minX, 1f)
        val height = max(maxY - minY, 1f)
        val aspectRatio = width / height
        val scale = CANONICAL_SIZE / max(width, height)

        // Rescale & compute centroid
        var sumX = 0f
        var sumY = 0f
        val scaledPoints = ArrayList<DrawingPoint>(resampled.size)

        for (pt in resampled) {
            val sx = (pt.x - minX) * scale
            val sy = (pt.y - minY) * scale
            scaledPoints.add(DrawingPoint(sx, sy))
            sumX += sx
            sumY += sy
        }

        val centroidX = sumX / scaledPoints.size
        val centroidY = sumY / scaledPoints.size

        // Translate so centroid is at (0, 0)
        val centeredPoints = scaledPoints.map { pt ->
            DrawingPoint(pt.x - centroidX, pt.y - centroidY)
        }

        return PointCloud(centeredPoints, aspectRatio)
    }

    internal fun resampleStrokes(
        strokes: List<DrawingStroke>,
        targetCount: Int
    ): List<DrawingPoint> {
        val validStrokes = strokes.filter { it.points.isNotEmpty() }
        if (validStrokes.isEmpty()) return emptyList()

        val allPoints = validStrokes.flatMap { it.points }
        if (allPoints.size <= targetCount) {
            // If very few points, pad by repeating or duplicating
            if (allPoints.isEmpty()) return emptyList()
            val result = ArrayList<DrawingPoint>(targetCount)
            for (i in 0 until targetCount) {
                result.add(allPoints[i % allPoints.size])
            }
            return result
        }

        // Calculate total path length across all strokes
        var totalLength = 0f
        for (stroke in validStrokes) {
            for (i in 0 until stroke.points.size - 1) {
                totalLength += distance(stroke.points[i], stroke.points[i + 1])
            }
        }

        if (totalLength <= 0.001f) {
            val first = allPoints.first()
            return List(targetCount) { first }
        }

        val interval = totalLength / (targetCount - 1)
        var accumulatedDist = 0f
        val result = ArrayList<DrawingPoint>(targetCount)
        result.add(validStrokes.first().points.first())

        for (stroke in validStrokes) {
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val segmentLen = distance(p1, p2)

                if (segmentLen <= 0f) continue

                if (accumulatedDist + segmentLen >= interval) {
                    var currentP1 = p1
                    var remainingSegment = segmentLen

                    while (accumulatedDist + remainingSegment >= interval && result.size < targetCount) {
                        val t = (interval - accumulatedDist) / remainingSegment
                        val qx = currentP1.x + t * (p2.x - currentP1.x)
                        val qy = currentP1.y + t * (p2.y - currentP1.y)
                        val interpolated = DrawingPoint(qx, qy)
                        result.add(interpolated)

                        // Update current position along this segment
                        accumulatedDist = 0f
                        currentP1 = interpolated
                        remainingSegment = distance(currentP1, p2)
                    }
                    accumulatedDist = remainingSegment
                } else {
                    accumulatedDist += segmentLen
                }
            }
        }

        while (result.size < targetCount) {
            result.add(allPoints.last())
        }

        return result
    }

    internal fun computePointCloudDistance(cloud1: PointCloud, cloud2: PointCloud): Float {
        val pts1 = cloud1.points
        val pts2 = cloud2.points

        // Average forward nearest-neighbor distance (pts1 -> pts2)
        var sumDist1 = 0f
        for (p in pts1) {
            var minD = Float.MAX_VALUE
            for (q in pts2) {
                val d = distance(p, q)
                if (d < minD) minD = d
            }
            sumDist1 += minD
        }
        val d1 = sumDist1 / pts1.size

        // Average backward nearest-neighbor distance (pts2 -> pts1)
        var sumDist2 = 0f
        for (q in pts2) {
            var minD = Float.MAX_VALUE
            for (p in pts1) {
                val d = distance(q, p)
                if (d < minD) minD = d
            }
            sumDist2 += minD
        }
        val d2 = sumDist2 / pts2.size

        // Base bidirectional point cloud distance
        var totalDist = (d1 + d2) / 2f

        // Aspect ratio penalty: prevents a vertical line and a horizontal line from matching
        val ar1 = max(cloud1.aspectRatio, 0.05f)
        val ar2 = max(cloud2.aspectRatio, 0.05f)
        val arRatio = max(ar1 / ar2, ar2 / ar1)
        if (arRatio > 2.0f) {
            totalDist += (arRatio - 2.0f) * 6.0f
        }

        return totalDist
    }

    private fun distance(p1: DrawingPoint, p2: DrawingPoint): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}
