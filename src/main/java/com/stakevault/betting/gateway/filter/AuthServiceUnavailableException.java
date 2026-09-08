package com.stakevault.betting.gateway.filter;

public class AuthServiceUnavailableException extends RuntimeException implements LocalizedFilterException {

	public AuthServiceUnavailableException(Throwable cause) {
		super("auth-service telegram-account lookup failed", cause);
	}

	@Override
	public String messageKey() {
		return "error.auth-service-unavailable";
	}

	@Override
	public int httpStatusCode() {
		return 503;
	}
}
