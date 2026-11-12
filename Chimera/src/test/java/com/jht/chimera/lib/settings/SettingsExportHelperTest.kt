package com.jht.chimera.lib.settings

import com.jht.datamatrixlib.DataDefault
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsExportHelperTest {
    @Before
    fun setup() {
        mockkStatic(DataMatrix::class)
        every { DataMatrix.getBoolean("NESTED_MOCK_SETTING_BOOL") } returns true
        every { DataMatrix.getFloat("NESTED_MOCK_SETTING_FLOAT") } returns 1.0f
        every { DataMatrix.getLong("NESTED_MOCK_SETTING_LONG") } returns 1L
        every { DataMatrix.getInt("NESTED_MOCK_SETTING_INT") } returns 1
        every { DataMatrix.getString("NESTED_MOCK_SETTING_STRING") } returns "string"
        every { DataMatrix.getDouble("NESTED_MOCK_SETTING_DOUBLE") } returns 0.5
    }

    @Test
    fun `test export all settings with no filter`() {
        val json = SettingsExportHelper.getDataMatrixSettings(true, null, MockSettingKeys.mockDataDefaultList)

        // We expect 6 entries corresponding to our 6 mock settings
        assertEquals(6, json.length())

        // Expected key, value, type for each setting (order matters)
        val expected = listOf(
            Triple("NESTED_MOCK_SETTING_BOOL", "true", "boolean"),
            Triple("NESTED_MOCK_SETTING_FLOAT", "1.0", "float"),
            Triple("NESTED_MOCK_SETTING_LONG", "1", "long"),
            Triple("NESTED_MOCK_SETTING_INT", "1", "integer"),
            Triple("NESTED_MOCK_SETTING_STRING", "string", "string"),
            Triple("NESTED_MOCK_SETTING_DOUBLE", "0.5", "double")
        )

        for (i in 0 until json.length()) {
            val jsonObject = json.getJSONObject(i)
            val (expectedKey, expectedValue, expectedType) = expected[i]
            assertEquals(expectedKey, jsonObject.getString("key"))
            assertEquals(expectedValue, jsonObject.getString("value"))
            assertEquals(expectedType, jsonObject.getString("type"))
        }
    }

    @Test
    fun `test export some settings with no filter`() {
        val json = SettingsExportHelper.getDataMatrixSettings(false, null, MockSettingKeys.mockDataDefaultList)

        // We expect 3 entries corresponding to our 3 mock settings with non-empty export string
        assertEquals(3, json.length())

        // Expected key, value, type for each setting (order matters)
        val expected = listOf(
            Triple("NESTED_MOCK_SETTING_BOOL", "true", "boolean"),
            Triple("NESTED_MOCK_SETTING_FLOAT", "1.0", "float"),
            Triple("NESTED_MOCK_SETTING_LONG", "1", "long"),
        )

        for (i in 0 until json.length()) {
            val jsonObject = json.getJSONObject(i)
            val (expectedKey, expectedValue, expectedType) = expected[i]
            assertEquals(expectedKey, jsonObject.getString("key"))
            assertEquals(expectedValue, jsonObject.getString("value"))
            assertEquals(expectedType, jsonObject.getString("type"))
        }
    }

    @Test
    fun `test export settings with a filter`() {
        val json = SettingsExportHelper.getDataMatrixSettings(false, { _, value -> value is Boolean }, MockSettingKeys.mockDataDefaultList)

        // We expect 1 entry corresponding to our 1 mock boolean setting
        assertEquals(1, json.length())

        // Expected key, value, type for each setting (order matters)
        val expected = listOf(
            Triple("NESTED_MOCK_SETTING_BOOL", "true", "boolean")
        )

        for (i in 0 until json.length()) {
            val jsonObject = json.getJSONObject(i)
            val (expectedKey, expectedValue, expectedType) = expected[i]
            assertEquals(expectedKey, jsonObject.getString("key"))
            assertEquals(expectedValue, jsonObject.getString("value"))
            assertEquals(expectedType, jsonObject.getString("type"))
        }
    }


    /*
     * Mocks SettingKeys.java
     */
    private object MockSettingKeys {
        val mockDataDefaultList = mutableListOf<DataDefault<*>>()

        init {
            for (s in NestedMockSettingKeys.entries) mockDataDefaultList.add(s.dataDefault)
        }

        enum class NestedMockSettingKeys(
            val dataDefault: DataDefault<*>,
        ){
            NESTED_MOCK_SETTING_BOOL(DataDefault(true, "all")),
            NESTED_MOCK_SETTING_FLOAT(DataDefault(1.0f,  "all")),
            NESTED_MOCK_SETTING_LONG(DataDefault(1L,  "all")),
            NESTED_MOCK_SETTING_INT(DataDefault(1)),
            NESTED_MOCK_SETTING_STRING(DataDefault("string")),
            NESTED_MOCK_SETTING_DOUBLE(DataDefault(0.5));

            init {
                dataDefault.name = name
            }
        }
    }
}
