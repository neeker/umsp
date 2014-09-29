package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.utils.HexUtils;

public class Login extends SmgpDataPacket {

	private static final long serialVersionUID = 1L;

	public String ClientID;// 8 Octet String

	public byte[] AuthenticatorClient;// 16 Octet String

	public int LoginMode;// 1 Integer

	public int TimeStamp;// 4 Integer

	public byte Version = Constants.VERSION;// 1 integer

	public Login() {
		super(RequestIDs.login);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, ClientID, 8);
		out.write(AuthenticatorClient, 0, 16);
		out.writeByte(LoginMode);
		out.writeInt(TimeStamp);
		out.writeByte(Version);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		ClientID = readFixedString(in, 8);

		AuthenticatorClient = new byte[16];
		in.readFully(AuthenticatorClient, 0, 16);
		LoginMode = in.readByte();
		TimeStamp = in.readInt();
		Version = in.readByte();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 30;
	}

	@Override
	public Login clone() {
		return (Login) super.clone();
	}

	@Override
	public String toString() {
		return "电信SMGP连接请求包 [客户编号=" + ClientID + ", 协商密钥=" + HexUtils.toHex(AuthenticatorClient) + ", 请求类型="
				+ LoginMode + ", 时间戳=" + TimeStamp + ", 版本=" + Version + "]";
	}

}
