@file:OptIn(ExperimentalCoroutinesApi::class)

package com.jht.firsttimesetup

import com.jht.firsttimesetup.ui.Segment
import com.jht.firsttimesetup.ui.SegmentState
import com.jht.firsttimesetup.ui.SegmentType
import com.jht.firsttimesetup.ui.SetupState
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.FirstTimeSetupViewModel
import com.jht.interfaces.models.SystemModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.models.MockUIBaseModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import java.lang.Thread.sleep


class FirstTimeSetupViewModelTest {

    private val ftsuModel = MockFirstTimeSetupModel()

    private lateinit var viewModel: FirstTimeSetupViewModel

    @MockK
    private lateinit var systemModel: SystemModel

    @MockK
    private lateinit var uiModel: MockUIBaseModel

    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private var longWait: Long = 0// for waiting more than required time
    private var shortWait: Long = 0// for waiting less than required time but not going instantly

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        MockKAnnotations.init(this)

        every { systemModel.lcbType } returns 1 // enables machine calibration
        every { systemModel.languageList } returns emptyList()
        every { systemModel.serialNumberFrame } returns "CellMartiniFrame"
        every { uiModel.isDemoMode } returns MockBoolValue(false)

        ftsuModel.canCalibrate = true
        viewModel = FirstTimeSetupViewModel(
            systemModel = systemModel,
            ftsuModel = ftsuModel,
            ioDispatcher = dispatcher,
            mainDispatcher = dispatcher,
            uiModel = uiModel
        )
        viewModel.ignoreNextButtonTimeout = true
        longWait = viewModel.MILLIS_TO_WATI_FOR_NEXT_MOVE.toLong() * 2.toLong()
        shortWait = viewModel.MILLIS_TO_WATI_FOR_NEXT_MOVE.toLong() / 2
    }

    /**
     *  A -> B ->  C -> D
     */
    @Test
    fun `Given a simple graph we expect onNextClicked navigation to navigate properly`() {
        val simpleGraph = mutableListOf(
            Segment(type = TestSegmentType.A),
            Segment(type = TestSegmentType.B),
            Segment(type = TestSegmentType.C),
            Segment(type = TestSegmentType.D),
        )
        viewModel.setupList = simpleGraph
        viewModel.setupGraphForTesting(simpleGraph) //called to reset list

        assertEquals(TestSegmentType.A, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.B, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.C, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.D, viewModel.uiState.value.currentSegment.type)
    }

    /**
     *  A -> B ->  C -> D
     *  |    |
     *  E   H
     *  |
     *  F
     *  |
     *  G
     */
    @Test
    fun `Given a the following graph we expect onNextClicked navigation to navigate properly`() {
        val simpleGraph = mutableListOf(
            //A
            Segment(type = TestSegmentType.A).apply {
                this.subsegments = listOf(
                    Segment(type = TestSegmentType.E, parent = this).also { e ->
                        e.subsegments = listOf(
                            Segment(type = TestSegmentType.F).also { f ->
                                f.subsegments = listOf(
                                    Segment(type = TestSegmentType.G)
                                )
                            }
                        )
                    }
                )
            },
            Segment(type = TestSegmentType.B).apply {
                this.subsegments = listOf(Segment(type = TestSegmentType.H, parent = this))
            },
            Segment(type = TestSegmentType.C),
            Segment(type = TestSegmentType.D),
        )

        viewModel.setupList = simpleGraph
        viewModel.setupGraphForTesting(simpleGraph) //called to reset list
        assertEquals(TestSegmentType.G, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.H, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.C, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.D, viewModel.uiState.value.currentSegment.type)
    }

    /**
     * A - I
     * |
     * B - C - J
     *     |
     *     D - E
     *         |
     *         F - G
     *             |
     *             H
     */
    @Test
    fun `Given a the following graph we expect onNextClicked navigation to navigate properly test 2`() {
        val simpleGraph = mutableListOf(
            Segment(type = TestSegmentType.A).apply {
                this.subsegments = listOf(
                    Segment(type = TestSegmentType.B, parent = this),
                    Segment(type = TestSegmentType.C, parent = this).also { c ->
                        c.subsegments = listOf(
                            Segment(type = TestSegmentType.D, parent = c),
                            Segment(type = TestSegmentType.E, parent = c).also { e ->
                                e.subsegments = listOf(
                                    Segment(type = TestSegmentType.F, parent = e),
                                    Segment(type = TestSegmentType.G, parent = e).also { g ->
                                        g.subsegments = listOf(
                                            Segment(type = TestSegmentType.H, parent = g)
                                        )
                                    }
                                )

                            }
                        )
                    }
                )
            },
            Segment(type = TestSegmentType.I)
        )

        viewModel.setupList = simpleGraph
        viewModel.setupGraphForTesting(simpleGraph) //called to reset list
        assertEquals(TestSegmentType.B, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.D, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.F, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.H, viewModel.uiState.value.currentSegment.type)

        viewModel.moveToNextSegment()
        assertEquals(TestSegmentType.I, viewModel.uiState.value.currentSegment.type)
    }


    @Test
    fun `Given a frame can be calibrated we expect to have a calibration segment`() {
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_TUNER)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_IMPORT_CHANNELS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_EDIT_CHANNELS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.MACHINE_CALIBRATION)
    }


    @Test
    fun `Given a frame cannot be calibrated we expect to navigate to the next segment`() {
        ftsuModel.canCalibrate = false
        viewModel.completeFrameSetup()

        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_TUNER)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_IMPORT_CHANNELS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_EDIT_CHANNELS)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.SUMMARY, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Given canCalibrate is false then we expect no calibration segment`() =
        runTest(dispatcher) {
            ftsuModel.canCalibrate = false

            viewModel = FirstTimeSetupViewModel(
                systemModel,
                ftsuModel,
                ioDispatcher = dispatcher,
                mainDispatcher = dispatcher,
                uiModel = uiModel
            )

            assert(viewModel.setupList.size == 5)

            val containsMachineCalibration =
                viewModel.setupList.indexOfFirst { it.type == SetupState.MACHINE_CALIBRATION }

            assert(containsMachineCalibration == -1)
        }

    @Test
    fun `Given navigateToSegment and markPreviousSegmentsAsComplete as true we expect our setuplist to reflect those changes`() =
        runTest(dispatcher) {
            assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)
            viewModel.navigateToSegment(SetupState.SUMMARY, true)
            assert(viewModel.uiState.value.currentSegment.type == SetupState.SUMMARY)
            //language
            val languageIndex = viewModel.setupList.indexOfFirst { it.type == SetupState.LANGUAGE }
            val languageSegment = viewModel.setupList[languageIndex]
            assert(languageSegment.state == SegmentState.COMPLETED)
            //network
            val networkIndex = viewModel.setupList.indexOfFirst { it.type == SetupState.NETWORK }
            val networkSegment = viewModel.setupList[networkIndex]
            assert(networkSegment.state == SegmentState.COMPLETED)

            // facility id
            val facilityIdIndex =
                viewModel.setupList.indexOfFirst { it.type == SetupState.FACILITY_ID }
            val facilitySegment = viewModel.setupList[facilityIdIndex]
            assert(facilitySegment.state == SegmentState.COMPLETED)

            // settings
            val settingsIndex = viewModel.setupList.indexOfFirst { it.type == SetupState.SETTINGS }
            val settingsSegment = viewModel.setupList[settingsIndex]
            assert(settingsSegment.state == SegmentState.COMPLETED)


            // machine calibration
            val machineIndex = viewModel.setupList.indexOfFirst { it.type == SetupState.SETTINGS }
            val machineSegment = viewModel.setupList[machineIndex]
            assert(machineSegment.state == SegmentState.COMPLETED)
        }

    @Test
    fun `Given the default setup list using moveToNextSegment() navigates the whole tree`() {
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_TUNER)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_IMPORT_CHANNELS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_EDIT_CHANNELS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.MACHINE_CALIBRATION)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.SUMMARY)

        val rootSegment = viewModel.setupList[3]
        viewModel.resetSegment(rootSegment)
    }

    @Test
    fun `Given a segment using completeSegment() changes the segment and the subsegments to completed`() {
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.completeSegment(viewModel.setupList[viewModel.uiState.value.segmentIndex])
        assertEquals(
            SegmentState.COMPLETED,
            viewModel.setupList[viewModel.uiState.value.segmentIndex].state
        )

        val newSeg = viewModel.navigateTree()
        assertEquals(SetupState.NETWORK, newSeg?.type)

        viewModel.moveToNextSegment()
        viewModel.moveToNextSegment()
        viewModel.moveToNextSegment()

        val tvSegment = viewModel.uiState.value.currentSegment
        assertEquals(SetupState.TV_SETUP_TUNER, tvSegment.type)

        val parent = tvSegment.parent!!
        viewModel.completeSegment(parent)

        assertEquals(SegmentState.COMPLETED, parent.state)
        parent.subsegments.forEach { segment ->
            assertEquals(SegmentState.COMPLETED, segment.state)
        }
    }

    @Test
    fun `Given a Segment list using moveToNextAuntNode() moves to the next Aunt Segment`(){
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_TUNER)

        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.MACHINE_CALIBRATION, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Given the setupList calling moveToNextAuntNode() when there is parent to current segment moves to next segment`(){
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.NETWORK, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Testing move to next segment while waiting the required time`(){
        viewModel.ignoreNextButtonTimeout = false

        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.LANGUAGE, viewModel.uiState.value.currentSegment.type)

        sleep(longWait)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.NETWORK, viewModel.uiState.value.currentSegment.type)
        viewModel.moveToNextSegment()
        assertEquals(SetupState.NETWORK, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Testing move to next segment while waiting less then the required time`(){
        viewModel.ignoreNextButtonTimeout = false

        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.LANGUAGE, viewModel.uiState.value.currentSegment.type)

        sleep(shortWait)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.LANGUAGE, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Testing move to next Aunt segment while waiting the required time`() {
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assertEquals(SetupState.TV_SETUP_TUNER, viewModel.uiState.value.currentSegment.type)

        viewModel.ignoreNextButtonTimeout = false

        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.TV_SETUP_TUNER, viewModel.uiState.value.currentSegment.type)

        sleep(longWait)

        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.MACHINE_CALIBRATION, viewModel.uiState.value.currentSegment.type)
    }

    @Test
    fun `Testing move to next Aunt segment while waiting less than the required time`() {
        viewModel.completeFrameSetup()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.LANGUAGE)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.NETWORK)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.FACILITY_ID)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.GENERAL_SETTINGS)

        viewModel.moveToNextSegment()
        assert(viewModel.uiState.value.currentSegment.type == SetupState.TV_SETUP_TUNER)

        viewModel.ignoreNextButtonTimeout = false
        sleep(shortWait)

        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.TV_SETUP_TUNER, viewModel.uiState.value.currentSegment.type)

        sleep(longWait)
        viewModel.moveToNextAuntNode()
        assertEquals(SetupState.MACHINE_CALIBRATION, viewModel.uiState.value.currentSegment.type)
    }

    enum class TestSegmentType : SegmentType {
        A {
            override val nameResourceId: Int
                get() = 0
        },
        B {
            override val nameResourceId: Int
                get() = 1
        },
        C {
            override val nameResourceId: Int
                get() = 2
        },
        D {
            override val nameResourceId: Int
                get() = 3
        },
        E {
            override val nameResourceId: Int
                get() = 4
        },
        F {
            override val nameResourceId: Int
                get() = 5
        },
        G {
            override val nameResourceId: Int
                get() = 6
        },
        H {
            override val nameResourceId: Int
                get() = 7
        },
        I {
            override val nameResourceId: Int
                get() = 8
        },
        J {
            override val nameResourceId: Int
                get() = 9
        },
        K {
            override val nameResourceId: Int
                get() = 10
        },
        L {
            override val nameResourceId: Int
                get() = 11
        },
        M {
            override val nameResourceId: Int
                get() = 12
        },
        N {
            override val nameResourceId: Int
                get() = 13
        },
        O {
            override val nameResourceId: Int
                get() = 14
        },
        P {
            override val nameResourceId: Int
                get() = 15
        },
        Q {
            override val nameResourceId: Int
                get() = 16
        },
        R {
            override val nameResourceId: Int
                get() = 17
        },
        S {
            override val nameResourceId: Int
                get() = 18
        },
        T {
            override val nameResourceId: Int
                get() = 19
        },
        U {
            override val nameResourceId: Int
                get() = 20
        },
        V {
            override val nameResourceId: Int
                get() = 21
        },
        W {
            override val nameResourceId: Int
                get() = 22
        },
        X {
            override val nameResourceId: Int
                get() = 23
        },
        Y {
            override val nameResourceId: Int
                get() = 24
        },
        Z {
            override val nameResourceId: Int
                get() = 25
        }
    }

    private fun facility(id: String) = Facility(
        facilityId = id,
        name = "Facility $id",
        location = null,
        digitalProducts = emptyList(),
        timeZoneIndex = 9,
        systemSettings = null,
    )

    @Test
    fun `a new search result replaces the previously imported facility`() {
        viewModel.onSearchedFacilityChanged(facility("205"))
        viewModel.facilityProfile = facility("205") // user pressed Import Facility Profile

        viewModel.onSearchedFacilityChanged(facility("63"))

        assertEquals(null, viewModel.facilityProfile)
    }

    @Test
    fun `the same search result again keeps the imported facility`() {
        viewModel.onSearchedFacilityChanged(facility("205"))
        viewModel.facilityProfile = facility("205")

        viewModel.onSearchedFacilityChanged(facility("205")) // e.g. recomposition

        assertEquals("205", viewModel.facilityProfile?.facilityId)
    }

    @Test
    fun `no search result keeps the imported facility`() {
        viewModel.facilityProfile = facility("205") // e.g. imported from USB

        viewModel.onSearchedFacilityChanged(null)

        assertEquals("205", viewModel.facilityProfile?.facilityId)
    }

    @Test
    fun `an import after the search is kept`() {
        viewModel.onSearchedFacilityChanged(facility("63"))
        viewModel.facilityProfile = facility("205") // e.g. imported from USB after searching

        viewModel.onSearchedFacilityChanged(facility("63"))

        assertEquals("205", viewModel.facilityProfile?.facilityId)
    }
}
