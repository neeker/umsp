package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Constants.TlvTags;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.HexUtils;
import com.partsoft.utils.StringUtils;

public class Deliver extends SmgpTlvDataPacket {

	private static final long serialVersionUID = 3L;

	public int NodeId;

	public int NodeTime;

	public int NodeSequenceId;

	// public String MsgID;// 10 Integer 网关产生的短消息流水号，由三部分组成：
	// 网关代码：3字节(BCD码)
	// 时间：4字节(BCD码)MMDDHHMM(月日时分)
	// 序列号：3字节(BCD码)

	public byte IsReport;// 1 Integer 是否状态报告(0=不是，1=是)

	public byte MsgFormat;// 1 Integer 短消息格式(参见短消息格式代码表)

	public String RecvTime;// 14 Octet String
							// 短消息接收时间(格式：yyyymmddhhmiss，例如20010301200000)

	public String SrcTermID;// 21 Octet String 短消息发送号码

	public String DestTermID;// 21 Octet String 短消息接收号码

	public int MsgLength;// 1 Integer 短消息长度

	public byte[] MsgContent;// ≤252 Octet String 短消息内容

	public String Reserve;// 8 Octet String 保留

	// 提交次数
	public int submitCount;

	public Deliver() {
		super(RequestIDs.deliver);
	}

	public byte[] getMsgID() {
		return SmgpUtils.generateMsgID(NodeId, NodeTime, NodeSequenceId);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);

		out.write(getMsgID());

