package com.partsoft.umsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AttributesMap implements Attributes {
	
	Map<String, Object> _map;

	public AttributesMap() {
		_map = new HashMap<String, Object>();
	}

	public AttributesMap(Map<String, Object> map) {
		_map = map;
	}

	public void removeAttribute(String name) {
		_map.remove(name);
	}

	public void setAttribute(String name, Object attribute) {
		if (attribute == null)
			_map.remove(name);
		else
			_map.put(name, attribute);
	}

	public Object getAttribute(String name) {
		return _map.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(_map.keySet());
	}

	public static Enumeration<String> getAttributeNamesCopy(Attributes attrs) {
		if (attrs instanceof AttributesMap)
			return Collections.enumeration(((AttributesMap) attrs)._map.keySet());
		ArrayList<String> names = new ArrayList<String>();
		Enumeration<String> e = attrs.getAttributeNames();
		while (e.hasMoreElements())
			names.add(e.nextElement());
		return Collections.enumeration(names);
	}

	public void clearAttributes() {
		_map.clear();
	}

	public String toString() {
		return _map.toString();
	}

}
