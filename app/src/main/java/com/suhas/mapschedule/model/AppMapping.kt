package com.suhas.mapschedule.model

import com.suhas.mapschedule.drawing.DrawingFingerprint
import java.util.UUID

/**
 * Represents a saved 1-to-1 mapping between a user's custom drawing (the widget design)
 * and an installed Android app.
 */
data class AppMapping(
    val id: String = UUID.randomUUID().toString(),
    val mappingCharacterStrokes: List<DrawingStroke> = emptyList(),
    val mappingCharacterHash: String = DrawingFingerprint.computeHash(mappingCharacterStrokes),
    val widgetDesignStrokes: List<DrawingStroke> = emptyList(),
    val widgetDesignPreview: String? = null,
    val appName: String,
    val packageName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Primary unified drawing accessors
    val drawingStrokes: List<DrawingStroke>
        get() = if (widgetDesignStrokes.isNotEmpty()) widgetDesignStrokes else mappingCharacterStrokes

    val drawingHash: String
        get() = if (widgetDesignStrokes.isNotEmpty()) {
            DrawingFingerprint.computeHash(widgetDesignStrokes)
        } else {
            mappingCharacterHash
        }

    val drawingPreview: String?
        get() = widgetDesignPreview

    // Reference design accessors (Section 32)
    val referenceStrokes: List<DrawingStroke>
        get() = drawingStrokes

    val referencePreview: String?
        get() = drawingPreview

    // Legacy getters
    val strokes: List<DrawingStroke>
        get() = drawingStrokes

    val previewPath: String?
        get() = drawingPreview

    // Unified 1-drawing constructor
    constructor(
        id: String = UUID.randomUUID().toString(),
        drawingStrokes: List<DrawingStroke>,
        drawingHash: String = DrawingFingerprint.computeHash(drawingStrokes),
        drawingPreview: String? = null,
        appName: String,
        packageName: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        mappingCharacterStrokes = drawingStrokes,
        mappingCharacterHash = drawingHash,
        widgetDesignStrokes = drawingStrokes,
        widgetDesignPreview = drawingPreview,
        appName = appName,
        packageName = packageName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // Legacy compatibility constructor
    constructor(
        id: String = UUID.randomUUID().toString(),
        appName: String,
        packageName: String,
        strokes: List<DrawingStroke>,
        drawingHash: String = DrawingFingerprint.computeHash(strokes),
        previewPath: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        mappingCharacterStrokes = strokes,
        mappingCharacterHash = drawingHash,
        widgetDesignStrokes = emptyList(),
        widgetDesignPreview = previewPath,
        appName = appName,
        packageName = packageName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
