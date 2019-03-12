


# RabbitMQ Client API  

## A Java Library to perfrom standard AMQP operation on a RabbitMQ Server  

</center>

This library provide Java classes to perfrom submit and subscribe AMQP operation, modelling an Event object and provide an Event creation method, implements an event callback listener. It makes it easy all operations on a RabbitMQ Broker Server.

### Initial Configuration

To permit a dynamic configuration of Logger and Application, the configuration files are external to the application jar. Then is important use the following VM Arguments when run the main application (or in a .bat / .sh run file or into VM Arguments into development IDE Environment).  

_-Drabbitmq_config_file=C:\resources\conf.properties -Dlog4j.configuration=file:C:\resources\log4j.properties_  

**NOTE:** if the library will be installed on a Linux O.S. is important use a Linux well formed paths  

#### Application configuration

The first step before using the library is edit the configuration parameters detailed below. The file named _conf.properties_ is located in the external resource folder of the library (see the "Initial Configuration" introduction section).  
Follow an example of configuration file:  

	client.subjectID=username  
	client.accessToken=changeit  
	client.portNumber=5273  
	client.mgmPortNumber=15673  
	client.host=172.25.13.206  
	client.vhost=Raw_Events_Broker_Service  
	client.destinationName=task_queue  
	client.exchange=exchange_name  
	client.queue=queue_name  
	client.tls=false  
	client.pattern=namespaceName.resourceName  
##### EXTRA PUBLISHING SETTINGS #####  
	maxRnd=20  
	minRnd=10  
	#in ms  
	delay=1000  
Details about parameters:

*   _client.subjectID_ - username authorised to connect at RabbitMQ Server endpoint
*   _client.accessToken_ - the password to perform the authentication on RabbitMQ Server
*   _client.portNumber_ - the port number where RabbitMQ Service are listened
*   _client.mgmPortNumber_ - the port number of RabbitMQ Management Console
*   _client.host_ - the ip address of the RabbitMQ Server
*   _client.vhost_ - the RabbitMQ Virtual Host name
*   _client.destinationName_ - the exchange/queue name used by publisher/subscriber. This parameter is used when the client.exchange or client.queue isn't present
*   _client.exchange_ - the exchange name used by publisher. If this paremeter isn't present the client use the value of destinationName
*   _client.queue_ - the queue name used by subscriber. If this paremeter isn't present the client use the value of destinationName
*   _client.tls_ - flag to specify if the RabbitMQ connection must use a TLS channel
*   _client.pattern_ - a specific string (usually the namespace path) to use as routingKey
*   Other extra settings

**NOTE:** it is crucial that all the resources just mentioned (virtual host, exchange, queue, bind exchange-queue, etc.) is already created instance of RabbitMQ specified, by their system administrator.

#### Logging configuration

In the external resource folder of the library (see the "Initial Configuration" introduction section) is present a logging configuration file named _log4j.properties_. It is possible edit this configuration file to change the file location and dimension, the logging level of the logger. Following an example of the config logging file:  

	# Root logger option  
	log4j.rootLogger=INFO, stdout, file  
# Redirect log messages to console  
	log4j.appender.stdout=org.apache.log4j.ConsoleAppender  
	log4j.appender.stdout.Target=System.out  
	log4j.appender.stdout.layout=org.apache.log4j.PatternLayout  
	log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n  
# Redirect log messages to a log file, support file rolling.  
	log4j.appender.file=org.apache.log4j.RollingFileAppender  
	log4j.appender.file.File=C\:\\rabbitmq_client.log  
	#log4j.appender.file.File=/opt/logs/rabbitmq_client.log  
	log4j.appender.file.MaxFileSize=5MB  
	log4j.appender.file.MaxBackupIndex=10  
	log4j.appender.file.layout=org.apache.log4j.PatternLayout  
	log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n  
