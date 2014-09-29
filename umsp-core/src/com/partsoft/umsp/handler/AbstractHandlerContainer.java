package com.partsoft.umsp.handler;

import com.partsoft.umsp.Handler;
import com.partsoft.umsp.HandlerContainer;
import com.partsoft.utils.ListUtils;

public abstract class AbstractHandlerContainer extends AbstractHandler implements HandlerContainer {
	public AbstractHandlerContainer() {
	}

	public Handler[] getChildHandlers() {
		Object list = expandChildren(null, null);
		return (Handler[]) ListUtils.toArray(list, Handler.class);
	}

	/* ------------------------------------------------------------ */
	public Handler[] getChildHandlersByClass(Class<?> byclass) {
		Object list = expandChildren(null, byclass);
		return (Handler[]) ListUtils.toArray(list, Handler.class);
	}

	/* ------------------------------------------------------------ */
	public Handler getChildHandlerByClass(Class<?> byclass) {
		// TODO this can be more efficient?
		Object list = expandChildren(null, byclass);
		if (list == null)
			return null;
		return (Handler) ListUtils.get(list, 0);
	}

	protected Object expandChildren(Object list, Class<?> byClass) {
		return list;
	}

	protected Object expandHandler(Handler handler, Object list, Class<?> byClass) {
		if (handler == null)
			return list;

		if (handler != null && (byClass == null || byClass.isAssignableFrom(handler.getClass())))
			list = ListUtils.add(list, handler);

		if (handler instanceof AbstractHandlerContainer)
			list = ((AbstractHandlerContainer) handler).expandChildren(list, byClass);
		else if (handler instanceof HandlerContainer) {
			HandlerContainer container = (HandlerContainer) handler;
			Handler[] handlers = byClass == null ? container.getChildHandlers() : container
					.getChildHandlersByClass(byClass);
			list = ListUtils.addArray(list, handlers);
		}
		return list;
	}

}