		out.writeByte(IsReport);
		out.writeByte(MsgFormat);
		writeFixedString(out, RecvTime, 14);
		writeFixedString(out, SrcTermID, 21);
		writeFixedString(out, DestTermID, 21);
		out.writeByte(MsgLength);
		out.write(MsgContent, 0, MsgLength);
		writeFixedString(out, Reserve, 8);
		writeTlvDatas(out);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);

		byte[] msg_id_bytes = new byte[10];
		in.readFully(msg_id_bytes, 0, msg_id_bytes.length);
		int index = 0;
		NodeId = HexUtils.intFromHex(HexUtils.toHex(msg_id_bytes, index, 3));
		index += 3;
		NodeTime = HexUtils.intFromHex(HexUtils.toHex(msg_id_bytes, index, 4));
		index += 4;
		NodeSequenceId = HexUtils.intFromHex(HexUtils.toHex(msg_id_bytes, index, 3));

		IsReport = in.readByte();
		MsgFormat = in.readByte();
		RecvTime = readFixedString(in, 14);
		SrcTermID = readFixedString(in, 21);
		DestTermID = readFixedString(in, 21);
		MsgLength = in.readUnsignedByte();
		Assert.isTrue(MsgLength <= 252);
		MsgContent = new byte[MsgLength];
		in.readFully(MsgContent, 0, MsgLength);
		Reserve = readFixedString(in, 8);
		readTlvDatas(in);
	}

	public String getMessageContent() {
		return getTp_udhi() == 1 ? UmspUtils.fromGsmBytes(MsgContent, 6, MsgLength - 6, MsgFormat) : UmspUtils
				.fromGsmBytes(MsgContent, MsgFormat);
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	public void setMessageContent(String msg, int code) {
		MsgFormat = (byte) code;
		MsgContent = UmspUtils.toGsmBytes(msg, code);
		removeDynamicProperty(TlvTags.TP_udhi);
		MsgLength = MsgContent == null ? 0 : MsgContent.length;
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.getTp_udhi() == 1) {
			result = this.MsgContent[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.getTp_udhi() == 1) {
			result = this.MsgContent[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.getTp_udhi() == 1) {
			result = this.MsgContent[5];
		}
		return result & 0xFF;
	}

	public byte getTp_udhi() {
		return this.hasDynamicProperty(TlvTags.TP_udhi) ? getDynamicProperty(TlvTags.TP_udhi)[0] : 0;
	}

	public void setTp_udhi(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.TP_udhi, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.TP_udhi);
		}
	}

	public byte getTp_pid() {
		return this.hasDynamicProperty(TlvTags.TP_udhi) ? getDynamicProperty(TlvTags.TP_udhi)[0] : 0;
	}

	public void setTp_pid(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.TP_udhi, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.TP_udhi);
		}
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 77 + MsgLength;
	}

	@Override
	public Deliver clone() {
		return (Deliver) super.clone();
	}

	public String getLinkId() {
		return hasDynamicProperty(TlvTags.LinkID) ? UmspUtils.fromGsmBytes(getDynamicProperty(TlvTags.LinkID)) : "";
	}

	public void setLinkId(String linkId) {
		if (StringUtils.hasLength(linkId)) {
			setDynamicProperty(TlvTags.LinkID, UmspUtils.string2FixedBytes(linkId, 20));
		} else {
			removeDynamicProperty(TlvTags.LinkID);
		}
	}

	public byte getSrcTermType() {
		return hasDynamicProperty(TlvTags.SrcTermType) ? getDynamicProperty(TlvTags.SrcTermType)[0] : 0;
	}

	public void setSrcTermType(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.LinkID, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.LinkID);
		}
	}

	public String getSrcTermPseudo() {
		return hasDynamicProperty(TlvTags.SrcTermPseudo) ? UmspUtils
				.fromGsmBytes(getDynamicProperty(TlvTags.SrcTermPseudo)) : "";
	}

	public void setSrcTermPseudo(String value) {
		if (StringUtils.hasLength(value)) {
			setDynamicProperty(TlvTags.SrcTermPseudo, UmspUtils.string2FixedBytes(value, value.length()));
		} else {
			removeDynamicProperty(TlvTags.SrcTermPseudo);
		}
	}

	public String getSPDealResult() {
		return hasDynamicProperty(TlvTags.SPDealReslt) ? UmspUtils
				.fromGsmBytes(getDynamicProperty(TlvTags.SPDealReslt)) : "";
	}

	public void setSPDealResult(String value) {
		if (StringUtils.hasLength(value)) {
			setDynamicProperty(TlvTags.SPDealReslt, UmspUtils.string2FixedBytes(value, value.length()));
		} else {
			removeDynamicProperty(TlvTags.SPDealReslt);
		}
	}

	public byte getPkTotal() {
		return this.hasDynamicProperty(TlvTags.PkTotal) ? getDynamicProperty(TlvTags.PkTotal)[0] : 0;
	}

	public void setPkTotal(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.PkTotal, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.PkTotal);
		}
	}

	public byte getPkNumber() {
		return this.hasDynamicProperty(TlvTags.PkNumber) ? getDynamicProperty(TlvTags.PkNumber)[0] : 0;
	}

	public void setPkNumber(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.PkNumber, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.PkNumber);
		}
	}

	public String getSrcTermIdTrimCNPrefix() {
		String result = this.SrcTermID;
		if (StringUtils.hasText(this.SrcTermID) && this.SrcTermID.startsWith("86")) {
			result = this.SrcTermID.substring(2);
		}
		return result;
	}

	public void setCascadeMessageContent(String msg, int refid, int count, int index) {
		setCascadeMessageContent(msg, MessageCodes.UCS2, refid, count, index);
	}

	public void setCascadeMessageContent(String msg, int messageCode, int refid, int count, int index) {
		if (msg.length() > SMS.MAX_SMS_CASCADEMSG_CONTENT) {
			throw new IllegalArgumentException(String.format("cascade message length must be less than or equals %d",
					SMS.MAX_SMS_CASCADEMSG_CONTENT));
		}
		byte[] msg_content = UmspUtils.toGsmBytes(msg, messageCode);
		ByteArrayBuffer byte_buffer = new ByteArrayBuffer(msg_content.length + 6);
		byte_buffer.put((byte) 5);
		byte_buffer.put((byte) 0);
		byte_buffer.put((byte) 3);
		byte_buffer.put((byte) refid);
		byte_buffer.put((byte) count);
		byte_buffer.put((byte) index);
		this.MsgFormat = (byte) messageCode;
		this.MsgLength = byte_buffer.length();
		this.MsgContent = byte_buffer.array();
		setTp_udhi((byte) 1);
		setPkTotal((byte) count);
		setPkNumber((byte) index);
	}

	@Override
	public String toString() {
		return "电信SMGP上行数据包 [节点号=" + NodeId + ", 节点时间=" + NodeTime + ", 是否报告=" + IsReport + ", 消息格式=" + MsgFormat
				+ ", 接收时间=" + RecvTime + ", 来源号码=" + SrcTermID + ", 目标号码=" + DestTermID + ", 消息长度=" + MsgLength
				+ ", 消息内容=" + getMessageContent() + "{" + HexUtils.toHex(this.MsgContent) + "}, 序号=" + sequenceId
				+ ", 创建时间=" + createTimeMillis + ", TP_UDHI=" + getTp_udhi() + ", 业务唯一标识=" + getLinkId() + ", 来源号码类型="
				+ getSrcTermType() + ", TERMPSEUDO=" + getSrcTermPseudo() + ", SPDEALRESULT=" + getSPDealResult() + "]";
	}

}
