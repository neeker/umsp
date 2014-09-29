package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.springframework.util.StringUtils;

import com.partsoft.umsp.sgip.Constants.Commands;

public class Report extends MoForwardPacket {

	private static final long serialVersionUID = 5L;

	public int submit_node_id;

	public int submit_time_stamp;

	public int submit_sequence_id;

	public byte report_type;

	public String user_number;

	public byte state;

	public byte error_code;

	public String sp_number;

	public Report() {
		super(Commands.REPORT);
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeInt(submit_node_id);
		out.writeInt(submit_time_stamp);
		out.writeInt(submit_sequence_id);

		out.writeByte(report_type);
		writeFixedString(out, user_number, 21);
		out.writeByte(state);
		out.writeByte(error_code);
		writeFixedString(out, reserve, 8);
	}

	public void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		submit_node_id = in.readInt();
		submit_time_stamp = in.readInt();
		submit_sequence_id = in.readInt();

		report_type = in.readByte();
		user_number = readFixedString(in, 21);
		state = in.readByte();
		error_code = in.readByte();
		reserve = readFixedString(in, 8);
	}

	@Override
	public Report clone() {
		return (Report) super.clone();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 36;
	}

	public String getUserNumberTrimCNPrefix() {
		String result = this.user_number;
		if (StringUtils.hasText(this.user_number) && this.user_number.startsWith("86")) {
			result = this.user_number.substring(2);
		}
		return result;
	}

	@Override
	public String toString() {
		return "联通SGIP短信发送报告数据 [节点号=" + node_id + ", 时间戳=" + timestamp + ", 序号=" + sequence + ", 提交节点号=" + submit_node_id
				+ ", 提交时间戳=" + submit_time_stamp + ", 提交序号=" + submit_sequence_id + ", 报告类型=" + report_type + ", 下发号码="
				+ user_number + ", 状态=" + state + ", 错误代码=" + error_code + ", 发送号码=" + sp_number + "]";
	}

}
