package com.redhat.zgrinber.demos.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.util.Date;
import java.util.Objects;

public class BookModel {
    private static final String supportedGenres = "Romance|Science|Popular Science|Religion|Fashion|Thriller|Adventures|Cooking|Comedy|Horror";
    public static final String regexPatternForAuthor = "[a-zA-Z]+[a-zA-Z0-9 -.]*[a-zA-Z0-9]";
    @NotBlank
    private String id;
    @NotBlank
    private String name;
    @Pattern(regexp = supportedGenres,message = "Unsupported genre specified, must be one of : " + supportedGenres)
    private String genre;
    @Min(message="Number of pages in the book must be positive",value=1)
    private Integer numOfPages;
    @NotBlank
    @Pattern(regexp = regexPatternForAuthor, message = "Must adhere to regex pattern=" + regexPatternForAuthor +" that is, author name must start with at least one letter, then contain any sequence of letters and digits, dashes and points, and must be terminated with one decimal digit or one english letter")
    private String authorName;
    @Min(value=1, message="Price of book must be positive, there are no free books!")
    @Max(value=1000,message = "Prices Over 1000 are exaggerated and not acceptable!")
    private Integer price;


    @Past(message = "Publishing Date Must be in the past")
    @JsonFormat(shape = JsonFormat.Shape.STRING , pattern = "yyyy-MM-dd")
    private Date publishingDate;

    public BookModel(String id, String name, String genre, Integer numOfPages, String authorName, Integer price,Date publishingDate) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.numOfPages = numOfPages;
        this.authorName = authorName;
        this.price = price;
        this.publishingDate = publishingDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookModel bookModel = (BookModel) o;
        return Objects.equals(id, bookModel.id) && Objects.equals(name, bookModel.name) && Objects.equals(genre, bookModel.genre) && Objects.equals(numOfPages, bookModel.numOfPages) && Objects.equals(authorName, bookModel.authorName) && Objects.equals(price, bookModel.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, genre, numOfPages, authorName, price);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getNumOfPages() {
        return numOfPages;
    }

    public void setNumOfPages(Integer numOfPages) {
        this.numOfPages = numOfPages;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Date getPublishingDate() {
        return publishingDate;
    }

    public void setPublishingDate(Date publishingDate) {
        this.publishingDate = publishingDate;
    }

}