` 

> **NOTE:** if the library will be installed on a Linux O.S. is important change the path of log file (see the commented line)

### Library dependencies

The library has 2 dependencies:

*   log4j, a Java Logging Library (log4j-1.2.17.jar)
*   AMQP Client, to interact with RabbitMQ Server (amqp-client-3.6.0.jar)

Then, with the source project, in the libs folder, will be provided the two jars mentioned, and the application that use the library must import also this two jars.

### An example of use

Following some example of Library usage.

#### Publish

Use the RequestFactory class to retrieve a client instance of Publisher passing a operation of type 'PUBLISH'. To test periodical publishing functionality you can created a thread with 1 second of time sleeping.  

	RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.PUBLISH);  

	if(client.isConnected() && client instanceof Publisher){  
		publishingRunnable = new PublishingRunnable((Publisher)client, "publish");  
		publishingThread = new Thread(publishingRunnable);  

	if(client.isConnected())  
		publishingThread.start();  

	}  

A snipped code as example of thread run() method  

	Event ensE;  
	try {  
		stop = false;  
		while (!stop && pubApp.isConnected()) {  
			ensE = createEvent();  
			pubApp.publish(ensE);  
			Thread.sleep(DELAY);  
			System.out.println("Event published: " + ensE.toString());  
		}  
	} catch [...]

#### Event creation

The following lines code represent an example of Event creation with a random value generator:  

	private Event createEvent () {  
		StringBuffer payload = new StringBuffer();  
		Map <string,object>headers = new HashMap<string,object>();  

		float value = getRandomFloat();  
		headers.put("Power", value + "");  
		payload.append(value);  

		value = getRandomFloat();  
		headers.put("Voltage", value + "");  
		payload.append(", ");  
		payload.append(value);  

		value = getRandomFloat();  
		headers.put("Current", value + "");  
		payload.append(", ");  
		payload.append(value);  

		try {  
			return new BasicEventFactory().create(  
			headers,payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);  
		} catch (UnsupportedEncodingException e) {  
			e.printStackTrace();  
			throw new RuntimeException(e);  
		}  

	}

#### Subscribe

Look like the publisher client, to retrieve a Subscriber client instance you can use the RequestFactory class passing the 'SUBSCRIBE' operation type.  

	RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.SUBSCRIBE);  

	Subscriber sub;  
	if(client.isConnected() && client instanceof Subscriber){  
		sub = (Subscriber) client;  
		sub.registerEventListener( new SubscriberListener() );  
		sub.subscribe();  
	}

#### Event listener

A Subscriber client permits a callback function registration through the registerEventListener() method. An Event Listener must implement the EventListener interface and the onEvent() method. The following example of onEvent() method, in this case simple print the event as a toString function:  

	public void onEvent(Event event) {  
		i++;  
		StringBuilder msg = new StringBuilder();  
		msg.append("-----------------------------------------------------\n");  
		msg.append("Message #" + i + "\n");  
		msg.append("PublisherID:: " + event.getApplicationID() + "\n");  
		msg.append("Timestamp:: " + dateFormatter.format(event.getTimestamp()) + "\n");  
		msg.append("Namespace pattern:: " + event.getPattern() + "\n");  
		msg.append("Persistent? " + event.isPersistent() + "\n");  
		msg.append("Payload media type:: " + event.getContentType() + "\n");  
		msg.append("Payload encoding:: " + event.getContentEncoding() + "\n");  
		byte[] payload = event.getPayload();  
		if (payload == null)  
			msg.append("No payload");  
		else if (payload.length == 0)  
			msg.append("Empty payload");  
		else if (event.getContentType().startsWith("text")) {  
			try {  
				String body = new String(payload, event.getContentEncoding());  
				msg.append("Payload:: " + body + "\n");  
			} catch (UnsupportedEncodingException e) {  
				e.printStackTrace();  
			}  
		} else  
			msg.append("Payload size:: " + payload.length + " bytes");  
		Map <string,object>headers = event.getHeaders();  
		if (headers == null) {  
		msg.append("No headers");  
		} else {  
			msg.append("Headers:");  
			for (String key: headers.keySet()) {  
				msg.append("\t\n");  
				msg.append(key);  
				msg.append(": ");  
				msg.append(headers.get(key));  
			}  
		}  
		msg.append("\n-----------------------------------------------------\n");  

		System.out.println(msg.toString());  
	}

