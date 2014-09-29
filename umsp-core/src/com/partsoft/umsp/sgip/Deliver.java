package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.springframework.util.StringUtils;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.utils.UmspUtils;

public class Deliver extends MoForwardPacket {

	private static final long serialVersionUID = 4L;

	public String user_number;

	public String sp_number;

	public byte tp_pid;

	public byte tp_udhi;

	public byte message_coding;

	public int message_length;

	public byte[] message_content;

	public Deliver() {
		super(Commands.DELIVER);
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, user_number, 21);
		writeFixedString(out, sp_number, 21);
		out.writeByte(tp_pid);
		out.writeByte(tp_udhi);
		out.writeByte(message_coding);
		out.writeInt(message_length);
		out.write(message_content);
		writeFixedString(out, reserve, 8);
	}

	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		user_number = readFixedString(in, 21);
		sp_number = readFixedString(in, 21);
		tp_pid = in.readByte();
		tp_udhi = in.readByte();
		message_coding = in.readByte();
		message_length = in.readInt();
		message_content = new byte[message_length];
		in.readFully(message_content);
		reserve = readFixedString(in, 8);
	}

	@Override
	public Deliver clone() {
		return (Deliver) super.clone();
	}

	@Override
	public int getDataSize() {
		return PACKET_SIZE + message_length + 49;
	}

	public String getUserNumberTrimCNPrefix() {
		String result = this.user_number;
		if (StringUtils.hasText(this.user_number) && this.user_number.startsWith("86")) {
			result = this.user_number.substring(2);
		}
		return result;
	}

	public String getMessageContent() {
		return tp_udhi == 1 ? UmspUtils.fromGsmBytes(message_content, 6, message_length - 6, message_coding)
				: UmspUtils.fromGsmBytes(message_content, message_coding);
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	public void setMessageContent(String msg, int code) {
		message_coding = (byte) code;
		message_content = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		message_length = message_content == null ? 0 : message_content.length;
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[5];
		}
		return result & 0xFF;
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
		this.message_coding = (byte) messageCode;
		this.message_length = byte_buffer.length();
		this.message_content = byte_buffer.array();
	}

	@Override
	public String toString() {
		return "联通SGIP短信上行数据包 [节点号=" + node_id + ", 时间戳=" + timestamp + ", 序号=" + sequence + ", 来源号码=" + user_number
				+ ", 目标号码=" + sp_number + ", TP_PID=" + tp_pid + ", TP_UDHI=" + tp_udhi + ", 消息格式=" + message_coding
				+ ", 消息长度=" + message_length + ", 消息内容=" + getMessageContent() + "]";
	}

}
