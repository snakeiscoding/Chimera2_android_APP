package com.jht.chimera.service.gattserver2.services

import com.jht.androidcommonalgorithms.timer.PeriodicTimer
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.services.CscService

class CscServiceConsole(server: IGattServer) : CscService(server) {
    private val cscTimer = PeriodicTimer(0, 1000) { sendCscData() }
    private val dataMatrix = DataMatrix()
    override fun start() {
        // setup value listeners for DataMatrix
        dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT) { rpm: Int -> cadenceUpdated(rpm) }
        cscTimer.start()
    }

    override fun stop() {
        // clean up DataMatrix
        dataMatrix.finish()
        cscTimer.stop()
    }
}
