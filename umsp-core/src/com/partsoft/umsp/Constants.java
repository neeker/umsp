package com.partsoft.umsp;

public abstract class Constants {

	public static class SMS {

		// 单条短信最大字符数
		public static final int MAX_SMS_ONEMSG_CONTENT = 70;

		// 长短信一条最大字符数(宽字符,一个字符占两个字节)
		public static final int MAX_SMS_CASCADEMSG_CONTENT = 67;

		// 长短信级最大联数量
		public static final int MAX_SMS_CASCADES = 255;

		// 短信最长字符数量
		public static final int MAX_SMS_TOTAL_CONTENT = MAX_SMS_CASCADEMSG_CONTENT * MAX_SMS_CASCADES;

		// 同时最多给多少用户下发短信
		public static final int MAX_SMS_USER_NUMBERS = 50;
		
		//最大签名长度
		public static final int MAX_SMS_SIGN_LEN = 24;
	}
	
	public static final class MessageCodes {

		public static final int ASCII = 0;

		public static final int CARD = 3;

		public static final int BYTES = 4;

		public static final int UCS2 = 8;

		public static final int GBK = 15;
	}
	
	public static class LineTypes {

		// 移动
		public static final int CMPP = 1;

		// 电信
		public static final int SMGP = 2;

		// 联通
		public static final int SGIP = 3;
		
		//未知
		public static final int UNKN = 0;

	}
	

	public static class LineProtocols {
		//移动
		public static final String CMPP = com.partsoft.umsp.cmpp.Constants.PROTOCOL_NAME;  
		
		// 电信
		public static final String SMGP = com.partsoft.umsp.smgp.Constants.PROTOCOL_NAME;

		// 联通
		public static final String SGIP = com.partsoft.umsp.sgip.Constants.PROTOCOL_NAME;
		
		// 未知
		public static final String UNKN = "UNKN";
		
	}
	
}
