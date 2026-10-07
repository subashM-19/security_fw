package com.sentinal.securityfw;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SecurityFilter implements Filter
{
	@Override public void init(FilterConfig filterConfig)
	{
	}

	@Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		HttpServletResponse httpResponse = (HttpServletResponse) response;

		System.out.println(
			"SecurityFilter invoked: "
				+ httpRequest.getRequestURI()
		);

		AuthorizationProvider authorizationProvider = SecurityManager.getAuthorizationProvider();

		AuthenticationProvider authenticationProvider = SecurityManager.getAuthenticationProvider();

		RateLimiter rateLimiter = SecurityManager.getRateLimiter();

		try
		{
			if (authorizationProvider.isPublic(httpRequest))
			{
				rateLimiter.check(null, httpRequest);

				chain.doFilter(request, response);

				return;
			}

			SecurityContext securityContext = authenticationProvider.authenticate(httpRequest);

			if (securityContext == null || !securityContext.isAuthenticated())
			{
				throw new AuthenticationException("Authentication required");
			}

			SecurityContextHolder.set(securityContext);

			rateLimiter.check(securityContext, httpRequest);

			authorizationProvider.authorize(securityContext, httpRequest);

			chain.doFilter(request, response);
		}
		catch (AuthenticationException e)
		{
			writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "authentication_required", e.getMessage());
		}
		catch (AuthorizationException e)
		{
			writeError(httpResponse, HttpServletResponse.SC_FORBIDDEN, "access_denied", e.getMessage());
		}
		catch (RateLimitException e)
		{
			writeError(httpResponse, 429, "rate_limit_exceeded", e.getMessage());
		}
		finally
		{
			SecurityContextHolder.clear();
		}
	}

	private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException
	{
		if (response.isCommitted())
		{
			return;
		}

		response.reset();

		response.setStatus(status);
		response.setContentType("application/json");

		String safeMessage = message == null ? "" : escapeJson(message);

		response.getWriter().write("{" + "\"error\":\"" + escapeJson(error) + "\"," + "\"message\":\"" + safeMessage + "\"" + "}");
	}

	private String escapeJson(String value)
	{
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	@Override public void destroy()
	{
	}
}