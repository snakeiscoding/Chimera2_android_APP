package com.jht.widgets.survey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurveyBridgeMessageTest {

    @Test
    fun parsesComplete() =
        assertEquals(SurveyBridgeMessage.Complete, SurveyBridgeMessage.parse("""{"type":"survey-complete"}"""))

    @Test
    fun parsesFailedWithType() =
        assertEquals(
            SurveyBridgeMessage.Failed("survey-failed-submit"),
            SurveyBridgeMessage.parse("""{"type":"survey-failed-submit","detail":"500"}"""),
        )

    @Test
    fun parsesFailedWithBarePrefix() =
        assertEquals(SurveyBridgeMessage.Failed("survey-failed-"), SurveyBridgeMessage.parse("""{"type":"survey-failed-"}"""))

    @Test
    fun unknownTypeIsUnknown() =
        assertTrue(SurveyBridgeMessage.parse("""{"type":"survey-started"}""") is SurveyBridgeMessage.Unknown)

    @Test
    fun missingTypeIsUnknown() =
        assertTrue(SurveyBridgeMessage.parse("""{"kind":"survey-complete"}""") is SurveyBridgeMessage.Unknown)

    @Test
    fun nonJsonIsUnknown() =
        assertTrue(SurveyBridgeMessage.parse("[object Object]") is SurveyBridgeMessage.Unknown)

    @Test
    fun nullIsUnknown() = assertTrue(SurveyBridgeMessage.parse(null) is SurveyBridgeMessage.Unknown)

    @Test
    fun unknownKeepsRawForLogging() =
        assertEquals(SurveyBridgeMessage.Unknown("oops"), SurveyBridgeMessage.parse("oops"))

    @Test
    fun everyMessageCloses() {
        listOf("""{"type":"survey-complete"}""", """{"type":"survey-failed-load"}""", "garbage", null)
            .forEach { assertTrue(SurveyBridgeMessage.parse(it).closesSurvey) }
    }
}
