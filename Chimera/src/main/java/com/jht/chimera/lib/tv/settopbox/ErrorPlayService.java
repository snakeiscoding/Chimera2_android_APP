package com.jht.chimera.lib.tv.settopbox;

/**
 * Created by mutombolin on 2019/02/12.
 */

public enum ErrorPlayService {
	OK,
	DisplayingDeviceSetting,
	NoSpecifiedService,
	IncorrectParameter;
	
	public int getValue() {
		return ordinal();
	}
}
