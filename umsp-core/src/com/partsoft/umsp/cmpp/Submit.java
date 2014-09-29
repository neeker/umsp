package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class Submit extends CmppDataPacket {

	private static final long serialVersionUID = 4L;

	public int nodeId; // nodeId 与 nodeTime 组合生成MsgId: 8 Unsigned Integer 信息标识。

	public int nodeTime;// nodeId 与 nodeTime 组合生成MsgId: 8 Unsigned Integer 信息标识。

	public int nodeSeq;//

	public int pkTotal; // 1 Unsigned Integer 相同Msg_Id的信息总条数，从1开始。

	public byte pkNumber; // 1 Unsigned Integer 相同Msg_Id的信息序号，从1开始。

	public byte registeredDelivery; // 1 Unsigned Integer 是否要求返回状态确认报告： 0：不需要；
									// 1：需要。

	public byte msgLevel; // 1 Unsigned Integer 信息级别。

	public String serviceId; // 10 Octet String 业务标识，是数字、字母和符号的组合。

	public byte feeUserType; // 1 Unsigned Integer 计费用户类型字段： 0：对目的终端MSISDN计费；
								// 1：对源终端MSISDN计费；
								// 2：对SP计费；
								// 3：表示本字段无效，对谁计费参见Fee_terminal_Id字段。

	public String feeTerminalId; // 32 Octet String
									// 被计费用户的号码，当Fee_UserType为3时该值有效，当Fee_UserType为0、1、2时该值无意义。

	public byte feeTerminalType; // 1 Unsigned Integer 被计费用户的号码类型，0：真实号码；1：伪码。

	public byte tp_pid; // 1 Unsigned Integer GSM协议类型。详细是解释请参考GSM03.40中的9.2.3.9。

	public byte tp_udhi; // 1 Unsigned Integer
							// GSM协议类型。详细是解释请参考GSM03.40中的9.2.3.23,仅使用1位，右对齐。

	public byte msgFormat; // 1 Unsigned Integer 信息格式： 0：ASCII串； 3：短信写卡操作；
							// 4：二进制信息； 8：UCS2编码； 15：含GB汉字。。。。。。

	public String spId; // 6 Octet String 信息内容来源(SP_Id)。

	public String feeType; // 2 Octet String 资费类别： 01：对“计费用户号码”免费；
							// 02：对“计费用户号码”按条计信息费； 03：对“计费用户号码”按包月收取信息费。

	public String feeCode; // 6 Octet String 资费代码(以分为单位)。

	public String expireTime; // 17 Octet String 存活有效期，格式遵循SMPP3.3协议。

	public String atTime; // 17; // Octet String 定时发送时间，格式遵循SMPP3.3协议。

	public String sourceId; // 21 Octet String 源号码。SP的服务代码或前缀为服务代码的长号码,
							// 网关将该号码完整的填到SMPP协议Submit_SM消息相应的source_addr字段，该号码最终在用户手机上显示为短消息的主叫号码。

	public int destUserCount; // 1 Unsigned Integer 接收信息的用户数量(小于100个用户)。

	public String destTerminalIds[]; // 32*DestUsr_tl Octet String
										// 接收短信的MSISDN号码。

	public byte destTerminalType; // 1 Unsigned Integer
									// 接收短信的用户的号码类型，0：真实号码；1：伪码。

	public int msgLength; // 1 Unsigned Integer
							// 信息长度(Msg_Fmt值为0时：<160个字节；其它<=140个字节)，取值大于或等于0。

	public byte[] msgContent; // Msg_length Octet String 信息内容。

	public String linkId; // 20 Octet String 点播业务使用的LinkID，非点播类业务的MT流程不使用该字段。

	// 提交次数
	public int submitCount;

	public Submit() {
		super(Commands.CMPP_SUBMIT);
	}

	public long getMsgId() {
		return CmppUtils.generateMsgID(this.nodeId, this.nodeTime, this.nodeSeq);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeLong(getMsgId());
		out.writeByte(pkTotal);
		out.writeByte(pkNumber);
		out.writeByte(registeredDelivery);
		out.writeByte(msgLevel);
		writeFixedString(out, serviceId, 10);

		out.writeByte(feeUserType);
		if (protocolVersion == Constants.VERSION3) {
			writeFixedString(out, feeTerminalId, 32);
			out.writeByte(feeTerminalType);
		} else {
			writeFixedString(out, feeTerminalId, 21);
		}

		out.writeByte(tp_pid);
		out.writeByte(tp_udhi);
		out.writeByte(msgFormat);

		writeFixedString(out, spId, 6);
		writeFixedString(out, feeType, 2);
		writeFixedString(out, feeCode, 6);
		writeFixedString(out, expireTime, 17);
		writeFixedString(out, atTime, 17);
		writeFixedString(out, sourceId, 21);

		out.writeByte(destUserCount);

		if (protocolVersion == Constants.VERSION3) {
			for (int i = 0; i < destUserCount; i++) {
				writeFixedString(out, destTerminalIds[i], 32);
			}
			out.writeByte(destTerminalType);
		} else {
			for (int i = 0; i < destUserCount; i++) {
				writeFixedString(out, destTerminalIds[i], 21);
			}
		}

		out.writeByte(msgLength);
		out.write(msgContent, 0, msgLength);

		if (protocolVersion == Constants.VERSION3) {
			writeFixedString(out, linkId, 20);
		} else {
			writeFixedString(out, "", 8);
		}

	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);

		long temp_msgid = in.readLong();
		nodeId = CmppUtils.extractNodeIdFromMsgID(temp_msgid);
		nodeTime = CmppUtils.extractNodeTimeFromMsgID(temp_msgid);
		nodeSeq = CmppUtils.extractNodeSeqFromMsgID(temp_msgid);

		pkTotal = in.readByte();
		pkNumber = in.readByte();
		registeredDelivery = in.readByte();
		msgLevel = in.readByte();

		serviceId = readFixedString(in, 10);

		feeUserType = (byte) in.readUnsignedByte();

		if (protocolVersion == Constants.VERSION3) {
			feeTerminalId = readFixedString(in, 32);
			feeTerminalType = in.readByte();
		} else {
			feeTerminalId = readFixedString(in, 21);
		}

		tp_pid = in.readByte();
		tp_udhi = in.readByte();
		msgFormat = in.readByte();

		spId = readFixedString(in, 6);
		feeType = readFixedString(in, 2);
		feeCode = readFixedString(in, 6);
		expireTime = readFixedString(in, 17);
		atTime = readFixedString(in, 17);
		sourceId = readFixedString(in, 21);

		destUserCount = in.readUnsignedByte();
		destTerminalIds = new String[destUserCount];

		if (protocolVersion == Constants.VERSION3) {
			for (int i = 0; i < destUserCount; i++) {
				destTerminalIds[i] = readFixedString(in, 32);
			}
			destTerminalType = in.readByte();
		} else {
			for (int i = 0; i < destUserCount; i++) {
				destTerminalIds[i] = readFixedString(in, 21);
			}
		}

		msgLength = in.readUnsignedByte();
		msgContent = new byte[msgLength];
		in.readFully(msgContent, 0, msgLength);
		if (protocolVersion == Constants.VERSION3) {
			linkId = readFixedString(in, 20);
		} else {
			readFixedString(in, 8);
		}
	}

	@Override
	public int getDataSize() {
		int result = 0;
		if (protocolVersion == Constants.VERSION3) {
			result = super.getDataSize() + 151 + msgLength + destUserCount * 32;
		} else {
			result = super.getDataSize() + 126 + msgLength + destUserCount * 21;
		}
		return result;
	}

	public void setMessageContent(String msg, int code) {
		msgFormat = (byte) code;
		msgContent = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		msgLength = msgContent == null ? 0 : msgContent.length;
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	@SuppressWarnings("unchecked")
	public void addUserNumber(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		if (!new HashSet<String>(ListUtils.array2List(destTerminalIds)).contains(userNumber)) {
			destUserCount++;
			destTerminalIds = (String[]) ListUtils.addToArray(destTerminalIds, userNumber, String.class);
		}
	}

	public void setUserNumbers(String value) {
		if (StringUtils.hasText(value)) {
			this.destUserCount = 0;
			this.destTerminalIds = null;
			if (value.indexOf(',') >= 0) {
				for (String number : value.split(",")) {
					addUserNumber(number.trim());
				}
			} else {
				addUserNumber(value.trim());
			}
		}
	}

	public String getUserNumbersTrimCNPrefix() {
		StringBuffer buffer = new StringBuffer(this.destUserCount * 21);
		boolean appended = false;
		for (int i = 0; i < this.destUserCount; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			if (destTerminalIds[i].startsWith("86")) {
				buffer.append(destTerminalIds[i].substring(2));
			} else {
				buffer.append(destTerminalIds[i]);
			}
		}
		return buffer.toString();
	}

	public String getUserNumbers() {
		StringBuffer buffer = new StringBuffer(this.destUserCount * 21);
		boolean appended = false;
		for (int i = 0; i < this.destUserCount; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			buffer.append(destTerminalIds[i]);
		}
		return buffer.toString();
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.tp_udhi == 1 && this.msgContent[1] == 0) {
			result = this.msgContent[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.tp_udhi == 1 && this.msgContent[1] == 0) {
			result = this.msgContent[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.tp_udhi == 1 && this.msgContent[1] == 0) {
			result = this.msgContent[5];
		}
		return result & 0xFF;
	}

	public String getMessageContent() {
		if (tp_udhi == 1 && (msgContent[1] == 0)) {
			return UmspUtils.fromGsmBytes(msgContent, 6, msgLength - 6, msgFormat);
		} else {
			// TODO 还可能需要做更多的内容判断
			return UmspUtils.fromGsmBytes(msgContent, 0, msgLength, msgFormat);
		}
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
		byte_buffer.put(msg_content);
		this.tp_udhi = 1;
		this.pkNumber = (byte) index;
		this.pkTotal = count;
		this.msgFormat = (byte) messageCode;
		this.msgLength = byte_buffer.length();
		this.msgContent = byte_buffer.array();
	}

	@Override
	public Submit clone() {
		return (Submit) super.clone();
	}

	@Override
	public String toString() {
		return "移动CMPP短信提交包[节点号=" + nodeId + ", 节点时间=" + nodeTime + ", 节点序号=" + nodeSeq + ", 长短信拆分条目=" + pkTotal
				+ ", 长短信标识=" + pkNumber + ", 是否需要报告=" + registeredDelivery + ", 消息优先级=" + msgLevel + ", 服务代码="
				+ serviceId + ", 计费用户类型=" + feeUserType + ", 收费用户号码=" + feeTerminalId + ", 收费用户类型=" + feeTerminalType
				+ ", 消息格式=" + msgFormat + ", 发送服务号码=" + spId + ", 计费类型=" + feeType + ", 计费代码=" + feeCode + ", 超时时间="
				+ expireTime + ", 定时发送时间=" + atTime + ", 发送原号码=" + sourceId + ", 目标用户总计=" + destUserCount + ", 目标号码={"
				+ getUserNumbers() + "}, 目标号码类型=" + destTerminalType + ", 短信长度=" + msgLength + ", 短信内容="
				+ getMessageContent() + ", 业务唯一标识=" + linkId + ", TP_PID=" + tp_pid + ", TP_UDHI=" + tp_udhi + ", 序号="
				+ sequenceId + ", 创建时间=" + createTimeMillis + ", 版本协议=" + protocolVersion + "]";
	}

}
