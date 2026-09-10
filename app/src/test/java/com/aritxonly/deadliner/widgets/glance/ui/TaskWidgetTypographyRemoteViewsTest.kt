package com.aritxonly.deadliner.widgets.glance.ui

import android.app.Application
import android.content.Context
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetTypographyRemoteViewsTest {
    @Test
    fun `font hierarchy survives glance remote views translation`() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val remoteViews = TypographyTestWidget().compose(
            context = context,
            size = DpSize(160.dp, 160.dp),
        )
        val root = remoteViews.apply(context, FrameLayout(context))

        assertEquals(
            "sans-serif-medium",
            root.findTextView(PRIMARY_TEXT)?.typefaceFamily(),
        )
        assertEquals(
            "sans-serif",
            root.findTextView(SUPPORTING_TEXT)?.typefaceFamily(),
        )
        assertEquals(
            400,
            root.findTextView(PRIMARY_TEXT)?.textAppearanceSpan()?.textFontWeight,
        )
        assertEquals(
            400,
            root.findTextView(SUPPORTING_TEXT)?.textAppearanceSpan()?.textFontWeight,
        )
        assertEquals(
            700,
            root.findTextView(ACCENT_TEXT)?.textAppearanceSpan()?.textFontWeight,
        )
    }

    private fun TextView.typefaceFamily(): String? {
        val spanned = text as? Spanned ?: return null
        return spanned.getSpans(0, spanned.length, TypefaceSpan::class.java)
            .singleOrNull()
            ?.family
    }

    private fun TextView.textAppearanceSpan(): TextAppearanceSpan? {
        val spanned = text as? Spanned ?: return null
        return spanned.getSpans(0, spanned.length, TextAppearanceSpan::class.java)
            .singleOrNull()
    }

    private fun View.findTextView(expectedText: String): TextView? {
        if (this is TextView && text.toString() == expectedText) return this
        if (this !is ViewGroup) return null
        repeat(childCount) { index ->
            getChildAt(index).findTextView(expectedText)?.let { return it }
        }
        return null
    }

    private class TypographyTestWidget : GlanceAppWidget(errorUiLayout = 0) {
        override val sizeMode: SizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent {
                Column {
                    Text(
                        text = PRIMARY_TEXT,
                        style = TextStyle(
                            fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    Text(
                        text = SUPPORTING_TEXT,
                        style = TextStyle(
                            fontFamily = TaskWidgetTypography.SUPPORTING_FONT_FAMILY,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    Text(
                        text = ACCENT_TEXT,
                        style = TextStyle(
                            fontFamily = TaskWidgetTypography.EMPHASIS_FONT_FAMILY,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val PRIMARY_TEXT = "Primary"
        const val SUPPORTING_TEXT = "Supporting"
        const val ACCENT_TEXT = "Accent"
    }
}
