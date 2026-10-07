package com.sentinal.securityfw;

public class AuthorizationException extends RuntimeException
{
	public AuthorizationException(String message)
	{
		super(message);
	}
}