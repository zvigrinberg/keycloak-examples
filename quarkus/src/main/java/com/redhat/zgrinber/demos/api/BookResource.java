package com.redhat.zgrinber.demos.api;

import com.redhat.zgrinber.demos.model.BookModel;
import com.redhat.zgrinber.demos.services.BooksService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;

@ApplicationScoped
@Path("/api/books")
@Authenticated
public class BookResource {
    private static final Logger LOG = Logger.getLogger(BookResource.class);
    @Inject
    SecurityIdentity identity;
    @Inject
    @Named("InMemoryBooksService")
    protected BooksService getBooksService;

    @GET
//    @RolesAllowed(value = "write_book")
    public List<BookModel> getAll() {
        return getBooksService.getAllBooks();
    }
    @GET
    @Path("/{id}")
//    @RolesAllowed(value = {"write_book", "read_book"})
    public BookModel getOne(@PathParam("id") String id) {
        return getBooksService.getBook(id);
    }
    @POST
//    @RolesAllowed(value = "write_book")
    public Response CreateOne(@RequestBody @Valid BookModel book) {
            Response result;
            try
            {
              getBooksService.CreateBook(book);
                result = Response.created(URI.create("/api/books/" + book.getId())).build();
            }
            catch (Exception e)
            {
                LOG.errorf("Error Creating book, error message: %s",e.getMessage());
                result = Response.serverError().entity("Error creating the book, Check the logs or reach out API's Administrator.").build();
            }
            return result;
    }

}
