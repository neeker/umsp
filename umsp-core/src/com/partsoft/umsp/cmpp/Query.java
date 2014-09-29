package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class Query extends CmppDataPacket {

	private static final long serialVersionUID = 6L;
	
	public String time;
	
	public byte query_type;
	
	public String query_code;
	
	public String reserve;
	
	public Query() {
		super(Commands.CMPP_QUERY);
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, this.time, 8);
		out.writeByte(query_type);
		writeFixedString(out, this.query_code, 10);
		writeFixedString(out, this.reserve, 8);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		this.time = readFixedString(in, 8);
		this.query_type = in.readByte();
		this.query_code = readFixedString(in, 10);
		this.reserve = readFixedString(in, 8);
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 27;
	}
	
	@Override
	public Query clone() {
		return (Query) super.clone();
	}

}
