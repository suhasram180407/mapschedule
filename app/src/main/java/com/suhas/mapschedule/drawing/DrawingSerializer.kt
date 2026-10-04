package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke

/**
 * Handles serialization and deserialization of vector drawing strokes.
 * Uses a pure Kotlin JSON-compatible format for zero external dependencies
 * and 100% testability across JVM unit tests and Android runtime.
 */
object DrawingSerializer {

    /**
     * Serializes a list of [DrawingStroke] into a JSON string.
     */
    fun serialize(strokes: List<DrawingStroke>): String {
        val sb = StringBuilder()
        sb.append("[")
        strokes.forEachIndexed { strokeIdx, stroke ->
            if (strokeIdx > 0) sb.append(",")
            sb.append("{")
            sb.append("\"w\":").append(stroke.strokeWidth).append(",")
            sb.append("\"t\":").append(stroke.timestamp).append(",")
            sb.append("\"pts\":[")
            stroke.points.forEachIndexed { ptIdx, pt ->
                if (ptIdx > 0) sb.append(",")
                sb.append("{\"x\":").append(pt.x).append(",\"y\":").append(pt.y).append("}")
            }
            sb.append("]}")
        }
        sb.append("]")
        return sb.toString()
    }

    /**
     * Deserializes a JSON string into a list of [DrawingStroke].
     * Returns an empty list if string is blank or invalid.
     */
    fun deserialize(json: String): List<DrawingStroke> {
        if (json.isBlank()) return emptyList()
        val strokes = mutableListOf<DrawingStroke>()
        try {
            var i = 0
            val len = json.length

            while (i < len) {
                // Find next stroke object
                val strokeStart = json.indexOf('{', i)
                if (strokeStart == -1) break
                val strokeEnd = findMatchingBrace(json, strokeStart)
                if (strokeEnd == -1) break

                val strokeContent = json.substring(strokeStart + 1, strokeEnd)
                val width = extractFloat(strokeContent, "\"w\":") ?: 12f
                val timestamp = extractLong(strokeContent, "\"t\":") ?: 0L

                val ptsKey = "\"pts\":["
                val ptsStart = strokeContent.indexOf(ptsKey)
                val points = mutableListOf<DrawingPoint>()

                if (ptsStart != -1) {
                    val arrayStart = ptsStart + ptsKey.length - 1
                    val arrayEnd = findMatchingBracket(strokeContent, arrayStart)
                    if (arrayEnd != -1) {
                        val ptsContent = strokeContent.substring(arrayStart + 1, arrayEnd)
                        var ptIdx = 0
                        while (ptIdx < ptsContent.length) {
                            val objStart = ptsContent.indexOf('{', ptIdx)
                            if (objStart == -1) break
                            val objEnd = ptsContent.indexOf('}', objStart)
                            if (objEnd == -1) break
                            val ptJson = ptsContent.substring(objStart + 1, objEnd)
                            val x = extractFloat(ptJson, "\"x\":") ?: 0f
                            val y = extractFloat(ptJson, "\"y\":") ?: 0f
                            points.add(DrawingPoint(x, y))
                            ptIdx = objEnd + 1
                        }
                    }
                }

                if (points.isNotEmpty()) {
                    strokes.add(DrawingStroke(points = points, strokeWidth = width, timestamp = timestamp))
                }

                i = strokeEnd + 1
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return strokes
    }

    private fun findMatchingBrace(s: String, openPos: Int): Int {
        var depth = 0
        for (i in openPos until s.length) {
            if (s[i] == '{') depth++
            else if (s[i] == '}') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    private fun findMatchingBracket(s: String, openPos: Int): Int {
        var depth = 0
        for (i in openPos until s.length) {
            if (s[i] == '[') depth++
            else if (s[i] == ']') {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    private fun extractFloat(text: String, key: String): Float? {
        val idx = text.indexOf(key)
        if (idx == -1) return null
        val start = idx + key.length
        val end = findTokenEnd(text, start)
        return text.substring(start, end).trim().toFloatOrNull()
    }

    private fun extractLong(text: String, key: String): Long? {
        val idx = text.indexOf(key)
        if (idx == -1) return null
        val start = idx + key.length
        val end = findTokenEnd(text, start)
        return text.substring(start, end).trim().toLongOrNull()
    }

    private fun findTokenEnd(text: String, start: Int): Int {
        var i = start
        while (i < text.length && text[i] != ',' && text[i] != '}' && text[i] != ']' && !text[i].isWhitespace()) {
            i++
        }
        return i
    }
}
