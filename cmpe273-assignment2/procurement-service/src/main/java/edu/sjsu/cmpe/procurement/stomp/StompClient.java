package edu.sjsu.cmpe.procurement.stomp;

import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.procurement.domain.Book;
import edu.sjsu.cmpe.procurement.domain.BookOrder;
import edu.sjsu.cmpe.procurement.domain.ShippedBook;



public class StompClient {

	ProcurementServiceConfiguration configuration = new ProcurementServiceConfiguration();
	String apolloUser = "admin";
	String apolloPassword = "password";
	String apolloHost = "54.215.133.131";
	String apolloPort = "61613";

	private String isbn;

	String queueName = "/queue/78201.book.orders";
	String topicName = "/topic/78201.book.";

	private static final String SHUTDOWN = "SHUTDOWN";

	public StompClient(){

		
	}


	public Connection createConnection() throws JMSException{
		StompJmsConnectionFactory factory= new StompJmsConnectionFactory();
		//System.out.println("apollo host" + configuration.getApolloHost());
		//System.out.println(" Creating connection");
		//System.out.println("Host"+apolloHost);
		factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
		Connection connection = factory.createConnection(apolloUser, apolloPassword);
		return connection;

	}


	public void closeConnection(Connection connection) throws JMSException{

		connection.close();
	}


	public BookOrder  receiveMessageFromQueue(Connection connection) throws JMSException{

		connection.start();
		// bookReuqest = new BookRequest();
		BookOrder requestOrder = new BookOrder();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		//String nameOfQueue = "/queue/78201.book.orders"
		Destination dest = new StompJmsDestination(queueName);
		MessageConsumer consumer = session.createConsumer(dest);

		while(true) {

			/**Wait for message for 1 minute*/
			Message msg = consumer.receive(5000);

			// may be I don't have a message which is ok
			if (msg == null)
				continue;
			else if( msg instanceof TextMessage ) {
				String body = ((TextMessage) msg).getText();
				if (SHUTDOWN.equalsIgnoreCase(body))
					break;
				else {
					// add the book to the requestOrder
					String split[] = body.split(":");
					isbn = split[1];
					System.out.println(" isbn number from substring"+isbn);
					requestOrder.get_order_book_isbns().add(Integer.parseInt(isbn));
					System.out.println("Received message = " + body);

				} 


			}
//			else if (msg instanceof StompJmsMessage) {
//				StompJmsMessage smsg = ((StompJmsMessage) msg);
//				String body = smsg.getFrame().contentAsString();
//				if ("SHUTDOWN".equals(body)) {
//					break;
//				}
//				System.out.println("Received message = " + body);
//
//			} else {
//				System.out.println("Unexpected message type: "+msg.getClass());
//				break;
//			}
		}

		return requestOrder;

	}


	public void publishBooksToTopic( Connection connection,ShippedBook responseShippedBook) throws JMSException{


		for(Book book : responseShippedBook.getshipped_books()){

			String message = book.getIsbn() + ":" +book.getTitle() + ":" +book.getCategory() + ":" +book.getCoverimage() ;
			System.out.println("message " + message);


			// Sending msg to topic with all categories
			String allCategoriesTopic = topicName  + "all";
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			StompJmsDestination dest = new StompJmsDestination(allCategoriesTopic);
			//dest.setPrefix(allCategoriesTopic);
			

			MessageProducer producer = session.createProducer(dest);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			

			TextMessage msg = session.createTextMessage(message);
			msg.setLongProperty("id", System.currentTimeMillis());
			producer.send(msg);
			
			//Sending msg to particular category

			String categoryTopic = topicName  + book.getCategory();
			StompJmsDestination dest2 = new StompJmsDestination(categoryTopic);
			
			

			MessageProducer producer2 = session.createProducer(dest2);
			producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			producer2.send(msg);

		}

	}

}


