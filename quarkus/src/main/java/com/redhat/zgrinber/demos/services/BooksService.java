package com.redhat.zgrinber.demos.services;

import com.redhat.zgrinber.demos.model.BookModel;

import java.util.List;

public interface BooksService {
    BookModel getBook(String bookId);

    void CreateBook(BookModel book);


    List<BookModel> getAllBooks();
}
