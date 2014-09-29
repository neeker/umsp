package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.utils.HexUtils;

public class SubmitResponse extends SmgpDataPacket {

	private static final long serialVersionUID = 0x80000002L;

	public int NodeId;

	public int NodeTime;

	public int NodeSequenceId;

	// 网关代码：3字节(BCD码)
	// 时间：4字节(BCD码)
	// 序列号：3字节(BCD码)

	public int Status;// 4 Integer Submit请求返回结果(参见错误代码表)

	public SubmitResponse() {
		super(RequestIDs.submit_resp);
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
		int index = 0;
		NodeId = Integer.parseInt(HexUtils.toHex(msg_id_bytes, index, 3));
		index += 3;
		NodeTime = Integer.parseInt(HexUtils.toHex(msg_id_bytes, index, 4));
		index += 4;
		NodeSequenceId = Integer.parseInt(HexUtils.toHex(msg_id_bytes, index, 3));
		Status = in.readInt();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 14;
	}

	@Override
	public SubmitResponse clone() {
		return (SubmitResponse) super.clone();
	}

	public int getNodeId() {
		return NodeId;
	}

	public void setNodeId(int nodeId) {
		NodeId = nodeId;
	}

	public int getNodeTime() {
		return NodeTime;
	}

	public void setNodeTime(int nodeTime) {
		NodeTime = nodeTime;
	}

	public int getStatus() {
		return Status;
	}

	public void setStatus(int status) {
		Status = status;
	}

	@Override
	public String toString() {
		return "电信SMGP提交应答包 [节点号=" + NodeId + ", 节点时间=" + NodeTime + ", 节点序号=" + NodeSequenceId + ", 应答状态=" + Status
				+ "]";
	}

}
