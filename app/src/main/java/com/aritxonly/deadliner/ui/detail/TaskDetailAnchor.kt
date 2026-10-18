package com.aritxonly.deadliner.ui.detail

import android.app.Activity
import android.app.ActivityOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TaskDetailAnchor internal constructor(
    private val layer: GraphicsLayer,
    private val scope: CoroutineScope,
    private val cornerRadiusPx: Float,
) {
    private var bounds: Rect? = null
    private var launching = false
    private var appearance: TaskDetailCardAppearance? = null
    val modifier = Modifier.onGloballyPositioned { bounds = it.detailBoundsOnScreen() }
    internal val foregroundModifier = Modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }

    internal fun updateAppearance(background: Color, indicator: Color, progress: Float) {
        appearance = TaskDetailCardAppearance(background, indicator, progress.coerceIn(0f, 1f))
    }

    fun open(activity: Activity, uid: String) {
        if (launching) return
        launching = true
        val rect = bounds?.takeIf { it.width > 0f && it.height > 0f }
        val cardAppearance = appearance
        scope.launch {
            var snapshotKey: String? = null
            try {
                val image = if (rect != null && rect.width * rect.height * 4 <= TaskDetailSnapshotStore.MaxSnapshotBytes) {
                    // A slow/failed capture must not hold up navigation.
                    runCatching { withTimeoutOrNull(100) { layer.toImageBitmap() } }.getOrNull()
                } else null
                ensureActive()
                if (activity.isFinishing || activity.isDestroyed) return@launch
                val intent = DeadlineDetailActivity.newIntent(activity, uid)
                if (rect != null) {
                    intent.putExtra(TaskDetailSourceExtra, floatArrayOf(rect.left, rect.top, rect.right, rect.bottom))
                    intent.putExtra(TaskDetailCornerExtra, cornerRadiusPx)
                    cardAppearance?.let {
                        snapshotKey = TaskDetailSnapshotStore.put(TaskDetailSnapshot(it, image))
                        intent.putExtra(TaskDetailSnapshotExtra, snapshotKey)
                    }
                }
                activity.startActivity(intent, ActivityOptions.makeCustomAnimation(activity, 0, 0).toBundle())
            } catch (error: Exception) {
                TaskDetailSnapshotStore.take(snapshotKey)
                throw error
            } finally {
                launching = false
            }
        }
    }
}

internal const val TaskDetailSourceExtra = "task_detail_source_bounds"
internal const val TaskDetailCornerExtra = "task_detail_source_corner"
internal const val TaskDetailSnapshotExtra = "task_detail_source_snapshot"

@Composable
fun rememberTaskDetailAnchor(): TaskDetailAnchor {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val corner = dimensionResource(R.dimen.item_corner_radius)
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    return remember(layer, scope, cornerPx) { TaskDetailAnchor(layer, scope, cornerPx) }
}

/** Absolute display coordinates keep source and target consistent across offset/split windows. */
internal fun LayoutCoordinates.detailBoundsOnScreen(): Rect =
    boundsInWindow(clipBounds = false).translate(positionOnScreen() - positionInWindow())
