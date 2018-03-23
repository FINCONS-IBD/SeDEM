package com.fincons.util;


/**
 * Defines the implementation of the {@link BrokerParameterLis} interface, a class to manage and pass structured configuration setting<br/>
 * 
 * @author Fincons Group AG
 */
public class BrokerParameterListImpl implements BrokerParameterList{
	
	private final String subjectID;
	private final String accessToken;
	private final String brokerHost;
    private final int brokerPort;
    private final String destinationName; //exchange param
    private final String queueName;
    private final String exchangeName;
    private final String pattern;
    private final String virtualHost;
    private final boolean tlsEnabled;
    private final int mgmtPort;
    private final boolean cphEnabled;
    private final boolean anticipatedKey;
    private final int anticipatedKeySeconds;
    private final String cphAlg;
    private final String cphEnc;
    private final String cphKty;
    private final String cphCrv;
    private final String cphProxyIp;
    private final String cphProxyPort;
    private final String cphProxyId;
    private final String cphPolicy;
    private final String storageType;
    private final String dbIp;
    private final String dbPort;
    private final String dbAuthUser;
    private final String dbAuthPwd;
    private final String dbDatabase;
    private final String dbTable;
    private final String pubParams;
    private final String cpabeKey;
    private final boolean cpabeEnabled;
  //  private final int refreshNM;
    
    
	public BrokerParameterListImpl(String subjectID, String accessToken, String brokerHost,
			int brokerPort, String destinationName, String exchangeName,
			String queueName, String pattern, String virtualHost, boolean tlsEnabled,
			int mgmtPort, boolean cphEnabled, boolean anticipatedKey, int anticipatedKeySeconds, String cphAlg, String cphEnc, String cphKty,
			String cphCrv, String cphProxyIp, String cphProxyPort, String cphProxyId, String cphPolicy,
			String storageType, String dbIp, String dbPort, String dbAuthUser, String dbAuthPwd, String dbDatabase, String dbTable,
			String pubParams, String cpabeKey, boolean cpabeEnabled/*, int refreshNM*/) {
		this.subjectID = subjectID;
		this.accessToken = accessToken;
		this.brokerHost = brokerHost;
		this.brokerPort = brokerPort;
		this.destinationName = destinationName;
		this.queueName = queueName;
		this.exchangeName = exchangeName;
		this.pattern = pattern;
		this.virtualHost = virtualHost;
		this.tlsEnabled = tlsEnabled;
		this.mgmtPort = mgmtPort;
		this.cphEnabled = cphEnabled;
		this.anticipatedKey = anticipatedKey;
		this.anticipatedKeySeconds = anticipatedKeySeconds;
		this.cphAlg = cphAlg;
		this.cphEnc = cphEnc;
		this.cphKty = cphKty;
		this.cphCrv = cphCrv;
		this.cphProxyIp = cphProxyIp;
		this.cphProxyPort = cphProxyPort;
		this.cphProxyId = cphProxyId;
		this.cphPolicy = cphPolicy;
		this.storageType = storageType;
		this.dbIp = dbIp;
		this.dbPort = dbPort;
		this.dbAuthUser = dbAuthUser;
		this.dbAuthPwd = dbAuthPwd;
		this.dbDatabase = dbDatabase;
		this.dbTable = dbTable;
		this.pubParams = pubParams;
		this.cpabeKey = cpabeKey;
		this.cpabeEnabled = cpabeEnabled;
		//this.refreshNM = refreshNM;
	}

	public String getSubjectID() {
		return subjectID;
	}
	
    public String getAccessToken() {
		return accessToken;
	}

	public String getBrokerHost() {
		return brokerHost;
	}

	public int getBrokerPort() {
		return brokerPort;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public String getPattern() {
		return pattern;
	}

	public String getVirtualHost() {
		return virtualHost;
	}

	public boolean isTlsEnabled() {
		return tlsEnabled;
	}

	public int getMgmtPort() {
		return mgmtPort;
	}

	public String getQueueName() {
	    return queueName;
    }

	public String getExchangeName() {
	   return exchangeName;
    }
	
	public boolean isCphEnabled() {
		return cphEnabled;
	}
	
	public boolean isAnticipatedKey() {
		return anticipatedKey;
	}

	public int getAnticipatedKeySeconds(){
		return anticipatedKeySeconds;
	}
	
	public String getCphAlg() {
		return cphAlg;
	}

	public String getCphEnc() {
		return cphEnc;
	}

	public String getCphKty() {
		return cphKty;
	}

	public String getCphCrv() {
		return cphCrv;
	}

	public String getCphProxyIp() {
		return cphProxyIp;
	}

	public String getCphProxyPort() {
		return cphProxyPort;
	}
	
	public String getCphProxyId() {
		return cphProxyId;
	}

	public String getCphPolicy() {
		return cphPolicy;
	}

	public String getStorageType() {
		return storageType;
	}

	public String getDbIp() {
		return dbIp;
	}

	public String getDbPort() {
		return dbPort;
	}

	public String getDbAuthUser() {
		return dbAuthUser;
	}

	public String getDbAuthPwd() {
		return dbAuthPwd;
	}

	public String getDbDatabase() {
		return dbDatabase;
	}

	public String getDbTable() {
		return dbTable;
	}
	
	public String getPubParams() {
		return pubParams;
	}
	
	public String getCpabeKey() {
		return cpabeKey;
	}
	
	public boolean isCpabeEnabled() {
		return cpabeEnabled;
	}
	
//	public int getRefreshNM() {
//		return refreshNM;
//	}

}
