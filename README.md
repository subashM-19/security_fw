# Sentinal Security Framework

Sentinal Security Framework is a lightweight Java servlet security framework that provides authentication, authorization, request-scoped security context management, authenticated principals, and rate-limiting integration.

The framework is designed around provider interfaces so that applications can plug in their own authentication, authorization, and rate-limiting strategies without changing the core security pipeline.

---

## Overview

The framework sits in front of application request handling and processes security concerns before the request reaches the application layer.

```text
HTTP Request
      |
      v
SecurityFilter
      |
      +-- Public Route Check
      |
      +-- Authentication
      |
      +-- Security Context Creation
      |
      +-- Rate Limiting
      |
      +-- Authorization
      |
      v
Application
```

The framework itself does not define how users authenticate or what roles an application uses.

Instead, those decisions are delegated to provider implementations.

---

## Features

- Servlet filter based security pipeline
- Pluggable authentication
- Pluggable authorization
- Pluggable rate limiting
- Authenticated principal abstraction
- Role support
- Request-scoped security context
- Thread-local context storage
- Automatic security context cleanup
- Public endpoint support
- Authentication failure handling
- Authorization failure handling
- Rate-limit failure handling
- Reusable across Java servlet applications

---

## Project Structure

```text
security_fw/
├── README.md
├── build.gradle
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/
    │   └── resources/
    └── test/
        └── java/
```

---

## Requirements

- Java 17
- Gradle
- Javax Servlet API
- Javax Servlet compatible container
- Tomcat 9 compatible environment

---

## Build

Run:

```bash
./gradlew clean build
```

---

## Core Components

The main framework components are:

```text
SecurityFilter
SecurityManager
SecurityContext
SecurityContextHolder
AuthenticatedPrincipal

AuthenticationProvider
AuthorizationProvider
RateLimiter

AuthenticationException
AuthorizationException
RateLimitException
```

---

## Security Pipeline

The framework processes a request in the following order:

```text
Incoming Request
      |
      v
Check whether route is public
      |
      +---- Public
      |        |
      |        v
      |   Rate Limiting
      |        |
      |        v
      |   Application
      |
      v
Authenticate Request
      |
      v
Validate Security Context
      |
      v
Store Security Context
      |
      v
Rate Limiting
      |
      v
Authorization
      |
      v
Application
      |
      v
Clear Security Context
```

The security context is cleared in a `finally` block after request processing.

This prevents authenticated state from leaking between servlet threads.

---

## AuthenticationProvider

Authentication is application-defined through the `AuthenticationProvider` interface.

```java
public interface AuthenticationProvider
{
    SecurityContext authenticate(
        HttpServletRequest request
    );
}
```

An authentication provider receives the incoming servlet request and is responsible for returning a valid `SecurityContext`.

Possible implementations include:

- JWT authentication
- OAuth/OIDC access token validation
- API key authentication
- session authentication
- custom internal authentication

---

## Authentication Example

A basic provider could conceptually create a principal after validating a request:

```java
Set<Integer> roles =
    Collections.singleton(
        1
    );

AuthenticatedPrincipal principal =
    new AuthenticatedPrincipal(
        1001L,
        "user",
        roles
    );

return new SecurityContext(
    principal
);
```

The authentication mechanism itself belongs to the application-specific provider.

---

## AuthorizationProvider

Authorization is application-defined through the `AuthorizationProvider` interface.

```java
public interface AuthorizationProvider
{
    boolean isPublic(
        HttpServletRequest request
    );

    void authorize(
        SecurityContext context,
        HttpServletRequest request
    );
}
```

The provider has two responsibilities.

### Public Route Detection

```java
boolean isPublic(
    HttpServletRequest request
);
```

This determines whether the current request requires authentication.

Example:

```java
@Override
public boolean isPublic(
    HttpServletRequest request)
{
    return request
        .getRequestURI()
        .endsWith(
            "/health"
        );
}
```

### Authorization

```java
void authorize(
    SecurityContext context,
    HttpServletRequest request
);
```

This method decides whether the authenticated principal has permission to access the current request.

Example:

```java
@Override
public void authorize(
    SecurityContext context,
    HttpServletRequest request)
{
    if (
        !context.hasRole(
            10
        )
    )
    {
        throw new AuthorizationException(
            "Required role missing"
        );
    }
}
```

---

## RateLimiter

Applications can provide their own rate-limiting implementation through:

```java
public interface RateLimiter
{
    void check(
        SecurityContext context,
        HttpServletRequest request
    );
}
```

The rate limiter is invoked for both public and authenticated routes.

Example:

