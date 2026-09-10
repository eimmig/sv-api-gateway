package com.stakevault.betting.gateway.filter;

public class InvalidTokenException extends RuntimeException implements LocalizedFilterException {

	public InvalidTokenException() {
		super("missing, malformed, tampered or expired PASETO token");
	}

	@Override
	public String messageKey() {
		return "error.invalid-token";
	}

	@Override
	public int httpStatusCode() {
		return 401;
	}
}
