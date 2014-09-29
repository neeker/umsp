package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.umsp.packet.PacketInputStream;

public class ConnectResponse extends CmppDataPacket {

	private static final long serialVersionUID = 0x80000001L;

	public int status;

	public String authenticationToken;

	public ConnectResponse() {
		super(Commands.CMPP_CONNECT_RESP);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		if (this.protocolVersion == Constants.VERSION2) {
			out.writeByte(this.status);
		} else {
			out.writeInt(this.status);
		}
		writeFixedString(out, this.authenticationToken, 16);
		out.writeByte(this.protocolVersion);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		PacketInputStream packet_stream = null;
		if (in instanceof PacketInputStream) {
			packet_stream = (PacketInputStream) in;
			packet_stream.mark(30);
		}
		try {
			this.status = in.readInt();
			this.authenticationToken = readFixedString(in, 16);
			this.protocolVersion = in.readUnsignedByte();

			if (packet_stream != null) {
				packet_stream.reset();
				packet_stream.skip(getDataSize());
			}
		} catch (IOException e) {
			// 读取出错啦？ 可能是CMPP2.0
			if (packet_stream != null) {
				packet_stream.reset();
			}
			this.status = in.readUnsignedByte();
			this.authenticationToken = readFixedString(in, 16);
			this.protocolVersion = in.readUnsignedByte();
		}
	}

	@Override
	public int getDataSize() {
		int result = super.getDataSize();
		if (this.protocolVersion == Constants.VERSION3) {
			result += 21;
		} else {
			result += 18;
		}
		return result;
	}

	@Override
	public ConnectResponse clone() {
		return (ConnectResponse) super.clone();
	}

	@Override
	public String toString() {
		return "移动连接应答包 [应答状态=" + status + ", 协商令牌=" + authenticationToken + ", 协议版本=" + protocolVersion + "]";
	}

}