```java
@Override
public void check(
    SecurityContext context,
    HttpServletRequest request)
{
    // application-defined rate-limit logic
}
```

Possible implementations include:

- in-memory token bucket
- fixed window
- sliding window
- Redis-backed limiter
- API gateway based limiter

---

## SecurityManager

`SecurityManager` stores the configured framework providers.

Initialization:

```java
SecurityManager.initialize(
    authenticationProvider,
    authorizationProvider,
    rateLimiter
);
```

Example:

```java
SecurityManager.initialize(
    new ApplicationAuthenticationProvider(),
    new ApplicationAuthorizationProvider(),
    new ApplicationRateLimiter()
);
```

The framework can then access the configured providers during request processing.

Initialization is intended to happen once during application startup.

---

## AuthenticatedPrincipal

`AuthenticatedPrincipal` represents the authenticated identity.

```java
AuthenticatedPrincipal principal =
    new AuthenticatedPrincipal(
        principalID,
        principalType,
        roles
    );
```

The principal contains:

```text
principalID
principalType
roles
```

---

## Principal ID

The principal ID represents the application's authenticated identity.

Example:

```java
long principalID =
    principal.getPrincipalID();
```

---

## Principal Type

The principal type allows applications to identify the category of principal.

Example:

```java
String principalType =
    principal.getPrincipalType();
```

Possible values are application-defined.

Examples could include:

```text
user
service
system
```

---

## Principal Roles

Roles are represented as integer values.

Example:

```java
Set<Integer> roles =
    principal.getRoles();
```

Check a role:

```java
boolean allowed =
    principal.hasRole(
        roleID
    );
```

---

## Immutable Roles

The principal stores its roles as an unmodifiable set.

This prevents application code from changing the authenticated principal's roles after authentication.

---

## SecurityContext

`SecurityContext` wraps the authenticated principal for the current request.

Create a context:

```java
SecurityContext context =
    new SecurityContext(
        principal
    );
```

Check authentication:

```java
context.isAuthenticated();
```

Get principal:

```java
AuthenticatedPrincipal principal =
    context.getPrincipal();
```

Get principal ID:

```java
long principalID =
    context.getPrincipalID();
```

Check role:

```java
boolean allowed =
    context.hasRole(
        roleID
    );
```

---

## SecurityContextHolder

`SecurityContextHolder` exposes the security context associated with the current request thread.

The implementation uses `ThreadLocal`.

Store a context:

```java
SecurityContextHolder.set(
    context
);
```

Read the current context:

```java
SecurityContext context =
    SecurityContextHolder.get();
```

Read it optionally:

```java
SecurityContext context =
    SecurityContextHolder.getOrNull();
```

Clear it:

```java
SecurityContextHolder.clear();
```

---

## Why ThreadLocal

Servlet containers commonly process different requests using pooled threads.

The security context must therefore be isolated per request.

```text
Request A
Thread 1
SecurityContext A

Request B
Thread 2
SecurityContext B
```

Using `ThreadLocal` allows downstream application code to access the current authenticated principal without passing security objects through every method.

---

## Security Context Cleanup

The framework clears the context after request completion.

Conceptually:

```java
try
{
    SecurityContextHolder.set(
        context
    );

    chain.doFilter(
        request,
        response
    );
}
finally
{
    SecurityContextHolder.clear();
}
```

This cleanup is important because servlet-container threads are reused.

---

## SecurityFilter

`SecurityFilter` is the main HTTP entry point for the framework.

Its responsibilities include:

- checking public routes
- authenticating protected requests
- storing the security context
- invoking rate limiting
- invoking authorization
- mapping security exceptions to HTTP responses
- clearing the security context

---

## Public Request Flow

A public route follows:

```text
HTTP Request
      |
      v
AuthorizationProvider.isPublic()
      |
      v
RateLimiter.check()
      |
      v
Filter Chain
```

Authentication is not required for a route marked as public.

---

## Protected Request Flow

A protected request follows:

```text
HTTP Request
      |
      v
AuthenticationProvider.authenticate()
      |
      v
SecurityContext
      |
      v
SecurityContextHolder.set()
      |
      v
RateLimiter.check()
      |
      v
AuthorizationProvider.authorize()
      |
      v
Filter Chain
      |
      v
SecurityContextHolder.clear()
```

---

## Authentication Failure

Authentication failures are represented using:

```text
AuthenticationException
```

The filter maps authentication failures to:

```text
HTTP 401 Unauthorized
```

Typical reasons include:

- missing authentication credentials
- invalid token
- expired token
- malformed credentials
- unavailable security context

---

## Authorization Failure

