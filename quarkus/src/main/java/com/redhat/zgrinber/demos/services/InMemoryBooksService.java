package com.redhat.zgrinber.demos.services;

import com.redhat.zgrinber.demos.model.BookModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.time.Instant;
import java.util.*;

//favour @ApplicationScoped over @Singleton, So the Service bean will be created lazily and not eagerly.
@ApplicationScoped
@Named("InMemoryBooksService")
public class InMemoryBooksService implements BooksService {
    private final static Map<String,BookModel> booksDatabase;

    static
    {
        booksDatabase = new HashMap<>();
        booksDatabase.put("test",new BookModel("test","test-book","Comedy",250,"John Doe",60,new Date(84,0,4)));
    }
    @Override
    public BookModel getBook(String bookId) {

        return booksDatabase.get(bookId);
    }

    @Override
    public void CreateBook(BookModel book) {
        booksDatabase.put(book.getId(),book);
    }

    @Override
    public List<BookModel> getAllBooks()
    {
        List<BookModel> result = new ArrayList();
        booksDatabase.forEach((bookId, bookModel) ->  result.add(bookModel));
        return result;
    }
}
