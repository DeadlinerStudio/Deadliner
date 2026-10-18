package com.aritxonly.deadliner.ui.detail

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID

/** Keep the card body, progress and text independent throughout the container transform. */
internal data class TaskDetailCardAppearance(
    val background: Color,
    val indicator: Color,
    val progress: Float,
)

internal data class TaskDetailSnapshot(
    val appearance: TaskDetailCardAppearance,
    val foreground: ImageBitmap?,
)

/** One-shot, in-process handoff: never put bitmap pixels in an Intent or write them to disk. */
internal object TaskDetailSnapshotStore {
    const val MaxSnapshotBytes = 4 * 1024 * 1024
    private val images = object : LruCache<String, TaskDetailSnapshot>(MaxSnapshotBytes * 2) {
        override fun sizeOf(key: String, value: TaskDetailSnapshot): Int =
            value.foreground?.let { it.width * it.height * 4 }?.coerceAtLeast(1) ?: 1
    }

    fun put(snapshot: TaskDetailSnapshot): String? {
        val image = snapshot.foreground
        if (image != null && image.width.toLong() * image.height * 4 > MaxSnapshotBytes) return null
        return UUID.randomUUID().toString().also { images.put(it, snapshot) }
    }
    fun take(key: String?): TaskDetailSnapshot? = key?.let(images::remove)
}
