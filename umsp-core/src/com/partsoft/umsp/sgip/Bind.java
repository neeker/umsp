package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.sgip.Constants.Commands;

public class Bind extends SgipDataPacket implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int USER_SIZE = 16;

	public static final int PWD_SIZE = 16;

	// 1：SP向SMG建立的连接，用于发送命令
	// 2：SMG向SP建立的连接，用于发送命令
	// 3：SMG之间建立的连接，用于转发命令
	// 4：SMG向GNS建立的连接，用于路由表的检索和维护
	// 5：GNS向SMG建立的连接，用于路由表的更新
	// 6：主备GNS之间建立的连接，用于主备路由表的一致性
	// 11：SP与SMG以及SMG之间建立的测试连接，用于跟踪测试
	// 其它：保留
	public byte type = 1;

	public String user;

	public String pwd;

	public Bind() {
		super(Commands.BIND);
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(type);
		writeFixedString(out, user, USER_SIZE);
		writeFixedString(out, pwd, PWD_SIZE);
		writeFixedString(out, reserve, RESERVE_SIZE);
	}

	public void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		type = in.readByte();
		user = readFixedString(in, USER_SIZE);
		pwd = readFixedString(in, PWD_SIZE);
		reserve = readFixedString(in, RESERVE_SIZE);
	}

	@Override
	public Bind clone() {
		return (Bind) super.clone();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + USER_SIZE + PWD_SIZE + Buffer.BYTE_SIZE;
	}

	@Override
	public String toString() {
		return "联通SGIP连接请求包 [节点号=" + node_id + ", 时间戳=" + timestamp + ", 序号=" + sequence + ", 请求类型=" + type + ", 用户="
				+ user + ", 密码=" + pwd + "]";
	}

}
