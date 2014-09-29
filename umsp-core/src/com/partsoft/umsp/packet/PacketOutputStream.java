package com.partsoft.umsp.packet;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;

public class PacketOutputStream extends DataOutputStream implements DataOutput {

	public PacketOutputStream(OutputStream out) {
		super(out);
	}
	
}
