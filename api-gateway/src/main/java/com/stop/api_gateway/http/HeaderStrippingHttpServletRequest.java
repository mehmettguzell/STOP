package com.stop.api_gateway.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HeaderStrippingHttpServletRequest extends HttpServletRequestWrapper {

    private static final Set<String> STRIP_LOWER = Set.of("x-user-id", "x-user-role");

    private HeaderStrippingHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public static HttpServletRequest stripIdentityHeaders(HttpServletRequest request) {
        return new HeaderStrippingHttpServletRequest(request);
    }

    private static boolean strip(String name) {
        return name != null && STRIP_LOWER.contains(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getHeader(String name) {
        if (strip(name)) {
            return null;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (strip(name)) {
            return Collections.emptyEnumeration();
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = new ArrayList<>();
        Enumeration<String> enumeration = super.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String n = enumeration.nextElement();
            if (!strip(n)) {
                names.add(n);
            }
        }
        return Collections.enumeration(names);
    }
}
