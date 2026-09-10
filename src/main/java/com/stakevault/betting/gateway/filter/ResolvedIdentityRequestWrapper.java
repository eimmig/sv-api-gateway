package com.stakevault.betting.gateway.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class ResolvedIdentityRequestWrapper extends HttpServletRequestWrapper {

	public static final String USER_ID_HEADER = "X-User-Id";
	public static final String TENANT_ID_HEADER = "X-Tenant-Id";
	public static final String ROLE_HEADER = "X-User-Role";
	private static final String AUTHORIZATION_HEADER = "Authorization";

	/**
	 * Never forwarded downstream, regardless of which filter resolved the identity - the
	 * PASETO path never sets these, and the X-Service-Key path must not leak the shared secret
	 * or the caller-supplied telegram id to bets-service/stats-service.
	 */
	private static final Set<String> STRIPPED_HEADERS = Set.of(AUTHORIZATION_HEADER,
			ServiceKeyAuthenticationFilter.SERVICE_KEY_HEADER, ServiceKeyAuthenticationFilter.TELEGRAM_USER_ID_HEADER);

	private final String userId;
	private final String tenantId;
	/** Null on the X-Service-Key path (telegram-integration) - no role concept there, no admin route reachable via the bot; absent header downstream reads the same as "not admin". */
	private final String role;

	public ResolvedIdentityRequestWrapper(HttpServletRequest request, String userId, String tenantId, String role) {
		super(request);
		this.userId = userId;
		this.tenantId = tenantId;
		this.role = role;
	}

	private static boolean isStripped(String name) {
		return STRIPPED_HEADERS.stream().anyMatch(stripped -> stripped.equalsIgnoreCase(name));
	}

	@Override
	public String getHeader(String name) {
		if (USER_ID_HEADER.equalsIgnoreCase(name)) {
			return userId;
		}
		if (TENANT_ID_HEADER.equalsIgnoreCase(name)) {
			return tenantId;
		}
		if (ROLE_HEADER.equalsIgnoreCase(name)) {
			return role;
		}
		if (isStripped(name)) {
			return null;
		}
		return super.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (USER_ID_HEADER.equalsIgnoreCase(name)) {
			return Collections.enumeration(Set.of(userId));
		}
		if (TENANT_ID_HEADER.equalsIgnoreCase(name)) {
			return Collections.enumeration(Set.of(tenantId));
		}
		if (ROLE_HEADER.equalsIgnoreCase(name)) {
			return role == null ? Collections.emptyEnumeration() : Collections.enumeration(Set.of(role));
		}
		if (isStripped(name)) {
			return Collections.emptyEnumeration();
		}
		return super.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
		names.removeIf(name -> USER_ID_HEADER.equalsIgnoreCase(name) || TENANT_ID_HEADER.equalsIgnoreCase(name)
				|| ROLE_HEADER.equalsIgnoreCase(name) || isStripped(name));
		names.add(USER_ID_HEADER);
		names.add(TENANT_ID_HEADER);
		if (role != null) {
			names.add(ROLE_HEADER);
		}
		return Collections.enumeration(names);
	}
}
