package com.jht.chimera.service.systemsettings.tv_channel_manager.accessors

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import io.mockk.mockk
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Serializable

@RunWith(JUnitParamsRunner::class)
class ChannelListAccessorFactoryTest {
    private val context: Context = mockk()

    @Test
    @Parameters(method = "getSupportedTvSourceTestCases")
    fun testSupportedTvSources(tvSource: TVSource, expectedChannelListAccessor: Class<ChannelListAccessor>) {
        try {
            val channelListAccessor = ChannelListAccessorFactory.create(tvSource, context)
            assertTrue(
                "Expected $expectedChannelListAccessor for TV source $tvSource but got ${channelListAccessor::class.java} instead.",
                expectedChannelListAccessor.isInstance(channelListAccessor)
            )
        } catch (_: ChannelListAccessorFactory.UnsupportedTvSourceException) {
            fail("Expected $expectedChannelListAccessor for TV source $tvSource but got UnsupportedTvSourceException instead.")
        }
    }

    @Test
    @Parameters(method = "getUnsupportedTvSourceTestCases")
    fun testUnsupportedTvSources(tvSource: TVSource) {
        try {
            val channelListAccessor = ChannelListAccessorFactory.create(tvSource, context)
            fail("Expected UnsupportedTvSourceException for TV source $tvSource but got ${channelListAccessor::class.java} instead.")
        } catch (_: ChannelListAccessorFactory.UnsupportedTvSourceException) {}
    }

    private companion object {
        // Map of supported TV sources and their corresponding ChannelListAccessor class
        // Used to determine which TV sources are supported or not
        private val supportedTvSources: Map<TVSource, Class<out ChannelListAccessor>> = mapOf(
            TVSource.KM6 to LgChannelListAccessor::class.java,
            TVSource.KM7 to LgChannelListAccessor::class.java,
            TVSource.US8 to LgChannelListAccessor::class.java,
            TVSource.US9 to LgChannelListAccessor::class.java,
            TVSource.BR3 to LgChannelListAccessor::class.java,
            TVSource.JP1 to LgChannelListAccessor::class.java,
        )

        @JvmStatic
        @Suppress("unused")
        private fun getSupportedTvSourceTestCases(): Array<Array<Serializable>> {
            return supportedTvSources.map { arrayOf(it.key, it.value) }.toTypedArray()
        }

        @JvmStatic
        @Suppress("unused")
        private fun getUnsupportedTvSourceTestCases(): Array<Serializable> {
            return TVSource.entries.filter { it !in supportedTvSources.keys }.map { arrayOf(it) }.toTypedArray()
        }
    }
}