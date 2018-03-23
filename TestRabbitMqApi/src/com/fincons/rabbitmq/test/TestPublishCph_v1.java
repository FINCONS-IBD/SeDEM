package com.fincons.rabbitmq.test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fincons.rabbitmq.client.RabbitMqClient;
import com.fincons.rabbitmq.client.factory.RequestFactory;
import com.fincons.rabbitmq.event.Event;
import com.fincons.rabbitmq.publisher.BasicEventFactory;
import com.fincons.rabbitmq.publisher.Publisher;
import com.fincons.util.ApplicationPropertiesRepository;
/**
 * Provide a test case about the use of RabbitMQ Client API Library publish functionalities 
 * 
 * @author Fincons Group AG
 *
 */
public class TestPublishCph_v1 {
	
	public static void main(String[] args) {

		RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.PUBLISH);
		Event ensE;
		if(client.isConnected() && client instanceof Publisher){		
        	Publisher pubApp = (Publisher)client;
        	ensE = createEvent("Voltage", 10, 40);
        	pubApp.publish(ensE, 0);
		}
	}
		
	private static Event createEvent(String topic, int min, int max) {
		StringBuffer payload = new StringBuffer();
		Map<String,Object> headers = new HashMap<String,Object>();

		Random random = new Random();
		float value = min + (random.nextFloat() * (max-min) + 1);
					
		headers.put(topic, value);
		//payload.append(value);

		try {
			return new BasicEventFactory().create(headers, payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
