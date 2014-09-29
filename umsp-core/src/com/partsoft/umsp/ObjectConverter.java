package com.partsoft.umsp;

public interface ObjectConverter<T, E> {

	T convert(E object);
	
}
