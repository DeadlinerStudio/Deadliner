package com.aritxonly.deadliner.ui.detail

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskDetailSnapshotStoreTest {
    @Test fun snapshotsAreOneShotAndOversizedCapturesAreRejected() {
        val image = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).asImageBitmap()
        val appearance = TaskDetailCardAppearance(Color.White, Color.Blue, 0.45f)
        val snapshot = TaskDetailSnapshot(appearance, image)
        val token = TaskDetailSnapshotStore.put(snapshot)
        assertNotNull(token)
        assertSame(snapshot, TaskDetailSnapshotStore.take(token))
        assertNull(TaskDetailSnapshotStore.take(token))
        assertNull(TaskDetailSnapshotStore.take(null))
        assertNull(TaskDetailSnapshotStore.put(TaskDetailSnapshot(appearance,
            Bitmap.createBitmap(1025, 1024, Bitmap.Config.ARGB_8888).asImageBitmap())))
    }

    @Test fun failedForegroundCaptureStillPreservesTheCardBodyAndProgress() {
        val appearance = TaskDetailCardAppearance(Color.White, Color.Blue, 0.45f)
        val token = TaskDetailSnapshotStore.put(TaskDetailSnapshot(appearance, null))
        val restored = TaskDetailSnapshotStore.take(token)!!
        assertEquals(appearance, restored.appearance)
        assertNull(restored.foreground)
    }
}
