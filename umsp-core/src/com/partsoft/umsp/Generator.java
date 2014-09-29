package com.partsoft.umsp;

import java.io.IOException;

import com.partsoft.umsp.io.Buffer;

public interface Generator {

	public static final boolean LAST = true;

	public static final boolean MORE = false;

	void addContent(Buffer content, boolean last) throws IOException;

	boolean addContent(byte b) throws IOException;

	void complete() throws IOException;

	long flush() throws IOException;

	int getContentBufferSize();

	long getContentWritten();

	boolean isContentWritten();

	void increaseContentBufferSize(int size);

	boolean isBufferFull();

	boolean isCommitted();

	boolean isComplete();

	boolean isPersistent();

	void reset(boolean returnBuffers);

	void resetBuffer();

	boolean isIdle();

	void setPersistent(boolean persistent);

}
