package edu.sjsu.cmpe.library.Stomp;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.fusesource.stomp.jms.StompJmsDestination;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class StompListener implements Runnable {
	
	
	private final LibraryServiceConfiguration config;
	private final BookRepositoryInterface bookRepository;
	
	public StompListener(LibraryServiceConfiguration config , BookRepositoryInterface bookRepository) {
		this.config = config;
		this.bookRepository = bookRepository;
		
	}

	private void listen() throws JMSException, MalformedURLException{
		
		

		StompClient stomp = new StompClient(config ,bookRepository);
		Connection connection = stomp.createConnection();
		stomp.subscribeToTopic(connection);

	}

	@Override
	public void run() {
		
		System.out.println("Hello World");
		try {
			listen();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




}
