package com.jht.chimera.lib.tv.settopbox;

/**
 * Created by mutombolin on 2019/02/12.
 */

public enum ErrorReboot {
	OK; // Schedule reboot. Start in 3 sec.
	
	public int getValue() {
		return ordinal();
	}
}
