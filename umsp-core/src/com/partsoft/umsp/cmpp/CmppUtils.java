package com.partsoft.umsp.cmpp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.packet.PacketInputStream;
import com.partsoft.umsp.packet.PacketOutputStream;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.RandomUtils;

public abstract class CmppUtils {
	
	public static final String ARG_SUBMIT_PERSECOND_MAX = "cmpp.submit.persecond.max";
	
	public static final String ARG_DELIVER_PERSECOND_MAX = "cmpp.deliver.persecond.max"; 

	/**
	 * 发送流量控制最后计数
	 */
	public static final String ARG_FLOW_TOTAL = "cmpp.flow.total";

	/**
	 * 发送流量控制最后记录时间
	 */
	public static final String ARG_FLOW_LASTTIME = "cmpp.flow.lasttime";
	

	/**
	 * 接收流量控制最后计数
	 */
	public static final String ARG_RECV_FLOW_TOTAL = "cmpp.flow.total.recv";

	/**
	 * 接收流量控制最后记录时间
	 */
	public static final String ARG_RECV_FLOW_LASTTIME = "cmpp.flow.lasttime.recv";
		
	
	
	/**
	 * 请求连接数统计前缀
	 */
	public static final String ARG_REQUEST_CONNS = "cmpp.conns.";

	/**
	 * 服务号码
	 */
	public static final String ARG_SERVICE_NUMBER = "cmpp.service.number";

	/**
	 * 服务签名
	 */
	public static final String ARG_SERVICE_SIGN = "cmpp.service.sign";

	/**
	 * 输出流参数
	 */
	public static final String ARG_OUTPUT_STREAM = "cmpp.packet.output.stream";

	/**
	 * 输入流参数
	 */
	public static final String ARG_INPUT_STREAM = "cmpp.packet.input.stream";

	/**
	 * 请求数据包参数
	 */
	public static final String ARG_REQUEST_PACKET = "cmpp.request.packet";

	/**
	 * 用户是否绑定参数
	 */
	public static final String ARG_REQUEST_BINDED = "cmpp.user.binded";
	
	public static final String ARG_REQUEST_BINDING = "cmpp.user.binding";

	/**
	 * 上下文序列号参数
	 */
	public static final String ARG_CONTEXT_SEQUENCE = "cmpp.context.sequence";

	/**
	 * 请求序列号参数
	 */
	public static final String ARG_REQUEST_SEQUENCE = "cmpp.request.sequence";

	/**
	 * 已提交服务器的消息列表
	 */
	public static final String ARG_SUBMITTED_LIST = "cmpp.submitted.list";

	/**
	 * 已提交服务器的消息总数
	 */
	public static final String ARG_SUBMITTED_TOTAL = "cmpp.submitted.total";

	/**
	 * 已提交服务器并收到返回消息的总数
	 */
	public static final String ARG_SUBMITTED_RESULTS = "cmpp.post.results";

	/**
	 * @brief 已测试次数
	 */
	public static final String ARG_ACTIVE_TESTS = "cmpp.active.tests";

	/**
	 * 协议版本
	 */
	public static final String ARG_PROTOCOL_VERSION = "cmpp.protocol.version";

	public static int extractNodeTimeFromMsgID(long msgid) {
		long temp = (msgid >>> 38) & (long) 0x7FFFFFFFFFL;
		return (int) ((temp & 0x3F) + ((temp >>> 6) & 0x3F) * 100 + ((temp >>> 12) & 0x1F) * 10000
				+ ((temp >>> 17) & 0x1F) * 1000000 + ((temp >>> 22) & 0x0F) * 100000000);
	}

	public static int extractNodeIdFromMsgID(long msgid) {
		return (int) (((long) (msgid >>> 16) & 0x3FFFFF));
	}

	public static int extractNodeSeqFromMsgID(long msgid) {
		return (int) (msgid & 0xFFFF);
	}

