package com.fincons.rabbitmq.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

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
public class TestPublishCph_v4 {	
    private static PublishingRunnable publishingRunnable;
    private static Thread publishingThread;
    private static ArrayList<Event> e;
    
    /* Time between each publication */
	private static final long DELAY = Long.parseLong(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(
													RequestFactory.ExtraSettings4Publisher.DELAY));
	
	public static void main(String[] args) {

		RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.PUBLISH);
		
		TimerTask t = new New_Event();
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(t, 0, DELAY);
		
		if(client.isConnected() && client instanceof Publisher){
			e = new ArrayList<Event>();
            publishingRunnable = new PublishingRunnable((Publisher)client);
            publishingThread = new Thread(publishingRunnable);       
            if(client.isConnected())
            	publishingThread.start();			
		}	
	}
	
	
	private static class New_Event extends TimerTask{

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			task();
		}
		
		private void task(){
			Event ensE;
			ensE = createEvent(10, 40);
			e.add(ensE);
		}
	}
	
	
	private static class PublishingRunnable implements Runnable{
		/* Number of publications */
		private static final long PX_DATA = Long.parseLong(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(
														RequestFactory.ExtraSettings4Publisher.PX_DATA));
		
		private long num_publication = 0;

		private Publisher publisher;

		public PublishingRunnable (Publisher pubApp) {
			this.publisher = pubApp;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				long startTime = System.currentTimeMillis();
				while(publisher.isConnected() && num_publication < PX_DATA) {
					Event event = getEvent();
					if(event != null){
						publisher.publish(event, num_publication);
						num_publication++;
					}
				}
				long endTime   = System.currentTimeMillis();
				long totalTime = endTime - startTime;
				try {
					File TextFile = new File("time_full_pub.csv"); 
					FileWriter TextOut = new FileWriter(TextFile, true);
					TextOut.write(totalTime + "\n");
					TextOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private synchronized Event getEvent(){
			Event event = null;
			if(!e.isEmpty()){
				event = e.get(0);
				e.remove(event);
			}
			return event;
		}
	}
	
	private static Event createEvent(int min, int max) {
		StringBuffer payload = new StringBuffer();
		Map<String,Object> headers = new HashMap<String,Object>();
		
		Random random = new Random();
		float value = min + (random.nextFloat() * (max-min) + 1);
					
		//headers.put(topic, value);
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		headers.put("timestamp", System.currentTimeMillis());
		
		payload.append(value);

		try {
			return new BasicEventFactory().create(headers, payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
