package com.partsoft.umsp.utils;

import java.io.UnsupportedEncodingException;

import com.partsoft.umsp.Constants.LineProtocols;
import com.partsoft.umsp.Constants.LineTypes;
import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.utils.StringUtils;

public class UmspUtils {
	
	public static String lineType2Protocol(int type) {
		String protocol_result = null;
		switch (type) {
		case 1:
			protocol_result = LineProtocols.CMPP;
			break;
		case 2:
			protocol_result = LineProtocols.SMGP;
			break;
		case 3:
			protocol_result = LineProtocols.SGIP;
			break;
		default:
			protocol_result = LineProtocols.UNKN;
			break;
		}
		return protocol_result;
	}
	
	public static int lineProtocol2Type(String protocol) {
		int line_type_result = LineTypes.UNKN;
		if (LineProtocols.CMPP.equalsIgnoreCase(protocol)) {
			line_type_result = LineTypes.CMPP;
		} else if (LineProtocols.SMGP.equalsIgnoreCase(protocol)) {
			line_type_result = LineTypes.SMGP;
		} else if (LineProtocols.SGIP.equalsIgnoreCase(protocol)) {
			line_type_result = LineTypes.SGIP;
		}
		return line_type_result;
	}
	

	public static byte[] string2FixedBytes(String s, int fixLength) {
		ByteArrayBuffer resultBuffer = new ByteArrayBuffer(fixLength);
		byte sBytes[] = s == null ? new byte[0] : new byte[s.length()];
		int byLen = sBytes.length;
		for (int i = 0; i < byLen; i++) {
			sBytes[i] = (byte) s.charAt(i);
		}
		int fixZero = 0;
		if (byLen > fixLength) {
			byLen = fixLength;
		} else if (byLen < fixLength) {
			fixZero = fixLength - byLen;
		}
		resultBuffer.put(sBytes);

		for (; fixZero > 0; fixZero--) {
			resultBuffer.put((byte)0);
		}
		return resultBuffer.array();
	}

	public static int fromBcdBytes(byte[] bcdBytes, int index, int length) {
		StringBuffer temp = new StringBuffer(length * 2);
		for (int i = index; i < index + length; i++) {
			temp.append((char) ((bcdBytes[i] & 0xF0) >>> 4 | 0x30));
			temp.append((char) (bcdBytes[i] & 0x0F | 0x30));
		}
		return Integer.parseInt(temp.toString());
	}

	public static int fromBcdBytes(byte[] bcdBytes) {
		return fromBcdBytes(bcdBytes, 0, bcdBytes.length);
	}

	public static byte[] toBcdBytes(int value) {
		String str_value = "" + value;
		if (str_value.length() % 2 != 0)
			str_value = "0" + str_value;
		byte[] result = new byte[str_value.length() / 2];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) ((((str_value.charAt(i * 2) - '0') & 0x0F) << 4) | ((str_value.charAt(i * 2 + 1) - '0') & 0x0F));
		}
		return result;
	}

	public static byte[] toGsmBytes(String msg) {
		return toGsmBytes(msg, MessageCodes.ASCII);
	}

	public static byte[] toGsmBytes(String msg, int code) {
		byte result[] = null;
		if (msg != null) {
			switch (code) {
			case MessageCodes.UCS2:
				result = StringUtils.toUCS2Bytes(msg);
				break;
			case MessageCodes.GBK:
				try {
					result = msg.getBytes("GBK");
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			default:
				result = new byte[msg.length()];
				for (int i = 0; i < result.length; i++) {
					result[i] = (byte) msg.charAt(i);
				}
				break;
			}
		}
		return result;
	}

	public static String fromGsmBytes(byte[] msg) {
		return fromGsmBytes(msg, 0, msg == null ? 0 : msg.length, MessageCodes.ASCII);
	}

	public static String fromGsmBytes(byte[] msg, int index, int length) {
		return fromGsmBytes(msg, index, length, MessageCodes.ASCII);
	}

	public static String fromGsmBytes(byte[] msg, int index, int length, int code) {
		String result = null;
		if (msg != null) {
			switch (code) {
			case MessageCodes.UCS2:
				result = StringUtils.fromUCS2Bytes(msg, index, length);
				break;
			case MessageCodes.GBK:
				try {
					result = new String(msg, index, length, "GBK");
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			default:
				char msg_chars[] = new char[msg.length];
				for (int i = 0; i < msg.length; i++) {
					msg_chars[i] = (char) msg[i];
				}
				result = new String(msg_chars, index, length);
				break;
			}
		}
		return result;
	}

	public static String fromGsmBytes(byte[] msg, int code) {
		String result = null;
		if (msg != null) {
			result = fromGsmBytes(msg, 0, msg.length, code);
		}
		return result;
	}

	/**
	 * 是否为移动号码
	 * 
	 * @param userNumber
	 * @return
	 */
	@Deprecated
	public static boolean isPhoneNumberOfCM(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		return userNumber.startsWith("86134") || userNumber.startsWith("86135") || userNumber.startsWith("86136")
				|| userNumber.startsWith("86137") || userNumber.startsWith("86138") || userNumber.startsWith("86139")
				|| userNumber.startsWith("86147") || userNumber.startsWith("86150") || userNumber.startsWith("86151")
				|| userNumber.startsWith("86152") || userNumber.startsWith("86157") || userNumber.startsWith("86158")
				|| userNumber.startsWith("86159") || userNumber.startsWith("86187") || userNumber.startsWith("86188") 
				|| userNumber.startsWith("86183") || userNumber.startsWith("86182");
	}

	/**
	 * 是否为联通号码
	 * 
	 * @param userNumber
	 * @return
	 */
	@Deprecated
	public static boolean isPhoneNumberOfCU(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		return userNumber.startsWith("86130") || userNumber.startsWith("86131") || userNumber.startsWith("86132")
				|| userNumber.startsWith("86145") || userNumber.startsWith("86155") || userNumber.startsWith("86156")
				|| userNumber.startsWith("86186");
	}

	/**
	 * 是否为电信号码
	 * 
	 * @param userNumber
	 * @return
	 */
	@Deprecated
	public static boolean isPhoneNumberOfCT(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		return userNumber.startsWith("86133") || userNumber.startsWith("86153") || userNumber.startsWith("86180")
				|| userNumber.startsWith("86189");
	}

	@Deprecated
	public static String getSmsProtocolPrefixByPhoneNumber(String userNumber) throws IllegalArgumentException {
		String protocal_name = null;
		if (isPhoneNumberOfCT(userNumber)) {
			protocal_name = "SMGP";
		} else if (isPhoneNumberOfCU(userNumber)) {
			protocal_name = "SGIP";
		} else if (isPhoneNumberOfCM(userNumber)) {
			protocal_name = "CMPP";
		} else {
			throw new IllegalArgumentException("error phone number");
		}
		return protocal_name;
	}
	
	public static String getStandardPhoneNumberOfCN(String userNumber) {
		if (userNumber.startsWith("01")) {
			userNumber = userNumber.substring(1);
		}
		if (userNumber.startsWith("1")) {
			if (!userNumber.startsWith("86")) {
				userNumber = "86" + userNumber;
			}
		}
		return userNumber;
	}

}
