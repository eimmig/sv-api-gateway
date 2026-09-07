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
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private final String userId;
	private final String tenantId;

	public ResolvedIdentityRequestWrapper(HttpServletRequest request, String userId, String tenantId) {
		super(request);
		this.userId = userId;
		this.tenantId = tenantId;
	}

	@Override
	public String getHeader(String name) {
		if (USER_ID_HEADER.equalsIgnoreCase(name)) {
			return userId;
		}
		if (TENANT_ID_HEADER.equalsIgnoreCase(name)) {
			return tenantId;
		}
		if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
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
		if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
			return Collections.emptyEnumeration();
		}
		return super.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
		names.removeIf(name -> USER_ID_HEADER.equalsIgnoreCase(name) || TENANT_ID_HEADER.equalsIgnoreCase(name)
				|| AUTHORIZATION_HEADER.equalsIgnoreCase(name));
		names.add(USER_ID_HEADER);
		names.add(TENANT_ID_HEADER);
		return Collections.enumeration(names);
	}
}
