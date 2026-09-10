package com.aritxonly.deadliner.ui.main.simplified

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.MiuixDialog
import com.aritxonly.deadliner.ui.base.MiuixDialogActions
import com.aritxonly.deadliner.ui.base.TextButton
import com.image.cropview.CropType
import com.image.cropview.EdgeType
import com.image.cropview.ImageCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProfileEditor(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsState by KmpV6Runtime.settingsViewModel.uiState.collectAsState()
    val profile = settingsState.profile
    var nickname by remember(profile?.nickname) {
        mutableStateOf(TextFieldValue(profile?.nickname.orEmpty()))
    }

    // 裁剪状态
    var showCropper by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 图片选择器
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        cropBitmap = bmp
                        showCropper = bmp != null
                    }
                }
            }
        }
    }

    // 当前头像
    val avatarFile = remember(profile?.avatarFileName) {
        profile?.avatarFileName?.let { File(context.filesDir, "avatars/$it") }
    }
    var imageBitmap by remember(avatarFile?.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(profile?.avatarFileName) {
        val f = avatarFile
        imageBitmap = if (f?.exists() == true) withContext(Dispatchers.IO) {
            decodeImageBitmapSized(f, 512)
        } else null
    }

    // --- UI ---
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🟢 头像 + 编辑按钮
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_person_wght400),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                shape = CircleShape,
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(32.dp)
            ) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_camera_wght400),
                    contentDescription = stringResource(R.string.change_avatar)
                )
            }
        }

        // 🟣 昵称输入
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.nickname), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(R.string.enter_nickname)) }
            )
        }

        // 🔘 保存按钮
        Button(
            onClick = {
                scope.launch {
                    KmpV6Runtime.settingsViewModel.updateNickname(nickname.text.trim())
                }
            },
            enabled = profile != null && nickname.text.trim() != profile.nickname,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.save_change))
        }

        // 🗑️ 移除头像
        if (!profile?.avatarFileName.isNullOrBlank()) {
            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    removeAvatarFile(context, profile?.avatarFileName)
                    KmpV6Runtime.settingsViewModel.updateAvatar(null)
                }
            }) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_delete_wght400),
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.remove_avatar))
            }
        }
    }

    // 🟡 裁剪对话框
    if (showCropper && cropBitmap != null) {
        AvatarCropperDialog(
            src = cropBitmap!!,
            onCancel = { showCropper = false },
            onDone = { cropped ->
                showCropper = false
                scope.launch(Dispatchers.IO) {
                    val previous = profile?.avatarFileName
                    val fileName = saveAvatarFile(context, cropped)
                    KmpV6Runtime.settingsViewModel.updateAvatar(fileName)
                    removeAvatarFile(context, previous)
                }
            }
        )
    }
}

private fun saveAvatarFile(context: Context, bitmap: Bitmap): String {
    val directory = File(context.filesDir, "avatars").apply { mkdirs() }
    val fileName = "${UUID.randomUUID()}.jpg"
    FileOutputStream(File(directory, fileName)).use { output ->
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val square = Bitmap.createBitmap(bitmap, x, y, size, size)
        val scaled = if (square.width == 512) square else Bitmap.createScaledBitmap(square, 512, 512, true)
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 95, output)) { "Could not encode avatar" }
    }
    return fileName
}

private fun removeAvatarFile(context: Context, fileName: String?) {
    if (fileName.isNullOrBlank()) return
    File(context.filesDir, "avatars/$fileName").takeIf { it.exists() }?.delete()
}

@Composable
fun AvatarCropperDialog(
    src: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit
) {
    val imageCrop = remember(src) { ImageCrop(src) }
    val aspect = src.width.toFloat() / src.height.toFloat()
    val onCompleteCrop = {
        val cropped = imageCrop.onCrop()
        val size = min(cropped.width, cropped.height)
        val x = (cropped.width - size) / 2
        val y = (cropped.height - size) / 2
        onDone(Bitmap.createScaledBitmap(
            Bitmap.createBitmap(cropped, x, y, size, size),
            512,
            512,
            true,
        ))
    }

    MiuixDialog(
        show = true,
        onDismissRequest = onCancel,
        maxWidth = 560.dp,
    ) {
        AvatarCropperDialogContent(
            imageCrop = imageCrop,
            aspect = aspect,
            contentPadding = 0.dp,
            useMiuixActions = true,
            onCancel = onCancel,
            onCompleteCrop = onCompleteCrop,
        )
    }
}

@Composable
private fun AvatarCropperDialogContent(
    imageCrop: ImageCrop,
    aspect: Float,
    contentPadding: androidx.compose.ui.unit.Dp,
    useMiuixActions: Boolean,
    onCancel: () -> Unit,
    onCompleteCrop: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.crop_avatar),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCancel) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_close_wght400),
                    contentDescription = stringResource(R.string.close),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            imageCrop.ImageCropView(
                modifier = Modifier.matchParentSize(),
                guideLineColor = Color.LightGray,
                guideLineWidth = 1.dp,
                edgeCircleSize = 8.dp,
                showGuideLines = true,
                cropType = CropType.PROFILE_CIRCLE,
                edgeType = EdgeType.CIRCULAR,
            )
        }

        if (useMiuixActions) {
            MiuixDialogActions(
                secondaryButton = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                },
                primaryButton = {
                    Button(onClick = onCompleteCrop) { Text(stringResource(R.string.accept)) }
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onCompleteCrop) { Text(stringResource(R.string.accept)) }
            }
        }
    }
}

private fun decodeImageBitmapSized(file: File, reqSize: Int = 512): ImageBitmap? {
    if (!file.exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val maxDim = max(bounds.outWidth, bounds.outHeight).toFloat()
    var sample = 1
    if (maxDim > reqSize) {
        var half = maxDim / 2f
        while ((half / sample) > reqSize) sample *= 2
    }

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    return bmp.asImageBitmap()
}
