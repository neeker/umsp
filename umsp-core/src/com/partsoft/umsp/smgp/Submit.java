package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Constants.TlvTags;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class Submit extends SmgpTlvDataPacket {

	private static final long serialVersionUID = 2L;
	
	public int nodeId;
	
	public int nodeTime;
	
	public int nodeSeq;

	public byte MsgType;// 1 integer 0＝MO 消息(终端发给SP)；
	// 6＝MT 消息(SP 发给终端，包括WEB 上发送的点对点短消息)；
	// 7＝点对点短消息；

	public byte NeedReport;// 1 integer 是否要求返回状态报告(0=不要求，=要求)

	public int Priority;// 1 Integer 发送优先级(从0到9)

	public String ServiceID;// 10 Octet String 业务类型

	public String FeeType;// 2 Octet String 收费类型(参见收费类型代码表)

	public String FeeCode;// 6 Octet String 资费代码(单位为分)

	public String FixedFee;// 6 包月封顶

	public byte MsgFormat;// 1 Octet String 短消息格式(参见短消息格式代码表)

	public String ValidTime;// 17 Octet String 有效时间，格式遵循SMPP3.3协议

	public String AtTime;// 17 Octet String 定时发送时间，格式遵循SMPP3.3协议

	public String SrcTermID;// 21 Octet String 短消息发送用户号码

	public String ChargeTermID;// 21 Octet String 计费用户号码

	public int DestTermIDCount;// 1 Integer 短消息接收号码总数(≤100)

	public String[] DestTermID;// 21* DestTerm Count Octet String
								// 短消息接收号码(连续存储DestTermIDCount个号码)
	public int MsgLength;// 1 Integer 短消息长度

	public byte[] MsgContent;// ≤252 Octet String 短消息内容

	public String Reserve;// 8 Octet String 保留

	// 提交次数
	public int submitCount;

	public Submit() {
		super(RequestIDs.submit);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(MsgType);
		out.writeByte(NeedReport);
		out.writeByte(Priority);
		writeFixedString(out, ServiceID, 10);
		writeFixedString(out, FeeType, 2);
		writeFixedString(out, FeeCode, 6);
		writeFixedString(out, FixedFee, 6);
		out.writeByte(MsgFormat);
		writeFixedString(out, ValidTime, 17);
		writeFixedString(out, AtTime, 17);
		writeFixedString(out, SrcTermID, 21);
		writeFixedString(out, ChargeTermID, 21);
		out.writeByte(DestTermIDCount);
		for (int i = 0; i < DestTermIDCount; i++) {
			if (DestTermID[i].startsWith("86")) {
				// 修复SMGP不接收开头为86的号码
				writeFixedString(out, DestTermID[i].substring(2), 21);
			} else {
				writeFixedString(out, DestTermID[i], 21);
			}
		}
		out.writeByte(MsgLength);
		out.write(MsgContent, 0, MsgLength);
		writeFixedString(out, Reserve, 8);
		writeTlvDatas(out);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		MsgType = in.readByte();
		NeedReport = in.readByte();
		Priority = in.readByte();
		ServiceID = readFixedString(in, 10);
		FeeType = readFixedString(in, 2);
		FeeCode = readFixedString(in, 6);
		FixedFee = readFixedString(in, 6);
		MsgFormat = in.readByte();
		ValidTime = readFixedString(in, 17);
		AtTime = readFixedString(in, 17);
		SrcTermID = readFixedString(in, 21);
		ChargeTermID = readFixedString(in, 21);
		DestTermIDCount = in.readUnsignedByte();
		Assert.isTrue(DestTermIDCount <= 100);
		DestTermID = new String[DestTermIDCount];
		for (int i = 0; i < DestTermIDCount; i++) {
			DestTermID[i] = readFixedString(in, 21);
		}
		MsgLength = in.readUnsignedByte();
		Assert.isTrue(MsgLength <= 252);
		MsgContent = new byte[MsgLength];
		in.readFully(MsgContent, 0, MsgLength);
		Reserve = readFixedString(in, 8);
		readTlvDatas(in);
		// tp_udhi = this.hasDynamicProperty(TlvTags.TP_udhi) ?
		// getDynamicProperty(TlvTags.TP_udhi)[0] : 0;
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 114 + DestTermIDCount * 21 + MsgLength;
	}

	public void setMessageContent(String msg, int code) {
		MsgFormat = (byte) code;
		MsgContent = UmspUtils.toGsmBytes(msg, code);
		MsgLength = MsgContent == null ? 0 : MsgContent.length;
		// tp_udhi = 0;
		this.setTp_udhi((byte) 0);
		this.setPkTotal((byte) 0);
		this.setPkNumber((byte) 0);
		// this.removeDynamicProperty(TlvTags.TP_udhi);
		// this.removeDynamicProperty(TlvTags.PkTotal);
		// this.removeDynamicProperty(TlvTags.PkNumber);
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	@SuppressWarnings("unchecked")
	public void addUserNumber(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		if (!new HashSet<String>(ListUtils.array2List(DestTermID)).contains(userNumber)) {
			DestTermIDCount++;
			DestTermID = (String[]) ListUtils.addToArray(DestTermID, userNumber, String.class);
		}
	}

	public void setUserNumbers(String value) {
		if (StringUtils.hasText(value)) {
			this.DestTermIDCount = 0;
			this.DestTermID = null;
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
		StringBuffer buffer = new StringBuffer(this.DestTermIDCount * 21);
		boolean appended = false;
		for (int i = 0; i < this.DestTermIDCount; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			if (DestTermID[i].startsWith("86")) {
				buffer.append(DestTermID[i].substring(2));
			} else {
				buffer.append(DestTermID[i]);
			}
		}
		return buffer.toString();
	}

	public String getUserNumbers() {
		StringBuffer buffer = new StringBuffer(this.DestTermIDCount * 21);
		boolean appended = false;
		for (int i = 0; i < this.DestTermIDCount; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			buffer.append(DestTermID[i]);
		}
		return buffer.toString();
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.getTp_udhi() == 1 && this.MsgContent[1] == 0) {
			result = this.MsgContent[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.getTp_udhi() == 1 && this.MsgContent[1] == 0) {
			result = this.MsgContent[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.getTp_udhi() == 1 && this.MsgContent[1] == 0) {
			result = this.MsgContent[5];
		}
		return result & 0xFF;
	}

	public String getMessageContent() {
		if (getTp_udhi() == 1 && (this.MsgContent[1] == 0)) {
			return UmspUtils.fromGsmBytes(MsgContent, 6, MsgLength - 6, MsgFormat);
		} else {
			// TODO 还可能需要做更多的内容判断
			return UmspUtils.fromGsmBytes(MsgContent, 0, MsgLength, MsgFormat);
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
		this.MsgFormat = (byte) messageCode;
		this.MsgLength = byte_buffer.length();
		this.MsgContent = byte_buffer.array();
		setTp_udhi((byte) 1);
		setPkTotal((byte) count);
		setPkNumber((byte) index);
		// this.setDynamicProperty(TlvTags.TP_udhi, new byte[] { 1 });
		// this.setDynamicProperty(TlvTags.PkTotal, new byte[] { (byte) count
		// });
		// this.setDynamicProperty(TlvTags.PkNumber, new byte[] { (byte) index
		// });
	}

	public byte getTp_pid() {
		return this.hasDynamicProperty(TlvTags.TP_udhi) ? getDynamicProperty(TlvTags.TP_udhi)[0] : 0;
	}

	public void setTp_pid(byte value) {
		setDynamicProperty(TlvTags.TP_udhi, new byte[] { value });
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

	public String getMsgSrc() {
		return hasDynamicProperty(TlvTags.MsgSrc) ? UmspUtils.fromGsmBytes(getDynamicProperty(TlvTags.MsgSrc)) : "";
	}

	public void setMsgSrc(String linkId) {
		if (StringUtils.hasLength(linkId)) {
			setDynamicProperty(TlvTags.MsgSrc, UmspUtils.string2FixedBytes(linkId, 8));
		} else {
			removeDynamicProperty(TlvTags.MsgSrc);
		}
	}

	public byte getChargeUserType() {
		return this.hasDynamicProperty(TlvTags.ChargeUserType) ? getDynamicProperty(TlvTags.ChargeUserType)[0] : 0;
	}

	public void setChargeUserType(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.ChargeUserType, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.ChargeUserType);
		}
	}

	public byte getChargeTermType() {
		return this.hasDynamicProperty(TlvTags.ChargeTermType) ? getDynamicProperty(TlvTags.ChargeTermType)[0] : 0;
	}

	public void setChargeTermType(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.ChargeTermType, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.ChargeTermType);
		}
	}

	public String getChargeTermPseudo() {
		return hasDynamicProperty(TlvTags.ChargeTermPseudo) ? UmspUtils
				.fromGsmBytes(getDynamicProperty(TlvTags.ChargeTermPseudo)) : "";
	}

	public void setChargeTermPseudo(String chargeTermPseudo) {
		if (StringUtils.hasLength(chargeTermPseudo)) {
			setDynamicProperty(TlvTags.ChargeTermPseudo,
					UmspUtils.string2FixedBytes(chargeTermPseudo, chargeTermPseudo.length()));
		} else {
			removeDynamicProperty(TlvTags.ChargeTermPseudo);
		}
	}

	public byte getDestTermTypeType() {
		return this.hasDynamicProperty(TlvTags.DestTermType) ? getDynamicProperty(TlvTags.DestTermType)[0] : 0;
	}

	public void setDestTermType(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.DestTermType, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.DestTermType);
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

	public byte getSubmitMsgType() {
		return this.hasDynamicProperty(TlvTags.SubmitMsgType) ? getDynamicProperty(TlvTags.SubmitMsgType)[0] : 0;
	}

	public void setSubmitMsgType(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.SubmitMsgType, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.SubmitMsgType);
		}
	}

	public byte getSPDealResult() {
		return this.hasDynamicProperty(TlvTags.SPDealReslt) ? getDynamicProperty(TlvTags.SPDealReslt)[0] : 0;
	}

	public void setSPDealResult(byte value) {
		if (value != 0) {
			setDynamicProperty(TlvTags.SPDealReslt, new byte[] { value });
		} else {
			removeDynamicProperty(TlvTags.SPDealReslt);
		}
	}

	public String getMServiceID() {
		return hasDynamicProperty(TlvTags.MServiceID) ? UmspUtils.fromGsmBytes(getDynamicProperty(TlvTags.MServiceID))
				: "";
	}

	public void setMServiceID(String value) {
		if (StringUtils.hasLength(value)) {
			setDynamicProperty(TlvTags.MServiceID, UmspUtils.string2FixedBytes(value, 21));
		} else {
			removeDynamicProperty(TlvTags.MServiceID);
		}
	}

	public String getDestTermPseudo() {
		return hasDynamicProperty(TlvTags.DestTermPseudo) ? UmspUtils
				.fromGsmBytes(getDynamicProperty(TlvTags.DestTermPseudo)) : "";
	}

	public void setDestTermPseudo(String destTermPseudo) {
		if (StringUtils.hasLength(destTermPseudo)) {
			setDynamicProperty(TlvTags.DestTermPseudo,
					UmspUtils.string2FixedBytes(destTermPseudo, destTermPseudo.length()));
		} else {
			removeDynamicProperty(TlvTags.DestTermPseudo);
		}
	}

	@Override
	public Submit clone() {
		return (Submit) super.clone();
	}

	@Override
	public String toString() {
		return "电信SMGP短信提交包 [消息类型=" + MsgType + ", 是否需要报告=" + NeedReport + ", 优先级=" + Priority + ", 服务代码=" + ServiceID
				+ ", 计费类型=" + FeeType + ", 计费代码=" + FeeCode + ", 固定费率=" + FixedFee + ", 消息格式=" + MsgFormat + ", 有效时间="
				+ ValidTime + ", 定时发送=" + AtTime + ", 发送号码=" + SrcTermID + ", 计费号码=" + ChargeTermID + ", 目标号码数="
				+ DestTermIDCount + ", 目标号码=" + getUserNumbers() + ", 短信长度=" + MsgLength + ", TP_UDHI=" + getTp_udhi()
				+ ", 短信内容=" + getMessageContent() + ", TP_PID=" + getTp_pid() + ", TP_UDHI=" + getTp_udhi()
				+ ", 业务唯一标识=" + getLinkId() + ", 长短信拆分总计=" + getPkTotal() + ", 长短信标识=" + getPkNumber() + "]";
	}

}
