package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.datamatrixlib.DataMatrix

/** Every console-side command send goes through one of these, so only [Lg750EConnection] touches the WebSocket. */
fun interface Lg750ECommandSender {
    fun send(command: Lg750ECommand)

    companion object {
        /**
         * Relays the command over DataMatrix to [Lg750EConnection] in the HAL service, so it works
         * from any process. Uses `put` because a repeated command, such as a second volume-up
         * press, must still be sent.
         */
        @JvmField
        val DATA_MATRIX = Lg750ECommandSender { command ->
            DataMatrix.putString(TVKeys.ACTION_SEND_TUNER_COMMAND_STRING, Lg750ECommand.toRelay(command))
        }
    }
}
