package com.partsoft.umsp;

/**
 * 号码校验器
 * @author neeker
 */
public interface PhoneNumberValidator {
	
	/**
	 * 校验电话号码
	 * @param number 电话号码
	 * @return true表示校验通过
	 */
	boolean testValid(String numbers);
	
}
