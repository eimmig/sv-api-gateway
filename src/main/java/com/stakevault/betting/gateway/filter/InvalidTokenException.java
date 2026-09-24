package com.stakevault.betting.gateway.filter;

public class InvalidTokenException extends LocalizedRuntimeException {

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
