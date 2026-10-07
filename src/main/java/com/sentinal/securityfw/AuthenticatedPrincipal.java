package com.sentinal.securityfw;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthenticatedPrincipal
{
	private final long principalID;
	private final String principalType;
	private final Set<Integer> roles;

	public AuthenticatedPrincipal(long principalID, String principalType, Set<Integer> roles)
	{
		this.principalID = principalID;
		this.principalType = principalType;
		this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
	}

	public long getPrincipalID()
	{
		return principalID;
	}

	public String getPrincipalType()
	{
		return principalType;
	}

	public Set<Integer> getRoles()
	{
		return roles;
	}

	public boolean hasRole(Integer role)
	{
		return roles.contains(role);
	}
}