package com.sentinal.securityfw;

public final class SecurityManager
{
	private static AuthenticationProvider authenticationProvider;
	private static AuthorizationProvider authorizationProvider;
	private static RateLimiter rateLimiter;

	private static volatile boolean initialized;

	private SecurityManager()
	{
	}

	public static synchronized void initialize(AuthenticationProvider authenticationProvider, AuthorizationProvider authorizationProvider, RateLimiter rateLimiter)
	{
		if (initialized)
		{
			throw new IllegalStateException("SecurityManager already initialized");
		}

		if (authenticationProvider == null)
		{
			throw new IllegalArgumentException("AuthenticationProvider is required");
		}

		if (authorizationProvider == null)
		{
			throw new IllegalArgumentException("AuthorizationProvider is required");
		}

		if (rateLimiter == null)
		{
			throw new IllegalArgumentException("RateLimiter is required");
		}

		SecurityManager.authenticationProvider = authenticationProvider;

		SecurityManager.authorizationProvider = authorizationProvider;

		SecurityManager.rateLimiter = rateLimiter;

		initialized = true;
	}

	public static AuthenticationProvider getAuthenticationProvider()
	{
		validateInitialized();
		return authenticationProvider;
	}

	public static AuthorizationProvider getAuthorizationProvider()
	{
		validateInitialized();
		return authorizationProvider;
	}

	public static RateLimiter getRateLimiter()
	{
		validateInitialized();
		return rateLimiter;
	}

	private static void validateInitialized()
	{
		if (!initialized)
		{
			throw new IllegalStateException("SecurityManager is not initialized");
		}
	}
}