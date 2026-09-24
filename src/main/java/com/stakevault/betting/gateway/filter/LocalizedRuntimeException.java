package com.stakevault.betting.gateway.filter;

public abstract class LocalizedRuntimeException extends RuntimeException implements LocalizedFilterException {

	private final transient Object[] args;

	protected LocalizedRuntimeException(String message, Object... args) {
		super(message);
		this.args = args;
	}

	protected LocalizedRuntimeException(String message, Throwable cause, Object... args) {
		super(message, cause);
		this.args = args;
	}

	@Override
	public Object[] messageArgs() {
		return args.clone();
	}
}
