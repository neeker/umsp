package com.partsoft.umsp;

import java.util.Map;
import java.util.Properties;

import com.partsoft.umsp.Constants.LineTypes;
import com.partsoft.utils.StringUtils;

public class PhoneNumberLineValidateSelector extends AbstractPhoneNumberLineValidateSelector {

	public PhoneNumberLineValidateSelector() {
		setSegmentExpress("1=134,135,136,137,138,139,147,150,151,152,157,158,159,187,188,183,184,182;2=133,153,180,189,181,177;3=130,131,132,145,155,156,186,185");
	}
	
	/**
	 * @param segmentExpress
	 *            设置号码段表达式：<br/>
	 *            <号段类型1>=<号段前缀1> [[, <号段前缀N>]; <号段类型2>=...] <br/>
	 *            示例：<br/>
	 *            1=134,135,136,137,138,139,147,150,151,152,157,158,159,187, 188
	 *            ,183,182;2=133,153,180,189,181,177;3=130,131,132,145,155,156,186
	 *            ,185 <br/>
	 *            号段类型:1表示移动，2表示电信，3表示联通
	 */
	public void setSegmentExpress(String segmentExpress) {
		clearPhoneNumberSegments();
		if (!StringUtils.hasText(segmentExpress))
			return;

		String[] segmentsLines = segmentExpress.split(";");
		if (segmentsLines == null) {
			segmentsLines = new String[] { segmentExpress };
		}

		Properties segmentProps = StringUtils.splitArrayElementsIntoProperties(segmentsLines, "=");
		for (Map.Entry<Object, Object> prop : segmentProps.entrySet()) {
			int lineType = LineTypes.UNKN;
			try {
				lineType = Integer.parseInt(prop.getKey().toString().trim());
			} catch (NumberFormatException e) {
				continue;
			}
			String segmentNumberStrs = prop.getValue().toString().trim();
			String[] segmentNumbers = segmentNumberStrs.split(",");
			if (segmentNumbers == null) {
				segmentNumbers = new String[] { segmentNumberStrs };
			}
			for (String segmentNumber : segmentNumbers) {
				if (!StringUtils.hasText(segmentNumber))
					continue;
				addPhoneNumberSegment(segmentNumber.trim(), lineType);
			}
		}
	}

}
