package com.partsoft.umsp;

import java.io.IOException;

import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.packet.PacketRequest;

public interface Connector extends BufferPools, LifeCycle {

	String getName();

	String getProtocol();

	void open() throws IOException;

	void close() throws IOException;

	void setOrigin(OriginHandler server);

	OriginHandler getOrigin();

	void customize(EndPoint endpoint, PacketRequest request) throws IOException;

	void persist(EndPoint endpoint) throws IOException;

	String getHost();

	void setHost(String hostname);

	void setPort(int port);

	int getPort();

	int getLocalPort();

	int getMaxIdleTime();

	void setMaxIdleTime(int ms);

	int getLowResourceMaxIdleTime();

	void setLowResourceMaxIdleTime(int ms);

	Object getConnection();

	boolean getResolveNames();

	public int getRequests();

	public long getConnectionsDurationMin();

	public long getConnectionsDurationTotal();

	public int getConnectionsOpenMin();

	public int getConnectionsRequestsMin();

	public int getConnections();

	public int getConnectionsOpen();

	public int getConnectionsOpenMax();

	public long getConnectionsDurationAve();

	public long getConnectionsDurationMax();

	public int getConnectionsRequestsAve();

	public int getConnectionsRequestsMax();

	public void statsReset();

	public void setStatsOn(boolean on);

	public boolean getStatsOn();

	public long getStatsOnMs();

	int getRequestBufferSize();

	void setRequestBufferSize(int requestBufferSize);

	int getResponseBufferSize();

	void setResponseBufferSize(int responseBufferSize);

	boolean isConfidential(PacketRequest request);

}
