package edu.sjsu.cmpe.library.Stomp;

import java.net.MalformedURLException;
import java.net.URL;

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
import org.fusesource.stomp.jms.message.StompJmsMessage;

import com.yammer.dropwizard.config.Configuration;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class StompClient {

	private String apolloUser ;
	private String apolloPassword;
	private String apolloHost;
	private String apolloPort;
	private String libraryName;
	private String queueName;
	private String topicName;
	private BookRepositoryInterface bookRepository;

	

	public StompClient(String apolloUser, String apolloPassword,
			String apolloHost, String apolloPort, String libraryName, String queueName, String topicName, BookRepositoryInterface bookRepository) {
		this.apolloUser = apolloUser;
		this.apolloPassword = apolloPassword;
		this.apolloHost = apolloHost;
		this.apolloPort = apolloPort;
		this.libraryName = libraryName;
		this.queueName = queueName;
		this.topicName = topicName;
		this.bookRepository= bookRepository;
	}
	

	public StompClient(LibraryServiceConfiguration config , BookRepositoryInterface bookrepo) {
		this.apolloUser = config.getapolloUser();
		this.apolloPassword = config.getApolloPassword();
		this.apolloHost = config.getApolloHost();
		this.apolloPort = config.getApolloPort();
		this.libraryName = config.getLibraryName();
		this.queueName = config.getStompQueueName();
		this.topicName = config.getStompTopicName();
		this.bookRepository = bookrepo;
	}


	public Connection createConnection() throws JMSException{
		StompJmsConnectionFactory factory= new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
		Connection connection = factory.createConnection(apolloUser, apolloPassword);
		return connection;

	}


	public void sendMessageToQueue(Connection connection , long isbn) throws JMSException{
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = new StompJmsDestination(queueName);
		MessageProducer producer = session.createProducer(destination);
		//producer.setTimeToLive(1000);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		String msgToQueue = libraryName + ":" + isbn;
		TextMessage message = session.createTextMessage(msgToQueue);
		message.setLongProperty("id", System.currentTimeMillis());

		producer.send(message);
		producer.send(session.createTextMessage("SHUTDOWN"));


	}

	public void  closeConnection(Connection connection) throws JMSException {
		connection.close();

	}

	public void subscribeToTopic(Connection connection) throws JMSException, MalformedURLException {
		connection.start();
		// bookReuqest = new BookRequest();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = new StompJmsDestination(topicName);
		MessageConsumer consumer = session.createConsumer(dest);
		
		
		while(true) {

			/**Wait for message for 5 sec*/
			Message msg = consumer.receive();
			if( msg instanceof  TextMessage ) {
				String body = ((TextMessage) msg).getText();
				System.out.println("Received message = " + body);
				updateLibrary(body);
				
				if( "SHUTDOWN".equals(body)) {
					break;
				}
			}
				else if (msg instanceof StompJmsMessage) {
					StompJmsMessage smsg = ((StompJmsMessage) msg);
					String body = smsg.getFrame().contentAsString();
					if ("SHUTDOWN".equals(body)) {
						break;
					}
					System.out.println("Received message = " + body);

				} else {
					System.out.println("Unexpected message type: "+msg.getClass());
					break;
					
				}
			}

		}
	
	
	public void updateLibrary( String queueMessage) throws MalformedURLException{
		
		
		String queueValues[] = queueMessage.split(":");
    	Long isbnValue = Long.parseLong(queueValues[0]);
    	System.out.println("isbn value "+ isbnValue);
    	if(bookRepository == null){
    		System.out.println("book repo is null");
    	}
    	
    	Book book = bookRepository.getBookByISBN(isbnValue);
    	
    	if ( book == null){
    		
    		Book newBook = new Book();
    		newBook.setIsbn(isbnValue);
    		System.out.println("isbn value" + newBook.getIsbn());
    		newBook.setTitle(queueValues[1]);
    		newBook.setCategory(queueValues[2]);

    		
    		String urlname = queueValues[3] + ":" + queueValues[4];
    		
    		System.out.println("url value" + urlname);
    		URL url = new URL(urlname);
    		newBook.setCoverimage(url);
    		
    		bookRepository.saveBook(newBook);
    		
    		System.out.println(" Updated new book with isbn " + newBook.getIsbn() +" to the repository");
    		
    	}
    	else if ( book.getStatus()==Status.lost){
    		System.out.println("Updating the status of book " + book.getIsbn() +"to available");
    		book.setStatus(Status.available);
    		
    	}
    	else{
    		System.out.println("Book " + book.getIsbn() +" is already there in the repository");
    	}
    	
    		
	}
	
	
}
