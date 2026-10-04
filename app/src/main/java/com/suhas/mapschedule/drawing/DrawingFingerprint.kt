package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.DrawingStroke
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Computes a deterministic canonical fingerprint hash for vector drawing strokes.
 * 
 * Strategy:
 * 1. Normalize strokes into a standard 100x100 bounding box (0 padding) preserving aspect ratio and centering.
 * 2. Quantize coordinates into integer grid cells [0..100] to eliminate floating-point epsilon jitter.
 * 3. Deduplicate consecutive identical points along the stroke to eliminate sample-rate variations.
 * 4. Build a canonical string representation encoding stroke count, stroke order, and point trajectories.
 * 5. Hash the canonical representation using SHA-256.
 * 
 * This strategy detects reused/duplicate character designs while safely allowing visually distinct
 * or intentionally different stroke structures without false positives.
 */
object DrawingFingerprint {

    private const val CANONICAL_SIZE = 100f

    fun computeHash(strokes: List<DrawingStroke>): String {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.isEmpty()) return ""

        val normalized = DrawingNormalization.normalize(
            strokes = strokes,
            targetWidth = CANONICAL_SIZE,
            targetHeight = CANONICAL_SIZE,
            paddingFraction = 0f
        )
        if (normalized.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append(normalized.size).append(":")

        for ((idx, stroke) in normalized.withIndex()) {
            if (idx > 0) sb.append("|")
            var lastX = -1
            var lastY = -1
            var pointCount = 0

            for (pt in stroke.points) {
                val qx = pt.x.roundToInt().coerceIn(0, 100)
                val qy = pt.y.roundToInt().coerceIn(0, 100)
                if (qx != lastX || qy != lastY) {
                    if (pointCount > 0) sb.append(",")
                    sb.append(qx).append("-").append(qy)
                    lastX = qx
                    lastY = qy
                    pointCount++
                }
            }
        }

        val canonicalString = sb.toString()
        return sha256(canonicalString)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
