package entities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.Gson;

import algorithms.AES;
import algorithms.CPABE;
import algorithms.Elliptic_Curve_Diffie_Hellman;
import algorithms.HMAC;
import cpabe.Common;
import messages.CPABE_Info;
import messages.CPABE_Policy;
import messages.EPK_Info;
import messages.Enc_CPABE_Info;
import messages.Enc_CPABE_InfoSign;
import messages.Enc_CPABE_InfoSignBody;
import messages.JSON_Web_Key;
import messages.Key_Id;
import messages.Key_Storage;
import messages.Metadata;
import messages.OrientDB_Recovered_Key;
import messages.Storage_Parameters;

/*
 * @author Salvador P�rez
 * @version 24/11/2016
 * 
 * @author Diego Pedone
 * @modified 07/06/2017
 */

public class Cph {
	private String id;
	private Key_Id symmetric_key_id;
	private String expirate_date;
	
	/* Algorithms */
	private Elliptic_Curve_Diffie_Hellman ecdh;
	private AES aes;
	
	/* Time */
//	private long time_dh;
//	private long time_aes;

	public Cph(String id){
		this.id = id;
//		this.time_dh = 0;
//		this.time_aes = 0;
	}
	
/***********************************************************************/
/***********************************************************************/
/************************ PROCESSES AND FLOWS **************************/
/***********************************************************************/
/***********************************************************************/

/* 1.1. Generate an ephemeral elliptic curve key pair */
	public void Generate_ekeys(String key_type, String cryptographic_curve){
//		long startTime = System.currentTimeMillis();
		
		this.ecdh = new Elliptic_Curve_Diffie_Hellman(key_type, cryptographic_curve);
		
//		long endTime   = System.currentTimeMillis();
//		this.time_dh += endTime - startTime;
	}
/* 1.1. Generate an ephemeral elliptic curve key pair */
	
/* 1.2. Send its ephemeral public key as well as the selected ECC curve to ABE proxy */
	public void sendMessageProxy(String protocol, String ip, String port, String context, String proxy_id, String alg, String enc){
		try{
			/* Get JSON "device_epk_info" */
			JSON_Web_Key jwk = new JSON_Web_Key(this.ecdh.getKey_type(), this.ecdh.getCryptographic_curve(), this.ecdh.getXparameterB64url(), this.ecdh.getYparameterB64url());
			String apu = Base64.getUrlEncoder().withoutPadding().encodeToString(this.id.getBytes());
			String apv = Base64.getUrlEncoder().withoutPadding().encodeToString(proxy_id.getBytes());
			EPK_Info device_epk_info_class = new EPK_Info(alg, enc, apu, apv, jwk);
			Gson gson = new Gson();
			String device_epk_info = gson.toJson(device_epk_info_class);

			/* Send message */
			String url_str = protocol+"://" + ip + ":" + port + context;
			URL url = new URL(url_str);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
	
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(device_epk_info);
			writer.close();
	
			InputStreamReader isrResponse = new InputStreamReader(connection.getInputStream());
			BufferedReader brResp = new BufferedReader(isrResponse);
			String line;
			String payload = "";
	        while ((line = brResp.readLine()) != null) {
	        	payload += line;
//	        	System.out.println(line);
	        }
	        
//	        long startTime = System.currentTimeMillis();
	        
/* 1.9. Calculate the shared secret by Diffie-Hellman algorithm */	
	        EPK_Info proxy_epk_info = gson.fromJson(payload, EPK_Info.class);
			this.ecdh.ECDH_ES(proxy_epk_info);
/* 1.9. Calculate the shared secret by Diffie-Hellman algorithm */
	        
/* 1.10. Calculate the symmetric shared key by Concat KDF algorithm */		
			this.ecdh.Concat_KDF(proxy_epk_info);
/* 1.10. Calculate the symmetric shared key by Concat KDF algorithm */
			
//			long endTime   = System.currentTimeMillis();
//			this.time_dh += endTime - startTime;
			
/* 1.11. Delete ephemeral key pair */		
			this.ecdh.Delete_ekeys();
/* 1.11. Delete ephemeral key pair */
			
//			System.out.println("SHARED SYMMETRIC KEY: " + Base64.getEncoder().withoutPadding().encodeToString(this.ecdh.getShared_Sym_Key()));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
/* 1.2. Send its ephemeral public key as well as the selected ECC curve to ABE proxy */

/* 2.1. Encrypt CP-ABE related information by AES (shared_sym_key) */
	public String get_cpabe_info(CPABE_Policy policy, Key_Storage encrypted_symmetric_key_storages){
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String timestamp = df.format(new Date());
		
		CPABE_Info cpabe_info_class = new CPABE_Info(timestamp, this.id, policy, encrypted_symmetric_key_storages);
		Gson gson = new Gson();
		String cpabe_info = gson.toJson(cpabe_info_class);
		
		return cpabe_info;
	}
	
	public String encrypt_cpabe_info(String cpabe_info){
		byte[] shared_sym_key = this.ecdh.getShared_Sym_Key();
		
		//calculate Key2for signature
		byte[] key2ToHmac= HMAC.generateHMACKeySha256(shared_sym_key);
	
		//calculate iv random
		byte[] iv=SecureRandom.getSeed(16);
		
//		long startTime = System.currentTimeMillis();
		
		this.aes = new AES();
		byte[] enc_cpabe_info_byte = this.aes.AES_encrypt(cpabe_info.getBytes(), shared_sym_key, iv);
		
//		long endTime   = System.currentTimeMillis();
//		this.time_aes += endTime - startTime;
		
		String encrypted_data_b64u = Base64.getUrlEncoder().withoutPadding().encodeToString(enc_cpabe_info_byte);
		
		String ivb64u= Base64.getUrlEncoder().withoutPadding().encodeToString(iv);
		//crea oggetto Enc_info con iv e encrypted_data_b64u
		Enc_CPABE_InfoSignBody enc_data_to_Sign=new Enc_CPABE_InfoSignBody(ivb64u,encrypted_data_b64u);
		//trasformarlo array di byte
		Gson gson = new Gson();
		String enc_data_to_SignString=gson.toJson(enc_data_to_Sign);
	
		//firmare l'oggetto con Key2
		byte[] signature=null;
		try {
			signature = HMAC.signHMACSHA256(enc_data_to_SignString.getBytes(), key2ToHmac);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		String tag= Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
		// creare Json da mandare
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String encryption_date = df.format(new Date());
		Metadata encryption_date_metadata = new Metadata("encryption-date", encryption_date);
		
		String encryptor = this.id;
		Metadata encryptor_metadata = new Metadata("encryptor", encryptor);
		
		ArrayList<Metadata> encrypted_data_metadatas = new ArrayList<Metadata>();
		encrypted_data_metadatas.add(encryption_date_metadata); encrypted_data_metadatas.add(encryptor_metadata);
		
		Enc_CPABE_InfoSign enc_cpabe_info_class = new Enc_CPABE_InfoSign(enc_data_to_Sign, encrypted_data_metadatas, tag);
	
		String enc_cpabe_info = gson.toJson(enc_cpabe_info_class);
		
		return enc_cpabe_info;
	}
/* 2.1. Encrypt CP-ABE related information by AES (shared_sym_key) */

/* 2.2. Send the encrypt CP-ABE related information to ABE proxy */
	public void sendMessageProxy(String protocol, String ip, String port, String context, String enc_cpabe_info){
		try{
			/* Send message */
			String url_str = protocol+"://" + ip + ":" + port + context;
			URL url = new URL(url_str);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
	
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(enc_cpabe_info);
			writer.close();
	
			InputStreamReader isrResponse = new InputStreamReader(connection.getInputStream());
			BufferedReader brResp = new BufferedReader(isrResponse);
			String line;
			String key_id = ""; //TEST: SEND ENC_SYM_KEY, NOT ID
	        while ((line = brResp.readLine()) != null) {
	        	key_id += line;
	        	//System.out.println(line);
	        }
	        Gson gson = new Gson();
	        Key_Id key_id_class = gson.fromJson(key_id, Key_Id.class);
	        this.symmetric_key_id = key_id_class;
	        /* To get the expiration date */
	        for(Metadata m : this.symmetric_key_id.getMetadata()){
	        	if(m.getName().equals("expiration-date")){
	        		this.expirate_date = m.getValue();
	        		break;
	        	}
	        }
	        
//	        String key_id_b64 = key_id_class.getEnc_sym_key_id();
//	        System.out.println("ID SHARED SYMMETRIC KEY: " + key_id_b64);
	        
/******************************************************************************************************************************/
	        
//	        int index = key_id_b64.indexOf(" ");
//	        String cph_b64 = key_id_b64.substring(0, index);
//	        String aes_b64 = key_id_b64.substring(index +  1);
//	        byte[][] enc_sym_key = new byte[2][];
//	        enc_sym_key[0] = Base64.getDecoder().decode(cph_b64);
//	        enc_sym_key[1] = Base64.getDecoder().decode(aes_b64);
//	        /* Decrypt shared_sym_key with CP-ABE */
//	        this.cpabe = new CPABE();
//	        byte[] shared_sym_key = this.cpabe.CPABE_decrypt(this.public_key, this.valid_cpabe_key, enc_sym_key);
//	        if(shared_sym_key != null)
//	        	System.out.println("DEC SHARED SYMMETRIC KEY WITH VALID CPABE KEY: " + Base64.getEncoder().withoutPadding().encodeToString(shared_sym_key));
//	        shared_sym_key = this.cpabe.CPABE_decrypt(this.public_key, this.invalid_cpabe_key, enc_sym_key);
//	        if(shared_sym_key != null)
//	        	System.out.println("DEC SHARED SYMMETRIC KEY WITH INVALID CPABE KEY: " + Base64.getEncoder().withoutPadding().encodeToString(shared_sym_key));
//	        /* Decrypt shared_sym_key with CP-ABE */
	        
/******************************************************************************************************************************/

		}catch(Exception e){
			e.printStackTrace();
		}
	}
/* 2.2. Send the encrypt CP-ABE related information to ABE proxy */
	
/* X.X. Get the CP-ABE key associated with a set of attributes */
//	public void getCPABEKey(String ip, String port, String context){
//		try{
//			/* Request KGS's public key */
//			Gson gson = new Gson();
//			String url_str = "http://" + ip + ":" + port + "/get_KGS_public_key";
//			URL url = new URL(url_str);
//			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//			connection.setDoOutput(true);
//			/* Request KGS's public key */
//		
//			/* Get KGS's public key */
//			InputStreamReader isrResponse = new InputStreamReader(connection.getInputStream());
//			BufferedReader brResp = new BufferedReader(isrResponse);
//			String line;
//			String pub_key_string = "";
//	        while ((line = brResp.readLine()) != null) {
//	        	pub_key_string += line;
//	        	//System.out.println(line);
//	        }
//	        CPABE_Public_Key KGS_public_key_class = gson.fromJson(pub_key_string, CPABE_Public_Key.class);
//	        this.public_key = Base64.getDecoder().decode(KGS_public_key_class.getPublic_Parameters());
//	        /* Get KGS's public key */
//			
//			/* Valid CP-ABE private key example */
//			url_str = "http://" + ip + ":" + port + context;
//			url = new URL(url_str);
//			connection = (HttpURLConnection) url.openConnection();
//			connection.setDoOutput(true);
//			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
//			writer.write(this.valid_atts);
//			writer.close();
//			isrResponse = new InputStreamReader(connection.getInputStream());
//			brResp = new BufferedReader(isrResponse);
//			String cpabe_key_b64 = "";
//	        while ((line = brResp.readLine()) != null) {
//	        	cpabe_key_b64 += line;
//	        	//System.out.println(line);
//	        }
//	        System.out.println("VALID CPABE KEY: " + cpabe_key_b64);
//	        this.valid_cpabe_key = Base64.getDecoder().decode(cpabe_key_b64);
//
//	        /* Invalid CP-ABE private key example */
//			connection = (HttpURLConnection) url.openConnection();
//			connection.setDoOutput(true);
//			writer = new OutputStreamWriter(connection.getOutputStream());
//			writer.write(this.invalid_atts);
//			writer.close();
//			isrResponse = new InputStreamReader(connection.getInputStream());
//			brResp = new BufferedReader(isrResponse);
//			cpabe_key_b64 = "";
//	        while ((line = brResp.readLine()) != null) {
//	        	cpabe_key_b64 += line;
//	        	//System.out.println(line);
//	        }
//	        System.out.println("INVALID CPABE KEY: " + cpabe_key_b64);
//	        this.invalid_cpabe_key = Base64.getDecoder().decode(cpabe_key_b64);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//	}
/* X.X. Get the CP-ABE key associated with a set of attributes */
	
	/* Phase 3. This method is called to encrypt different topic values */
	public String encrypt_value(String value, byte[]iv){
		byte[] shared_sym_key = this.ecdh.getShared_Sym_Key();
		this.aes = new AES();
		byte[] enc_cpabe_info_byte = this.aes.AES_encrypt(value.getBytes(), shared_sym_key, iv);
		String encrypted_data_b64u = Base64.getUrlEncoder().withoutPadding().encodeToString(enc_cpabe_info_byte);
		return encrypted_data_b64u;
	}
	
	/* Phase 3. This method is called to encrypt different the byte array passed like parameter */
	public String encrypt_value(byte[] data, byte[]iv){
		byte[] shared_sym_key = this.ecdh.getShared_Sym_Key();
		this.aes = new AES();
		byte[] enc_cpabe_info_byte = this.aes.AES_encrypt(data, shared_sym_key, iv);
		String encrypted_data_b64u = Base64.getUrlEncoder().withoutPadding().encodeToString(enc_cpabe_info_byte);
		return encrypted_data_b64u;
	}
	
	 private String getEncSymKey(Key_Storage key_storage, String key_id, String credentialsKS) throws Exception{
	    	String storage_type = key_storage.getStorage_type();
	    	switch(storage_type){
	    		/****** RABBIT MQ Communication: Recover the encrypted private key using RabbitMqLibrary ******/
	    		case "database":
//	    			String db_ip = "", db_port = "", db_auth_user = "", db_auth_pwd = "", db_database = "", db_table = "";
	    			String db_ip = "", db_port = "", db_database = "", db_table = "";
	    			ArrayList<Storage_Parameters> storage_parameters = key_storage.getStorage_parameters();
	    			for(Storage_Parameters sp : storage_parameters){
	    				if(sp.getName().equals("db_ip")) db_ip = sp.getValue();
	    				else if(sp.getName().equals("db_port")) db_port = sp.getValue();
	    				else if(sp.getName().equals("db_database")) db_database = sp.getValue();
	    				else if(sp.getName().equals("db_table")) db_table = sp.getValue();
	    			}

	    			String urlGet = "http://" + db_ip + ":" + db_port + "/query/" + db_database + "/sql/" +
	    							"select%20value%20from%20" + db_table + "%20where%20id=\"" + key_id + "\"";
	    			URL url = new URL(urlGet);
	    			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
	    			if(credentialsKS!=null){
	//	    			db_auth_user=servletConfig.get(db_ip+":"+db_port+"|username");
	//	    			db_auth_pwd=servletConfig.get(db_ip+":"+db_port+"|password");
//		    			String credentials = db_auth_user + ":" + db_auth_pwd;
		    			String basicAuth = "Basic " + new String(Base64.getUrlEncoder().encode(credentialsKS.getBytes()));
		    			connection.setRequestProperty("Authorization", basicAuth);
	    			}
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
	    		case "embedded":
	    			ArrayList<Storage_Parameters> storage_parameters_embedded = key_storage.getStorage_parameters();
	    			for(Storage_Parameters sp : storage_parameters_embedded){
	    				if(sp.getName().equals("encrypted_key"))
	    					return sp.getValue();
	    			}
	    			throw new Exception("The encrypted symmetric key has not been recovered");
	    		default:
	    			throw new Exception("The encrypted symmetric key has not been recovered");
	    	}
	    }
	
/***********************************************************************/
/***********************************************************************/
/************************ PROCESSES AND FLOWS **************************/
/***********************************************************************/
/***********************************************************************/

	public String getId() {
		return id;
	}
	
	public Key_Id getSymmetricKeyId() {
		return symmetric_key_id;
	}
	
	public String getExpirationDate() {
		return expirate_date;
	}
	
//	public long getTimeDH() {
//		return time_dh;
//	}
//	
//	public long getTimeAES() {
//		return time_aes;
//	}

	public static void main(String[] args) throws Exception {

		String alg="ECDH";
		String enc="A128GCM";
		String key_type="EC";
		String cryptographic_curve="P-256";
		String proxy_ip="127.0.0.1";
		String proxy_port="8080";
		String proxy_id="ABE-Proxy";
		String username="testDiego1";
		Cph new_cph = new Cph(username);
		/* 1.1. Generate an ephemeral elliptic curve key pair */
		new_cph.Generate_ekeys(key_type, cryptographic_curve);
		/* 1.2. Send its ephemeral public key as well as the selected ECC curve to ABE proxy. The shared symmetric key is calculated */
		new_cph.sendMessageProxy("http",proxy_ip, proxy_port, "/ABE-Proxy/generate_shared_secret", proxy_id, alg, enc);
		/* 2.1. Encrypt CP-ABE related information by AES (shared_sym_key) */
		/* Get cpabe_info */
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		Date date = new Date();
		String creation_date = df.format(date);
		Metadata creation_date_metadata = new Metadata("creation-date", creation_date);
		String name = "policy_" +username;
		Metadata name_metadata = new Metadata("name", name);
		String version = "1.0";
		Metadata version_metadata = new Metadata("version", version);
		String description = "Policy to encrypt symmetric key of " + username;
		Metadata description_metadata = new Metadata("description", description);
		String author = username;
		Metadata author_metadata = new Metadata("author", author);
		String library = "junwei-wang/cpabe";
		Metadata libray_metadata = new Metadata("library", library);
		ArrayList<Metadata> policy_metadatas = new ArrayList<Metadata>();
		policy_metadatas.add(creation_date_metadata);policy_metadatas.add(name_metadata);policy_metadatas.add(version_metadata);
		policy_metadatas.add(description_metadata);policy_metadatas.add(author_metadata);policy_metadatas.add(libray_metadata);
		String url = null;
		CPABE_Policy policy = new CPABE_Policy("c:IT", url, policy_metadatas);
//		ArrayList<Key_Storage> encrypted_symmetric_key_storages = new ArrayList<Key_Storage>();
		ArrayList<Storage_Parameters>storageParameters = new ArrayList<Storage_Parameters>();
		storageParameters.add(new Storage_Parameters("db_ip","89.207.106.75"));
		storageParameters.add(new Storage_Parameters("db_port","2480"));
		storageParameters.add(new Storage_Parameters("db_database","CpAbeKeyStoreDB"));
		storageParameters.add(new Storage_Parameters("db_table","EncSymKeys"));
		Key_Storage ks=new Key_Storage("database", storageParameters);
//		encrypted_symmetric_key_storages.add(ks);
		String cpabe_info = new_cph.get_cpabe_info(policy, ks);
	
		/* Get encrypted cpabe_info */
		String enc_cpabe_info = new_cph.encrypt_cpabe_info(cpabe_info);
		
		/* 2.2. Send the encrypt CP-ABE related information to ABE proxy */
		new_cph.sendMessageProxy("http", proxy_ip, proxy_port, "/ABE-Proxy/cpabe_information", enc_cpabe_info);
		Key_Id kid= new_cph.getSymmetricKeyId();
		System.out.println(kid);
		
		//Calculate IV
		byte[] iv=SecureRandom.getSeed(16);
		//Encrypt file with shared symmetric key and iv
		String encData=new_cph.encrypt_value("prova", iv);
		String ivb64u = Base64.getUrlEncoder().withoutPadding().encodeToString(iv);
		System.out.println(encData);

		Cph dec_cph = new Cph(username);
		System.out.println(username);	
		System.out.println(ivb64u);	
		
		String keySymKey= dec_cph.getEncSymKey(ks, kid.getEnc_sym_key_id(), "cp_abe_user:cp_abe_user");
		
	    String[] splitenc_sym_key_string=keySymKey.split(" ");	    
		byte[] cph_b = Base64.getUrlDecoder().decode(splitenc_sym_key_string[0]);
	    byte[] aes_b = Base64.getUrlDecoder().decode(splitenc_sym_key_string[1]); 
	    byte[][]enc_shared_sym_keyByte={cph_b,aes_b};
	           
	    String personal_key = "AAAAgFr-muAGowuYj-7s1ytzkJvXWKUdOzwuf-7InrR5M0ERUHmaZCw2iZJ3g04flRCC_ssqIuwscEPv-pHpyc9PDxt9Gws1wifaLk8JrP7dqjNxdh"
	    		+ "XH77aVJTuosQtg7QjDYfynVkCNFEhuf9lMViMUdbnP3zpdra4vnfsJgsNYxSXjAAAABwAAAAlzbjpQZWRvbmUAAACAVlE2Uq6UC-j5ho4WFZ5juKQ7xhHfeeQ3AVul"
	    		+ "6nsUIH7kvxThLgfN8f02hW0w993kJ7v4aPJkFTyfJJIxGlU3_R_1beULIX8Od03axuQ8AkTZEy64q-l5LX_col8qI3ygGIuWJluWYN5ebyTM2cMsF57fZ7SiHa3R9e"
	    		+ "H14qyzf34AAACAmeroolQ9X6o4hqZNtl4VlQSd-4aoXUqaW5KZQwm1irF1ryD5zWTbvDGgty50hMOtN9Kv7QEMRMXD3wMGlrTtkSqKfdwLxuDUEc86HeD5EyHABMQ3"
	    		+ "DEeCNtPOfAx5UORIRE8ou9elEILpWF05Nazb5UIkysyG-ECVW61BQjSjqNkAAAAibWFpbDpkaWVnby5wZWRvbmVAZmluY29uc2dyb3VwLmNvbQAAAIBFedRcWBzd7N"
	    		+ "y7Vu82ug9R2dAY5lM_5uQVoy37HAGF4THqOE7Vf1wa4_J_xzHpd_eoKziHMtJ0ZLod_DmSG8ftiaDR22_0uUNfwtx0gxHAOApLUh0qxXAXbSVq15rsgTSvYyGmwdeY"
	    		+ "F5-JGxp9cNCF-u_jFxElKR8t1f6do4ToRAAAAIAV0Yr3q07rhtAFOsbpLOPNYbUjhdK_oHtqbxeJ8s8lN4s3MHenUh4kDSTJogdy61f1pYDsx_e6fwhcBT8VeJIFST"
	    		+ "gyu5u6jJV0orcBYOdfggeAd4qv0PO2G__kUfBqFTVAhG2pJUj-wgBZWHiYjp5Rw5pqRlNz_ugfWe_XHvqAwgAAAA9jbjpEaWVnb19QZWRvbmUAAACAebj_n0NYEuwN"
	    		+ "QkRNoyLDl8W9ynAk1XpvaT3Olz0HqGYZl-NChm0FnLBn3d7wwNJMCiaQmH5zlq6QsY1KGixkhA-jshftsRkCmHdUOW3pZpZaouyWsvqP4nAGJiVLN9dIjwVvwydlL4"
	    		+ "RxErEbgGiohPes0BC9Jt1dR8PEhV9A1OcAAACAX_bfVZEQZDDnr0OzCW2-UNdW0YrP1wGfgdXLmHgou403v6XBFKUXr8k-HnzboCKrq7gdvy6-IdcuQtDGbLABGBqc"
	    		+ "kQF2yZ-JhXZnfpytV6SPtyQaae47Q_v1pi-7j-DfyCTSLuuZshoZzTMreYvW6HErvlVVNLHh0Svp34N7EmUAAAAYb3U6SUJEXyZfSW5ub3ZhdGlvbl9CYXJpAAAAgB"
	    		+ "l7aL1inhaYQVEAZi2WPewOEzRw3qElZktXAfm-AwwXmz1uBrt_PXROlEbcexT13JEalUTcz-Qlmll8j1TsSYiJupeEjbtv6VVdt44C_FtPGW58X0ZrIsY9r0W4se09"
	    		+ "p8F4ISY-siDb4lxhzkyCIW5qauQO7j9ScWJDn_PUHwWpAAAAgC3htge_LIrvMYVyDYu_2Bown4bXysQcECTQhxPo_LQbdsMQ6Zggs0IMpAkPCEDNYFxGNuNA3FGxVP"
	    		+ "ZL_cEm0zxqXjiEMaXFC0BPqh72MoQL0D1ldCCE6q00InyidkCEk8JiBltsNWxD3YQNjQtIkNAkcY0GpoPycegeVQPqQblMAAAAFm91OklCRF8mX0lubm92YXRpb25f"
	    		+ "QlUAAACAVTU05LJ-5WyCRjT_cgRBvoHE2_VJ-kUmGKErnBIk-_OAawYFWFcW8n9uMilPuifWVoaQKZQ7uVhS6aL5RAtNT0fYXMMmLCHiQ0BqO3fhBnHTPuV3G4UAe9"
	    		+ "5VV8VHFgV_EcL-Yr-14Iv1H4SSmTjooSmHLnS_B12A4nNCFZ_-ZEoAAACAa4SJJhwJ39Ihz8B2utv69xO9u8X3dEeFDBjE3MME2WaDIrDP6sawJmckGdnj6Y9ZEo69"
	    		+ "YxVGLx4bDrVFXs2_W05i_KfqCK8P6r7PQdZCL9jiwoxEu7nev3s9US3GjxUU1kkoQfct78J3cyzc1hkuAJ6B0cOMtSWBnZqJV1bccoIAAAAJbzpGSU5DT05TAAAAgA"
	    		+ "GA_CSwbfzq1tMtd4lnSL_OWEHd5PF8or0wfzF170mBwXIQMOgp9rzjm_40nKKaTZujvxRh0qP5vTzk4GhvUNwa3BlBWgNC5RKqdUnyQGqabglhB40icobSmab_D0St"
	    		+ "5WCW_z7xb0EdvyQkz8GbsLvLHIMplnK2CrUsuNtpm_k3AAAAgGVwaxleS34nk9yCRgIgzePtYWK46wuklsv0087YktD0TvFtkC80oN1dY9Y_qN34deon9Og52esnDu"
	    		+ "7w4qBFqaU3QHQf6_1WwHvYk4LFsA_tKeE9Jlc4eyrhZjy8XFTRUO_D9q_PyS1j2Zai92ZgvmzkgAJBuep4C_4tElnrKQ9sAAAABGM6SVQAAACAP0P0HrXrYvVHbJF5"
	    		+ "luByqsWtwe9lwqwnErOxZugmtxynPTz1GMcDRiAQBxv0yuOUcpgYCNBb0a4RIaunmibFOHvvac9szOb_pzzb2TYy6Vi46as0XlwcTH6dXnaoLhblEoe0u0SOVGtbkV"
	    		+ "VmbCFVanRRMg0RfRxUBr9D9DDTdQoAAACAT7BFR4upnib9ToWjQMbognNZdJ4B3yUUT5jhjE6TIuAFOrlPH_qDzPka_oGbt7_BUDRAwBQt4L-05LWfM0LbGYxFu1e2"
	    		+ "KCAVz1LWmIpUE7WGHlqt1KPf58mORG81Vux7cFyFYv_6Q3EByEhindGYzHdjGvnIDpv-sJBQd_doVYk=";
        byte[] personal_key_byte = Base64.getUrlDecoder().decode(personal_key);
//        long startTimeCPABE = System.currentTimeMillis();
        
        CPABE cpabe = new CPABE();
//        byte[][] enc_sym_key = cpabe.CPABE_encrypt(public_parameters, policy, shared_sym_key);
        byte[] public_parameters=Common.suckFile("C:/Users/diego.pedone/Cpabe/workspace/ABE-Proxy/WebContent/WEB-INF/cpabe_keys/pub_10");
     
        byte[] dec_shared_sym_key = cpabe.CPABE_decrypt(public_parameters, personal_key_byte, enc_shared_sym_keyByte);
        byte[] encDataBA = Base64.getUrlDecoder().decode(encData);
        dec_cph.aes = new AES();
		byte[] dec_data = dec_cph.aes.AES_decrypt(encDataBA,dec_shared_sym_key, iv);
		System.out.println(new String(dec_data));
		
		
		//TODO create enc_file 	var myjson={
								//		enc_sym_key_id:jsonResponse.enc_sym_key_id,
								//		storage:jsonResponse.storage,
								//		metadata:jsonResponse.metadata,
								//		nameFile:file.name,
								//		typeFile:file.type,
								//		encryptor:username,
								//		encfile:encryptedFile,
								//		iv:B64uIv
								//	}
		//TODO Store witj KeyStorage Service
		
	}
}
