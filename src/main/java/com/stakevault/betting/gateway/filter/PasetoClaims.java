package com.stakevault.betting.gateway.filter;

record PasetoClaims(String userId, String tenantId, String role, long iat, long exp) {

	boolean isComplete() {
		return hasText(userId) && hasText(tenantId) && hasText(role);
	}

	boolean isExpired(long nowEpochSeconds) {
		return exp <= nowEpochSeconds;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
