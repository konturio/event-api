package io.kontur.eventapi.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Semaphore;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Semaphore semaphore;

    public RateLimitingFilter(@Value("${ratelimit.concurrent:1000}") int concurrentLimit) {
        this.semaphore = new Semaphore(concurrentLimit);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (semaphore.tryAcquire()) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                semaphore.release();
            }
        } else {
            response.sendError(HttpServletResponse.SC_TOO_MANY_REQUESTS, "Too Many Requests");
        }
    }
}
