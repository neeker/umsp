package com.partsoft.umsp;

public interface SpClientInfoGetter {
	
	/**
	 * 检查是否需要检查IP
	 * @param uid
	 * @return
	 */
	boolean isMustCheckRemoteAddr(String uid);

	/**
	 * 判断远程地址是否可信
	 * @param remoteAddr
	 * @return
	 */
	boolean isRemoteAddrTrust(String remoteAddr);

	/**
	 * 根据用户名获得服务号码
	 * @param uid 用户登录ID
	 * @return 服务号码前缀
	 */
	String getServiceNumber(String uid);
	
	/**
	 * 根据用户名获得业务标识
	 * @param uid 用户名
	 * @return 业务标识
	 */
	String getBusinessCode(String uid);

	/**
	 * 根据用户名获得协商密钥
	 * @param uid 用户登录ID
	 * @return 协商密钥
	 */
	String getNegotiatedSecret(String uid);

	/**
	 * 获得签名信息
	 * @param uid 用户登录ID
	 * @return 签名数据
	 */
	String getSignature(String uid);

	/**
	 * 获得允许最大连接数
	 * @param uid 用户登录ID
	 * @return &lt;0表示不限制，=0表示不允许连接，>0表示最大限制连接数
	 */
	int getMaxConnections(String uid);
	
	/**
	 * 最大允许每秒允许提交数
	 * @return
	 */
	int getMaxSubmitPerSecond(String uid);
	
	/**
	 * 最大每秒转递数
	 * @return
	 */
	int getMaxDeliverPerSecond(String uid);

}
