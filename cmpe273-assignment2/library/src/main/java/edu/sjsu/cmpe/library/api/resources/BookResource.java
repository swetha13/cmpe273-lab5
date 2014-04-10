package edu.sjsu.cmpe.library.api.resources;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.yammer.dropwizard.jersey.params.LongParam;
import com.yammer.metrics.annotation.Timed;

import edu.sjsu.cmpe.library.Stomp.StompClient;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.dto.BookDto;
import edu.sjsu.cmpe.library.dto.BooksDto;
import edu.sjsu.cmpe.library.dto.LinkDto;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

@Path("/v1/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {
    /** bookRepository instance */
    private final BookRepositoryInterface bookRepository;
    private StompClient producer;

    /**
     * BookResource constructor
     * 
     * @param bookRepository
     *            a BookRepository instance
     */
    public BookResource(BookRepositoryInterface bookRepository , StompClient producer) {
	this.bookRepository = bookRepository;
	this.producer = producer;
    }

    @GET
    @Path("/{isbn}")
    @Timed(name = "view-book")
    public BookDto getBookByIsbn(@PathParam("isbn") LongParam isbn) {
	Book book = bookRepository.getBookByISBN(isbn.get());
	BookDto bookResponse = new BookDto(book);
	bookResponse.addLink(new LinkDto("view-book", "/books/" + book.getIsbn(),
		"GET"));
	bookResponse.addLink(new LinkDto("update-book-status", "/books/"
		+ book.getIsbn(), "PUT"));
	// add more links

	return bookResponse;
    }

    @POST
    @Timed(name = "create-book")
    public Response createBook(@Valid Book request) {
	// Store the new book in the BookRepository so that we can retrieve it.
	Book savedBook = bookRepository.saveBook(request);

	String location = "/books/" + savedBook.getIsbn();
	BookDto bookResponse = new BookDto(savedBook);
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));
	bookResponse
	.addLink(new LinkDto("update-book-status", location, "PUT"));

	return Response.status(201).entity(bookResponse).build();
    }

    @GET
    @Path("/")
    @Timed(name = "view-all-books")
    public BooksDto getAllBooks() {
	BooksDto booksResponse = new BooksDto(bookRepository.getAllBooks());
	booksResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return booksResponse;
    }

    @PUT
    @Path("/{isbn}")
    @Timed(name = "update-book-status")
    public Response updateBookStatus(@PathParam("isbn") LongParam isbn,
	    @DefaultValue("available") @QueryParam("status") Status status) {
	Book book = bookRepository.getBookByISBN(isbn.get());
	book.setStatus(status);
	
	if (status.getValue()=="lost"){
		
		try {
			Connection connectToQueue = producer.createConnection();
			long isbnNum = book.getIsbn();
			
			producer.sendMessageToQueue(connectToQueue, isbnNum);
			
			
			producer.closeConnection(connectToQueue);
			
			System.out.println(" Sent the message to the queue , so closing first connection");
			
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	BookDto bookResponse = new BookDto(book);
	String location = "/books/" + book.getIsbn();
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));

	return Response.status(200).entity(bookResponse).build();
    }

    @DELETE
    @Path("/{isbn}")
    @Timed(name = "delete-book")
    public BookDto deleteBook(@PathParam("isbn") LongParam isbn) {
	bookRepository.delete(isbn.get());
	BookDto bookResponse = new BookDto(null);
	bookResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return bookResponse;
    }
    
    
    /*@POST
    @Timed(name = "update-book")
    public Response updateBook(@Valid String queueMessage) throws MalformedURLException {
    	
    	
    	String queueValues[] = queueMessage.split(":");
    	Long isbnValue = Long.parseLong(queueValues[0]);
    	Book book = bookRepository.getBookByISBN(isbnValue);
    	
    	if ( book == null){
    		
    		Book newBook = new Book();
    		newBook.setIsbn(isbnValue);
    		newBook.setTitle(queueValues[1]);
    		newBook.setCategory(queueValues[2]);
    		
    		System.out.println("url value" + queueValues[3]);
    		URL url = new URL(queueValues[3]);
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
    	
    	
	  return Response.status(201).build();
    }

    */
    
    
    
    
    
    
    
    
    
    
}

