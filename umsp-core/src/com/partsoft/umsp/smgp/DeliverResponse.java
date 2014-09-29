package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class DeliverResponse extends SmgpDataPacket {
	
	private static final long serialVersionUID = 0x80000003L;

	public int NodeId;
	
	public int NodeTime;
	
	public int NodeSequenceId;
	
	//public String MsgID;// 10 Integer 网关产生的短消息流水号，由三部分组成：
	// 网关代码：3字节(BCD码)
	// 时间：4字节(BCD码)
	// 序列号：3字节(BCD码)

	public int Status;// 4 integer Deliver请求返回结果(参见错误代码表)

	public DeliverResponse() {
		super(RequestIDs.deliver_resp);
	}
	
	public byte[] getMsgID() {
		return SmgpUtils.generateMsgID(NodeId, NodeTime, NodeSequenceId);
	}	
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.write(getMsgID());
		out.writeInt(Status);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		
		byte[] msg_id_bytes = new byte[10];
		in.readFully(msg_id_bytes, 0, msg_id_bytes.length);
		
		Status = in.readInt();
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 14;
	}
	
	@Override
	public DeliverResponse clone() {
		return (DeliverResponse) super.clone();
	}

	
}
