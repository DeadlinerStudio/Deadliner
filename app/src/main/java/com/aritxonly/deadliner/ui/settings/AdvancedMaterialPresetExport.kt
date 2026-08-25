package com.aritxonly.deadliner.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.AdvancedMaterialPresetJson
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning

@Composable
fun rememberAdvancedMaterialPresetExportAction(
    blurRadius: Float,
    fineTuning: AdvancedMaterialFineTuning,
): () -> Unit {
    val context = LocalContext.current
    var pendingJson by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exported = runCatching {
            checkNotNull(context.contentResolver.openOutputStream(uri))
                .bufferedWriter(Charsets.UTF_8)
                .use { writer -> writer.write(pendingJson) }
        }.isSuccess
        Toast.makeText(
            context,
            if (exported) {
                R.string.settings_advanced_material_export_success
            } else {
                R.string.settings_advanced_material_export_failed
            },
            Toast.LENGTH_SHORT,
        ).show()
    }

    return remember(blurRadius, fineTuning, launcher) {
        {
            pendingJson = AdvancedMaterialPresetJson.encode(blurRadius, fineTuning)
            launcher.launch(AdvancedMaterialPresetJson.SuggestedFileName)
        }
    }
}
