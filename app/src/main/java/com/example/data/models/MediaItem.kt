package com.example.data.models

import android.graphics.Bitmap
import java.util.UUID

enum class MediaType {
    IMAGE, VIDEO
}

data class GeneratedMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val type: MediaType,
    val title: String,
    val prompt: String,
    val modelUsed: String,
    val aspectRatio: String,
    val bitmap: Bitmap? = null,
    val videoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceImageBitmap: Bitmap? = null,
    val isSimulated: Boolean = false
)
