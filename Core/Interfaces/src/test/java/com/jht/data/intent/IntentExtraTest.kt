package com.jht.data.intent

import org.junit.Assert
import org.junit.Test

/**
 * These run as plain JVM tests - no Bundle is ever materialized here, which is rather the point of
 * declaring extras as data instead of holding an android Bundle.
 */
class IntentExtraTest {

    private val panelOverrides = IntentExtras.of(
        IntentExtras.bundle(
            "com.jht.panels.overrides",
            IntentExtras.bool("com.jht.forceHidePausePanel", true),
        )
    )

    @Test
    fun jsonRoundTripPreservesTypes() {
        val extras = IntentExtras.of(
            IntentExtras.string("str", "hello"),
            IntentExtras.bool("flag", true),
            IntentExtras.integer("count", 5),
            IntentExtras.longValue("big", 9_007_199_254_740_993L), // past 2^53, would be lossy as a double
            IntentExtras.floatValue("ratio", 1.5f),
            IntentExtras.stringArray("names", "a", "b"),
        )

        Assert.assertEquals(extras, IntentExtras.fromJson(IntentExtras.toJson(extras)))
    }

    @Test
    fun nestedBundleExtrasRoundTrip() {
        Assert.assertEquals(
            panelOverrides,
            IntentExtras.fromJson(IntentExtras.toJson(panelOverrides))
        )
    }

    @Test
    fun jsonCarriesTypeDiscriminator() {
        val json = IntentExtras.toJson(panelOverrides)
        Assert.assertTrue(json, json.contains("\"type\":\"bundle\""))
        Assert.assertTrue(json, json.contains("\"type\":\"bool\""))
        Assert.assertTrue(json, json.contains("\"value\":true")) // real boolean, not "true"
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """[{"type":"bool","key":"flag","value":true,"somethingNewer":1}]"""
        Assert.assertEquals(
            IntentExtras.of(IntentExtras.bool("flag", true)),
            IntentExtras.fromJson(json)
        )
    }

    @Test
    fun equalityIsStructural() {
        val other = IntentExtras.of(
            IntentExtras.bundle(
                "com.jht.panels.overrides",
                IntentExtras.bool("com.jht.forceHidePausePanel", true),
            )
        )
        Assert.assertEquals(panelOverrides, other)
        Assert.assertEquals(panelOverrides.hashCode(), other.hashCode())
    }

    @Test
    fun toStringIsReadableForLogging() {
        // Bundle.toString() would be "Bundle[mParcelledData.dataSize=...]" in a console log
        Assert.assertTrue(
            panelOverrides.toString(),
            panelOverrides.toString().contains("forceHidePausePanel")
        )
    }

    // ---- dynamic values -------------------------------------------------------------------

    /** stands in for the console: units are imperial, and this build is not commercial */
    private val testResolver = DynamicExtraResolver { key, source ->
        when (source) {
            DynamicExtraValue.UnitsAreImperial -> BoolExtra(key, true)
            DynamicExtraValue.BrandingName -> StringExtra(key, "Matrix")
            DynamicExtraValue.CommercialFlag -> null // omitted on a non commercial console
        }
    }

    private val ifitExtras = IntentExtras.of(
        IntentExtras.bool("com.ifit.extra.IS_MATRIX", true),
        IntentExtras.dynamic("com.ifit.extra.IS_DEFAULT_UNIT_IMPERIAL", DynamicExtraValue.UnitsAreImperial),
        IntentExtras.dynamic("is_commercial", DynamicExtraValue.CommercialFlag),
    )

    @Test
    fun dynamicDeclarationsCarryTheSourceNotAValue() {
        val json = IntentExtras.toJson(ifitExtras)
        Assert.assertTrue(json, json.contains("\"type\":\"dynamic\""))
        Assert.assertTrue(json, json.contains("\"source\":\"UnitsAreImperial\""))
        Assert.assertEquals(ifitExtras, IntentExtras.fromJson(json))
    }

    @Test
    fun resolveReplacesDynamicAndDropsOmitted() {
        val resolved = ifitExtras.resolve(testResolver)
        Assert.assertEquals(
            listOf(
                BoolExtra("com.ifit.extra.IS_MATRIX", true),          // static, untouched
                BoolExtra("com.ifit.extra.IS_DEFAULT_UNIT_IMPERIAL", true), // filled in
            ),
            resolved, // is_commercial dropped entirely rather than sent as false
        )
    }

