package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.packet.PacketInputStream;
import com.partsoft.umsp.packet.PacketOutputStream;
import com.partsoft.utils.Assert;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.RandomUtils;

public abstract class SgipUtils {
	
	/**
	 * 接收流量控制最后计数
	 */
	public static final String ARG_RECV_FLOW_TOTAL = "sgip.flow.total.recv";

	/**
	 * 接收流量控制最后记录时间
	 */
	public static final String ARG_RECV_FLOW_LASTTIME = "sgip.flow.lasttime.recv";
	
	public static final String ARG_SUBMIT_PERSECOND_MAX = "sgip.submit.persecond.max";
	
	public static final String ARG_DELIVER_PERSECOND_MAX = "sgip.deliver.persecond.max"; 

	/**
	 * 流量控制最后计数
	 */
	public static final String ARG_FLOW_TOTAL = "sgip.flow.total";
	
	/**
	 * 请求连接数统计前缀
	 */
	public static final String ARG_REQUEST_CONNS = "sgip.conns.";

	/**
	 * 流量控制最后记录时间
	 */
	public static final String ARG_FLOW_LASTTIME = "sgip.flow.lasttime";

	/**
	 * 服务号码
	 */
	public static final String ARG_SERVICE_NUMBER = "sgip.service.number";

	/**
	 * 服务签名
	 */
	public static final String ARG_SERVICE_SIGN = "sgip.service.sign";
	
	/**
	 * 输出流参数
	 */
	public static final String ARG_OUTPUT_STREAM = "sgip.packet.output.stream";

	/**
	 * 输入流参数
	 */
	public static final String ARG_INPUT_STREAM = "sgip.packet.input.stream";

	/**
	 * 请求数据包参数
	 */
	public static final String ARG_REQUEST_PACKET = "sgip.request.packet";

	/**
	 * 用户是否绑定参数
	 */
	public static final String ARG_REQUEST_BINDED = "sgip.user.binded";
	
	public static final String ARG_REQUEST_BINDING = "sgip.user.binding";

	/**
	 * 序列号参数
	 */
	public static final String ARG_REQUEST_SEQUENCE = "sgip.request.sequence";
	
	public static final String ARG_CONTEXT_SEQUENCE = "sgip.context.sequence";

	/**
	 * 已提交服务器的消息列表
	 */
	public static final String ARG_SUBMITTED_LIST = "sgip.submitted.list";

	/**
	 * 已提交服务器的消息总数
	 */
	public static final String ARG_SUBMITTED_TOTAL = "sgip.submitted.total";

	/**
	 * 已提交服务器并收到返回消息的总数
	 */
	public static final String ARG_SUBMITTED_RESULTS = "sgip.post.results";

	/**
	 * 复制序列号
	 * 
	 * @param dest
	 * @param src
	 */
	public static void copySerialNumber(SgipDataPacket dest, SgipDataPacket src) {
		dest.node_id = src.node_id;
		dest.timestamp = src.timestamp;
		dest.sequence = src.sequence;
	}

