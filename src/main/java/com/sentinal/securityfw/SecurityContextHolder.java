package com.sentinal.securityfw;

public final class SecurityContextHolder
{
	private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

	private SecurityContextHolder()
	{
	}

	public static void set(SecurityContext context)
	{
		CONTEXT.set(context);
	}

	public static SecurityContext get()
	{
		SecurityContext context = CONTEXT.get();

		if (context == null)
		{
			throw new AuthenticationException("Security context not available");
		}

		return context;
	}

	public static SecurityContext getOrNull()
	{
		return CONTEXT.get();
	}

	public static void clear()
	{
		CONTEXT.remove();
	}
}