package com.partsoft.umsp.sgip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.partsoft.umsp.BatchPool;
import com.partsoft.umsp.Context;

/**
 * 简单的排队发送通道处理器
 * 
 * @author neeker
 */
public class SimpleQueuedSgipSPSendHandler extends BatchPoolSgipSPSendHandler {

	public static final String ARG_SUBMIT_QUEUE = "sgip.context.submit.queue";

	private List<Submit> _delayContextSubmits;

	public SimpleQueuedSgipSPSendHandler() {
		setBatchPool(new BatchPool<Submit>() {
			public List<Submit> takeObjects(int maxLength) {
				return takeSubmitsFromConext(maxLength);
			}

			public void returnObjects(List<Submit> objects) {
				pushSubmitsToContext(objects);
			}

			public boolean isPooling() {
				return getContextSubmitQueue().size() > 0;
			}
			
			public int countPooling(int maxLength) {
				int countResult = getContextSubmitQueue().size() ;
				return countResult < maxLength ? countResult : maxLength;
			}
			
		});
	}

	/**
	 * 获得待发消息队列
	 * 
	 * @param ctx
	 *            上下文
	 * @return
	 */
	protected Queue<Submit> getContextSubmitQueue() {
		Context ctx = getContext();
		synchronized (ctx) {
			@SuppressWarnings("unchecked")
			Queue<Submit> submits = (Queue<Submit>) ctx.getAttribute(ARG_SUBMIT_QUEUE);
			if (submits == null) {
				submits = new LinkedBlockingDeque<Submit>();
				ctx.setAttribute(ARG_SUBMIT_QUEUE, submits);
			}
			return submits;
		}
	}

	protected List<Submit> takeSubmitsFromConext(int takeCount) {
		Queue<Submit> submit_queue = getContextSubmitQueue();
		List<Submit> submit_result = new ArrayList<Submit>(takeCount);
		synchronized (submit_queue) {
			Submit submit = null;
			while (true) {
				submit = submit_queue.poll();
				if (submit != null) {
					submit_result.add(submit);
				}
				if (submit == null || submit_result.size() >= takeCount)
					break;
			}
		}
		return submit_result;
	}

	protected void pushSubmitsToContext(Collection<Submit> submits) {
		if (submits == null) return;
		Queue<Submit> queue = getContextSubmitQueue();
		synchronized (queue) {
			for (Submit submit : submits) {
				if (submit != null) {
					queue.offer(submit);
				}
			}
		}
	}

	/**
	 * 提交发送消息
	 * 
	 * @param submits
	 */
	public void postSubmit(List<Submit> submits) {
		if (isStarted()) {
			pushSubmitsToContext(submits);
		} else {
			_delayContextSubmits = submits;
		}
	}

	@Override
	protected void handleContextInitialized(Context ctx) {
		super.handleContextInitialized(ctx);
		if (_delayContextSubmits != null) {
			pushSubmitsToContext(_delayContextSubmits);
		}
	}

	@Override
	protected void handleContextDestroyed(Context ctx) {
		super.handleContextDestroyed(ctx);
		Queue<Submit> submits = getContextSubmitQueue();
		if (submits.size() > 0 && !isAutoReSubmit()) {
			RuntimeException tex = new IllegalArgumentException(String.format("%d unfinished post Submit",
					submits.size()));
			getOrigin().pushDelayException(tex);
		}
	}

}
