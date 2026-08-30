package com.aritxonly.deadliner

import android.app.Application
import android.content.Context
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.sync.SyncService
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.web.WebUtils

object AppSingletons {
    private lateinit var appContext: Context
    lateinit var db: DatabaseHelper
        private set
    lateinit var web: WebUtils
        private set
    lateinit var sync: SyncService
        private set

    fun init(app: Application) {
        appContext = app.applicationContext
        db = DatabaseHelper.getInstance(app)

        web = WebUtils(
            baseUrl = GlobalUtils.webDavBaseUrl,
            username = GlobalUtils.webDavUser,
            password = GlobalUtils.webDavPass
        )

        sync = SyncService(db, web)
    }

    fun appContextOrNull(): Context? = if (::appContext.isInitialized) appContext else null

    fun updateWeb() {
        web = WebUtils(
            baseUrl = GlobalUtils.webDavBaseUrl,
            username = GlobalUtils.webDavUser,
            password = GlobalUtils.webDavPass
        )

        sync = SyncService(db, web)
    }
}