    @Test
    fun resolveRecursesIntoNestedBundles() {
        val extras = IntentExtras.of(
            IntentExtras.bundle("extras", IntentExtras.dynamic("brand", DynamicExtraValue.BrandingName))
        )
        Assert.assertEquals(
            listOf(BundleExtra("extras", listOf(StringExtra("brand", "Matrix")))),
            extras.resolve(testResolver),
        )
    }

    @Test
    fun resolveIsANoOpForPurelyStaticDeclarations() {
        Assert.assertEquals(panelOverrides, panelOverrides.resolve(testResolver))
    }

    @Test
    fun unknownSourceNameSurvivesDecodingVerbatim() {
        val json = """[{"type":"dynamic","key":"com.partner.units","source":"UnitsAreImperal"}]"""
        val decoded = IntentExtras.fromJson(json) // typo'd name must not fail the parse
        Assert.assertEquals(listOf(DynamicExtra("com.partner.units", "UnitsAreImperal")), decoded)
        Assert.assertNull((decoded[0] as DynamicExtra).dynamicExtraValue)
    }

    @Test
    fun unknownSourcesNamesTheOffender() {
        val extras = IntentExtras.of(
            IntentExtras.dynamic("known", DynamicExtraValue.BrandingName),
            DynamicExtra("com.partner.units", "UnitsAreImperal"),
            IntentExtras.bundle("extras", DynamicExtra("nested", "SomethingNewer")),
        )
        Assert.assertEquals(
            listOf("com.partner.units=UnitsAreImperal", "nested=SomethingNewer"),
            extras.unknownSources(),
        )
    }

    @Test
    fun resolveDropsUnknownSourcesAndKeepsTheRest() {
        val extras = IntentExtras.of(
            IntentExtras.dynamic("brand", DynamicExtraValue.BrandingName),
            DynamicExtra("com.partner.units", "UnitsAreImperal"),
        )
        Assert.assertEquals(listOf(StringExtra("brand", "Matrix")), extras.resolve(testResolver))
    }

    @Test
    fun bundleWhoseChildrenAllResolveAwayIsDropped() {
        // CommercialFlag resolves to null on this console, leaving the bundle with nothing to say
        val extras = IntentExtras.of(
            IntentExtras.bundle("overrides", IntentExtras.dynamic("club", DynamicExtraValue.CommercialFlag))
        )
        Assert.assertEquals(emptyList<IntentExtra>(), extras.resolve(testResolver))
    }

    @Test
    fun emptyBundlesPruneUpwardThroughNesting() {
        val extras = IntentExtras.of(
            IntentExtras.bundle("outer",
                IntentExtras.bundle("inner", IntentExtras.dynamic("club", DynamicExtraValue.CommercialFlag)))
        )
        Assert.assertEquals(emptyList<IntentExtra>(), extras.resolve(testResolver))
    }

    @Test
    fun bundleKeepsSurvivingChildren() {
        val extras = IntentExtras.of(
            IntentExtras.bundle("overrides",
                IntentExtras.dynamic("club", DynamicExtraValue.CommercialFlag),
                IntentExtras.bool("hidePause", true))
        )
        Assert.assertEquals(
            listOf(BundleExtra("overrides", listOf(BoolExtra("hidePause", true)))),
            extras.resolve(testResolver),
        )
    }

    // ---- forward compatibility ------------------------------------------------------------

    @Test
    fun unknownTypeDoesNotTakeOutTheEntriesBesideIt() {
        val json = """[
            {"type":"bool","key":"com.ifit.extra.IS_MATRIX","value":true},
            {"type":"somethingNewer","key":"com.partner.thing","value":5},
            {"type":"string","key":"brand","value":"Matrix"}
        ]"""
        val decoded = IntentExtras.fromJson(json)
        Assert.assertEquals(
            listOf(
                BoolExtra("com.ifit.extra.IS_MATRIX", true),
                UnknownExtra("com.partner.thing", "somethingNewer"),
                StringExtra("brand", "Matrix"),
            ),
            decoded,
        )
    }

    @Test
    fun unknownTypesNamesTheOffenderIncludingNested() {
        val json = """[
            {"type":"somethingNewer","key":"top"},
            {"type":"bundle","key":"overrides","children":[{"type":"alsoNewer","key":"nested"}]}
        ]"""
        Assert.assertEquals(
            listOf("top=somethingNewer", "nested=alsoNewer"),
            IntentExtras.fromJson(json).unknownTypes(),
        )
    }

    @Test
    fun malformedJsonStillThrows() {
        // unknown vocabulary degrades, broken syntax does not - that distinction is the contract
        try {
            IntentExtras.fromJson("[{\"type\":\"bool\",")
            Assert.fail("expected malformed json to throw")
        } catch (expected: Exception) {
            // expected
        }
    }
}
