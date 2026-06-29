package com.stop.api_gateway.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeaderAddingHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, List<String>> addedHeaders = new LinkedHashMap<>();

    public HeaderAddingHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String name, String value) {
        addedHeaders.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public String getHeader(String name) {
        List<String> headers = addedHeaders.get(name);
        if (headers != null && !headers.isEmpty()) {
            return headers.getFirst();
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> added = addedHeaders.get(name);
        if (added != null) {
            return Collections.enumeration(added);
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
        names.addAll(addedHeaders.keySet());
        return Collections.enumeration(names);
    }
}
