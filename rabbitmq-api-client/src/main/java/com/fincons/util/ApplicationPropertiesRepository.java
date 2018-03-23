package com.fincons.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This class load  in memory the properties configuration file called conf.properties.
 * 
 * @author Fincons Group AG
 *
 */
public class ApplicationPropertiesRepository {

	final static Logger logger = Logger.getLogger(ApplicationPropertiesRepository.class);

	public static final String PUBLISH = "publish";
	public static final String SUBSCRIBE = "subscribe";

	private static final String PROPERTY_NAME_PREFIX = "client.";
	private static final String PROPERTY_NAME_PREFIX_CONS = "consumer.";

	public static final String SUBJECT_ID = PROPERTY_NAME_PREFIX + "subjectID";
	public static final String SUBJECT_PWD = PROPERTY_NAME_PREFIX + "accessToken";
	public static final String PORT_NUMBER = PROPERTY_NAME_PREFIX + "portNumber";
	public static final String MGM_PORT_NUMBER = PROPERTY_NAME_PREFIX + "mgmPortNumber";
	public static final String HOST_NAME = PROPERTY_NAME_PREFIX + "host";
	public static final String VIRTUAL_HOST = PROPERTY_NAME_PREFIX + "vhost";
	public static final String TLS_ENABLED =  PROPERTY_NAME_PREFIX + "tls";
	public static final String DEST_NAME = PROPERTY_NAME_PREFIX + "destinationName";
	public static final String PATTERN = PROPERTY_NAME_PREFIX + "pattern";


	private static final String CER_PROPERTY_NAME_PREFIX = PROPERTY_NAME_PREFIX + "cer.";
	private static final String RESOURCE_URI_PROPERTY_NAME_PREFIX = PROPERTY_NAME_PREFIX + "resourceURI.";

	public static final String EXCHANGE_NAME = PROPERTY_NAME_PREFIX + "exchange";
	public static final String QUEUE_NAME = PROPERTY_NAME_PREFIX + "queue";
	public static final String ROUTING_KEY = PROPERTY_NAME_PREFIX + "routingKey";

	public static final String USERNAME = PROPERTY_NAME_PREFIX + "usr";
	public static final String PASSWORD = PROPERTY_NAME_PREFIX + "pwd";

	public static final String SCHEMAS_DIR = PROPERTY_NAME_PREFIX + "schemasDir";
	public static final String KEYSTORE = PROPERTY_NAME_PREFIX + "keystore";

	public static final String KEYSTORE_PWD = PROPERTY_NAME_PREFIX + "keystore.pwd";
	public static final String PRIVATE_KEY_PWD = CER_PROPERTY_NAME_PREFIX + "privateKey.pwd";
	public static final String CAPABILITY_DIR = PROPERTY_NAME_PREFIX + "capability.dir";

	public static final String RESOURCE_URI_SCHEME = RESOURCE_URI_PROPERTY_NAME_PREFIX + "scheme";
	public static final String RESOURCE_URI_AUTHORITY = RESOURCE_URI_PROPERTY_NAME_PREFIX + "authority";
	public static final String RESOURCE_URI_SERVICE = RESOURCE_URI_PROPERTY_NAME_PREFIX + "service";

	public static final String DEFAULT_DATA_URI = PROPERTY_NAME_PREFIX + "defaultDataURIs";
	
	public static final String CPH_ENABLED = PROPERTY_NAME_PREFIX + "cph";
	public static final String ANTICIPATED_KEY_ENABLED = PROPERTY_NAME_PREFIX + "anticipated_key";
	public static final String ANTICIPATED_KEY_SECONDS = PROPERTY_NAME_PREFIX + "anticipated_key_seconds";
	public static final String CPH_ALG = PROPERTY_NAME_PREFIX + "alg";
	public static final String CPH_ENC = PROPERTY_NAME_PREFIX + "enc";
	public static final String CPH_KTY = PROPERTY_NAME_PREFIX + "kty";
	public static final String CPH_CRV = PROPERTY_NAME_PREFIX + "crv";
	public static final String CPH_PROXY_IP = PROPERTY_NAME_PREFIX + "proxy_ip";
	public static final String CPH_PROXY_PORT = PROPERTY_NAME_PREFIX + "proxy_port";
	public static final String CPH_PROXY_ID = PROPERTY_NAME_PREFIX + "proxy_id";
	public static final String CPH_POLICY = PROPERTY_NAME_PREFIX + "policy";
	public static final String STORAGE_TYPE = PROPERTY_NAME_PREFIX + "storage_type";
	public static final String DB_IP = PROPERTY_NAME_PREFIX + "db_ip";
	public static final String DB_PORT = PROPERTY_NAME_PREFIX + "db_port";
	public static final String DB_AUTH_USER = PROPERTY_NAME_PREFIX + "db_auth_user";
	public static final String DB_AUTH_PWD = PROPERTY_NAME_PREFIX + "db_auth_pwd";
	public static final String DB_DATABASE = PROPERTY_NAME_PREFIX + "db_database";
	public static final String DB_TABLE = PROPERTY_NAME_PREFIX + "db_table";
	
	public static final String PUB_PARAMS = PROPERTY_NAME_PREFIX_CONS + "public_params";
	public static final String CPABE_KEY = PROPERTY_NAME_PREFIX_CONS + "cpabe_priv_key";
	