Authorization failures are represented using:

```text
AuthorizationException
```

The filter maps authorization failures to:

```text
HTTP 403 Forbidden
```

Typical reasons include:

- missing required role
- attempting to access a protected resource
- insufficient application permissions

---

## Rate Limit Failure

Rate-limit failures are represented using:

```text
RateLimitException
```

The filter maps these failures to:

```text
HTTP 429 Too Many Requests
```

---

## HTTP Security Responses

The main framework security responses are:

```text
401
    Authentication failure

403
    Authorization failure

429
    Rate-limit failure
```

---

## Application Access to Current Principal

Application code can retrieve the current principal without accepting identity information from the client.

```java
SecurityContext context =
    SecurityContextHolder.get();

long currentPrincipalID =
    context.getPrincipalID();
```

This allows applications to derive identity from trusted authentication state.

---

## Role-Based Authorization

Applications may define role IDs such as:

```text
1 = USER
2 = ADMIN
3 = SUPPORT
```

Authorization can then use:

```java
if (
    !context.hasRole(
        ADMIN_ROLE
    )
)
{
    throw new AuthorizationException(
        "Admin permission required"
    );
}
```

Role meaning remains application-defined.

---

## Provider-Based Design

The framework separates generic security infrastructure from application-specific security policy.

The framework owns:

```text
SecurityFilter
SecurityContext
SecurityContextHolder
AuthenticatedPrincipal
provider interfaces
exception mapping
request security lifecycle
```

The application owns:

```text
authentication mechanism
token validation
user lookup
role definitions
route permissions
rate limits
```

This separation allows the same framework to be reused by different applications.

---

## Example Framework Initialization

```java
AuthenticationProvider authenticationProvider =
    new ApplicationAuthenticationProvider();

AuthorizationProvider authorizationProvider =
    new ApplicationAuthorizationProvider();

RateLimiter rateLimiter =
    new ApplicationRateLimiter();

SecurityManager.initialize(
    authenticationProvider,
    authorizationProvider,
    rateLimiter
);
```

---

## Example Authorization Provider

```java
public class ApplicationAuthorizationProvider
    implements AuthorizationProvider
{
    private static final int ADMIN_ROLE =
        2;

    @Override
    public boolean isPublic(
        HttpServletRequest request)
    {
        String uri =
            request.getRequestURI();

        return uri.endsWith(
            "/health"
        );
    }

    @Override
    public void authorize(
        SecurityContext context,
        HttpServletRequest request)
    {
        String uri =
            request.getRequestURI();

        if (
            uri.startsWith(
                "/api/admin/"
            ) &&
            !context.hasRole(
                ADMIN_ROLE
            )
        )
        {
            throw new AuthorizationException(
                "Admin permission required"
            );
        }
    }
}
```

---

## Example Rate Limiter

```java
public class ApplicationRateLimiter
    implements RateLimiter
{
    @Override
    public void check(
        SecurityContext context,
        HttpServletRequest request)
    {
        // Application-defined implementation.
    }
}
```

The core framework does not force one rate-limiting algorithm.

---

## Example Authentication Provider

```java
public class ApplicationAuthenticationProvider
    implements AuthenticationProvider
{
    @Override
    public SecurityContext authenticate(
        HttpServletRequest request)
    {
        long principalID =
            1001L;

        Set<Integer> roles =
            Collections.singleton(
                1
            );

        AuthenticatedPrincipal principal =
            new AuthenticatedPrincipal(
                principalID,
                "user",
                roles
            );

        return new SecurityContext(
            principal
        );
    }
}
```

This example only demonstrates the provider interface.

A real provider should validate trusted authentication credentials before creating the principal.

---

## Separation of Authentication and Authorization

Authentication answers:

```text
Who is making this request?
```

Authorization answers:

```text
Is this authenticated principal allowed to perform this action?
```

The framework keeps these responsibilities separate.

---

## Separation of Security and Business Logic

Security decisions should happen before business methods execute where possible.

```text
Request
   |
   v
Authentication
   |
   v
Authorization
   |
   v
Business Logic
```

Application code can still perform resource-level authorization when access depends on domain data.

---

## Thread Safety

The framework stores request-specific security state inside `ThreadLocal`.

Provider implementations should be thread-safe if shared across servlet requests.

---

## Stateless Authentication

The framework works well with stateless authentication designs because `AuthenticationProvider` can be called for each protected request.

```text
HTTP Request
      |
      v
Credential Validation
      |
      v
AuthenticatedPrincipal
      |
      v
SecurityContext
```

The core framework does not require HTTP sessions.

---

## Error Handling

