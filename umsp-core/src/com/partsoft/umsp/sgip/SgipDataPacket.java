package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.DataPacket;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.packet.AbstractDataPacket;
import com.partsoft.utils.Assert;
import com.partsoft.utils.HexUtils;

public abstract class SgipDataPacket extends AbstractDataPacket implements DataPacket, Cloneable {

	private static final long serialVersionUID = -6138701092774894007L;

	public static final int RESERVE_SIZE = 8;

	protected static final int PACKET_SIZE = 4 * Buffer.INT_SIZE + RESERVE_SIZE;

	protected final int command;

	// 3个4位长度的序列号，由node_id， 时间戳，顺序号组成
	public int node_id;

	public int timestamp;

	public int sequence;

	public String reserve = "00000000";

	public boolean enabled = true;

	protected SgipDataPacket(int command) {
		super();
		this.command = command;
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		if (out == null)
			new IOException();
		out.writeInt(command);
		out.writeInt(node_id);
		out.writeInt(timestamp);
		out.writeInt(sequence);
	}

	protected void readDataInput(DataInput in) throws IOException {
		if (in == null)
			new IOException();
		int tmp_command = in.readInt();
		Assert.isTrue(tmp_command == this.command);
		node_id = in.readInt();
		timestamp = in.readInt();
		sequence = in.readInt();
	}

	public int getCommand() {
		return command;
	}

	@Override
	public SgipDataPacket clone() {
		SgipDataPacket new_obj = (SgipDataPacket) super.clone();
		return new_obj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + command;
		result = prime * result + node_id;
		result = prime * result + sequence;
		return result;
	}

	public int getDataSize() {
		return PACKET_SIZE;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SgipDataPacket other = (SgipDataPacket) obj;
		if (command != other.command)
			return false;
		if (node_id != other.node_id)
			return false;
		if (sequence != other.sequence)
			return false;
		return true;
	}

	public int getBufferSize() {
		return getDataSize() + Buffer.INT_SIZE;
	}

	public String getId() {
		return HexUtils.toHex(node_id) + "-" + HexUtils.toHex(timestamp) + "-" + HexUtils.toHex(sequence);
	}
	
	public int getCommandID() {
		return this.command;
	}

	@Override
	public String toString() {
		return "联通SGIP协议包 [指令=" + command + ", 节点号=" + node_id + ", 时间戳=" + timestamp + ", 序号=" + sequence + "]";
	}

}
