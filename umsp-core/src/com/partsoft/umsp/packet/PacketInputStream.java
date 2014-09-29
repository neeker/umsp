package com.partsoft.umsp.packet;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;

public class PacketInputStream extends DataInputStream implements DataInput {

	public PacketInputStream(InputStream in) {
		super(in);
	}

}