Security providers may throw framework exceptions when processing fails.

Authentication:

```java
throw new AuthenticationException(
    "Authentication required"
);
```

Authorization:

```java
throw new AuthorizationException(
    "Access denied"
);
```

Rate limiting:

```java
throw new RateLimitException(
    "Too many requests"
);
```

The filter translates these exceptions into the corresponding HTTP status.

---

---

## Testing

Run tests using:

```bash
./gradlew test
```

Run the complete build using:

```bash
./gradlew clean build
```

Tests should cover:

- principal creation
- role checks
- immutable roles
- authenticated context behavior
- unauthenticated context behavior
- thread-local context storage
- context cleanup
- public route handling
- protected route handling
- authentication failures
- authorization failures
- rate-limit failures
- provider initialization

---

## Deployment

The framework is packaged as a reusable Java JAR.

Build:

```bash
./gradlew clean build
```

Typical output:

```text
build/
└── libs/
    └── security_fw.jar
```

The JAR can then be added to a Java servlet-based application.

---

## Servlet Integration

The framework is designed to execute as a servlet filter before application request processing.

```text
Servlet Container
       |
       v
SecurityFilter
       |
       v
Application Servlet / REST Layer
```

The consuming application is responsible for registering the filter using its servlet configuration.

---

## Design Goals

Sentinal Security Framework was designed to:

- separate authentication from business logic
- separate authorization from authentication
- support reusable security providers
- expose authenticated identity throughout the request
- avoid passing identity manually through every method
- provide simple role checking
- support application-defined rate limiting
- keep framework responsibilities explicit
- remain compatible with standard servlet applications

---

## Why Provider Interfaces?

Security mechanisms differ between applications.

One application may use:

```text
JWT
```

Another may use:

```text
API keys
```

Another may use:

```text
HTTP sessions
```

By defining:

```text
AuthenticationProvider
AuthorizationProvider
RateLimiter
```

the core request pipeline stays the same while application-specific implementations can change.

---

## Why SecurityContext?

Without a request security context, application code may need to repeatedly pass authentication information through every method.

Instead:

```text
SecurityFilter
      |
      v
SecurityContextHolder
      |
      v
Downstream Application Code
```

can access the current authenticated principal.

---

## Why Immutable Principal Roles?

Authentication establishes trusted roles for the request.

Those roles should not be modified by arbitrary application code after authentication.

For that reason, `AuthenticatedPrincipal` exposes roles as an unmodifiable set.

---

## Current Scope

The framework currently focuses on servlet-based request security.

It provides:

```text
authentication pipeline
authorization integration
authenticated principal
request security context
role checks
rate-limiter integration
HTTP security error handling
```

It intentionally does not provide:

```text
user database management
password storage
OAuth server functionality
identity-provider hosting
application-specific role definitions
application-specific permission models
```

Those responsibilities belong to the consuming application or external identity system.

---

## Scaling Considerations

The provider model allows security implementations to evolve independently.

For example, an application can initially use:

```text
in-memory rate limiter
```

and later replace it with:

```text
Redis-backed rate limiter
```

without changing the core `SecurityFilter`.

Authentication and authorization implementations can similarly be replaced without changing the framework request lifecycle.

---

## Future Improvements

Possible future improvements include:

- method-level authorization support
- reusable permission abstractions
- richer authorization policies
- standardized security error response bodies
- configurable public-route matching
- authentication caching hooks
- audit-event hooks
- request correlation IDs
- security metrics
- configurable filter diagnostics
- distributed rate-limiter implementations
- additional servlet-container integration tests
- improved startup validation
- provider health checks
- richer principal attributes

---

## Project Purpose

Sentinal Security Framework was created to implement and understand the core responsibilities normally handled by larger Java security frameworks.

The project demonstrates concepts such as:

- servlet filters
- authentication pipelines
- authorization
- request-scoped identity
- `ThreadLocal`
- role-based access
- provider abstractions
- exception-to-HTTP-status mapping
- separation of framework and application security concerns

---

## Related Projects

### Sentinal REST Framework

Provides:

- HTTP routing
- parameter extraction
- Java type conversion
- method invocation

Repository:

```text
rest_fw
```

### Sentinal Persistence Framework

Provides:

- query construction
- criteria
- joins
- row management
- inserts
- updates

Repository:

```text
persistence_fw
```

---

## Security Notes

Do not commit:

```text
access tokens
refresh tokens
passwords
private keys
keystores
API secrets
environment credentials
production certificates
```

Authentication providers should obtain sensitive configuration from secure runtime configuration.

---

---

## Status

The framework is under active development.

---