	public static long generateMsgID(int node_id, int create_time, int seq) {
		long result = 0;
		byte time_bytes[] = UmspUtils.toBcdBytes(create_time);
		int second_value = time_bytes.length > 0 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 1, 1) : 0;
		int minute_value = time_bytes.length > 1 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 2, 1) : 0;
		int hours_value = time_bytes.length > 2 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 3, 1) : 0;
		int day_value = time_bytes.length > 3 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 4, 1) : 0;
		int month_value = time_bytes.length > 4 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 5, 1) : 0;

		result = (second_value & 0x3F) | ((minute_value & 0x3F) << 6) | ((hours_value & 0x1F) << 12)
				| (((day_value & 0x1F)) << 17) | ((month_value & 0x0F) << 22);
		return (result << 38) | (((long) (node_id & 0x3FFFFF) << 16)) | (seq & 0xFFFF);
	}

	public static String generateClientToken(String clientid, String sharedSecret, int timestamp) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(clientid.length() + sharedSecret.length() + 19);
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		for (int i = 0; i < clientid.length(); i++) {
			array_buffer.put((byte) clientid.charAt(i));
		}

		array_buffer.put(new byte[9]);
		for (int i = 0; i < sharedSecret.length(); i++) {
			array_buffer.put((byte) sharedSecret.charAt(i));
		}
		String timestamp_str = String.format("%010d", timestamp);
		for (int i = 0; i < timestamp_str.length(); i++) {
			array_buffer.put((byte) timestamp_str.charAt(i));
		}
		byte[] md5_bytes = md5_encode.digest(array_buffer.array());
		char[] md5_chars = new char[md5_bytes.length];
		for (int i = 0; i < md5_bytes.length; i++) {
			md5_chars[i] = (char) md5_bytes[i];
		}
		return new String(md5_chars);
	}

	/**
	 * 产生服务端返回密钥
	 * 
	 * @param status
	 * @param authenticator_client
	 * @param sharedSecret
	 *            必须为ascii码字符串
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String generateServerToken(int status, String authenticator_client, String sharedSecret) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(Buffer.INT_SIZE + authenticator_client.length()
				+ sharedSecret.length());
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		array_buffer.putInt(status);
		for (int i = 0; i < authenticator_client.length(); i++) {
			array_buffer.put((byte) authenticator_client.charAt(i));
		}
		for (int i = 0; i < sharedSecret.length(); i++) {
			array_buffer.put((byte) sharedSecret.charAt(i));
		}
		byte[] md5_bytes = md5_encode.digest(array_buffer.array());
		char[] md5_chars = new char[md5_bytes.length];
		for (int i = 0; i < md5_bytes.length; i++) {
			md5_chars[i] = (char) md5_bytes[i];
		}
		return new String(md5_chars);
	}

	/**
	 * 短连接发送(不重试，出错直接返回)
	 * 
	 * @param smgp_smg_host
	 * @param port
	 * @param sp_number
	 * @param enterprise_id
	 * @param uid
	 * @param pwd
	 * @param submits
	 * @throws Exception
	 */
	public static void postSubmit(String smgp_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits) throws Exception {
		postSubmit(smgp_smg_host, port, sp_number, enterprise_id, uid, pwd, submits, null);
	}

	/**
	 * 短连接发送(不重试，出错直接返回)
	 * 
	 * @param smgp_smg_host
	 * @param port
	 * @param sp_number
	 * @param enterprise_id
	 * @param uid
	 * @param pwd
	 * @param submits
	 * @param listener
	 * @throws Exception
	 */
	public static void postSubmit(String smgp_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits, TransmitListener listener) throws Exception {
		Client client = new Client(Constants.PROTOCOL_NAME, smgp_smg_host, port);
		client.setAutoReConnect(false);
		SimpleQueuedSmgpSPSendHandler smgp_handler = new SimpleQueuedSmgpSPSendHandler();
		smgp_handler.setAccount(uid);
		smgp_handler.setPassword(pwd);
		smgp_handler.setSpNumber(Integer.parseInt(sp_number));
		smgp_handler.setEnterpriseId(Integer.parseInt(enterprise_id));
		List<Submit> submits_list = new LinkedList<Submit>();
		for (Submit sb : submits) {
			submits_list.addAll(convertSubmits(sb));
		}
		smgp_handler.postSubmit(submits_list);
		if (listener != null) {
			smgp_handler.addTransmitListener(listener);
		}

		PacketContextHandler pktchr = new PacketContextHandler();
		pktchr.setContextProtocol(Constants.PROTOCOL_NAME);
		pktchr.setHandler(smgp_handler);
		client.setHandler(pktchr);
		client.start();
		client.join();
	}

	public static List<Submit> convertSubmits(Submit well_convert_submit) {
		return convertSubmits(well_convert_submit, 0, SMS.MAX_SMS_USER_NUMBERS);
	}

	public static List<Submit> convertSubmits(Submit well_convert_submit, int signPlaceHolderLen) {
		return convertSubmits(well_convert_submit, signPlaceHolderLen, SMS.MAX_SMS_USER_NUMBERS);
	}
	
	/**
	 * 把submit转换为Submit列表<br>
	 * <ul>
	 * <li>如果目标接收电话太多则自动分条目</li>
	 * <li>如果内容过长则自动俺长短信模式分条目</li>
	 * </ul>
	 * 
	 * @param well_convert_submit
	 *            原始下发对象
	 * @param signPlaceHolderLen
	 *            签名占位符长度
	 * @param maxDestUserOneSubmit
	 *            一次目标下发多少个手机
	 * @return
	 */
	public static List<Submit> convertSubmits(Submit well_convert_submit, int signPlaceHolderLen,
			int maxDestUserOneSubmit) {
		Assert.isTrue(maxDestUserOneSubmit >= 1 && maxDestUserOneSubmit <= SMS.MAX_SMS_USER_NUMBERS);
		Assert.isTrue(signPlaceHolderLen >= 0 && signPlaceHolderLen <= SMS.MAX_SMS_SIGN_LEN);

		String messageContext = well_convert_submit.getMessageContent();
		int maxOneMessageLen = SMS.MAX_SMS_ONEMSG_CONTENT - signPlaceHolderLen;
		int maxCascadeMessageLen = SMS.MAX_SMS_CASCADEMSG_CONTENT;
		int maxMessageLen = maxCascadeMessageLen * SMS.MAX_SMS_CASCADES;

		int pt = 1;
		int messageContextLen = messageContext.length();
		if (messageContextLen > maxOneMessageLen) {
			int realMessageContextLen = messageContextLen + signPlaceHolderLen;
			pt = realMessageContextLen / maxCascadeMessageLen;
			pt = (realMessageContextLen % maxCascadeMessageLen) > 0 ? pt + 1 : pt;
		}

		if (pt > SMS.MAX_SMS_CASCADES) {
			throw new IllegalArgumentException(String.format("短信内容最大长度为%s个字", maxMessageLen));
		}

		List<Submit> packetResults = new LinkedList<Submit>();
		int maxUserCount = well_convert_submit.destUserCount;
		int nlStart = 0;
		while (nlStart < maxUserCount) {
			List<Submit> results = new LinkedList<Submit>();
			String[] user_numbers = null;
			int nlLength = maxUserCount;
			if (maxUserCount <= maxDestUserOneSubmit) {
				user_numbers = well_convert_submit.destTerminalIds;
			} else {
				nlLength = maxUserCount - nlStart;
				nlLength = nlLength > maxDestUserOneSubmit ? maxDestUserOneSubmit : nlLength;
				user_numbers = new String[nlLength];
				System.arraycopy(well_convert_submit.destTerminalIds, nlStart, user_numbers, 0, nlLength);
			}
			nlStart += nlLength;
			if (pt > 1) {
				int i = 0;
				int blStart = 0;
				int blEnd = maxCascadeMessageLen > messageContextLen ? messageContextLen : maxCascadeMessageLen;
				byte rondPack = (byte) (((byte) (RandomUtils.randomInteger() % 255)) & 0xFF);
				while (blStart < messageContextLen) {
					i = i + 1;
					String packetMessage = messageContext.substring(blStart, blEnd);
					Submit submit = well_convert_submit.clone();
					submit.setCascadeMessageContent(packetMessage, rondPack, pt, i);
					submit.destUserCount = nlLength;
					submit.destTerminalIds = user_numbers;
					results.add(submit);
					blStart += packetMessage.length();
					blEnd += packetMessage.length();
					if (blEnd > messageContextLen) {
						blEnd = messageContextLen;
					}
				}
				if (i < pt) {
					i = i + 1;
					Submit submit = well_convert_submit.clone();
					submit.setCascadeMessageContent("", rondPack, pt, i);
					submit.destUserCount = nlLength;
					submit.destTerminalIds = user_numbers;
					results.add(submit);
				}
			}
			if (results.size() <= 0) {
				Submit submit = well_convert_submit.clone();
				submit.destUserCount = nlLength;
				submit.destTerminalIds = user_numbers;
				results.add(submit);
			}
			packetResults.addAll(results);
		}
		return new ArrayList<Submit>(packetResults);
	}

	// --
	/**
	 * 从协议的请求中提取协议数据包
	 * 
	 * @param request
	 * @return {@link SmgpDataPacket}
	 */
	public static CmppDataPacket extractRequestPacket(Request request) {
		return (CmppDataPacket) request.getAttribute(ARG_REQUEST_PACKET);
	}

	/**
	 * 设置协议的请求数据包
	 * 
	 * @param request
	 * @param packet
	 */
	public static void setupRequestPacket(Request request, CmppDataPacket packet) {
		if (packet != null) {
			request.setAttribute(ARG_REQUEST_PACKET, packet);
		} else {
			request.removeAttribute(ARG_REQUEST_PACKET);
		}
	}

	/**
	 * 判断请求是否已绑定
	 * 
	 * @param request
	 * @return true 表示已绑定
	 */
	public static boolean testRequestBinded(Request request) {
		Object value = request.getAttribute(ARG_REQUEST_BINDED);
		return CompareUtils.nullSafeEquals(value, Boolean.TRUE);
	}
	
	/**
	 * 配置请求是否绑定
	 * 
	 * @param request
	 * @param binded
	 */
	public static void setupRequestBinded(Request request, boolean binded) {
		if (binded) {
			request.setAttribute(ARG_REQUEST_BINDED, Boolean.TRUE);
		} else {
			request.removeAttribute(ARG_REQUEST_BINDED);
		}
	}

	/**
	 * 产生SEQ
	 * 
	 * @return
	 */
	public static int generateContextSequence(Context context) {
		int result = 1;
		synchronized (context) {
			Integer seq = (Integer) context.getAttribute(ARG_CONTEXT_SEQUENCE);
			if (seq == null) {
				seq = 0;
			}
			seq = seq + 1;
			if (seq >= 0xFFFF) {
				seq = 1;
			}
			result = seq;
			context.setAttribute(ARG_CONTEXT_SEQUENCE, seq);
		}
		return result;
	}

	public static int generateRequestSequence(Request request) {
		int result = 1;
		synchronized (request) {
			Integer seq = (Integer) request.getAttribute(ARG_REQUEST_SEQUENCE);
			if (seq == null) {
				seq = 0;
			}
			seq = seq + 1;
			if (seq >= Integer.MAX_VALUE) {
				seq = 1;
			}
			result = seq;
			request.setAttribute(ARG_REQUEST_SEQUENCE, seq);
		}
		return result;
	}

	/**
	 * 获得提交已返回总数
	 * 
	 * @return
	 */
	public static int extractRequestSubmittedRepliedCount(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_SUBMITTED_RESULTS);
		return result == null ? 0 : result < 0 ? 0 : result;
	}

	/**
	 * 更新提交已返回的总数
	 * 
	 * @param request
	 * @param count
	 */
	public static void updateSubmittedRepliedCount(Request request, int count) {
		if (testRequestSubmiting(request)) {
			request.setAttribute(ARG_SUBMITTED_RESULTS, count);
		}
	}

	/**
	 * 判断是否存在提交的submit请求但尚未返回
	 * 
	 * @return
	 */
	public static boolean testRequestSubmiting(Request request) {
		boolean result = extractRequestSubmittedCount(request) > 0;
		return result;
	}

	/**
	 * 获得已提交服务器的消息个数
	 * 
	 * @return
	 */
	public static int extractRequestSubmittedCount(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_SUBMITTED_TOTAL);
		return result == null ? 0 : result < 0 ? 0 : result;
	}

	/**
	 * 获得已提交服务器的消息列表 (包括尚未提交完成的)
	 * 
	 * @param request
	 * @return
	 */
	public static List<?> extractRequestSubmitteds(Request request) {
		List<?> result = (List<?>) request.getAttribute(ARG_SUBMITTED_LIST);
		result = result == null ? Collections.EMPTY_LIST : result;
		return result;
	}

	/**
	 * 清除请求序列中所有的提交信息
	 * 
	 * @param request
	 */
	public static void cleanRequestSubmitteds(Request request) {
		request.removeAttribute(ARG_SUBMITTED_LIST);
		request.removeAttribute(ARG_SUBMITTED_TOTAL);
		request.removeAttribute(ARG_SUBMITTED_RESULTS);
	}

	/**
	 * 把已发送至服务器的消息列表保存快照
	 * 
	 * @param list
	 *            列表
	 * @param total
	 *            总数
	 */
	public static void updateSubmitteds(Request request, List<?> list, int total) {
		List<?> posts = extractRequestSubmitteds(request);
		if (posts != list) {
			request.setAttribute(ARG_SUBMITTED_LIST, list);
		}
		request.setAttribute(ARG_SUBMITTED_TOTAL, total);
	}

	/**
	 * 获取请求相关的数据包输入流
	 * 
	 * @param request
	 * @return {@link PacketInputStream}
	 * @throws IOException
	 */
	public static PacketInputStream extractRequestPacketStream(Request request) throws IOException {
		PacketInputStream datastream = (PacketInputStream) request.getAttribute(ARG_INPUT_STREAM);
		if (datastream == null) {
			datastream = new PacketInputStream(request.getInputStream());
			request.setAttribute(ARG_INPUT_STREAM, datastream);
		}
		return datastream;
	}

	/**
	 * 获取应答相关的数据包输出流
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public static PacketOutputStream extractResponsePacketStream(Request request, Response response) throws IOException {
		PacketOutputStream datastream = (PacketOutputStream) request.getAttribute(ARG_OUTPUT_STREAM);
		if (datastream == null) {
			datastream = new PacketOutputStream(response.getOutputStream());
			request.setAttribute(ARG_OUTPUT_STREAM, datastream);
		}
		return datastream;
	}

	/**
	 * 把数据包输出至数据包输出流
	 * 
	 * @param pakcetOutputStream
	 * @param packet
	 * @throws IOException
	 */
	public static void renderDataPacket(PacketOutputStream pakcetOutputStream, CmppDataPacket packet)
			throws IOException {
		pakcetOutputStream.writeInt(packet.getBufferSize());
		packet.writeExternal(pakcetOutputStream);
	}

	/**
	 * 把数据包输出至请求关联应答中。
	 * 
	 * @param request
	 * @param response
	 * @param packet
	 * @throws IOException
	 */
	public static void renderDataPacket(Request request, Response response, CmppDataPacket packet) throws IOException {
		renderDataPacket(extractResponsePacketStream(request, response), packet);
	}

	/**
	 * 清除链路测试信息
	 * 
	 * @param request
	 */
	public static void cleanRequestActiveTesting(Request request) {
		request.removeAttribute(ARG_ACTIVE_TESTS);
	}

	/**
	 * 判断是否正在进行链路测试
	 * 
	 * @return
	 */
	public static boolean testRequestActiveTesting(Request request) {
		Integer test_totals = (Integer) request.getAttribute(ARG_ACTIVE_TESTS);
		return test_totals != null && test_totals > 0;
	}

	public static void stepIncreaseRequestActiveTest(Request request) {
		request.setAttribute(ARG_ACTIVE_TESTS, extractRequestActiveTestCount(request) + 1);
	}

	/**
	 * 返回已测试的次数
	 * 
	 * @return
	 */
	public static int extractRequestActiveTestCount(Request request) {
		Integer test_totals = (Integer) request.getAttribute(ARG_ACTIVE_TESTS);
		return test_totals == null || test_totals < 0 ? 0 : test_totals;
	}

	public static void updateRequestProtocolVersion(Request request, int version) {
		request.setAttribute(ARG_PROTOCOL_VERSION, version);
	}

	public static void cleanRequestAttributes(Request request) {
		List<String> names = new LinkedList<String>();
		Enumeration<String> enums = request.getAttributeNames();
		while (enums.hasMoreElements()) {
			names.add(enums.nextElement());
		}
		for (String name : names) {
			request.removeAttribute(name);
		}
	}
	
	public static void setupRequestMaxSubmitPerSecond(Request request, int max) {
		if (max > 0) {
			request.setAttribute(ARG_SUBMIT_PERSECOND_MAX, max);
		} else {
			request.removeAttribute(ARG_SUBMIT_PERSECOND_MAX);
		}
	}
	
	public static int extractRequestMaxSubmitPerSecond(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_SUBMIT_PERSECOND_MAX);
		return result == null ? 0 : result.intValue();
	}
	
	
	public static void setupRequestMaxDeliverPerSecond(Request request, int max) {
		if (max > 0) {
			request.setAttribute(ARG_DELIVER_PERSECOND_MAX, max);
		} else {
			request.removeAttribute(ARG_DELIVER_PERSECOND_MAX);
		}
	}
	
	public static int extractRequestMaxDeliverPerSecond(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_DELIVER_PERSECOND_MAX);
		return result == null ? 0 : result.intValue();
	}

	public static void setupRequestServiceNumber(Request request, String serviceNumber) {
		request.setAttribute(ARG_SERVICE_NUMBER, serviceNumber);
	}

	public static void setupRequestServiceSignature(Request request, String signature) {
		request.setAttribute(ARG_SERVICE_SIGN, signature);
	}

	public static String extractRequestServiceNumber(Request request) {
		return (String) request.getAttribute(ARG_SERVICE_NUMBER);
	}
	
	public static String extractRequestServiceSignature(Request request) {
		return (String) request.getAttribute(ARG_SERVICE_SIGN);
	}

	public static void updateRequestFlowTotal(Request request, long flowLastTime, int count) {
		request.setAttribute(ARG_FLOW_TOTAL, count);
		request.setAttribute(ARG_FLOW_LASTTIME, flowLastTime);
	}

	public static long extractRequestFlowLastTime(Request request) {
		Long result = (Long) request.getAttribute(ARG_FLOW_LASTTIME);
		return result == null ? System.currentTimeMillis() : result;
	}

	public static int extractRequestFlowTotal(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_FLOW_TOTAL);
		return result == null ? 0 : result;
	}
	
	public static int extractRequestConnectionTotal(Request request, String serviceNumber) {
		String arg_params;
		Integer conns_total = 0;
		if (!StringUtils.hasText(serviceNumber)) {
			arg_params = ARG_REQUEST_CONNS + "0000";
		} else { 
			arg_params = ARG_REQUEST_CONNS + serviceNumber;
		}
		Context context = request.getContext();
		synchronized (context) {
			conns_total = (Integer) context.getAttribute(arg_params);
		}
		return conns_total == null ? 0 : conns_total.intValue();
	}
	
	public static void stepIncreaseRequestConnection(Request request, String serviceNumber) {
		String arg_params;
		Integer conns_total = 0;
		if (!StringUtils.hasText(serviceNumber)) {
			arg_params = ARG_REQUEST_CONNS + "0000";
		} else { 
			arg_params = ARG_REQUEST_CONNS + serviceNumber;
		}
		Context context = request.getContext();
		synchronized (context) {
			conns_total = (Integer) context.getAttribute(arg_params);
			conns_total = conns_total == null ? 0 : conns_total;
			if (conns_total < 0) { 
				conns_total = 0;
			}
			conns_total = conns_total + 1;
			context.setAttribute(arg_params, conns_total);
		}
	}
	
	
	public static void stepDecrementRequestConnection(Request request, String serviceNumber) {
		String arg_params;
		Integer conns_total = 0;
		if (!StringUtils.hasText(serviceNumber)) {
			arg_params = ARG_REQUEST_CONNS + "0000";
		} else { 
			arg_params = ARG_REQUEST_CONNS + serviceNumber;
		}
		Context context = request.getContext();
		synchronized (context) {
			conns_total = (Integer) context.getAttribute(arg_params);
			conns_total = conns_total == null ? 0 : conns_total - 1;
			if (conns_total <= 0) { 
				context.removeAttribute(arg_params);
			} else {
				context.setAttribute(arg_params, conns_total);
			}
		}
	}


	public static boolean testRequestBinding(Request request) {
		Object value = request.getAttribute(ARG_REQUEST_BINDING);
		return CompareUtils.nullSafeEquals(value, Boolean.TRUE);
	}
	
	public static void setupRequestBinding(Request request, boolean binded) {
		if (binded) {
			request.setAttribute(ARG_REQUEST_BINDING, Boolean.TRUE);
		} else {
			request.removeAttribute(ARG_REQUEST_BINDING);
		}
	}	
	
	public static void updateRequestReceiveFlowTotal(Request request, long flowLastTime, int count) {
		request.setAttribute(ARG_RECV_FLOW_TOTAL, count);
		request.setAttribute(ARG_RECV_FLOW_LASTTIME, flowLastTime);
	}

	public static long extractRequestReceiveFlowLastTime(Request request) {
		Long result = (Long) request.getAttribute(ARG_RECV_FLOW_LASTTIME);
		return result == null ? System.currentTimeMillis() : result;
	}

	public static int extractRequestReceiveFlowTotal(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_RECV_FLOW_TOTAL);
		return result == null ? 0 : result;
	}
	
	public static void main(String[] args) {
		Submit submit = new Submit();
		submit.setUserNumbers("13317312768");
		submit.setMessageContent("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		System.out.println(convertSubmits(submit, 6, 1).size());
	}

}
