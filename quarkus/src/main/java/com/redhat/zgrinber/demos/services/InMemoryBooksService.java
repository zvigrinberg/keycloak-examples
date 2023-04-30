package com.redhat.zgrinber.demos.services;

import com.redhat.zgrinber.demos.model.BookModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//favour @ApplicationScoped over @Singleton, So the Service bean will be created lazily and not eagerly.
@ApplicationScoped
@Named("InMemoryBooksService")
public class InMemoryBooksService implements BooksService {
    private final Map<String,BookModel> booksDatabase = new HashMap<>();
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
