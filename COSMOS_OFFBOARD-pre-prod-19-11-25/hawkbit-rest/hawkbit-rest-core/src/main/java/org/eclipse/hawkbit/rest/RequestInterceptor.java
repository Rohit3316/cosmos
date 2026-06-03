package org.eclipse.hawkbit.rest;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class RequestInterceptor implements HandlerInterceptor{
	
	private static final String CORRELATION_ID = "correlationId";
	
	private static final String SESSION_ID_HEADER = "sessionId";
	
	private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    	HandlerMethod handlerMethod = null;
    	
    	MDC.put(CORRELATION_ID, getCorrelationId());
    	 if (handler instanceof HandlerMethod) {
             handlerMethod = (HandlerMethod) handler;
    	 }
    	if(handlerMethod != null) {
    		LOG.info("Correlation ID "+ MDC.get(CORRELATION_ID) +" logged for " + handlerMethod.getMethod());
    	}else {
    		LOG.info("Correlation ID "+ MDC.get(CORRELATION_ID));
    	}

    	String sessionId = request.getHeader(SESSION_ID_HEADER);

        if (sessionId != null) {
            LOG.info("Session ID from header: " + sessionId);
        }

        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
       Object handler, ModelAndView modelAndView) throws Exception {

    	String sessionId = request.getHeader(SESSION_ID_HEADER);

        if (sessionId != null) {
            LOG.info("Request Completion for: " + sessionId);
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.clear();
    }

    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }

}

