package com.partsoft.umsp.sgip;

import com.partsoft.utils.NumericUtils;
import com.partsoft.utils.StringUtils;

public class Constants {

	public static final String PROTOCOL_NAME = "SGIP";

	public static final int MAJOR_VERSION = 1;

	public static final int MINOR_VERSION = 2;

	public static final int VERSION_NUMBER = NumericUtils.buildVersion(MAJOR_VERSION, MINOR_VERSION);

	public static final String VERSION_STRING = StringUtils.buildVersionString(MAJOR_VERSION, MINOR_VERSION);

	public static final String VERSION_FULL = PROTOCOL_NAME + "-" + VERSION_STRING;

	public static final class Commands {

		public static final int BIND = 1;

		public static final int BIND_RESPONSE = -2147483647;

		public static final int UNBIND = 2;

		public static final int UNBIND_RESPONSE = -2147483646;

		public static final int SUBMIT = 3;

		public static final int SUBMIT_RESPONSE = -2147483645;

		public static final int DELIVER = 4;

		public static final int DELIVER_RESPONSE = -2147483644;

		public static final int REPORT = 5;

		public static final int REPORT_RESPONSE = -2147483643;

	}

	public static final class BindTypes {

		public static final int SP_TO_SMG = 1;

		public static final int SMG_TO_SP = 2;

		public static final int SMG_TO_SMG = 3;

		public static final int SMG_TO_GNS = 4;

		public static final int GNS_TO_SMG = 5;

		public static final int GNS_TO_GNS = 6;

		public static final int TEST_SMG_SP = 11;

	}

	public static final class BindResults {

		// 成功
		public static final int SUCCESS = 0;

		// IP认证错误
		public static final int IPERROR = 57;

		// 用户名密码错误
		public static final int ERROR = 1;

	}

}
