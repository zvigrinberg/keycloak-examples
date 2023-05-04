# Quarkus Keycloak demo

## Goal 

To Show how to use Quarkus keycloak Extension in order to protect REST API Resources in Quarkus and manage the access control to it using KeyCloak as an Authentication And Authorization Server.
We'll leverage quarkus Dev Service (and running the quarkus application in DEV Mode off course) to use its automatically provisioned keycloak instance in a container. 




This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./auth-scopes.png compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

**A Keycloak instance was provisioned and its details available at http://localhost:8080/q/dev-ui/dev-services**

## KeyCloak Server Configuration.

1. Enter the keycloak instance written in `keycloak.url` property  and concatenate the `/admin` url path(e.g http://localhost:36271/admin), and authenticate using the default admin/admin credentials.
2. Switch To `Quarkus` Realm
3. Click on `Clients` menu on the left panel
4. Choose "quarkus-app" Client ID and enter it.

### Client Capability Config
Go to `Settings` tab, and go to `Capability config`
```properties
Client authentication= On
Authorization= On
```
- [X] Standard flow
- [X] Direct access grants
- [X] Implicit flow
- [ ] OAuth 2.0 Device Authorization Grant 
- [ ] OIDC CIBA Grant

Finally Click on `save Button`
### E

### Creating Client Roles
1. Go to `Roles` Tab
2. Click on `Create role` Button
3. Type in Role name field read_book -> Click on `Save` Button 
4. Repeat Steps 2-3 for write_book role.

### Creating Users

1. On the left panel, go to users, and click on `Add User` Button.
2. Write down in username field "bookreader" and click on create button, then go to `Credentials` tab -> Click on "Set password" button" --> fill password and password confirmation, and toggle off Temporary field--> Click on save.
3. Go to `Role mapping` tab, and click on `Assign role` Button -> Filter by clients -> Choose read_book `role` of "quarkus-app" `Client` --> click on assign.
4. Click again on Users left menu -> click on `Add User` Button.
5. Write down in username field "bookadmin" and click on create button, then go to `Credentials` tab -> Click on "Set password" button" --> fill password and password confirmation, and toggle off Temporary field--> Click on save.
6. Go to `Role mapping` tab, and click on `Assign role` Button -> Filter by clients -> Choose write_book `role` of "quarkus-app" `Client` --> click on assign.

### Authorization Settings
We're going to protect `/api/books` RESTFUL API resource which we defined in this quarkus application.
Go to Clients menu -> Choose "quarkus-app" Client" -> Go to `Authorization` Tab.
On `Settings` sub-tab (Under Authorization tab), choose the following:


Policy enforcement mode  - 
<label for="Enforcing">
  <input type="radio" id="Enforcing" name="Enforcing" value="yes" checked />
Enforcing
</label>
<label for="Permissive">
  <input type="radio" id="Permissive" name="Permissive" value="yes" unchecked />
Permissive
</label>
<label for="Disabled">
  <input type="radio" id="Disabled" name="Disabled" value="yes" unchecked />
Disabled
</label>


Decision Strategy -
<label for="Unanimous">
  <input type="radio" id="Unanimous" name="Unanimous" value="yes" unchecked />
Unanimous
</label>
<label for="Affirmative">
  <input type="radio" id="Affirmative" name="Affirmative" value="yes" checked />
Affirmative
</label>

Remote Resource Management= On

#### Define Authorization Scopes
1. Go to Client --> Choose `Authorization` tab, and go to `Scopes` sub-tab
2. Click on button `Create Authorization Scope`
3. fill in the following:
```properties
Name=read
DisplayName=read
Icon URI=
```
4. Click on Save Button.
5. Repeat 1-4 in order to create "write" Authorization Scope.


You should see 2 Authorization Scopes:

![sample](./resources/pictures/auth-scopes.png)
#### Define protected Resource
1. Go to Client --> Choose `Authorization` tab, and go to `Resources` sub-tab
2. Click on button `Create resource`
3. Fill the details according to the next screenshot, and Click on Save

![img.png](./resources/pictures/protect-resource.png)

#### Define Policies
1. Go to Client --> Choose `Authorization` tab, and go to `Policies` sub-tab
2. Click on button `Create policy
3. Choose Policy type With Name = Role.
4. Fill in the details like the following, to create read_book policy, and click on save at the end:

![img.png](./resources/pictures/policy-read.png)

5. Repeat Steps 1-4 for creating write_book policy, you should fill the following values (and click on save at the end):

![img.png](./resources/pictures/policy-write.png)

#### Define Permissions
1. Go to Client --> Choose `Authorization` tab, and go to `Permissions` sub-tab
2. Click on button `Create Permission`, and in the Drop down list, choose "Create scope-based Permission":
3. Create Permission "read_book_permission" with the following details, and click save :

![img.png](./resources/pictures/read_book_permission.png)

4. Repeat 1-3 for another permission "write_book_permission" with the following details:

![img.png](./resources/pictures/write_book_permission.png)


### Quarkus Extension Configuration + Code

__application.properties:__

```properties
quarkus.keycloak.policy-enforcer.enable=true

# Enables mapping http methods to Scopes, that way we can distinguish between
# different methods/operation on the same rest resource 
# and can adhere to REST API best practices
quarkus.keycloak.policy-enforcer.http-method-as-scope=true

quarkus.keycloak.policy-enforcer.paths.1.name=read_book_permission
quarkus.keycloak.policy-enforcer.paths.1.path=/api/books
quarkus.keycloak.policy-enforcer.paths.1.enforcement-mode=ENFORCING
quarkus.keycloak.policy-enforcer.paths.1.methods.get.method=GET
quarkus.keycloak.policy-enforcer.paths.1.methods.get.scopes=read
#
#
quarkus.keycloak.policy-enforcer.paths.2.name=write_book_permission
quarkus.keycloak.policy-enforcer.paths.2.path=/api/books
quarkus.keycloak.policy-enforcer.paths.2.enforcement-mode=ENFORCING
quarkus.keycloak.policy-enforcer.paths.2.methods.post.method=POST
quarkus.keycloak.policy-enforcer.paths.2.methods.post.scopes=write
```

bookResource.java
```java
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
    public List<BookModel> getAll() {
        return getBooksService.getAllBooks();
    }
    @GET
    @Path("/{id}")
//    Don't need @RolesAllowed annotation as Keycloak manages it and the extension getting its decision based on permission and its policies on the resource.  
//    @RolesAllowed(value = {"write_book", "read_book"})
    public BookModel getOne(@PathParam("id") String id) {
        return getBooksService.getBook(id);
    }
    @POST
    public Response CreateOne(@RequestBody @Valid BookModel book) {
            Response result;
            try
            {
              getBooksService.CreateBook(book);
                result = Response.created(URI.create("localhost")).build();
            }
            catch (Exception e)
            {
                LOG.errorf("Error Creating book, error message: %s",e.getMessage());
                result = Response.serverError().entity("Error creating the book, Check the logs or reach out API's Administrator.").build();
            }
            return result;
    }

}
```

### Test The Application

#### Trying to consume 

## Packaging and running the application

The application can be packaged using:
```shell script
./auth-scopes.png package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./auth-scopes.png package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./auth-scopes.png package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./auth-scopes.png package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- SmallRye OpenTracing ([guide](https://quarkus.io/guides/opentracing)): Trace your services with SmallRye OpenTracing
- Keycloak Authorization ([guide](https://quarkus.io/guides/security-keycloak-authorization)): Policy enforcer using Keycloak-managed permissions to control access to protected resources
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validate object properties (field, getter) and method parameters for your beans (REST, CDI, Jakarta Persistence)
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- Camel Kubernetes ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/kubernetes.html)): Perform operations against Kubernetes API
- OpenTelemetry ([guide](https://quarkus.io/guides/opentelemetry)): Use OpenTelemetry to trace services
