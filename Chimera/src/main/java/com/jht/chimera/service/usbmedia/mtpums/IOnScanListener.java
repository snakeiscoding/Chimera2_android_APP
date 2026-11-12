package com.jht.chimera.service.usbmedia.mtpums;

import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;

public interface IOnScanListener {
    void onOneCompletion(Track track, boolean isVideo);
    void onAllCompletion();
}
