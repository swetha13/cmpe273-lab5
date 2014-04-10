package edu.sjsu.cmpe.library;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnection;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.Stomp.StompClient;
import edu.sjsu.cmpe.library.Stomp.StompListener;
import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;



public class LibraryService extends Service<LibraryServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    

    public static void main(String[] args) throws Exception {
	
	
	
	new LibraryService().run(args);
	
    }

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	bootstrap.addBundle(new ViewBundle());
	bootstrap.addBundle(new AssetsBundle());
    }

    @Override
    public void run(LibraryServiceConfiguration configuration,
	    Environment environment) throws Exception {
	// This is how you pull the configurations from library_x_config.yml
	String queueName = configuration.getStompQueueName();
	String topicName = configuration.getStompTopicName();
	log.debug("{} - Queue name is {}. Topic name is {}",
		configuration.getLibraryName(), queueName,
		topicName);
	// TODO: Apollo STOMP Broker URL and login

	String user = configuration.getapolloUser();
	String password = configuration.getApolloPassword();
	String host = configuration.getApolloHost();
	String port = configuration.getApolloPort();
	String libraryName = configuration.getLibraryName();
	
	log.debug(log+"\nApollo User: "+user+"\nApollo Password: "+password+"\nApollo Host: "+
			host+"\nApollo Port: "+port);
	
	
	/** Root API */
	environment.addResource(RootResource.class);
	/** Books APIs */
	BookRepositoryInterface bookRepository = new BookRepository();
	
	
	
	StompClient stomp = new StompClient(user, password, host, port, libraryName, queueName, topicName, bookRepository);
	
	environment.addResource(new BookResource(bookRepository,stomp));
	//environment.
	/** UI Resources */
	
	
	
	environment.addResource(new HomeResource(bookRepository));
	
	
	int numThreads = 1;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    
    StompListener backgroundTask = new StompListener(configuration ,bookRepository );
    
  	System.out.println("About to submit the background task");
	executor.execute(backgroundTask);
	System.out.println("Submitted the background task");

	executor.shutdown();
	System.out.println("Finished the background task");
	
	
	
	
    }
    
    
    
}
