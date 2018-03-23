package com.fincons.rabbitmq.subscriber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

import com.fincons.rabbitmq.client.BasicRabbitMqClient;
import com.fincons.rabbitmq.event.Event;
import com.fincons.rabbitmq.event.EventListener;
import com.fincons.util.AMQPConstants;
import com.fincons.util.BrokerParameterList;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import algorithms.AES;
import cpabe.Common;
import cpabe.Cpabe;
import messages.Key_Storage;
import messages.OrientDB_Recovered_Key;
import messages.Storage_Parameters;

import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Provides the default implementation of {@link Subscriber}. 
 * 
 * @author Fincons Group AG
 *
 */
public class BasicSubscriber extends BasicRabbitMqClient implements Subscriber {

	final static Logger logger = Logger.getLogger(BasicSubscriber.class);
	
	private String queueName;

	private boolean isSubscribed;

	private DefaultConsumer basicConsumer;
	private final EventFactory eventFactory;
	
    private Map<String, EventListener> listeners = new HashMap<String, EventListener>();
    
    private Cpabe cpabe;
    private Map<String, byte[]> sym_keys; //Set of decrypted symmetric keys
    private byte[] cpabe_private_key;
    private byte[] public_parameters;
    private boolean cpabe_enabled;
    
//    private long time_aes;
//    private long time_cpabe;
    
    //At the moment, only one storage
    private Key_Storage key_storage; 
    
	
	public BasicSubscriber(EventFactory eventFactory) {
		super();
		this.eventFactory = eventFactory;
	}
	
