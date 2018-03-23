package com.fincons.rabbitmq.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.fincons.util.AMQPConnectionHelper;
import com.fincons.util.ApplicationPropertiesRepository;
import com.fincons.util.BrokerParameterList;
import com.fincons.util.BrokerParameterListImpl;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * Defines an abstract implementation of the {@link RabbitMqClient} interface.<br/>
 * This implementation is abstract because a generic client is useless because can
 * only connect and disconnect to the RabbitMQ Server but cannot use the session. Subclasses (i.e.
 * classes that models publishers and subscribers) should use the operative broker's
 * connection parameters to connect to RabbitMQ resource they have been authorised to access
 * and publish or subscribe to an event stream.
 * 
 * @author Fincons Group AG
 */
public abstract class BasicRabbitMqClient implements RabbitMqClient {

	final static Logger logger = Logger.getLogger(BasicRabbitMqClient.class);

	/**
	 * The AMQP connection to the operative broker.
	 */
	protected Connection connection;

	/**
	 * The AMQP channel to the operative broker.
	 */
	protected Channel channel;

	protected BasicRabbitMqClient () {}

	/* (non-Javadoc)
	 * @see com.fincons.rabbitmq.client.RabbitMqClient#connect()
	 */
	@Override
	public void connect() {

//		logger.info("Calling the connect() method...");

		String brokerHost = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.HOST_NAME);
		Integer brokerPort = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PORT_NUMBER));
		Integer mgmtPort = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.MGM_PORT_NUMBER));
		String username = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.SUBJECT_ID);
		String password = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.SUBJECT_PWD);
		String virtualHost = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.VIRTUAL_HOST);
		boolean tlsEnabled = new Boolean (ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.TLS_ENABLED));
		String dest_name = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DEST_NAME);
		String exchange_name = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.EXCHANGE_NAME);
		String queue_name = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.QUEUE_NAME);
		String pattern = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PATTERN);
		boolean cphEnabled = new Boolean (ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ENABLED));
		boolean anticipatedKey = new Boolean (ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.ANTICIPATED_KEY_ENABLED));
		Integer anticipatedKeySeconds = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.ANTICIPATED_KEY_SECONDS));
		String cphAlg = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ALG);
		String cphEnc = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ENC);
		String cphKty = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_KTY);
		String cphCrv = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_CRV);
		String cphProxyIp = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_IP);
		String cphProxyPort = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_PORT);
		String cphProxyId = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_ID);
		String cphPolicy = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_POLICY);
		String storageType = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.STORAGE_TYPE);
		String dbIp = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_IP);
		String dbPort = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_PORT);
		String dbAuthUser = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_AUTH_USER);
		String dbAuthPwd = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_AUTH_PWD);
		String dbDatabase = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_DATABASE);
		String dbTable = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_TABLE);
		String pubParams = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PUB_PARAMS);
		String cpabeKey = ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPABE_KEY);
		boolean cpabeEnabled = new Boolean (ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPABE_ENABLED));
		//Integer refreshNM = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.REFRESH_NUM_MESSAGES));

		AMQPConnectionHelper mc = new AMQPConnectionHelper();

		BrokerParameterList operativeBrokerConnParams = 
				new BrokerParameterListImpl(username, password, brokerHost, 
						brokerPort, dest_name, exchange_name, queue_name, pattern, 
						virtualHost, tlsEnabled, mgmtPort, cphEnabled, anticipatedKey, anticipatedKeySeconds, cphAlg, cphEnc,
						cphKty, cphCrv, cphProxyIp, cphProxyPort, cphProxyId, cphPolicy,
						storageType, dbIp, dbPort, dbAuthUser, dbAuthPwd, dbDatabase, dbTable,
						pubParams, cpabeKey, cpabeEnabled/*, refreshNM*/);
		
		useENSBrokerConnectionParameters(operativeBrokerConnParams);

		try {
			connection = mc.connection(username, password, virtualHost, brokerHost, brokerPort);
			channel = mc.getChannel();
		} catch (IOException e) {
			logger.error("Error during the connection and channel opening", e);
			e.printStackTrace();
		}

	}

	/**
	 * Uses the connection parameters according to the kind of RabbitMQ client<br/>Subclasses should use this
	 * method to retrieve from the connection parameters above  
	 * 
	 * @param parameters the operative broker's connection parameters.
	 */	
	protected abstract void useENSBrokerConnectionParameters (BrokerParameterList parameters);

	/* (non-Javadoc)
	 * @see com.fincons.rabbitmq.client.RabbitMqClient#disconnect()
	 */
	@Override
	public void disconnect() {
		logger.info("Calling the disconnect() method...");
		closeConnection();
	}

	/**
	 * Close the channel and the RabbitMQ connection
	 */
	protected void closeConnection () {
		try {
			if (isConnected()) {
				channel.close();
				connection.close();
			}
		} catch (IOException e) {
			logger.error("Error during the channel or connection closing", e);
			e.printStackTrace();
		} catch (TimeoutException e) {
			logger.error("Timeout during the channel or connection closing", e);
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see com.fincons.rabbitmq.client.RabbitMqClient#isConnect()
	 */
	@Override
	public boolean isConnected () {
		if (connection == null)
			return false;
		return connection.isOpen();
	}

}
