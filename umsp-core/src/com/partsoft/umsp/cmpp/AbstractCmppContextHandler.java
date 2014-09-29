package com.partsoft.umsp.cmpp;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.partsoft.umsp.Handler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.AbstractContextHandler;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.packet.PacketInputStream;
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.utils.Assert;

public abstract class AbstractCmppContextHandler extends AbstractContextHandler implements Handler {

	private static final Map<Integer, CmppDataPacket> cmpp_packet_maps = new HashMap<Integer, CmppDataPacket>(10);

	static {
		addCmppCommands(new Connect());
		addCmppCommands(new ConnectResponse());

		addCmppCommands(new Submit());
		addCmppCommands(new SubmitResponse());

		addCmppCommands(new Deliver());
		addCmppCommands(new DeliverResponse());

		addCmppCommands(new ActiveTest());
		addCmppCommands(new ActiveTestResponse());

		addCmppCommands(new Terminate());
		addCmppCommands(new TerminateResponse());
	}

	protected Map<Integer, CmppDataPacket> context_cmpp_packet_maps;

	protected static void addCmppCommands(CmppDataPacket packet) {
		cmpp_packet_maps.put(packet.getCommandId(), packet);
	}

	protected int protocolVersion = Constants.VERSION2;

	//收到错误数据包是否抛出异常
	protected boolean errorCommandException = false;
	
	//包提交重试次数
	protected int packetSubmitRetryTimes = 3;
	
	public void setPacketSubmitRetryTimes(int packetSubmitRetryTimes) {
		Assert.isTrue(packetSubmitRetryTimes >= 1, "包重复提交次数必须大于等于1次");
		this.packetSubmitRetryTimes = packetSubmitRetryTimes;
	}
	
	public void setErrorCommandException(boolean errorCommandException) {
		this.errorCommandException = errorCommandException;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		context_cmpp_packet_maps = Collections.unmodifiableMap(new HashMap<Integer, CmppDataPacket>(cmpp_packet_maps));
		// 设置版本号
		for (CmppDataPacket packet : context_cmpp_packet_maps.values()) {
			packet.protocolVersion = this.protocolVersion;
		}
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		context_cmpp_packet_maps = null;
	}

	public AbstractCmppContextHandler() {

	}

	protected void doBind(Request request, Response response) throws IOException {
		if (CmppUtils.testRequestBinded(request)) {
			Log.warn(String.format("收到重复的绑定请求包:\n%s\n", CmppUtils.extractRequestPacket(request).toString()));
			response.finalBuffer();
		}
	}

	protected void doActiveTest(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
		ActiveTest active_test = (ActiveTest) CmppUtils.extractRequestPacket(request);
		ActiveTestResponse test_response = ((ActiveTestResponse) this.context_cmpp_packet_maps
				.get(Commands.CMPP_ACTIVE_TEST_RESP)).clone();
		test_response.sequenceId = active_test.sequenceId;
		CmppUtils.renderDataPacket(request, response, test_response);
		response.flushBuffer();
	}

	protected void doActiveTestRequest(Request request, Response response) throws IOException {
		// TODO 临时日志
		// Log.warn("超时发送链路侦测包...");
		CmppUtils.stepIncreaseRequestActiveTest(request);
		ActiveTest test = ((ActiveTest) this.context_cmpp_packet_maps.get(Commands.CMPP_ACTIVE_TEST)).clone();
		test.sequenceId = CmppUtils.generateRequestSequence(request);
		CmppUtils.renderDataPacket(request, response, test);
		response.flushBuffer();
	}

	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
		ActiveTestResponse test_response = (ActiveTestResponse) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(test_response.toString());
		}
	}

	protected void doBindResponse(Request request, Response response) throws IOException {

	}

	protected void doUnBind(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
		Terminate terminate = (Terminate) CmppUtils.extractRequestPacket(request);
		TerminateResponse exit_res = (TerminateResponse) this.context_cmpp_packet_maps
				.get(Commands.CMPP_TERMINATE_RESP).clone();
		exit_res.sequenceId = terminate.sequenceId;
		CmppUtils.renderDataPacket(request, response, exit_res);
		response.finalBuffer();
	}

	protected void doUnBindResponse(Request request, Response response) throws IOException {
		response.finalBuffer();
	}

	protected void doDeliver(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
	}

	protected void doDeliverResponse(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
	}

	protected void doSubmit(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));

	}

	protected void doSubmitResponse(Request request, Response response) throws IOException {
		Assert.isTrue(CmppUtils.testRequestBinded(request));
	}

	protected abstract void doBindRequest(Request request, Response response) throws IOException;

	private void doRequestStart(Request request, Response response) throws IOException {
		doBindRequest(request, response);
	}

	@Override
	protected void handleConnect(Request request, Response response) throws IOException {
		super.handleConnect(request, response);
		doRequestStart(request, response);
	}

	@Override
	protected void handleDisConnect(Request request, Response response) {
		CmppUtils.cleanRequestAttributes(request);
		super.handleDisConnect(request, response);
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			CmppUtils.cleanRequestActiveTesting(request);
			CmppDataPacket packet = readPacketObject(CmppUtils.extractRequestPacketStream(request));
			if (packet == null)
				return;
			CmppUtils.setupRequestPacket(request, packet);
			switch (packet.getCommandId()) {
			case Commands.CMPP_CONNECT:
				doBind(request, response);
				break;
			case Commands.CMPP_CONNECT_RESP:
				doBindResponse(request, response);
				break;
			case Commands.CMPP_TERMINATE:
				doUnBind(request, response);
				break;
			case Commands.CMPP_TERMINATE_RESP:
				doUnBindResponse(request, response);
				break;
			case Commands.CMPP_DELIVER:
				doDeliver(request, response);
				break;
			case Commands.CMPP_DELIVER_RESP:
				doDeliverResponse(request, response);
				break;
			case Commands.CMPP_SUBMIT:
				doSubmit(request, response);
				break;
			case Commands.CMPP_SUBMIT_RESP:
				doSubmitResponse(request, response);
				break;
			case Commands.CMPP_ACTIVE_TEST:
				doActiveTest(request, response);
				break;
			case Commands.CMPP_ACTIVE_TEST_RESP:
				doActiveTestResponse(request, response);
				break;
			default:
				response.finalBuffer();
				break;
			}
		} finally {
			response.flushBuffer();
		}
	}

	public CmppDataPacket readPacketObject(PacketInputStream in) throws PacketException {
		CmppDataPacket result = null;
		try {
			in.mark(Buffer.INT_SIZE);
			int command = in.readInt();
			in.reset();
			CmppDataPacket packet = context_cmpp_packet_maps.get(command);
			if (packet != null) {
				packet = packet.clone();
				packet.readExternal(in);
				result = packet;
			} else {
				IOException errException = new IOException(String.format("收到网关错误的指令: %d", command));
				int dataCount = in.available();
				ByteArrayBuffer buffer = new ByteArrayBuffer(dataCount);
				try {
					int readedCount = buffer.readFrom(in, dataCount);
					Log.warn(String.format("收到网关错误的数据包(%d字节)\n%s\n", readedCount, buffer.toAllDetailString()));
				} catch (Throwable e) {
					Log.error(
							String.format("读取并打印网关错误指令包失败: %s\n，已读取到的内容: %s ", e.getMessage(),
									buffer.toAllDetailString()), e);
				}
				if (this.errorCommandException) {
					throw errException;
				} else {
					Log.warn(String.format("忽略错误(%s)", errException.getMessage()), errException);
				}
			}
		} catch (IOException e) {
			throw new PacketException(e);
		}
		return result;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

}
