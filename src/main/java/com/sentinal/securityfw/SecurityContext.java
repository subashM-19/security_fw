package com.sentinal.securityfw;

public class SecurityContext
{
	private final AuthenticatedPrincipal principal;

	public SecurityContext(AuthenticatedPrincipal principal)
	{
		this.principal = principal;
	}

	public boolean isAuthenticated()
	{
		return principal != null;
	}

	public AuthenticatedPrincipal getPrincipal()
	{
		if (principal == null)
		{
			throw new AuthenticationException("Authentication required");
		}

		return principal;
	}

	public long getPrincipalID()
	{
		return getPrincipal().getPrincipalID();
	}

	public boolean hasRole(Integer role)
	{
		return principal != null && principal.hasRole(role);
	}
}