	/**
	 * 填充序列号
	 * 
	 * @param dest
	 *            填充目标包对象
	 * @param request
	 *            请求对象
	 * @param node_id
	 *            节点编号
	 * @param mills_time
	 *            时间(毫秒)
	 */
	@Deprecated
	public static void stuffSerialNumber(SgipDataPacket dest, Request request, int node_id, long mills_time) {
		dest.node_id = node_id;
		dest.timestamp = CalendarUtils.getTimestampInYearDuring(mills_time);
		dest.sequence = generateRequestSequence(request);
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
	 * <li>如果内容过长则自动按长短信模式分条目</li>
	 * </ul>
	 * 
	 * @param well_convert_submit
	 *            原始submit对象
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
		int maxUserCount = well_convert_submit.user_count;
		int nlStart = 0;
		while (nlStart < maxUserCount) {
			List<Submit> results = new LinkedList<Submit>();

			String[] user_numbers = null;
			int nlLength = maxUserCount;
			if (maxUserCount <= maxDestUserOneSubmit) {
				user_numbers = well_convert_submit.user_number;
			} else {
				nlLength = maxUserCount - nlStart;
				nlLength = nlLength > maxDestUserOneSubmit ? maxDestUserOneSubmit : nlLength;
				user_numbers = new String[nlLength];
				System.arraycopy(well_convert_submit.user_number, nlStart, user_numbers, 0, nlLength);
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
					submit.user_count = nlLength;
					submit.user_number = user_numbers;
					submit.setCascadeMessageContent(packetMessage, rondPack, pt, i);
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
					submit.user_count = nlLength;
					submit.user_number = user_numbers;
					submit.setCascadeMessageContent("", rondPack, pt, i);
					results.add(submit);
				}
			}
			if (results.size() <= 0) {
				Submit submit = well_convert_submit.clone();
				submit.user_count = nlLength;
				submit.user_number = user_numbers;
				results.add(submit);
			}
			packetResults.addAll(results);
		}
		return new ArrayList<Submit>(packetResults);
	}

	/**
	 * 短连接提交短信
	 * 
	 * @param sgip_smg_host
	 *            网关IP地址
	 * @param port
	 *            端口
	 * @param sp_number
	 *            SP接入号
	 * @param enterprise_id
	 *            企业号
	 * @param uid
	 *            用户名
	 * @param pwd
	 *            密码
	 * @param submits
	 *            提交的短信数组
	 * @throws Exception
	 */
	public static void postSubmit(String sgip_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits) throws Exception {
		postSubmit(sgip_smg_host, port, sp_number, enterprise_id, uid, pwd, submits, null);
	}

	/**
	 * 短连接提交短信
	 * 
	 * @param sgip_smg_host
	 *            网关IP地址
	 * @param port
	 *            端口
	 * @param sp_number
	 *            SP接入号
	 * @param enterprise_id
	 *            企业号
	 * @param uid
	 *            用户名
	 * @param pwd
	 *            密码
	 * @param submits
	 *            提交的短信数组
	 * @param listener
	 *            提交监听器
	 * @throws Exception
	 */
	public static void postSubmit(String sgip_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits, TransmitListener listener) throws Exception {
		Client client = new Client(Constants.PROTOCOL_NAME, sgip_smg_host, port);
		client.setMaxConnection(1);
		client.setAutoReConnect(false);
		SimpleQueuedSgipSPSendHandler sgip_handler = new SimpleQueuedSgipSPSendHandler();
		sgip_handler.setAccount(uid);
		sgip_handler.setPassword(pwd);
		sgip_handler.setSpNumber(sp_number);
		sgip_handler.setEnterpriseId(enterprise_id);
		sgip_handler.postSubmit(Arrays.asList(submits));
		if (listener != null) {
			sgip_handler.addTransmitListener(listener);
		}
		PacketContextHandler pktchr = new PacketContextHandler();
		pktchr.setContextProtocol(Constants.PROTOCOL_NAME);
		pktchr.setHandler(sgip_handler);
		client.setHandler(pktchr);
		client.start();
		client.join();
	}

	/**
	 * 从SGIP协议的请求中提取SGIP协议数据包
	 * 
	 * @param request
	 * @return {@link SgipDataPacket}
	 */
	public static SgipDataPacket extractRequestPacket(Request request) {
		return (SgipDataPacket) request.getAttribute(ARG_REQUEST_PACKET);
	}

	/**
	 * 设置SGIP协议的请求数据包
	 * 
	 * @param request
	 * @param packet
	 */
	public static void setupRequestPacket(Request request, SgipDataPacket packet) {
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
	
	public static int generateContextSequence(Context ctx) {
		int result = 1;
		synchronized (ctx) {
			Integer seq = (Integer) ctx.getAttribute(ARG_CONTEXT_SEQUENCE);
			if (seq == null) {
				seq = 0;
			}
			seq = seq + 1;
			if (seq >= Integer.MAX_VALUE) {
				seq = 1;
			}
			result = seq;
			ctx.setAttribute(ARG_CONTEXT_SEQUENCE, seq);
		}
		return result;
	}


	/**
	 * 产生SEQ
	 * 
	 * @return
	 */
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
	public static void renderDataPacket(PacketOutputStream pakcetOutputStream, SgipDataPacket packet)
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
	public static void renderDataPacket(Request request, Response response, SgipDataPacket packet) throws IOException {
		renderDataPacket(extractResponsePacketStream(request, response), packet);
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
	
	public static void setupRequestMaxSubmitPerSecond(Request request, int max) {
		if (max > 0) {
			request.setAttribute(ARG_SUBMIT_PERSECOND_MAX, max);
		} else {
			request.removeAttribute(ARG_SUBMIT_PERSECOND_MAX);
		}
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
	
	public static void setupRequestMaxDeliverPerSecond(Request request, int max) {
		if (max > 0) {
			request.setAttribute(ARG_DELIVER_PERSECOND_MAX, max);
		} else {
			request.removeAttribute(ARG_DELIVER_PERSECOND_MAX);
		}
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
	
	public static int extractRequestMaxSubmitPerSecond(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_SUBMIT_PERSECOND_MAX);
		return result == null ? 0 : result.intValue();
	}
	

	public static long extractRequestReceiveFlowLastTime(Request request) {
		Long result = (Long) request.getAttribute(ARG_RECV_FLOW_LASTTIME);
		return result == null ? System.currentTimeMillis() : result;
	}

	public static int extractRequestReceiveFlowTotal(Request request) {
		Integer result = (Integer) request.getAttribute(ARG_RECV_FLOW_TOTAL);
		return result == null ? 0 : result;
	}
	
	public static void updateRequestReceiveFlowTotal(Request request, long flowLastTime, int count) {
		request.setAttribute(ARG_RECV_FLOW_TOTAL, count);
		request.setAttribute(ARG_RECV_FLOW_LASTTIME, flowLastTime);
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

	public static String generateClientToken(String clientid, String sharedSecret, long timestamp) {
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

}