    /* (non-Javadoc)
     * @see com.fincons.rabbitmq.client.BasicRabbitMqClient#useENSBrokerConnectionParameters
     */
	@Override
	protected void useENSBrokerConnectionParameters(
			BrokerParameterList parameters) {
		
		if( parameters.getQueueName()!=null&& !parameters.getQueueName().equals("")){
			this.queueName = parameters.getQueueName();
		}else{
			this.queueName = parameters.getDestinationName();
		}
		
		/* Check if Cph flag is activated. This implicates received data are encrypted */
		if(parameters.isCpabeEnabled()){
			try {
//				this.time_cpabe = 0;
//				this.time_aes = -1;
				this.cpabe = new Cpabe();
				this.cpabe_enabled = parameters.isCpabeEnabled();
				String public_parameters_file = parameters.getPubParams(); //example
				this.public_parameters = Common.suckFile(public_parameters_file);
				String cpabe_key_file = parameters.getCpabeKey(); //example
				this.cpabe_private_key = Common.suckFile(cpabe_key_file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if(parameters.isCphEnabled()){
//			this.time_cpabe = 0;
//			this.time_aes = 0;
        	this.cpabe = new Cpabe();
        	this.sym_keys = new HashMap<String, byte[]>();
        	
        	/* Public params and cpabe private key */
			try {
				/* Firstly, it is necessary to get the storages in which the symmetric key will be saved */
	        	getParamsCph(parameters);
				String public_parameters_file = parameters.getPubParams(); //example
				this.public_parameters = Common.suckFile(public_parameters_file);
				String cpabe_key_file = parameters.getCpabeKey(); //example
				this.cpabe_private_key = Common.suckFile(cpabe_key_file);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
	}

	/* (non-Javadoc)
     * @see com.fincons.rabbitmq.subscriber.Subscriber#subscribe()
     */
    @Override
    public void subscribe() throws IllegalStateException{
    	
		logger.info("Calling the subscribe() method...");
    	
        if (isSubscribed){
    		logger.error("A subscription is already active");
            throw new IllegalStateException ("A subscription is already active");
        }
        try {
        	
            if (null == channel || !channel.isOpen())
                channel = connection.createChannel();
                        
            basicConsumer = new BasicConsumer(channel);
            
            String consumerTag = channel.basicConsume(queueName, true, basicConsumer);

            isSubscribed = true;
            
            logger.debug("Basic consumer created: consumerTag=" + consumerTag);
        } catch (IOException e) {
			logger.error("Error during event subscribing", e);
        	e.printStackTrace();
        }
        
    }	
	
	public void unsubscribe() throws IllegalStateException {

		logger.info("Calling the unsubscribe() method...");
		
		if (!isSubscribed){
			logger.error("Error during event unsubscribing. No subscription active");
			throw new IllegalStateException ("No subscription active");
		}
		this.isSubscribed = false;

		try {
			channel.basicCancel(basicConsumer.getConsumerTag());
			channel.close();

		} catch (IOException e) {
			logger.error("Error during event unsubscribing", e);
			e.printStackTrace();
			closeConnection();
		} catch (TimeoutException e) {
			logger.error("Timeout during event unsubscribing", e);
			e.printStackTrace();
		}


	}

	@Override
	public boolean isSubscribed() {
		return isSubscribed;
	}
	
    @Override
    public String registerEventListener(EventListener listener) {
        if (listener == null){
			logger.error("The listener cannot be null");
            throw new IllegalArgumentException ("The listener cannot be null");
        }
        UUID listenerID = UUID.randomUUID();
        String listenerID_STR = listenerID.toString();
        listeners.put(listenerID.toString(), listener);
        return listenerID_STR;
    }

	@Override
	public void unregisteredEventListener(String listenerID) {
        if (listeners.remove(listenerID) == null){
			logger.error("No listener associated to ID" + listenerID);
            throw new IllegalArgumentException("No listener associated to ID " + listenerID);
        }
	}	
	
	
    private class BasicConsumer extends DefaultConsumer{
        /**
         * @param channel
         */ 
        public BasicConsumer(Channel channel) {
            super(channel);
        }

        /* (non-Javadoc)
         * @see com.rabbitmq.client.DefaultConsumer#handleDelivery(java.lang.String, com.rabbitmq.client.Envelope, com.rabbitmq.client.AMQP.BasicProperties, byte[])
         */
        @Override
        public void handleDelivery(String consumerTag,
                Envelope envelope, BasicProperties properties,
                byte[] body) throws IOException {
            
        	/* TEST 1: CONSUMED MEMORY: CONSUMER */
//        	time_cpabe = 0;
//        	time_aes = -1;
//        	Runtime runtime = Runtime.getRuntime();
//    		runtime.gc();
//    		long startTime = System.currentTimeMillis();
        	
        	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			
//        	long creation_time = Long.parseLong(properties.getHeaders().get("timestamp").toString());
//			long diff_time = startTime - creation_time;
			
			/* TEST 1: DIFF.TIME */
//			try {
//				File TextFile = new File("diff_time.csv"); 
//				FileWriter TextOut = new FileWriter(TextFile, true);
//				TextOut.write(diff_time + "\n");
//				TextOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	
        	/* Check if received data are encrypted; and then, it will try to decrypt them */
        	byte[] new_body = body;
			StringBuffer payload = new StringBuffer();
        	if(cpabe_enabled){
        		
        		/*  TEST 1: CONSUMED TIME: CONSUMER */
//        		long startTimeCPABE = System.currentTimeMillis();
        		
        		String value = decrypt_payload_cpabe(body);
        		
        		/*  TEST 1: CONSUMED TIME: CONSUMER */
//        		long endTimeCPABE   = System.currentTimeMillis();
//        		time_cpabe += endTimeCPABE - startTimeCPABE;
        		
        		payload.append(value);
        		new_body = payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING);
        	}else if(cpabe != null){
//        		logger.info("--> Starting decryption operations...");
        		String value = decrypt_payload(properties.getHeaders(), body);
        		payload.append(value);
        		new_body = payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING);
        	}
        	
        	String routing_key = envelope.getRoutingKey(); 
        	Map<String, Object> headers = properties.getHeaders(); 
    		String contentType = properties.getContentType() == null ? Event.DEFAULT_CONTENT_TYPE : properties.getContentType();
    		String contentEncoding = properties.getContentEncoding() == null ? Event.DEFAULT_CONTENT_ENCODING : properties.getContentEncoding();
    		Integer priority = properties.getPriority() == null ? Event.DEFAULT_PRIORITY : properties.getPriority() ;
            Date timestamp = properties.getTimestamp(); 
            boolean deliveryMode = properties.getDeliveryMode() == AMQPConstants.NOT_PERSISTENT_DELIVERY ? false : true;
            String appId = properties.getAppId();
        	
    		//create(headers, payload, Event.DEFAULT_CONTENT_TYPE, Event.DEFAULT_CONTENT_ENCODING,
            //Event.DEFAULT_PRIORITY, isPersistent);
            
        	Event event = eventFactory.create(routing_key, headers, new_body, contentType, contentEncoding, priority, timestamp, deliveryMode, appId);
            for (Entry<String, EventListener> listenerEntry : listeners.entrySet())
                listenerEntry.getValue().onEvent(event);

            /*  TEST 1: CONSUMED MEMORY: CONSUMER */
//            long endTime   = System.currentTimeMillis();
//            runtime = Runtime.getRuntime();
//			runtime.gc();
//			long memory = runtime.totalMemory() - runtime.freeMemory();
//			long totalTime = endTime - startTime;
//			try {
//				File TextFile = new File("mem_pub_cons.csv"); 
//				FileWriter TextOut = new FileWriter(TextFile, true);
//				TextOut.write(memory/1024L + "\n");
//				TextOut.close();
//				TextFile = new File("time_pub_cons.csv"); 
//				TextOut = new FileWriter(TextFile, true);
//				TextOut.write(totalTime + ";");
//				if(time_aes > -1) TextOut.write(time_aes + ";");
//				TextOut.write(time_cpabe + "\n");
//				TextOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	
        }

        /* (non-Javadoc)
         * @see com.rabbitmq.client.DefaultConsumer#handleShutdownSignal(java.lang.String, com.rabbitmq.client.ShutdownSignalException)
         */
        @Override
        public void handleShutdownSignal(String consumerTag,
                ShutdownSignalException sig) {
            try {
                //cannot unsubscribe because the channel is not working
                isSubscribed = true;
                disconnect();
            } catch (IllegalStateException e) {
                closeConnection();
            }
        }
    }
    
    private void getParamsCph(BrokerParameterList parameters){
    	String storage_type = parameters.getStorageType();
    	switch(storage_type){
    		case "database":
    			Storage_Parameters param1 = new Storage_Parameters("db_ip", parameters.getDbIp());
    			Storage_Parameters param2 = new Storage_Parameters("db_port", parameters.getDbPort());
    			Storage_Parameters param3 = new Storage_Parameters("db_auth_user", parameters.getDbAuthUser());
    			Storage_Parameters param4 = new Storage_Parameters("db_auth_pwd", parameters.getDbAuthPwd());
    			Storage_Parameters param5 = new Storage_Parameters("db_database", parameters.getDbDatabase());
    			Storage_Parameters param6 = new Storage_Parameters("db_table", parameters.getDbTable());
    			ArrayList<Storage_Parameters> storage_parameters = new ArrayList<Storage_Parameters>();
    			storage_parameters.add(param1);storage_parameters.add(param2);storage_parameters.add(param3);
    			storage_parameters.add(param4);storage_parameters.add(param5);storage_parameters.add(param6);
    			key_storage = new Key_Storage(storage_type, storage_parameters);
    			break;
    		default:
    			logger.error("The storage type is not allowed");
    			throw new IllegalStateException("The storage type is not allowed");
    	}
    }

    private String decrypt_payload(Map<String, Object> headers, byte[] body){
    	StringBuffer payload = new StringBuffer();
    	for(String key : headers.keySet()){
    		String value = headers.get(key).toString();
    		if(key.equals("key-id")){
    			String decrypted_value = decrypt_data(value, body);
    			payload.append(decrypted_value);
    		}else if(key.equals("next-key-id")){
    			anticipate_key(value);
    		}
		}
    	return payload.toString();
    }
    
    private String decrypt_payload_cpabe(byte[] body){
    	try{
			String encrypted_data = new String(body);
			int index = encrypted_data.indexOf(" ");
    		String cph_b64 = encrypted_data.substring(0, index);
    		String aes_b64 = encrypted_data.substring(index +  1);
    		byte[] cph = Base64.getUrlDecoder().decode(cph_b64);
	        byte[] aes = Base64.getUrlDecoder().decode(aes_b64);
	        byte[][] enc_sym_key = new byte[2][];
	        enc_sym_key[0] = cph;
	        enc_sym_key[1] = aes;
	        
	        byte[] payload_dec = this.cpabe.dec(this.public_parameters, this.cpabe_private_key, enc_sym_key);
	        if(payload_dec==null)
	        	throw new Exception("Error decrypting symmetric key");
			
	    	return new String(payload_dec); 
    	}catch(Exception e){
    		//e.printStackTrace();
    		return "The decryption operation failed!"; 
    	}
    }
    
    private String decrypt_data(String key_id, byte[] body){
    	try{
	    	/* Check if this data consumer has the key associated with the identifier */
	    	byte[] symmetric_key;
	    	if(!this.sym_keys.containsKey(key_id)){
//				logger.info("--- IT'S NECESSARY TO RECOVER AND DECRYPT THE KEY ---");
	    		String encrypted_symmetric_key = getEncSymKey(key_id);
	    		/* Decrypt the key by CP-ABE using its CP-ABE private key */
	    		int index = encrypted_symmetric_key.indexOf(" ");
	    		String cph_b64 = encrypted_symmetric_key.substring(0, index);
	    		String aes_b64 = encrypted_symmetric_key.substring(index +  1);
	    		byte[] cph = Base64.getUrlDecoder().decode(cph_b64);
		        byte[] aes = Base64.getUrlDecoder().decode(aes_b64);
		        byte[][] enc_sym_key = new byte[2][];
		        enc_sym_key[0] = cph;
		        enc_sym_key[1] = aes;
		        
//				long startTimeCPABE = System.currentTimeMillis();
				
		        symmetric_key = this.cpabe.dec(this.public_parameters, this.cpabe_private_key, enc_sym_key);
		        
//        		long endTimeCPABE   = System.currentTimeMillis();
//        		time_cpabe += endTimeCPABE - startTimeCPABE;
		        
		        if(symmetric_key==null)
		        	throw new Exception("Error decrypting symmetric key");
		        
		        /* Locally store the key for future decryption operations */
		        this.sym_keys.put(key_id, symmetric_key);
	    	}else{
//				logger.info("--- THE KEY WAS STORED ---");
	    		symmetric_key = this.sym_keys.get(key_id);
	    	}
	    	/* Decrypt the encrypted data */
	    	String encrypted_data = new String(body);
	    	logger.debug("decrypt String"+ encrypted_data);
	    	byte[] encrypted_data_byte = Base64.getUrlDecoder().decode(encrypted_data);
	    	
//	    	long startTimeAES = System.currentTimeMillis();
	    	
	    	AES aes = new AES();
	    	String decrypted_data = new String(aes.AES_decrypt(encrypted_data_byte, symmetric_key, new byte[16]));
	    	
//	    	long endTimeAES = System.currentTimeMillis();
//			time_aes = endTimeAES - startTimeAES;
			
	    	return decrypted_data; 
    	}catch(Exception e){
    		//e.printStackTrace();
    		return "The decryption operation failed!"; 
    	}
    }
    
    private String getEncSymKey(String key_id) throws Exception{
    	String storage_type = key_storage.getStorage_type();
    	switch(storage_type){
    		/****** RABBIT MQ Communication: Recover the encrypted private key using RabbitMqLibrary ******/
    		case "database":
    			String db_ip = "", db_port = "", db_auth_user = "", db_auth_pwd = "", db_database = "", db_table = "";
    			ArrayList<Storage_Parameters> storage_parameters = key_storage.getStorage_parameters();
    			for(Storage_Parameters sp : storage_parameters){
    				if(sp.getName().equals("db_ip")) db_ip = sp.getValue();
    				else if(sp.getName().equals("db_port")) db_port = sp.getValue();
    				else if(sp.getName().equals("db_auth_user")) db_auth_user = sp.getValue();
    				else if(sp.getName().equals("db_auth_pwd")) db_auth_pwd = sp.getValue();
    				else if(sp.getName().equals("db_database")) db_database = sp.getValue();
    				else if(sp.getName().equals("db_table")) db_table = sp.getValue();
    			}
    			
    			String credentials = db_auth_user + ":" + db_auth_pwd;
    			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(credentials.getBytes()));
    			
    			String urlGet = "http://" + db_ip + ":" + db_port + "/query/" + db_database + "/sql/" +
    							"select%20value%20from%20" + db_table + "%20where%20id=\"" + key_id + "\"";
    			URL url = new URL(urlGet);
    			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    			connection.setRequestProperty("Authorization", basicAuth);
    			connection.setRequestMethod("GET");
    			connection.setUseCaches(false);
    			connection.setDoInput(true);
    			connection.setDoOutput(true);
    						
    			if(connection.getResponseCode()==200){
    				String json_response = "";
    				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    				String text = "";
    				while ((text = br.readLine()) != null) {
    					json_response += text;
    				}
    				Gson gson = new Gson();
    				OrientDB_Recovered_Key message_class = gson.fromJson(json_response, OrientDB_Recovered_Key.class);
    				
    				return message_class.getResult().get(0).getValue(); //There will only be one result, since the key id is unique
    			}else	
    				throw new Exception("The encrypted symmetric key has not been recovered");		
    		default:
    			throw new Exception("The encrypted symmetric key has not been recovered");
    	}
    }
    
    
    private void anticipate_key(String key_id){
    	try{
	    	/* Check if this data consumer has the key associated with the identifier */
	    	if(!this.sym_keys.containsKey(key_id)){
				logger.info("--- IT'S NECESSARY TO RECOVER AND DECRYPT THE ANTICIPATED KEY ---");
	    		String encrypted_symmetric_key = getEncSymKey(key_id);
	    		/* Decrypt the key by CP-ABE using its CP-ABE private key */
	    		int index = encrypted_symmetric_key.indexOf(" ");
	    		String cph_b64 = encrypted_symmetric_key.substring(0, index);
	    		String aes_b64 = encrypted_symmetric_key.substring(index +  1);
	    		byte[] cph = Base64.getUrlDecoder().decode(cph_b64);
		        byte[] aes = Base64.getUrlDecoder().decode(aes_b64);
		        byte[][] enc_sym_key = new byte[2][];
		        enc_sym_key[0] = cph;
		        enc_sym_key[1] = aes;
		        byte[] symmetric_key = this.cpabe.dec(this.public_parameters, this.cpabe_private_key, enc_sym_key);
		        if(symmetric_key==null)
		        	throw new Exception("Error decrypting symmetric key");
		        /* Locally store the key for future decryption operations */
		        this.sym_keys.put(key_id, symmetric_key);
	    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
}