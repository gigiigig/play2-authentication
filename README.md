play2-authentication
====================

A module for play2 for Authentication and Authorization,
including OAuth2 Facebook and Google login.

This is not a complete authentication system,
it include just some helpful trait to mix in Controllers

Trait Secured
--------------

Trait Secured is the trait for manage Authentication and Authorization.

It provides just some methods for secure Action access,
there are some helpers:

    withUser[User] {
        Ok("it is secured")
    }

To use it just mix in you database access class the trait

    SecureUsersRetriever

and to user class the trait

    SecureUser

Now in you controller mix the trait Secured and implement the two functions:

    def secureUsersRetriever: SecureUsersRetriever
    override def onUnauthorized(request: RequestHeader)

In secureUsersRetriever you must return the Object or the Class which extends the SecureUsersRetriever trait
for example:

    override def onUnauthorized(request: RequestHeader) = {
        log.debug(s"on onUnauthorized ip : ${request.remoteAddress}")
        Results.Redirect(routes.AuthController.login)
    }

    def secureUsersRetriever: SecureUsersRetriever = Users


Trait OAuth
-----------

The trait OAuth provides some helpers to manage OAuth2 login,
Facebook ang Google implementations are present.

How to use it:

Mix the trait in your controller, in that example  AuthController

Add to routes file the GET and POST routes to oauth action

    GET     /oauth/:provider            controllers.AuthController.oauth(provider: String)
    POST    /oauth/:provider            controllers.AuthController.oauth(provider: String)

And implement the function redirectUrl returning the oauth route

    def redirectUrl(provider: String): Call = routes.AuthController.oauth(provider)

Now implement the function useEmail, this function will be called after the user authentication,
and will pass as parameters the user email as and the authentication provider, example:

    override def useEmail(email: String, provider: String): Result = {
        log.debug(s"user $email logged in with $provider")
    }






