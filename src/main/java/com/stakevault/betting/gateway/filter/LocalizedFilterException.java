package com.stakevault.betting.gateway.filter;

public interface LocalizedFilterException {

	String messageKey();

	int httpStatusCode();

	default Object[] messageArgs() {
		return new Object[0];
	}
}
