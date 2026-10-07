package com.sentinal.securityfw;

import javax.servlet.http.HttpServletRequest;

public interface AuthorizationProvider
{
	boolean isPublic(HttpServletRequest request);

	void authorize(SecurityContext context, HttpServletRequest request);
}