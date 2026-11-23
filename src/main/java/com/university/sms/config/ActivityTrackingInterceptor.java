package com.university.sms.config;

import com.university.sms.core.concurrent.PortalActivityMonitor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * UNIT 3 - Multithreading in a real server (Vivekanand).
 * Tomcat serves each request on its own worker thread (http-nio-8080-exec-N). This interceptor
 * runs on that thread before and after every portal request and updates the shared, thread-safe
 * {@link PortalActivityMonitor}. A {@link ThreadLocal} stores the start time per thread, so
 * concurrent requests never see each other's value.
 */
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();
    private final PortalActivityMonitor monitor;

    public ActivityTrackingInterceptor(PortalActivityMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        START_TIME.set(System.currentTimeMillis());
        String uri = request.getRequestURI();
        String portal = uri.startsWith("/admin") ? "Admin" : uri.startsWith("/faculty") ? "Faculty" : "Student";
        monitor.requestStarted(portal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long start = START_TIME.get();
        monitor.requestFinished(start == null ? 0 : System.currentTimeMillis() - start);
        START_TIME.remove();                     // avoid leaks: Tomcat re-uses its threads
    }
}
