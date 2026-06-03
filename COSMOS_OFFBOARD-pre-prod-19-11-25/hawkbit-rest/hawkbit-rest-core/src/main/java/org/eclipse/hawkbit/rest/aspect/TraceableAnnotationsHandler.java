package org.eclipse.hawkbit.rest.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableObject;
import org.slf4j.MDC;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Aspect for handling methods annotated with {@link org.cosmos.annotations.TraceableMethod}.
 * This aspect processes method parameters and their fields annotated with {@link org.cosmos.annotations.TraceableField}
 * or {@link org.cosmos.annotations.TraceableObject} and adds them to the MDC (Mapped Diagnostic Context).
 */
@Aspect
@Component
@Description("Aspect for handling methods annotated with @TraceableMethod. This aspect processes method parameters and their fields annotated with @TraceableField or @TraceableObject and adds them to the MDC (Mapped Diagnostic Context).")
public class TraceableAnnotationsHandler {

    /**
     * Around advice that processes method parameters annotated with {@link org.cosmos.annotations.TraceableField}
     * or {@link org.cosmos.annotations.TraceableObject}.
     *
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if any error occurs during method execution
     */
    @Around("@annotation(org.cosmos.annotations.TraceableMethod)")
    public Object handleTraceableMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        Object[] args = joinPoint.getArgs(); // Get method arguments
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = methodSignature.getMethod().getParameterAnnotations();

        // Check if the method has the TraceableMethod annotation
        if (!methodSignature.getMethod().isAnnotationPresent(org.cosmos.annotations.TraceableMethod.class)) {
            throw new IllegalArgumentException("Method is not annotated with @TraceableMethod");
        }

        // Loop through method parameters and their annotations
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Object arg = args[i]; // The actual argument
            if (arg != null) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof TraceableObject) {
                        processTraceableObject(arg); // Process fields of the object
                    } else if (annotation instanceof TraceableField) {
                        processTraceableField((TraceableField) annotation, arg, methodSignature, i);
                    }
                }
            }
        }
        return joinPoint.proceed(args);
    }

    /**
     * Processes an object whose fields are annotated with {@link org.cosmos.annotations.TraceableField}.
     *
     * @param obj the object to process
     */
    @SuppressWarnings("squid:S3011")
    public void processTraceableObject(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(TraceableField.class)) {
                TraceableField traceableField = field.getAnnotation(TraceableField.class);
                String key = traceableField.value().isEmpty() ? field.getName() : traceableField.value();
                field.setAccessible(true); // Access private fields
                try {
                    Object value = field.get(obj); // Extract field value
                    if (value != null) {
                        MDC.put(key, value.toString()); // Add to MDC
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Unable to access field value for tracing", e);
                }
            }
        }
    }

    /**
     * Processes a method parameter annotated with {@link org.cosmos.annotations.TraceableField}.
     *
     * @param arg the method parameter to process
     * @param annotation the annotation on the method parameter
     */

    public void processTraceableField(TraceableField annotation, Object arg, MethodSignature methodSignature, int i) {
        String key = annotation.value().isEmpty() ? methodSignature.getParameterNames()[i] : annotation.value();
        MDC.put(key, arg.toString()); // Add to MDC
    }

}
