package com.sentinal.securityfw;

import javax.servlet.http.HttpServletRequest;

public interface AuthenticationProvider
{
	SecurityContext authenticate(HttpServletRequest request);
}