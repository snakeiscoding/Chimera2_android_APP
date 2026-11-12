/**
 * Johnson Health Tech
 * 1600 Landmark Drive
 * Cottage Grove, WI  53527
 * 
 * Copyright (C) 2014
 * Proprietary and confidential
 * 
 * @author Blong.Thor
 */

package com.jht.chimera.service.usbmedia.mtpums.lib.mtp;

/**
 * Listener event handler interface for the content server.
 * 
 * @author blong.thor
 *
 */
public abstract class ContentServerListener
{
    abstract public boolean mediaRequest(String mediaName);
    abstract public boolean mediaRequest(String mediaName, int offset);
    abstract public boolean mediaRequest(int handle, int offset);
    abstract public void mediaStopped();
    abstract public int getMediaSize();
    abstract public byte[] getMediaData();
    abstract public boolean isFinish();
}
