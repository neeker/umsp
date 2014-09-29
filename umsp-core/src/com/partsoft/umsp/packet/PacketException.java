package com.partsoft.umsp.packet;

import java.io.IOException;

public class PacketException extends IOException {

	private static final long serialVersionUID = -2894919263014224774L;

	public PacketException(String reason) {
		super(reason);
	}
	
	public PacketException(Throwable e) {
		super(e);
	}

	public PacketException(String reason, Throwable e) {
		super(reason, e);
	}

	public PacketException(int size, int offset) {
		super("offset(" + offset + ") is out of range[0," + size + ")");
	}

	public PacketException(int size, int position, int offset) {
		super("offset(" + offset + ") is out of range[" + position + "," + size + ")");
	}
	
}