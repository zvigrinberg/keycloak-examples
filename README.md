# KeyCloak Examples


## BackGround

Keycloak is an Open source IAM (Identity and Access Management) Solution, Which Let you protect your application Resources
Using Client (App/Service/Entity) and User Authentication, Predefined Roles, client/users Authorization rules using permissions, scopes, policies, and protected resources to which to apply the permissions.
In order to protect application, first we demand that client will authenticate the user using the application/client, on behalf on the user.
Then, we are managing precise/fine-grained rules of access control, that is, after user authenticated , which users/groups/roles are allowed to access a protected resource.

In general , the authorization process for authenticated users  is done using the following process:

Given a client/user/group/role, denoted as `A`, and an action or verb , donated as `X`, and a resource, denoted as `R`, And `G` as a set of conditions that need to be satisfied , then a permission defined as follows:

(*) `A` can perform action `X` on resource `R` if a Set of Conditions `G` is satisfied:

in Keycloak Terms: 
- `A` corresponding to User that could belong to a group or that have a role, that is , the authorization to do something can be given directly to the user or indirectly through group or role.
- `X` corresponding to an optional authorization scope/client scope, and usually mapped to actions that are being performed on resources, such as read, write, update, delete, get and etc.
- `R` is a resource that is usually associated with endpoint/rest API Resource path, and may contain a definition of set of Authorization scopes supported for the resource.
- `G` corresponding to set of 1 or more instance of type `Policy` , which defined the conditions that must be fulfilled in order that authorization will be granted to user.
- The whole statement (*) is defined in keycloak as `Permission`, Which define a set of `Policies` to be evaluated with respect to client/user/user's group/user' role in order to decide whether to authorize or not the user/client to access( If scope is specified then also dictates the access type, like delete/update/create/reead) a particular `resource`   

## Features of Keycloak And Advantages
- Provides Authentication mechanism to services / clients/ applications.
- Oauth 2 , OpenId Connect (OIDC) and SAML support.
- Identity brokering - Authenticate with external OIDC or SAML Identity Providers.
- Provides a SSO Capabilities ( Authenticate once using one identity across a lot of different applications/clients).
- Various Java API clients for keycloak, and for JavaScripts, and Complete REST API to interact with the server for languages that don't have a keycloak client/adapter.
- Can act As an Authorization Server, and application can be a resource server, and authorization decisions can be fetched by resources server from authorization server (Using Keycloak Client/Adapter/Rest API) , hence completely decoupling authorization process and decisions and mechanism from application and its business logic. (Usually The fetching of authorization decisions is performed in PEP - Policy Enforcement Points in the application, usually by code not related to business logic like framework/filter/interceptor/proxy/decorator)   
- User Federation - Sync users from LDAP and Active Directory servers
- Kerberos bridge - Automatically authenticate users that are logged-in to a Kerberos server.

## Goal

To show how to use Keycloak as Authentication And Authorization Server in order to protect Rest API Resources and WEB applications.


## List of Demos

- Protecting RESTful API Service' resources using [keycloak and Quarkus](./quarkus).
- Protecting RESTful API Service' resources apps using [keycloak , Spring Boot and Spring Security](./springboot). 
- Protecting RESTful API Service' resources using [Java HTTP Filter and keycloak Authorization API Services Via Rest API](./springboot)
- Protecting endpoints using JAVA and proxy Design Pattern and keycloak Authorization client java API - TBD
- Using Interceptor of REST Resource in Quarkus - TBD.  
- Using Spring AOP Aspect of REST Resource in Spring Framework - TBD.  
- Protecting WEB UI Application in JAVA , using keycloak `/auth` endpoint
- Protecting WEB UI/CLI Application, Golang.
 