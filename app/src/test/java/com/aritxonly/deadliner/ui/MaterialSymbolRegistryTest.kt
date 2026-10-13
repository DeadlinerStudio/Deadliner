package com.aritxonly.deadliner.ui

import com.aritxonly.deadliner.R
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialSymbolRegistryTest {
    @Test
    fun everyFontDrawableHasMatchingComposeRegistration() {
        val drawableDirectory = listOf(
            File("src/main/res/drawable"),
            File("app/src/main/res/drawable")
        ).first { it.isDirectory }
        val descriptors = drawableDirectory.listFiles().orEmpty().filter {
            it.extension == "xml" && it.readText().contains("MaterialSymbolDrawable")
        }
        assertTrue("Expected Material Symbol font drawables", descriptors.isNotEmpty())
        val parser = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder()
        val namespace = "http://schemas.android.com/apk/res-auto"
        descriptors.forEach { file ->
            val resourceId = R.drawable::class.java.getField(file.nameWithoutExtension).getInt(null)
            val definition = MaterialSymbolRegistry.definitionFor(resourceId)
            assertNotNull("${file.name} must use the font painter in Compose", definition)
            val element = parser.parse(file).documentElement
            assertEquals(file.name, element.getAttributeNS(namespace, "symbolName"), definition!!.name)
            assertEquals(
                file.name,
                element.getAttributeNS(namespace, "symbolFilled").toBoolean(),
                definition.filled
            )
        }
    }
}
