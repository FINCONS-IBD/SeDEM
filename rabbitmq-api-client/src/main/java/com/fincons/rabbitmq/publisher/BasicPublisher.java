package com.fincons.rabbitmq.publisher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.fincons.rabbitmq.client.BasicRabbitMqClient;
import com.fincons.rabbitmq.event.Event;
import com.fincons.util.AMQPConstants;
import com.fincons.util.BrokerParameterList;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;

import algorithms.CPABE;
import cpabe.Common;
import entities.Cph;
import messages.CPABE_Policy;
import messages.Key_Id;
import messages.Key_Storage;
import messages.Metadata;
import messages.Storage_Parameters;

/**
 * Provides the default implementation of {@link Publisher}.
 * 
 * @author Fincons Group AG
 *
 */
public class BasicPublisher extends BasicRabbitMqClient implements Publisher {
    	
	final static Logger logger = Logger.getLogger(BasicPublisher.class);
	
	public static final String APPLICATION_ID = BasicPublisher.class.getName() + "V0.1";
	
	private String pattern;
    private String exchange;
    private String userID; //IMPORTANT: this ID must be unique
    
    /**** Parameters related with the shared symmetric key ****/
    private Cph cph;
    private String key_type;
    private String cryptographic_curve;
    private String proxy_ip;
    private String proxy_port;
    private String proxy_id;
    private String alg;
    private String enc;
    private String specs;
    //At the moment, only one storage
    private Key_Storage key_storage; 
    
    /* CP-ABE aproach */
    private boolean cpabe_enabled;
    private byte[] public_parameters;
    private String policy;
//    private int refresh_num_messages;
    
//    private long time_dh;
//    private long time_enc;
    
    /* For "anticipated keys" version */
    private boolean is_anticipated_key;
    private int anticipatedKeySeconds;
    private Cph anticipated_cph;
    private AnticipationThread anticipationThread;
    
    public BasicPublisher () {
		super();	
	}
    
    private class AnticipationThread implements Runnable{
		@Override
		public void run() {
			anticipated_cph = setup_symmetric_key();
		}
    }

