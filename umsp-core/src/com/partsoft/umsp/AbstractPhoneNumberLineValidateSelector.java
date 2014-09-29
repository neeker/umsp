package com.partsoft.umsp;

import java.util.HashMap;
import java.util.Map;

import com.partsoft.umsp.Constants.LineTypes;
import com.partsoft.utils.StringUtils;

public abstract class AbstractPhoneNumberLineValidateSelector implements PhoneNumberValidator, PhoneNumberLineSelector {

	private Map<Integer, Map<String, Integer>> segmentMaps = new HashMap<Integer, Map<String, Integer>>();

	public boolean testValid(String number) {
		boolean result = false;

		if (StringUtils.hasLength(number)) {
			if (number.startsWith("86")) {
				number = number.substring(2);
			} else if (number.startsWith("01")) {
				number = number.substring(1);
			}
		}

		if (number.length() < 11 || number.length() > 21 || !number.matches("\\d+")) {
			return result;
		}

		for (Map.Entry<Integer, Map<String, Integer>> mapEntry : this.segmentMaps.entrySet()) {
			String segmentPrefix = number.substring(0, mapEntry.getKey());
			if (mapEntry.getValue().containsKey(segmentPrefix)) {
				result = true;
				break;
			}
		}

		return result;
	}

	protected void clearPhoneNumberSegments() {
		segmentMaps.clear();
	}

	public void addPhoneNumberSegment(String segPrefix, int lineType) {
		Map<String, Integer> prefixMap = null;
		int segNameLen = segPrefix.length();
		if (segmentMaps.containsKey(segNameLen)) {
			prefixMap = segmentMaps.get(segNameLen);
		} else {
			prefixMap = new HashMap<String, Integer>();
			segmentMaps.put(segNameLen, prefixMap);
		}
		prefixMap.put(segPrefix, lineType);
	}

	public int selectNumberLine(String number) {
		int result = LineTypes.UNKN;
		if (StringUtils.hasLength(number)) {
			if (number.startsWith("86")) {
				number = number.substring(2);
			}
			if (number.startsWith("01")) {
				number = number.substring(1);
			}
		}
		if (number.length() < 11 || number.length() > 21 || !number.matches("\\d+"))
			return result;
		for (Map.Entry<Integer, Map<String, Integer>> mapEntry : this.segmentMaps.entrySet()) {
			String segmentPrefix = number.substring(0, mapEntry.getKey());
			if (mapEntry.getValue().containsKey(segmentPrefix)) {
				result = mapEntry.getValue().get(segmentPrefix);
				break;
			}
		}
		return result;
	}

}
