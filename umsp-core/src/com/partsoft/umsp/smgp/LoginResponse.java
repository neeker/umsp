package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Constants.StatusCodes;

public class LoginResponse extends SmgpDataPacket {
	
	private static final long serialVersionUID = 0x80000001L;
	
	public int Status = StatusCodes.ERR_AUTH;// 4 Integer login请求返回结果(参见错误代码表)
	
	public byte[] AuthenticatorServer;// 16 Octet String 服务器端认证码，当客户端认证出错时，此项为空
	// 其值通过单向MD5 hash计算得出，表示如下：
	// AuthenticatorServer =MD5(Status+AuthenticatorClient +shared secret)
	// Shared secret 由服务器端与客户端事先商定，AuthenticatorClient为客户端发送给服务器端的上一条消息login中的值。

	public byte Version = 0x1E;// 1 Integer 服务器支持的最高版本号

	public LoginResponse() {
		super(RequestIDs.login_resp);
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeInt(Status);
		out.write(AuthenticatorServer, 0, 16);
		out.writeByte(Version);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		Status = in.readInt();
		AuthenticatorServer  = new byte[16];
		in.readFully(AuthenticatorServer, 0, 16);
		Version = in.readByte();
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 21;
	}
	
	@Override
	public LoginResponse clone() {
		return (LoginResponse) super.clone();
	}

}
