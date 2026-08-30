package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 开关保护与当前 provider 配置校验均由 runtime 统一维护。
        if (!GlobalUtils.cloudSyncEnable || !KmpV6Runtime.hasCompleteSyncConfiguration()) {
            return@withContext Result.success()
        }

        val outcome = runCatching { KmpV6Runtime.syncOnce() }.getOrNull()
        if (outcome?.succeeded == true) Result.success() else Result.retry()
    }
}
