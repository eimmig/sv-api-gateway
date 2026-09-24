package com.stakevault.betting.gateway.filter;

public class InvalidServiceKeyException extends LocalizedRuntimeException {

	public InvalidServiceKeyException() {
		super("missing or mismatched X-Service-Key header");
	}

	@Override
	public String messageKey() {
		return "error.invalid-service-key";
	}

	@Override
	public int httpStatusCode() {
		return 401;
	}
}
