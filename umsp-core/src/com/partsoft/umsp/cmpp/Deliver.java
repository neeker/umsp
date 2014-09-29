package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.springframework.util.StringUtils;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.HexUtils;

public class Deliver extends CmppDataPacket {

	private static final long serialVersionUID = 5L;

	// Msg_Id 8 Unsigned Integer 信息标识。
	// 生成算法如下：
	// 采用64位(8字节)的整数：
	// (1) 时间(格式为MMDDHHMMSS，即月日时分秒)：bit64~bit39，其中
	// bit64~bit61：月份的二进制表示；
	// bit60~bit56：日的二进制表示；
	// bit55~bit51：小时的二进制表示；
	// bit50~bit45：分的二进制表示；
	// bit44~bit39：秒的二进制表示；
	// (2) 短信网关代码：bit38~bit17，把短信网关的代码转换为整数填写到该字段中；
	// (3) 序列号：bit16~bit1，顺序增加，步长为1，循环使用。
	// 各部分如不能填满，左补零，右对齐。
	public int nodeId;

	public int nodeTime;

	public int nodeSeq;

	public String destId; // 21 Octet String 目的号码。
							// SP的服务代码，一般4--6位，或者是前缀为服务代码的长号码；该号码是手机用户短消息的被叫号码。

	public String serviceId; // 10 Octet String 业务标识，是数字、字母和符号的组合。

	public byte tp_pid;// 1 Unsigned Integer GSM协议类型。详细解释请参考GSM03.40中的9.2.3.9。

	public byte tp_udhi;// 1 Unsigned Integer
						// GSM协议类型。详细解释请参考GSM03.40中的9.2.3.23，仅使用1位，右对齐。

	public byte msgFormat;// 1 Unsigned Integer 信息格式： 0：ASCII串； 3：短信写卡操作；
							// 4：二进制信息； 8：UCS2编码； 15：含GB汉字。

	public String sourceId;// 32 Octet String
							// 源终端MSISDN号码(状态报告时填为CMPP_SUBMIT消息的目的终端号码)。

	public byte sourceType;// 1 Unsigned Integer 源终端号码类型，0：真实号码；1：伪码。

	public byte registeredDelivery;// 1 Unsigned Integer 是否为状态报告： 0：非状态报告；
									// 1：状态报告。

	public int msgLength;// 1 Unsigned Integer 消息长度，取值大于或等于0。

	public byte msgContent[];// Msg_length Octet String 消息内容。

	public String linkId;// 20 Octet String 点播业务使用的LinkID，非点播类业务的MT流程不使用该字段。

	// 提交次数
	public int submitCount;

	public Deliver() {
		super(Commands.CMPP_DELIVER);
	}

	public long getMsgId() {
		return CmppUtils.generateMsgID(this.nodeId, this.nodeTime, this.nodeSeq);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);

		out.writeLong(this.getMsgId());

		writeFixedString(out, this.destId, 21);
		writeFixedString(out, serviceId, 10);

		out.writeByte(this.tp_pid);
		out.writeByte(this.tp_udhi);
		out.writeByte(this.msgFormat);

		if (protocolVersion == Constants.VERSION3) {
			writeFixedString(out, this.sourceId, 32);
			out.writeByte(this.sourceType);
		} else {
			writeFixedString(out, this.sourceId, 21);
		}

		out.writeByte(this.registeredDelivery);
		out.writeByte(this.msgLength);
		out.write(this.msgContent, 0, this.msgLength);

		if (protocolVersion == Constants.VERSION3) {
			writeFixedString(out, this.linkId, 20);
		} else {
			writeFixedString(out, "", 8);
		}
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);

		long temp_msg_id = in.readLong();

		this.nodeId = CmppUtils.extractNodeIdFromMsgID(temp_msg_id);
		this.nodeTime = CmppUtils.extractNodeTimeFromMsgID(temp_msg_id);
		this.nodeSeq = CmppUtils.extractNodeSeqFromMsgID(temp_msg_id);

		this.destId = readFixedString(in, 21);
		this.serviceId = readFixedString(in, 10);

		this.tp_pid = in.readByte();
		this.tp_udhi = in.readByte();

		this.msgFormat = in.readByte();

		if (protocolVersion == Constants.VERSION3) {
			this.sourceId = readFixedString(in, 32);
			this.sourceType = in.readByte();
		} else {
			this.sourceId = readFixedString(in, 21);
		}

		this.registeredDelivery = in.readByte();

		this.msgLength = in.readUnsignedByte();
		this.msgContent = new byte[this.msgLength];
		in.readFully(this.msgContent, 0, this.msgLength);
		if (protocolVersion == Constants.VERSION3) {
			this.linkId = readFixedString(in, 20);
		} else {
			readFixedString(in, 8);
		}
	}

	@Override
	public int getDataSize() {
		int result = 0;
		if (protocolVersion == Constants.VERSION3) {
			result = super.getDataSize() + 97 + this.msgLength;
		} else {
			result = super.getDataSize() + 73 + this.msgLength;
		}
		return result;
	}

	public String getMessageContent() {
		return tp_udhi == 1 ? UmspUtils.fromGsmBytes(msgContent, 6, msgLength - 6, msgFormat) : UmspUtils.fromGsmBytes(
				msgContent, msgFormat);
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	public void setMessageContent(String msg, int code) {
		msgFormat = (byte) code;
		msgContent = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		msgLength = msgContent == null ? 0 : msgContent.length;
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
		this.tp_udhi = 1;
		this.msgLength = byte_buffer.length();
		this.msgContent = byte_buffer.array();
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.msgContent[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.msgContent[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.msgContent[5];
		}
		return result & 0xFF;
	}

	public String getSourceIdTrimCNPrefix() {
		String result = this.sourceId;
		if (StringUtils.hasText(this.sourceId) && this.sourceId.startsWith("86")) {
			result = this.sourceId.substring(2);
		}
		return result;
	}

	@Override
	public Deliver clone() {
		return (Deliver) super.clone();
	}

	@Override
	public String toString() {
		return "移动上行数据包 [协议版本=" + this.protocolVersion + ", 来源节点=" + nodeId + ", 节点时间=" + nodeTime + ", 节点序号="
				+ nodeSeq + ", 目标号码=" + destId + ", 服务代码=" + serviceId + ", 消息格式=" + msgFormat + ", 来源号码=" + sourceId
				+ ", 来源类型=" + sourceType + ", 是否报告=" + registeredDelivery + ", 短信长度=" + msgLength + ", 数据内容={"
				+ HexUtils.toHex(this.msgContent, msgLength) + "}, 业务唯一标识=" + linkId + ", TP_PID=" + tp_pid
				+ ", TP_UDHI=" + tp_udhi + ", 序号=" + sequenceId + ", 创建时间=" + createTimeMillis + ", 版本协议="
				+ protocolVersion + "]";
	}

}
