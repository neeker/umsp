package com.partsoft.umsp.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import com.partsoft.umsp.LifeCycle;
import com.partsoft.umsp.log.Log;
import com.partsoft.utils.Assert;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.ListUtils;

@Deprecated
public class SpringContextLifeCycleManager implements ApplicationContextAware, ApplicationListener<ApplicationEvent>,
		InitializingBean {

	protected ApplicationContext context;

	protected Object managedObject;

	public SpringContextLifeCycleManager() {
	}

	public SpringContextLifeCycleManager(LifeCycle handler) {
		this.managedObject = handler;
	}
	
	public void join() {
		for (int i = 0; i < ListUtils.size(managedObject); i++) {
			LifeCycle wrapper = ((LifeCycle) ListUtils.get(managedObject, i));
			if (wrapper instanceof AbstractOriginHandler)
			try {
				((AbstractOriginHandler)wrapper).join();
			} catch (Throwable e) {
				Log.warn(String.format("join \"%s\" error", wrapper.toString()), e);
			}
		}
	}

	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	protected void doStart() {
		for (int i = 0; i < ListUtils.size(managedObject); i++) {
			LifeCycle wrapper = ((LifeCycle) ListUtils.get(managedObject, i)); 
			try {
				wrapper.start();
			} catch (Exception e) {
				Log.warn(String.format("start \"%s\" error", wrapper.toString()), e);
			}
		}
	}

	public void setManagedObject(LifeCycle managedObject) {
		this.managedObject = managedObject;
	}

	public void addManagedObject(LifeCycle managedObject) {
		this.managedObject = ListUtils.add(this.managedObject, managedObject);
	}

	public void removeManagedObject(LifeCycle managedObject) {
		ListUtils.remove(this.managedObject, managedObject);
	}
	
	public void setManagedObjects(LifeCycle[] cycles) {
		this.managedObject = ListUtils.addArray(this.managedObject, cycles);
	}

	protected void doStop() {
		for (int i = 0; i < ListUtils.size(managedObject); i++) {
			LifeCycle wrapper = ((LifeCycle) ListUtils.get(managedObject, i)); 
			try {
				wrapper.stop();
			} catch (Exception e) {
				Log.warn(String.format("stop \"%s\" error", wrapper.toString()), e);
			}
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (CompareUtils.nullSafeEquals(event.getSource(), context)) {
			if (event instanceof ContextRefreshedEvent) {
				doStart();
			} else if (event instanceof ContextClosedEvent) {
				doStop();
			}
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.managedObject);
	}

}