	public static final String CPABE_ENABLED = PROPERTY_NAME_PREFIX + "cpabe";
	public static final String REFRESH_NUM_MESSAGES = PROPERTY_NAME_PREFIX + "refresh";

	public static Properties APPLICATION_PROPERTIES = new Properties();

	static {

		InputStream input = null;

		try {

			String config_file_vm_var = System.getProperty("rabbitmq_config_file"); 
						
			if(config_file_vm_var == null){
				throw new EnvVariableNotFound();
			}else{

				input = new FileInputStream(config_file_vm_var);

				APPLICATION_PROPERTIES.load(input);
			}

			validateConfigurationProperties();
			
		} catch (IOException e) {
			logger.error("Error loading the file configuration...",e);
			e.printStackTrace();
			System.exit(0);
		} catch (EnvVariableNotFound ex) {
			logger.error("Error loading the <<rabbitmq_config_file>> environment variable...",ex);
			ex.printStackTrace();
			System.exit(0);
		}catch (NullPointerException exc) {
			logger.error("Mandatory configuration properties missing or file not found...",exc);
			exc.printStackTrace();
			System.exit(0);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Error closing the input stream...",e);
					e.printStackTrace();
				}
		}
	}

	/**
	 * Validate the presence of all mandatory configuration properties in conf.properties file. 
	 * The set of mandatory configuration properties are client.host, client.portNumber, client.subjectID,
	 * client.accessToken, client.vhost, client.destinationName, client.pattern.
	 * 
	 * @throws NullPointerException
	 */
	private static void validateConfigurationProperties() throws NullPointerException{
		if(APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.HOST_NAME) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PORT_NUMBER) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.SUBJECT_ID) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.SUBJECT_PWD) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.VIRTUAL_HOST) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DEST_NAME) == null ||
				APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PATTERN) == null){
			
			String errorMessage = ("Mandatory configuration properties missing. Check if the following properties exists in the conf.properties file: "+
					ApplicationPropertiesRepository.HOST_NAME+", "+
					ApplicationPropertiesRepository.PORT_NUMBER+", "+
					ApplicationPropertiesRepository.SUBJECT_ID+", "+
					ApplicationPropertiesRepository.SUBJECT_PWD+", "+
					ApplicationPropertiesRepository.VIRTUAL_HOST+", "+
					ApplicationPropertiesRepository.DEST_NAME+", "+
					ApplicationPropertiesRepository.PATTERN
					);
			
			logger.error(errorMessage);

			throw new NullPointerException(errorMessage);
		}
		
		if((APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ENABLED) != null && APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ENABLED).equals("true")) &&
				(APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.ANTICIPATED_KEY_ENABLED) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.ANTICIPATED_KEY_SECONDS) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ALG) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_CRV) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_ENC) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_KTY) == null ||
			 	 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_IP) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_PORT) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_PROXY_ID) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPH_POLICY) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.PUB_PARAMS) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.CPABE_KEY) == null 
				 //|| APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.REFRESH_NUM_MESSAGES) == null
				 )
				){
			
			String errorMessage = ("Mandatory configuration properties missing. Check if the following properties exists in the conf.properties file: "+
					ApplicationPropertiesRepository.ANTICIPATED_KEY_ENABLED+", "+
					ApplicationPropertiesRepository.ANTICIPATED_KEY_SECONDS+", "+
					ApplicationPropertiesRepository.CPH_ALG+", "+
					ApplicationPropertiesRepository.CPH_CRV+", "+
					ApplicationPropertiesRepository.CPH_ENC+", "+
					ApplicationPropertiesRepository.CPH_KTY+", "+
					ApplicationPropertiesRepository.CPH_PROXY_IP+", "+
					ApplicationPropertiesRepository.CPH_PROXY_PORT+", "+
					ApplicationPropertiesRepository.CPH_PROXY_ID+", "+
					ApplicationPropertiesRepository.CPH_POLICY+", "+
					ApplicationPropertiesRepository.PUB_PARAMS+", "+
					ApplicationPropertiesRepository.CPABE_KEY+", " +
					ApplicationPropertiesRepository.REFRESH_NUM_MESSAGES
					);
			
			logger.error(errorMessage);

			throw new NullPointerException(errorMessage);
		}
		
		if((APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.STORAGE_TYPE) != null && APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.STORAGE_TYPE).equals("database")) &&
				(APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_IP) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_PORT) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_AUTH_USER) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_AUTH_PWD) == null ||
			 	 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_DATABASE) == null ||
				 APPLICATION_PROPERTIES.getProperty(ApplicationPropertiesRepository.DB_TABLE) == null)){
			
			String errorMessage = ("Mandatory configuration properties missing. Check if the following properties exists in the conf.properties file: "+
					ApplicationPropertiesRepository.DB_IP+", "+
					ApplicationPropertiesRepository.DB_PORT+", "+
					ApplicationPropertiesRepository.DB_AUTH_USER+", "+
					ApplicationPropertiesRepository.DB_AUTH_PWD+", "+
					ApplicationPropertiesRepository.DB_DATABASE+", "+
					ApplicationPropertiesRepository.DB_TABLE
					);
			
			logger.error(errorMessage);

			throw new NullPointerException(errorMessage);
		}
	}


}
