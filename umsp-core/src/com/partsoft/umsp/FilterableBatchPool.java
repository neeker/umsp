package com.partsoft.umsp;

import java.io.Serializable;
import java.util.List;

public interface FilterableBatchPool<T extends Serializable> extends BatchPool<T> {

	void returnObjects(List<T> objects, Object filter);
	
	List<T> takeObjects(int maxLength, Object filter);
	
	@Deprecated
	boolean isPooling(Object filter);
	
	
	int countPooling(int maxLength, Object filter);
	
}
