package com.aritxonly.deadliner.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.R
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class LiveUpdateNotificationAdapterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun xiaomiMetadataPreservesStandardLayoutAndEntryPoint() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        val builder = NotificationCompat.Builder(context, "test")
            .setSmallIcon(R.drawable.ic_focus_notification).setContentTitle("Task")
            .setContentText("Remaining").setOngoing(true)
        val notification = LiveUpdateNotificationAdapter.apply(context, builder,
            "Task", "Remaining", "02:30", R.drawable.ic_focus_notification).build()
        assertEquals("Task", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Remaining", notification.extras.getString(Notification.EXTRA_TEXT))
        assertNull(notification.contentView)
        val params = JSONObject(notification.extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertFalse(params.getBoolean("filterWhenNoPermission"))
        assertFalse(params.getBoolean("enableFloat"))
        assertTrue(params.getBoolean("updatable"))
        val island = params.getJSONObject("param_island")
        assertEquals("02:30", island.getJSONObject("bigIslandArea").getJSONObject("textInfo").getString("title"))
        val pictureKey = island.getJSONObject("smallIslandArea").getJSONObject("combinePicInfo").getJSONObject("picInfo").getString("pic")
        val pictures = notification.extras.getBundle("miui.focus.pics")!!
        assertTrue(pictures.containsKey(pictureKey))
        assertEquals(android.graphics.drawable.Icon.TYPE_BITMAP,
            pictures.getParcelable(pictureKey, android.graphics.drawable.Icon::class.java)!!.type)
    }

    @Test fun rootAndShizukuChooseIslandInsteadOfRequestingBothSurfaces() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        val preferences = context.getSharedPreferences("island_bypass", 0)
        preferences.edit().putString("mode", "ROOT").commit()
        assertFalse(LiveUpdateNotificationAdapter.shouldRequestPromotion(context, sdk = 36))
        preferences.edit().putString("mode", "SHIZUKU").commit()
        assertFalse(LiveUpdateNotificationAdapter.shouldRequestPromotion(context, sdk = 36))
        preferences.edit().putString("mode", "SYSTEM_HOOK").commit()
        assertFalse(LiveUpdateNotificationAdapter.shouldRequestPromotion(context, sdk = 36))
        preferences.edit().putString("mode", "OFF").commit()
        assertTrue(LiveUpdateNotificationAdapter.shouldRequestPromotion(context, sdk = 36))
        assertFalse(LiveUpdateNotificationAdapter.shouldRequestPromotion(context, sdk = 35))
    }

    @Test fun otherManufacturersKeepOrdinaryNotificationOnAndroid15() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Google")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "google")
        val notification = LiveUpdateNotificationAdapter.apply(context,
            NotificationCompat.Builder(context, "test").setContentTitle("Task").setOngoing(true),
            "Task", "Remaining", "02:30", R.drawable.ic_focus_notification).build()
        assertFalse(notification.extras.containsKey("miui.focus.param"))
        assertFalse(notification.extras.getBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING))
    }

    @Test @Config(sdk = [33], application = android.app.Application::class) fun oldXiaomiDoesNotReceiveIslandProtocol() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        assertFalse(LiveUpdateNotificationAdapter.supportsIsland())
    }

    @Test fun completedNotificationDoesNotRequestSystemSurfaces() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        val notification = LiveUpdateNotificationAdapter.apply(context,
            NotificationCompat.Builder(context, "test").setContentTitle("Finished").setOngoing(false),
            "Finished", "Done", "00:00", R.drawable.ic_focus_notification, ongoing = false).build()
        assertFalse(notification.extras.containsKey("miui.focus.param"))
        assertEquals(0, notification.flags and Notification.FLAG_ONGOING_EVENT)
    }

    @Test fun bypassIsOptInAndModesAreMutuallyExclusive() {
        context.getSharedPreferences("island_bypass", 0).edit().clear().commit()
        assertEquals(IslandBypassMode.OFF, IslandWhitelistBypass.mode(context))
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", "ROOT").commit()
        assertEquals(IslandBypassMode.ROOT, IslandWhitelistBypass.mode(context))
        context.getSharedPreferences("island_bypass", 0).edit().putString("mode", "invalid").commit()
        assertEquals(IslandBypassMode.OFF, IslandWhitelistBypass.mode(context))
    }
}
