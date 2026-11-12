package com.jht.interfaces.models

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jht.firsttimesetup.FirstTimeSetupApi
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for MatrixFirstTimeSetupModel to ensure prefix-agnostic tuple validation works correctly.
 */
@RunWith(AndroidJUnit4::class)
class MatrixFirstTimeSetupModelTest {

    private lateinit var setupModel: MatrixFirstTimeSetupModel
    
    //`relaxed = true` tells MockK to return default values instead of throwing errors if unmocked methods are called.
    private val mockApi: FirstTimeSetupApi = mockk(relaxed = true)
    
    // In Instrumented tests, we retrieve the REAL Application Context provided by Android.
    // This allows singletons like ProductConfig, SettingKeys, and FrameConfigManager to access real SharedPreferences
    // and OS resources (like Build.PRODUCT), avoiding the ExceptionInInitializerError crashes.
    private val realContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testScope = TestScope()

    @Before
    fun setUp() {
        // pass the real Android Context into the model.
        // Because we are running on an emulator, the JVM has access to the physical Android Framework,
        // eliminating the need for complex `mockkStatic` or `mockkObject` interceptions.
        setupModel = MatrixFirstTimeSetupModel(mockApi, realContext, testScope)
    }

    @org.junit.After
    fun tearDown() {
        // No static mocks to clear!
    }

    /**
     * (Legacy) The map expects CTM556, and we scan CTM556. The system should pass it.
     */
    @Test
    fun testValidLegacyConsoleAndLegacyFramePasses() {
        // From map: CEP640 console allows FCB97 frame
        val console = "CEP64012345"
        val frame = "FCB9712345"

        Assert.assertTrue(
            "Legacy tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * (Modern) drops the 'C' and 'F', barcodes safely validate against maps.
     */
    @Test
    fun testValidModernConsoleAndModernFramePassesAgainstMap() {
        // Modern barcodes without C or F
        val console = "EP64012345"
        val frame = "CB9712345"

        Assert.assertTrue(
            "Modern prefix-less tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * One barcode is new format and the other is old format.
     */
    @Test
    fun testMixedCombinationLegacyConsoleAndModernFramePasses() {
        val console = "CEP64012345" // Legacy
        val frame = "CB9712345" // Modern

        Assert.assertTrue(
            "Mixed legacy/modern tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * Same as above, ensuring the inverse logic holds true.
     */
    @Test
    fun testMixedCombinationModernConsoleAndLegacyFramePasses() {
        val console = "EP64012345" // Modern
        val frame = "FCB9712345" // Legacy

        Assert.assertTrue(
            "Mixed modern/legacy tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     *
     * The new TR prefix should pass due to our .contains() logic
     */
    @Test
    fun testTestRunPrefixedLegacyBarcodesPass() {
        val console = "TRCEP64012345"
        val frame = "TRFCB9712345"

        Assert.assertTrue(
            "TR-prefixed legacy tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * Modern TR prefixed machines should also pass.
     */
    @Test
    fun testTestRunPrefixedModernBarcodesPass() {
        val console = "TREP64012345"
        val frame = "TRCB9712345"

        Assert.assertTrue(
            "TR-prefixed modern tuple should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * Models like 'FCS37' naturally start with an F should also pass because we no longer chop the F.
     */
    @Test
    fun testNativeFOrCBaseModelsPassWithoutDestructiveChopping() {
        // Map: CEP640 allows FCS37
        val console = "CEP64012345"
        val frame = "FCS3712345" // This is the base model name

        Assert.assertTrue(
            "Native F-model frame should not be chopped and should be valid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * "Vortex on a Bike" Both serials are mathematically valid individually, but the combination is illegal.
     * The `isSerialNumberTupleValid` acts as a firewall to prevent this.
     */
    @Test
    fun testInvalidCombinationOfValidConsoleAndValidFrameFails() {
        // Map: CEP640 allows FCB97. CEP641 allows CB206.
        // We will mix CEP640 console with a CB206 frame.
        val console = "CEP64012345"
        val frame = "CB20612345" // Valid frame, but NOT for this console

        Assert.assertFalse(
            "Vortex on a bike should be invalid",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * Validates failure handling for garbage data.
     */
    @Test
    fun testInvalidStringsFail() {
        val console = "ZZZ123456"
        val frame = "XXX123456"

        Assert.assertFalse(
            "Garbage strings should fail",
            setupModel.isSerialNumberTupleValid(console, frame)
        )
    }

    /**
     * Ensures the null/blank safety check at the top of the function works.
     */
    @Test
    fun testBlankInputsFail() {
        Assert.assertFalse("Blank strings should fail", setupModel.isSerialNumberTupleValid("", ""))
        Assert.assertFalse(
            "Blank console should fail",
            setupModel.isSerialNumberTupleValid("", "FCB97123")
        )
        Assert.assertFalse(
            "Blank frame should fail",
            setupModel.isSerialNumberTupleValid("CEP640123", "")
        )
    }
}