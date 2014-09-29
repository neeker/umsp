package com.partsoft.umsp.sgip;

import java.io.IOException;
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
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.utils.Assert;

public abstract class AbstractSgipContextHandler extends AbstractContextHandler implements Handler {

	private static final Map<Integer, SgipDataPacket> sgip_packet_maps = new HashMap<Integer, SgipDataPacket>(10);

	static {
		addSGIPCommands(new Bind());
		addSGIPCommands(new BindResponse());

		addSGIPCommands(new Submit());
		addSGIPCommands(new SubmitResponse());

		addSGIPCommands(new Deliver());
		addSGIPCommands(new DeliverResponse());

		addSGIPCommands(new Report());
		addSGIPCommands(new ReportResponse());

		addSGIPCommands(new UnBind());
		addSGIPCommands(new UnBindResponse());
	}

	protected Map<Integer, SgipDataPacket> context_sgip_packet_maps;

	//收到错误命令时是否抛出异常
	protected boolean errorCommandException = false;
	
	//包提交重试次数
	protected int packetSubmitRetryTimes = 3;
	
	public void setPacketSubmitRetryTimes(int packetSubmitRetryTimes) {
		this.packetSubmitRetryTimes = packetSubmitRetryTimes;
	}

	public void setErrorCommandException(boolean errorCommandException) {
		this.errorCommandException = errorCommandException;
	}

	protected static void addSGIPCommands(SgipDataPacket packet) {
		sgip_packet_maps.put(packet.getCommand(), packet);
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		context_sgip_packet_maps = new HashMap<Integer, SgipDataPacket>(sgip_packet_maps);
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		context_sgip_packet_maps = null;
	}

	public AbstractSgipContextHandler() {

	}

	protected void doBind(Request request, Response response) throws IOException {
		if (SgipUtils.testRequestBinded(request)) {
			Log.warn(String.format("duplicate error bind request packet, packet is:\n%s", SgipUtils
					.extractRequestPacket(request).toString()));
			response.finalBuffer();
		}
	}

	protected void doBindResponse(Request request, Response response) throws IOException {

	}

	protected void doUnBind(Request request, Response response) throws IOException {
		UnBind unbind = (UnBind) SgipUtils.extractRequestPacket(request);
		UnBindResponse resp = new UnBindResponse();
		resp.result = 0;
		SgipUtils.copySerialNumber(resp, unbind);
		SgipUtils.renderDataPacket(request, response, resp);
		response.finalBuffer();
	}

	protected void doUnBindResponse(Request request, Response response) throws IOException {
		response.finalBuffer();
	}

	protected void doReport(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
	}

	protected void doReportResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
	}

	protected void doDeliver(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
	}

	protected void doDeliverResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
	}

	protected void doSubmit(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
	}

	protected void doSubmitResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SgipUtils.testRequestBinded(request));
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
		SgipUtils.cleanRequestAttributes(request);
		super.handleDisConnect(request, response);
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			SgipDataPacket packet = readPacketObject(SgipUtils.extractRequestPacketStream(request));
			if (packet == null) return;
			SgipUtils.setupRequestPacket(request, packet);
			switch (packet.command) {
			case Commands.BIND:
				doBind(request, response);
				break;
			case Commands.BIND_RESPONSE:
				doBindResponse(request, response);
				break;
			case Commands.UNBIND:
				doUnBind(request, response);
				break;
			case Commands.UNBIND_RESPONSE:
				doUnBindResponse(request, response);
				break;
			case Commands.DELIVER:
				doDeliver(request, response);
				break;
			case Commands.DELIVER_RESPONSE:
				doDeliverResponse(request, response);
				break;
			case Commands.SUBMIT:
				doSubmit(request, response);
				break;
			case Commands.SUBMIT_RESPONSE:
				doSubmitResponse(request, response);
				break;
			case Commands.REPORT:
				doReport(request, response);
				break;
			case Commands.REPORT_RESPONSE:
				doReportResponse(request, response);
				break;
			default:
				response.finalBuffer();
				break;
			}
		} finally {
			SgipUtils.setupRequestPacket(request, null);
			response.flushBuffer();
		}
	}

	public SgipDataPacket readPacketObject(PacketInputStream in) throws PacketException {
		SgipDataPacket result = null;
		try {
			in.mark(Buffer.INT_SIZE);
			int command = in.readInt();
			in.reset();
			SgipDataPacket packet = context_sgip_packet_maps.get(command);
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
			Log.error(e.getMessage(), e);
			throw new PacketException(e);
		}
		return result;
	}

}
