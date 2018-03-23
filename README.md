# SeDEM

The Secure Data Exchange Middleware (SeDEM) is a JAVA library implementing a new approach for the secure exchange of events based on RabbitMQ. 
It provides Java classes to ease publish and subscribe operations through AMQP protocol, modelling an event-type object and making available a method for events creation at producer side, as well as an event listener to be used to manage arrival and processing of events at the consumer side, definitely making simple the interaction with RabbitMQ Broker Server made available by FINCONS. The security mechanism are provided by the CPABE Service detailed in the previous chapter and in particular by the ABE Proxy componentscomponents. 

## Summary of Functionalities

The SeDEM library provides the following functionalities:

* *Event Creation*: the library enables the creation of the Event dispatched to the RabbitMQ Server. An Event is made up by the following parts:
	* headers: a set of (key, value) pairs the publisher can use to indicate some data useful to enable subscribers to correctly understand the payload of the event and/or to put the informative content of the event;
	* payload: an opaque list of bytes that represents the informative content of the event. NOTE: the informative content carried by an event may be put in the headers hence the message payload may be empty.
* *Publishing*: provides the capability to send the generated Event to the RabbitMQ Server in a secure way.
* *Subscribing*: enables the creation of a consumer for a specific set of events dispatched by another client of the same RabbitMQ Server instance. The actual processing of the events is performed by registering an event listener that is invoked every time a new event has been received by the subscriber.

>NOTE: the **SeDEM** library is represented by *rabbitmq-client-api* folder. The other folders contain the components (except for *TestRabbitMqApi*) from which the SeDEM library depends. 
The *TestRabbitMqApi* project provide a test environment to show how to use the SeDEM library; in this project you can find also an example of configuration files. 
For more details about the library configuration, refers to [JavaDoc](rabbitmq-api-client/javadoc) folder.
