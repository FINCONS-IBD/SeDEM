package com.fincons.util;

/**
 * Represent a support class interface to manage and store information about the operative broker configuration parameters.
 * 
 * @author Fincons Group AG
 *
 */
public interface BrokerParameterList {
	public String getSubjectID();
	public String getBrokerHost();
	public int getBrokerPort();
	public String getDestinationName();
	public String getQueueName();
	public String getExchangeName();
	public String getPattern();
	public String getVirtualHost();
	public boolean isTlsEnabled();
	public int getMgmtPort();
	public boolean isCphEnabled();
	public boolean isAnticipatedKey();
	public int getAnticipatedKeySeconds();
	public String getCphAlg();
	public String getCphEnc();
	public String getCphKty();
	public String getCphCrv();
	public String getCphProxyIp();
	public String getCphProxyPort();
	public String getCphProxyId();
	public String getCphPolicy();
	public String getStorageType();
	public String getDbIp();
	public String getDbPort();
	public String getDbAuthUser();
	public String getDbAuthPwd();
	public String getDbDatabase();
	public String getDbTable();
	public String getPubParams();
	public String getCpabeKey();
	public boolean isCpabeEnabled();
//	public int getRefreshNM();
}
