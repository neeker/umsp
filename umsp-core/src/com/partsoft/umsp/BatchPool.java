package com.partsoft.umsp;

import java.io.Serializable;
import java.util.List;

/**
 * 批量返回对象池
 * @author neeker
 * @param <T>
 */
public interface BatchPool<T extends Serializable> {
	
	void returnObjects(List<T> objects);
	
	List<T> takeObjects(int maxLength);
	
	@Deprecated
	boolean isPooling();
	
	int countPooling(int maxLength);
}
