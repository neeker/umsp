package com.partsoft.umsp.cmpp;

public abstract class Constants {

	public static final String PROTOCOL_NAME = "CMPP";

	public static final byte VERSION3 = 0x30;
	
	public static final byte VERSION2 = 0x12;

	public static final class Commands {
		
		public static final int CMPP_CONNECT = 0x00000001;
		
		public static final int CMPP_CONNECT_RESP = 0x80000001;
		
		public static final int CMPP_TERMINATE = 0x00000002;
		
		public static final int CMPP_TERMINATE_RESP = 0x80000002;
		
		public static final int CMPP_SUBMIT = 0x00000004;
		
		public static final int CMPP_SUBMIT_RESP = 0x80000004;
		
		public static final int CMPP_DELIVER = 0x00000005;
		
		public static final int CMPP_DELIVER_RESP = 0x80000005;
		
		public static final int CMPP_QUERY = 0x00000006;
		
		public static final int CMPP_QUERY_RESP = 0x80000006;
		
		public static final int CMPP_CANCEL = 0x00000007;
		
		public static final int CMPP_CANCEL_RESP = 0x80000007;
		
		public static final int CMPP_ACTIVE_TEST = 0x00000008;
		
		public static final int CMPP_ACTIVE_TEST_RESP = 0x80000008;
		
		public static final int CMPP_FWD = 0x00000009;
		
		public static final int CMPP_FWD_RESP = 0x80000009;
		
		public static final int CMPP_MT_ROUTE = 0x00000010;
		
		public static final int CMPP_MT_ROUTE_RESP = 0x80000010;
		
		public static final int CMPP_MO_ROUTE = 0x00000011;
		
		public static final int CMPP_MO_ROUTE_RESP = 0x80000011;
		
		public static final int CMPP_GET_MT_ROUTE = 0x00000012;
		
		public static final int CMPP_GET_MT_ROUTE_RESP = 0x80000012;
		
		public static final int CMPP_MT_ROUTE_UPDATE = 0x00000013;
		
		public static final int CMPP_MT_ROUTE_UPDATE_RESP = 0x80000013;
		
		public static final int CMPP_MO_ROUTE_UPDATE = 0x00000014;
		
		public static final int CMPP_MO_ROUTE_UPDATE_RESP = 0x80000014;
		
		public static final int CMPP_PUSH_MT_ROUTE_UPDATE = 0x00000015;
		
		public static final int CMPP_PUSH_MT_ROUTE_UPDATE_RESP = 0x80000015;
		
		public static final int CMPP_PUSH_MO_ROUTE_UPDATE = 0x00000016;
		
		public static final int CMPP_PUSH_MO_ROUTE_UPDATE_RESP = 0x80000016;
		
		public static final int CMPP_GET_MO_ROUTE = 0x00000017;
		
		public static final int CMPP_GET_MO_ROUTE_RESP = 0x80000017;
		
	}
	
}
