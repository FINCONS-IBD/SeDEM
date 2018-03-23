package com.fincons.rabbitmq.test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

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
public class TestPublishCph_v2 {	
    private static PublishingRunnable publishingRunnable;
    private static Thread publishingThread;	
	
	public static void main(String[] args) {

		RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.PUBLISH);
		
		if(client.isConnected() && client instanceof Publisher){
            publishingRunnable = new PublishingRunnable((Publisher)client);
            publishingThread = new Thread(publishingRunnable);       
            if(client.isConnected())
            	publishingThread.start();			
		}	
	}
	
	private static class PublishingRunnable implements Runnable{
		/* Time between each publication */
		private static final long DELAY = Long.parseLong(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(
														RequestFactory.ExtraSettings4Publisher.DELAY));
		
		/* Number of publications */
		private static final long PX_DATA = Long.parseLong(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(
														RequestFactory.ExtraSettings4Publisher.PX_DATA));
		
		int min_random = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(RequestFactory.ExtraSettings4Publisher.MIN_RANDOM));
		int max_random = new Integer(ApplicationPropertiesRepository.APPLICATION_PROPERTIES.getProperty(RequestFactory.ExtraSettings4Publisher.MAX_RANDOM));

		
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
			Event ensE;
			try {
				while(publisher.isConnected() && num_publication < PX_DATA) {
					
					ensE = createEvent(min_random, max_random);
					
//					com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//					operatingSystemMXBean.getProcessCpuLoad();
//					long startTime = System.currentTimeMillis();
					
					publisher.publish(ensE, num_publication);
					num_publication++;
					
//					long endTime   = System.currentTimeMillis();
//					Runtime runtime = Runtime.getRuntime();
//					runtime.gc();
//					long memory = runtime.totalMemory() - runtime.freeMemory();
//					long totalTime = endTime - startTime;
//					
//					File TextFile = new File("results_pub"); 
//					FileWriter TextOut = new FileWriter(TextFile, true);
//					TextOut.write(memory/1024L + ",");
//					TextOut.write(operatingSystemMXBean.getProcessCpuLoad() + ",");
//					TextOut.write(totalTime + "\n");
//					TextOut.close();
//					
//					System.out.println("\n\n#### Publication: " + num_publication);
//	                System.out.println("#### TOTAL MEMORY (kilobytes): " + memory/1024L);
//					System.out.println("#### TOTAL CPU (%): " + operatingSystemMXBean.getProcessCpuLoad());
//					System.out.println("#### TOTAL TIME: " + totalTime + " ms.");
					
					Thread.sleep(DELAY);
				}
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
