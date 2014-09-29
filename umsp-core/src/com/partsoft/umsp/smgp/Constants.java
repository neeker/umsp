package com.partsoft.umsp.smgp; //

public class Constants {

	public static final String PROTOCOL_NAME = "SMGP";
	
	public static final byte VERSION = 0x1e;

	public static class RequestIDs {

		public static final int login = 0x00000001; // CP或SMGW登录请求

		public static final int login_resp = 0x80000001; // CP或SMGW登录回应

		public static final int submit = 0x00000002; // CP发送短消息请求

		public static final int submit_resp = 0x80000002; // CP发送短消息回应

		public static final int deliver = 0x00000003; // SMGW向CP发送短消息请求

		public static final int deliver_resp = 0x80000003; // SMGW向CP发送短消息回应

		public static final int active_test = 0x00000004; // 测试通信链路是否正常请求(由客户端发起，CP和SMGW可以通过定时发送此请求来维持连接)

		public static final int active_test_resp = 0x80000004; // 测试通信链路是否正常回应

		public static final int forward = 0x00000005; // SMGW转发MT/MO短消息请求

		public static final int forward_reCP = 0x80000005; // SMGW转发MT/MO短消息回应

		public static final int exit = 0x00000006; // 退出请求

		public static final int exit_resp = 0x80000006; // 退出回应

		public static final int query = 0x00000007; // CP统计查询请求

		public static final int query_resp = 0x80000007; // CP统计查询回应

		public static final int mt_route_update = 0x00000008; // MT路由更新请求

		public static final int mt_route_update_resp = 0x80000008; // MT路由更新回应

		public static final int mo_route_update = 0x00000009; // MO路由更新请求

		public static final int mo_route_update_resp = 0x80000009; // MO路由更新回应

	}
	
	public static final class LoginModes {
		
		public static final int SEND = 0;//发送模式(send mode)
		
		public static final int RECEIVE = 1; //接收模式(receive mode)
		
		public static final int TRANSMIT = 2; //收发模式(transmit mode)。
		
	}

	public static final class StatusCodes {

		public static final int ERR_SUCCESS = 0;// 成功

		public static final int ERR_SYSBUSY = 1;// 系统忙

		public static final int ERR_MAXCONNS = 2;// 超过最大连接数

		public static final int ERR_MSG = 10;// 消息结构错

		public static final int ERR_REQUEST = 11;// 命令字错

		public static final int ERR_DUPREQ = 12;// 序列号重复

		public static final int ERR_IP = 20;// IP地址错
		
		public static final int ERR_AUTH = 21;// 认证错
		
		public static final int ERR_VERSION = 22;// 版本太高

		public static final int ERR_MSGTYPE = 30;// 非法消息类型(SMType)

		public static final int ERR_MSGPRIO = 31;// 非法优先级(Priority)

		public static final int ERR_FEETYPE = 32;// 非法资费类型(FeeType)

		public static final int ERR_FEECODE = 33;// 非法资费代码(FeeCode)

		public static final int ERR_MSGFORMAT = 34;// 非法短消息格式(MsgFormat)

		public static final int ERR_TIMEFORMAT = 35;// 非法时间格式

		public static final int ERR_MSGLEN = 36;// 非法短消息长度(MsgLength)

		public static final int ERR_EXPIRED = 37;// 有效期已过

		public static final int ERR_QUERY = 38;// 非法查询类别(QueryType)

		public static final int ERR_ROUTE = 39;// 路由错误
		

	}
	
	public static class TlvTags {
		public static final short TP_pid = 0x0001;
		public static final short TP_udhi = 0x0002;
		public static final short LinkID = 0x0003;
		public static final short ChargeUserType = 0x0004;
		public static final short ChargeTermType = 0x0005;
		public static final short ChargeTermPseudo = 0x0006;
		public static final short DestTermType = 0x0007;
		public static final short DestTermPseudo = 0x0008;
		public static final short PkTotal = 0x0009;
		public static final short PkNumber = 0x000A;
		public static final short SubmitMsgType = 0x000B;
		public static final short SPDealReslt = 0x000C;
		public static final short SrcTermType = 0x000D;
		public static final short SrcTermPseudo = 0x000E;
		public static final short NodesCount = 0x000F;
		public static final short MsgSrc = 0x0010;
		public static final short SrcType = 0x0011;
		public static final short MServiceID = 0x0012;
	}

}
