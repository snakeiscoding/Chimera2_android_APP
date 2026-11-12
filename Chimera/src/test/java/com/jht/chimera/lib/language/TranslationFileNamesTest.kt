package com.jht.chimera.lib.language

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class TranslationFileNamesTest {

    private lateinit var dir: File

    @Before
    fun setup() {
        dir = File.createTempFile("translations-", "-test").let { placeholder ->
            placeholder.delete()
            placeholder.mkdirs()
            placeholder
        }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun write(name: String, body: String = "{}"): File =
        File(dir, name).apply { writeText(body) }

    //region normalizeTag / canonicalFileName

    @Test
    fun `normalizeTag treats dash and underscore and case as equal`() {
        val expected = "zh_cn"
        assertEquals(expected, TranslationFileNames.normalizeTag("zh-CN"))
        assertEquals(expected, TranslationFileNames.normalizeTag("zh_CN"))
        assertEquals(expected, TranslationFileNames.normalizeTag("ZH-cn"))
        assertEquals(expected, TranslationFileNames.normalizeTag("  zh_cn  "))
    }

    @Test
    fun `canonicalFileName is lower case with underscores`() {
        assertEquals("zh_cn.json", TranslationFileNames.canonicalFileName("zh-CN"))
        assertEquals("zh_tw.json", TranslationFileNames.canonicalFileName("zh_TW"))
        assertEquals("en.json", TranslationFileNames.canonicalFileName("en"))
    }

    @Test
    fun `isCanonicalFor matches only the canonical spelling`() {
        assertTrue(TranslationFileNames.isCanonicalFor(File(dir, "zh_cn.json"), "zh-CN"))
        assertFalse(TranslationFileNames.isCanonicalFor(File(dir, "zh-CN.json"), "zh-CN"))
        assertFalse(TranslationFileNames.isCanonicalFor(File(dir, "zh_CN.json"), "zh-CN"))
    }

    //endregion

    //region listTranslationFiles

    @Test
    fun `listTranslationFiles picks up json files case insensitively`() {
        write("en.json")
        write("DE.JSON")

        val names = TranslationFileNames.listTranslationFiles(dir).map { it.name }.toSet()

        assertEquals(setOf("en.json", "DE.JSON"), names)
    }

    @Test
    fun `listTranslationFiles ignores non-json files and half-written temp files`() {
        write("en.json")
        write("notes.txt")
        // The shape TranslationsWeb.writeTranslationFile creates before its rename.
        File.createTempFile("translation-", ".json.tmp", dir)

        val names = TranslationFileNames.listTranslationFiles(dir).map { it.name }

        assertEquals(listOf("en.json"), names)
    }

    @Test
    fun `listTranslationFiles is empty for a missing directory`() {
        val missing = File(dir, "does-not-exist")

        assertTrue(TranslationFileNames.listTranslationFiles(missing).isEmpty())
    }

    //endregion

    //region resolveByTag

    @Test
    fun `resolveByTag returns one entry per tag with nothing shadowed`() {
        write("en.json")
        write("zh_cn.json")

        val resolutions = TranslationFileNames.resolveByTag(TranslationFileNames.listTranslationFiles(dir))

        assertEquals(setOf("en", "zh_cn"), resolutions.keys)
        resolutions.forEach { (tag, resolution) ->
            assertTrue("expected nothing shadowed for $tag", resolution.shadowed.isEmpty())
        }
    }

    @Test
    fun `resolveByTag collapses dash and underscore spellings onto one tag`() {
        write("zh-CN.json")
        write("zh_cn.json")

        val resolutions = TranslationFileNames.resolveByTag(TranslationFileNames.listTranslationFiles(dir))

        assertEquals(setOf("zh_cn"), resolutions.keys)
        assertEquals(1, resolutions.getValue("zh_cn").shadowed.size)
    }

    @Test
    fun `resolveByTag prefers the canonical name over a legacy spelling even when older`() {
        val legacy = write("zh-CN.json", """{"key":"legacy"}""")
        val canonical = write("zh_cn.json", """{"key":"canonical"}""")
        // Make the legacy file the newest, so only the canonical-name rule can save us.
        assertTrue(canonical.setLastModified(1_000_000_000L))
        assertTrue(legacy.setLastModified(2_000_000_000L))

        val resolution = TranslationFileNames
            .resolveByTag(TranslationFileNames.listTranslationFiles(dir))
            .getValue("zh_cn")

        assertEquals("zh_cn.json", resolution.chosen.name)
        assertEquals(listOf("zh-CN.json"), resolution.shadowed.map { it.name })
    }

    @Test
    fun `resolveByTag falls back to the newest file when neither name is canonical`() {
        val older = write("zh-CN.json")
        val newer = write("ZH_CN.json")
        assertTrue(older.setLastModified(1_000_000_000L))
        assertTrue(newer.setLastModified(2_000_000_000L))

        val resolution = TranslationFileNames
            .resolveByTag(TranslationFileNames.listTranslationFiles(dir))
            .getValue("zh_cn")

        assertEquals("ZH_CN.json", resolution.chosen.name)
        assertEquals(listOf("zh-CN.json"), resolution.shadowed.map { it.name })
    }

    @Test
    fun `resolveByTag breaks equal timestamps by name so the winner is stable`() {
        val a = write("ZH-CN.json")
        val b = write("zh-CN.json")
        assertTrue(a.setLastModified(1_000_000_000L))
        assertTrue(b.setLastModified(1_000_000_000L))

        val files = TranslationFileNames.listTranslationFiles(dir)
        val forward = TranslationFileNames.resolveByTag(files).getValue("zh_cn").chosen.name
        val reversed = TranslationFileNames.resolveByTag(files.reversed()).getValue("zh_cn").chosen.name

        assertEquals("the winner must not depend on directory order", forward, reversed)
        assertEquals("ZH-CN.json", forward)
    }

    //endregion
}
