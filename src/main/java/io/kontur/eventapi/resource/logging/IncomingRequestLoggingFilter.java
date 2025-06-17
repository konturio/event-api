package io.kontur.eventapi.resource.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.list;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IncomingRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("httptrace");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String message = format("[received] [%s] [%s] [HEADERS: %s] [ADDRESS: %s]",
                request.getMethod(), request.getRequestURI(), headers(request), request.getRemoteAddr());
        LOG.info(message);
        filterChain.doFilter(request, response);
    }

    private Map<String, List<String>> headers(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, list(request.getHeaders(name)));
        }
        return headers;
    }
}
