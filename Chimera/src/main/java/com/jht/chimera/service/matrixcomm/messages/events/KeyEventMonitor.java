package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Monitor for key presses and send those events to third parties.
 */
public class KeyEventMonitor {
    public KeyEventMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {
        dataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, bitmapKey -> handler.post(() ->
                server.send(new KeyEvent(
                        KeyPress.getKeyCode(bitmapKey).matrixcommName,
                        KeyPress.getKeyState(bitmapKey).equals(KeyPress.KeyState.KEY_UP) ?
                                KeyEvent.KeyState.RELEASED : KeyEvent.KeyState.PRESSED).getRequest())));
    }
}
