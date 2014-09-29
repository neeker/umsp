package com.partsoft.umsp.handler;

import com.partsoft.umsp.ObjectConverter;
import com.partsoft.utils.Assert;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractDataConverterDispatchHandler<E> implements DataHandler<E> {

	private ObjectConverter dataConverter;

	private DataDispatcher dataDispatcher;

	public AbstractDataConverterDispatchHandler() {
	}

	public AbstractDataConverterDispatchHandler(DataDispatcher dataDispatcher, ObjectConverter dataConverter) {
		setDataDispatcher(dataDispatcher);
		setDataConverter(dataConverter);
	}

	public AbstractDataConverterDispatchHandler(DataDispatcher dataDispatcher) {
		setDataDispatcher(dataDispatcher);
	}

	public AbstractDataConverterDispatchHandler(ObjectConverter dataConverter) {
		setDataConverter(dataConverter);
	}

	public void setDataDispatcher(DataDispatcher dataDispatcher) {
		this.dataDispatcher = dataDispatcher;
	}

	public ObjectConverter getDataConverter() {
		return dataConverter;
	}

	public DataDispatcher getDataDispatcher() {
		return dataDispatcher;
	}

	public void setDataConverter(ObjectConverter objectConverter) {
		this.dataConverter = objectConverter;
	}

	public final void handle(E object) throws HandleException {
		if (object == null)
			return;
		Object converted = null;
		E submit = (E) object;

		try {
			beforeDispatch(submit);
		} catch (Throwable e) {
			throw new HandleException(e, object);
		}

		try {
			converted = convertData(submit);
		} catch (Throwable e) {
			throw new HandleException(e, object);
		}

		try {
			doDispatch(converted);
		} catch (Throwable e) {
			throw new HandleException(e, object);
		}
		try {
			afterDispatch(converted);
		} catch (Throwable e) {
			throw new HandleException(e, object);
		}
	}

	protected abstract void beforeDispatch(Object source_object);

	protected void afterDispatch(Object packet_convered) {
	}

	protected void doDispatch(Object packet_converted) {
		Assert.notNull(this.dataDispatcher, "委派分派器不能为空");
		this.dataDispatcher.dispatch(packet_converted);
	}

	protected Object convertData(E packet) {
		if (this.dataConverter == null) {
			return packet;
		}
		return this.dataConverter.convert(packet);
	}

}
