package com.aritxonly.deadliner.ui.main.modern.components

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.main.TextPageIndicator

@Composable
fun ModernMainHeader(
    activity: MainActivity,
    selectedPage: DeadlineType,
    onSelectedPageChange: (DeadlineType) -> Unit,
    avatarPainter: Painter?,
    onShowAiOverlay: () -> Unit,
    showPageTabs: Boolean = true,
) {
    Column {
        TopAppBar(
            title = "Deadliner",
            navigationIcon = {
                IconButton(onClick = {
                    val intent = Intent(activity, ArchiveActivity::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val options =
                            ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
                        activity.startActivity(intent, options)
                    } else {
                        activity.startActivity(intent)
                    }
                }) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_archive),
                        contentDescription = stringResource(R.string.archive),
                        modifier = expressiveTypeModifier
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                }) {
                    if (avatarPainter != null) {
                        Image(
                            painter = avatarPainter,
                            contentDescription = stringResource(R.string.user),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = expressiveTypeModifier
                        )
                    }
                }
            },
            mode = TopAppBarStyle.LARGE,
            forceMiuix = true,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showPageTabs) {
                TextPageIndicator(
                    text = stringResource(R.string.task),
                    onClick = { onSelectedPageChange(DeadlineType.TASK) },
                    selected = selectedPage.toString(),
                    tag = DeadlineType.TASK.toString(),
                    badgeConfig = Triple(false, 0, false),
                )
                TextPageIndicator(
                    text = stringResource(R.string.habit),
                    onClick = { onSelectedPageChange(DeadlineType.HABIT) },
                    selected = selectedPage.toString(),
                    tag = DeadlineType.HABIT.toString(),
                    badgeConfig = Triple(false, 0, false),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onShowAiOverlay,
                colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_lifi),
                        contentDescription = stringResource(R.string.ai_quick_add)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("问 AI")
                }
            }
        }
    }
}
