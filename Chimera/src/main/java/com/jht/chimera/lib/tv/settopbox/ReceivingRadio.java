package com.jht.chimera.lib.tv.settopbox;

/**
 * Created by mutombolin on 2019/02/12.
 */

public enum ReceivingRadio {
	UNKNOWN(0),
	DTB(1),
	BS(2),
	CS(3);
	
	private final int value;
	
	ReceivingRadio(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
