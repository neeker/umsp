package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class QueryResponse extends CmppDataPacket {
	
	private static final long serialVersionUID = 0x80000006L;
	
	public String time;

	public byte query_type;

	public String query_code;

	public int mt_message_total;

	public int mt_user_total;

	public int mt_success_total;

	public int mt_wait_total;

	public int mt_fail_total;

	public int mo_message_total;

	public int mo_wat_total;

	public int mo_fail_total;

	public QueryResponse() {
		super(Commands.CMPP_QUERY_RESP);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, this.time, 8);
		out.writeByte(query_type);
		writeFixedString(out, this.query_code, 10);
		out.writeInt(this.mt_message_total);
		out.writeInt(this.mt_user_total);
		out.writeInt(this.mt_success_total);
		out.writeInt(this.mt_wait_total);
		out.writeInt(this.mt_fail_total);
		out.writeInt(this.mo_message_total);
		out.writeInt(this.mo_wat_total);
		out.writeInt(this.mo_fail_total);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		this.time = readFixedString(in, 8);
		this.query_type = in.readByte();
		this.query_code = readFixedString(in, 10);
		this.mt_message_total = in.readInt();
		this.mt_user_total = in.readInt();
		this.mt_success_total = in.readInt();
		this.mt_wait_total = in.readInt();
		this.mt_fail_total = in.readInt();
		this.mo_message_total = in.readInt();
		this.mo_wat_total = in.readInt();
		this.mo_fail_total = in.readInt();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 51;
	}

	@Override
	public QueryResponse clone() {
		return (QueryResponse) super.clone();
	}

}
