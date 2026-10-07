package com.sentinal.securityfw;

import javax.servlet.http.HttpServletRequest;

public interface RateLimiter
{
	void check(SecurityContext context, HttpServletRequest request);
}