    /* (non-Javadoc)
     * @see com.fincons.rabbitmq.client.BasicRabbitMqClient#useENSBrokerConnectionParameters
     */
    @Override
    protected void useENSBrokerConnectionParameters(BrokerParameterList parameters) {
    	
//		logger.info("Calling the useENSBrokerConnectionParameters() method...");
    	
        pattern = parameters.getPattern();
    	if( parameters.getExchangeName()!=null && !parameters.getExchangeName().equals("")){
    		exchange = parameters.getExchangeName();
		}else{
			exchange = parameters.getDestinationName();
		}
        userID = parameters.getSubjectID();
        
        try {
			this.public_parameters = Common.suckFile(parameters.getPubParams());
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        /* Check if Cph flag is activated to establish the shared symmetric key */
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        this.cpabe_enabled = parameters.isCpabeEnabled();
        if(this.cpabe_enabled){
        	this.policy = parameters.getCphPolicy();
//        	this.time_enc = 0;
//        	this.time_enc = -1;
        }else if(parameters.isCphEnabled()){
        	this.policy = parameters.getCphPolicy();
//        	this.time_enc = 0;
//        	this.time_dh = 0;
//        	this.refresh_num_messages = parameters.getRefreshNM();
        	is_anticipated_key = parameters.isAnticipatedKey();
        	if(is_anticipated_key){
        		this.anticipatedKeySeconds = parameters.getAnticipatedKeySeconds();
        		anticipationThread = new AnticipationThread();
        	}
        	/* Firstly, it is necessary to get the storages in which the symmetric key will be saved */
        	getParamsCph(parameters);
        	this.cph = setup_symmetric_key();
        }
    }
    
    /* (non-Javadoc)
     * @see com.fincons.rabbitmq.publisher.Publisher#publish()
     */
    @Override
    public void publish(Event event, long numEvents) throws IllegalArgumentException, IllegalStateException {
    	
//		logger.info("Calling the publish() method...");

        if (!this.isConnected()){
        	logger.error("The publisher is not connected");
            throw new IllegalStateException("The publisher is not connected");
        }
        if (event == null){
        	logger.error("The event to be published cannot be null");
            throw new IllegalArgumentException("The event to be published cannot be null");
        }
        
        /* TEST 1: CONSUMED MEMORY AND TIME: RASPBERRRY */
//		this.time_enc = 0;
//		this.time_dh = -1;
//        Runtime runtime = Runtime.getRuntime();
//		runtime.gc();
//		long startTime = System.currentTimeMillis();

        /* Check if data must be encrypted */
        Event event_to_send = event;
        if(this.cpabe_enabled){
        	try {
				event_to_send = encrypt_data_cpabe(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }else if(this.cph != null){
        	try{
	        	/* Before encrypting, it is necessary to know if the current symmetric key is expired. In this case, a new shared symmetric key is generated. */
	        	/** BY DATE **/
        		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	        	Date expiration_date = df.parse(cph.getExpirationDate());
	    		Date current_date = new Date();
	    		if(expiration_date.compareTo(current_date) <= 0){
	    		/** BY MESSAGES **/
//	    		if(numEvents % refresh_num_messages == 0){
	    			/* Check if an anticipated key was calculated */
	    			if(anticipated_cph != null){
	    				this.cph = this.anticipated_cph;
	    				this.anticipated_cph = null;
	    			}else{
	    				this.cph = setup_symmetric_key();
//	    				this.time_enc += this.cph.getTimeAES();
//	    				this.time_dh = this.cph.getTimeDH();
	    			}
	        	}else{
		    		/* Anticipated key */
		    		if(is_anticipated_key){
			    		Date anticipation_date = expiration_date;
			    		anticipation_date.setSeconds(expiration_date.getSeconds() - this.anticipatedKeySeconds); //When to start to calculate next key
			    		/* A new anticipated key is calculated if there is none */
			    		if((anticipation_date.compareTo(current_date) <= 0) && this.anticipated_cph == null){
			    			Thread t = new Thread(anticipationThread);
			    			t.run();
			        	}
		    		}
	        	}
	        } catch (ParseException e) {
				e.printStackTrace();
			}
	        event_to_send = encrypt_data(event);
        }
        
        //build the AMQP message properties
        Builder messagePropertiesBuilder = new Builder();
        messagePropertiesBuilder.contentEncoding(event_to_send.getContentEncoding())
            .contentType(event_to_send.getContentType())
            .deliveryMode(event_to_send.isPersistent() ? AMQPConstants.PERSISTENT_DELIVERY : AMQPConstants.NOT_PERSISTENT_DELIVERY)
            .timestamp(event_to_send.getTimestamp())
            .userId(userID)
            .appId(APPLICATION_ID)
            .priority(event_to_send.getPriority())
            .headers(event_to_send.getHeaders()); 
        BasicProperties properties = messagePropertiesBuilder.build();
        
        try {
            
            channel.basicPublish(exchange, pattern, properties, event_to_send.getPayload());
            
            logger.debug("Published an event on the exchange '" + exchange + "'" +
                    " (pattern: '" + pattern + "') with properties " + properties  + " and a " +
                    MessageFormat.format("{0,number,integer} {0,choice,0#bytes|1#byte|1<bytes}",
                    		event_to_send.getPayload() == null? 0 : event_to_send.getPayload().length) + " payload");
            
            /*  TEST 1: CONSUMED MEMORY AND TIME: RASPBERRRY */
//            long endTime   = System.currentTimeMillis();
//            runtime = Runtime.getRuntime();
//			runtime.gc();
//			long memory = runtime.totalMemory() - runtime.freeMemory();
//			long totalTime = endTime - startTime;
//			try {
//				File TextFile = new File("mem_pub_rasp.csv"); 
//				FileWriter TextOut = new FileWriter(TextFile, true);
//				TextOut.write(memory/1024L + "\n");
//				TextOut.close();
//				TextFile = new File("time_pub_rasp.csv"); 
//				TextOut = new FileWriter(TextFile, true);
//				TextOut.write(totalTime + ";");
//				if(this.time_dh > -1) TextOut.write(this.time_dh + ";");
//				TextOut.write(this.time_enc + "\n");
//				TextOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
        } catch (IOException e) {
			logger.error("Error during event publishing", e);
            e.printStackTrace();
            closeConnection();
        }
		
    }
    
    private void getParamsCph(BrokerParameterList parameters){
    	key_type = parameters.getCphKty();
		cryptographic_curve = parameters.getCphCrv();
		proxy_ip = parameters.getCphProxyIp();
		proxy_port = parameters.getCphProxyPort();
		proxy_id = parameters.getCphProxyId();
		alg = parameters.getCphAlg();
		enc = parameters.getCphEnc();
		specs = parameters.getCphPolicy();
    	
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
    
    private Cph setup_symmetric_key(){
    	try{
			Cph new_cph = new Cph(this.userID);
    		/* 1.1. Generate an ephemeral elliptic curve key pair */
    		new_cph.Generate_ekeys(key_type, cryptographic_curve);
			/* 1.2. Send its ephemeral public key as well as the selected ECC curve to ABE proxy. The shared symmetric key is calculated */
    		new_cph.sendMessageProxy(proxy_ip, proxy_port, "/ABE-Proxy/generate_shared_secret", proxy_id, alg, enc);
			
			/* 2.1. Encrypt CP-ABE related information by AES (shared_sym_key) */
			/* Get cpabe_info */
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
			df.setTimeZone(tz);
			Date date = new Date();
			String creation_date = df.format(date);
			Metadata creation_date_metadata = new Metadata("creation-date", creation_date);
			String name = "policy_" + this.userID;
			Metadata name_metadata = new Metadata("name", name);
			String version = "1.0";
			Metadata version_metadata = new Metadata("version", version);
			String description = "Policy to encrypt symmetric key of " + this.userID;
			Metadata description_metadata = new Metadata("description", description);
			String author = this.userID;
			Metadata author_metadata = new Metadata("author", author);
			String library = "junwei-wang/cpabe";
			Metadata libray_metadata = new Metadata("library", library);
			ArrayList<Metadata> policy_metadatas = new ArrayList<Metadata>();
			policy_metadatas.add(creation_date_metadata);policy_metadatas.add(name_metadata);policy_metadatas.add(version_metadata);
			policy_metadatas.add(description_metadata);policy_metadatas.add(author_metadata);policy_metadatas.add(libray_metadata);
			String url = null;
			CPABE_Policy policy = new CPABE_Policy(specs, url, policy_metadatas);
			ArrayList<Key_Storage> encrypted_symmetric_key_storages = new ArrayList<Key_Storage>();
			encrypted_symmetric_key_storages.add(this.key_storage);
			String cpabe_info = new_cph.get_cpabe_info(policy, this.key_storage);
		
			/* Get encrypted cpabe_info */
			String enc_cpabe_info = new_cph.encrypt_cpabe_info(cpabe_info);			
			
			/* 2.2. Send the encrypt CP-ABE related information to ABE proxy */
			new_cph.sendMessageProxy(proxy_ip, proxy_port, "/ABE-Proxy/cpabe_information", enc_cpabe_info);
			
			return new_cph;
			
        }catch(Exception e){
			e.printStackTrace();
			return null;
		}
    }
    
    private Event encrypt_data_cpabe(Event event) throws Exception{
    	StringBuffer payload = new StringBuffer();
    	Map<String,Object> headers = event.getHeaders();
		/* Add the encrypted symmetric key id */
		headers.put("protection-mechanism", "CP-ABE");
    	
    	/*  TEST 1: CONSUMED TIME: RASPBERRRY */
//		long startTime = System.currentTimeMillis();

		CPABE cpabe = new CPABE();
		byte[][] encrypted_value_bytes = cpabe.CPABE_encrypt(public_parameters, policy, event.getPayload());		
		
		
//		Runtime runtime = Runtime.getRuntime();
//		runtime.gc();
//		long memory = runtime.totalMemory() - runtime.freeMemory();
//		try {
//			File TextFile = new File("mem_pub_rasp.csv"); 
//			FileWriter TextOut = new FileWriter(TextFile, true);
//			TextOut.write(memory/1024L + "\n");
//			TextOut.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		/*  TEST 1: CONSUMED TIME: RASPBERRRY */
//		long endTime   = System.currentTimeMillis();
//		this.time_enc += endTime - startTime;
		
    	String cph_b64 = Base64.getEncoder().withoutPadding().encodeToString(encrypted_value_bytes[0]);
        String aes_b64 = Base64.getEncoder().withoutPadding().encodeToString(encrypted_value_bytes[1]);
        String encrypted_value = cph_b64 + " " + aes_b64;
		payload.append(encrypted_value);

		try {
			return new BasicEventFactory().create(headers, payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }
    
    private Event encrypt_data(Event event){
    	StringBuffer payload = new StringBuffer();
//		Map<String,Object> headers = new HashMap<String,Object>();
    	Map<String,Object> headers = event.getHeaders();

		Key_Id symmetric_key_id = cph.getSymmetricKeyId();
		String id = symmetric_key_id.getEnc_sym_key_id();
		Key_Id a_symmetric_key_id = null;
		String a_id = "";
		if(anticipated_cph != null){
			a_symmetric_key_id = anticipated_cph.getSymmetricKeyId();
			a_id = a_symmetric_key_id.getEnc_sym_key_id();
		}
			
//		logger.info("ID encrypted symmetric key: " + id);
		
		/* The header values are encrypted */
//		Map<String,Object> event_headers = event.getHeaders();
//		for(String key : event_headers.keySet()){
//			if(!key.equals("timestamp")){
//				String value = event_headers.get(key).toString();
//				logger.info("--- " + key + " value: " + value);
//				String encrypted_value = cph.encrypt_value(value);
//				headers.put(key + "(encrypted)", encrypted_value);
//			}else{
//				String value = event_headers.get(key).toString();
//				headers.put(key, value);
//			}
//		}
		
		/* Add the encrypted symmetric key id */
		String encryption_algorithms = "";
		String expiration_date = "";
		for(Metadata m : symmetric_key_id.getMetadata()){
			if(m.getName().equals("protection-mechanism"))
				encryption_algorithms = m.getValue();
			else if(m.getName().equals("expiration-date"))
				expiration_date = m.getValue();
		}
		headers.put("key-id", id);
		headers.put("key-expiration-date", expiration_date);
		headers.put("protection-mechanism", encryption_algorithms);
		if(anticipated_cph != null){
			String ant_expiration_date = "";
			for(Metadata m : anticipated_cph.getSymmetricKeyId().getMetadata()){
				if(m.getName().equals("expiration-date")){
					ant_expiration_date = m.getValue();
					break;
				}
			}
			headers.put("next-key-id", a_id);
			headers.put("next-key-expiration-date", ant_expiration_date);
		}

//		Gson gson = new Gson();
//		String key_id_string = gson.toJson(symmetric_key_id);
//		payload.append(key_id_string);
		
		/*  TEST 1: CONSUMED TIME: RASPBERRRY */
//		long startTime = System.currentTimeMillis();

		String encrypted_value = cph.encrypt_value(new String(event.getPayload()), new byte[16]);
		payload.append(encrypted_value);
		
//		Runtime runtime = Runtime.getRuntime();
//		runtime.gc();
//		long memory = runtime.totalMemory() - runtime.freeMemory();
//		try {
//			File TextFile = new File("mem_pub_rasp.csv"); 
//			FileWriter TextOut = new FileWriter(TextFile, true);
//			TextOut.write(memory/1024L + "\n");
//			TextOut.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		/*  TEST 1: CONSUMED TIME: RASPBERRRY */
//		long endTime   = System.currentTimeMillis();
//		this.time_enc += endTime - startTime;
		
		try {
			return new BasicEventFactory().create(headers, payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }
}