package com.stakevault.betting.gateway.filter;

record PasetoClaims(String userId, String tenantId, String role, long iat, long exp) {
}
