package edu.sjsu.cmpe.procurement.domain;

import java.util.ArrayList;
import java.util.List;

public class ShippedBook {

		private List<Book> shipped_books = new ArrayList<Book>();
		
		public List<Book> getshipped_books(){
			
			return shipped_books;
		}
	
		public void setshipped_books(List<Book> shipped_books){
			this.shipped_books = shipped_books;
		}

}
