package org.eclipse.hawkbit.rest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.annotations.TraceableObject;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.aspect.TraceableAnnotationsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.Description;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link TraceableAnnotationsHandler} class.
 */
@Description("Unit tests for the TraceAspect class")
@ContextConfiguration(classes = {TestConfiguration.class})
class TraceableAnnotationsHandlerTest {

    public static final String TEST_VALUE = "testValue";
    public static final String FIELD_VALUE_3 = "fieldValue3";
    public static final String FIELD_VALUE_2 = "fieldValue2";
    public static final String FIELD_VALUE_1 = "fieldValue1";
    public static final String PARAM_1 = "param1";
    public static final String MDC_KEY_1 = "field1";
    public static final String MDC_KEY_2 = "field2";
    public static final String MDC_KEY_3 = "field3";
    public static final String PARAM = "param";
    private TraceableAnnotationsHandler traceableAnnotationsHandler;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        traceableAnnotationsHandler = new TraceableAnnotationsHandler();
        MDC.clear();
    }

    /**
     * Tests the handling of a traceable method.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the handling of a traceable method when given valid inputs")
    void givenValidInputsWhenHandleTraceableMethodThenCorrectlyHandle() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("traceableMethod", String.class, TestObject.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObject(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        traceableAnnotationsHandler.handleTraceableMethod(joinPoint);

        assertEquals(TEST_VALUE, MDC.get(PARAM_1));
        assertEquals(FIELD_VALUE_1, MDC.get(MDC_KEY_1));
        assertEquals(FIELD_VALUE_2, MDC.get(MDC_KEY_2));
        assertNull(MDC.get(MDC_KEY_3));
    }

    /**
     * Tests the handling of a traceable method with empty
     *
     * annotation keys.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the handling of a traceable field with empty annotation keys")
    void givenEmptyAnnotationKeysWhenHandleTraceableFieldThenCorrectlyHandles() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("traceableMethodWithEmptyAnnotation", String.class, TestObjectWithoutAnnotationKey.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{PARAM, "testObject"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObjectWithoutAnnotationKey(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        traceableAnnotationsHandler.handleTraceableMethod(joinPoint);

        assertEquals(TEST_VALUE, MDC.get(PARAM));
        assertEquals(FIELD_VALUE_1, MDC.get(MDC_KEY_1));
        assertEquals(FIELD_VALUE_2, MDC.get(MDC_KEY_2));
        assertNull(MDC.get(MDC_KEY_3));
    }

    /**
     * Tests the handling of a non-traceable method.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the handling of a non-traceable method")
    void givenNonTraceableMethodWhenHandleThenThrowsException() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("nonTraceableMethod", String.class, TestObject.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObject(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        assertThrows(IllegalArgumentException.class, () -> traceableAnnotationsHandler.handleTraceableMethod(joinPoint));
    }

    /**
     * Tests the processing of a traceable object.
     */
    @Test
    @Description("Tests the processing of a traceable object")
    void givenTraceableObjectWhenProcessThenCorrectlyProcesses() {
        TestObject testObject = new TestObject(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3);
        traceableAnnotationsHandler.processTraceableObject(testObject);
        assertEquals(FIELD_VALUE_1, MDC.get(MDC_KEY_1));
        assertEquals(FIELD_VALUE_2, MDC.get(MDC_KEY_2));
        assertNull(MDC.get(MDC_KEY_3));
    }

    /**
     * Tests the handling of a non-traceable object.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the handling of a non-traceable object")
    void givenNonTraceableObjectWhenHandleThenCorrectlyHandles() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("nonTraceableObject", String.class, TestObject.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObject(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        traceableAnnotationsHandler.handleTraceableMethod(joinPoint);

        assertEquals(TEST_VALUE, MDC.get(PARAM_1));
        assertNull(MDC.get(MDC_KEY_1));
        assertNull(MDC.get(MDC_KEY_2));
        assertNull(MDC.get(MDC_KEY_3));
    }

    /**
     * Tests the handling of a traceable field.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the handling of a traceable field")
    void givenTraceableFieldWhenHandleThenCorrectlyHandles() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("nonTraceableObject", String.class, TestObject.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObject(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        traceableAnnotationsHandler.processTraceableField(new TraceableField() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TraceableField.class;
            }

            @Override
            public String value() {
                return PARAM_1;
            }
        }, TEST_VALUE, methodSignature, 0);

        assertEquals(TEST_VALUE, MDC.get(PARAM_1));
    }

    /**
     * Tests the processing of a traceable field with an empty annotation value.
     *
     * @throws Throwable if any error occurs during the test execution
     */
    @Test
    @Description("Tests the processing of a traceable field with an empty annotation value")
    void givenEmptyAnnotationValueWhenProcessParamTraceableFieldThenCorrectlyProcesses() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = mock(MethodSignature.class);
        Method method = this.getClass().getMethod("traceableMethodWithEmptyAnnotation", String.class, TestObjectWithoutAnnotationKey.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{PARAM, "testObject"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{TEST_VALUE, new TestObjectWithoutAnnotationKey(FIELD_VALUE_1, FIELD_VALUE_2, FIELD_VALUE_3)});

        traceableAnnotationsHandler.processTraceableField(new TraceableField() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TraceableField.class;
            }

            @Override
            public String value() {
                return "";
            }
        }, TEST_VALUE, methodSignature, 0);

        assertEquals(TEST_VALUE, MDC.get(PARAM));
    }

    @TraceableMethod
    public void traceableMethod(@TraceableField(PARAM_1) String param1, @TraceableObject TestObject testObject) {
        // This method is traceable but currently lacks an implementation.
        // Uncomment and implement the tracing logic or remove if unnecessary.
        throw new UnsupportedOperationException("traceableMethod is not implemented yet.");
    }

    public void nonTraceableMethod(@TraceableField(PARAM_1) String param1, @TraceableObject TestObject testObject) {
        // This method is intentionally non-traceable.
        // If this method should perform an operation, implement the required logic here.
        // Otherwise, throwing an UnsupportedOperationException ensures it won't be used unintentionally.
        throw new UnsupportedOperationException("nonTraceableMethod is not implemented yet.");
    }


    @TraceableMethod
    public void nonTraceableObject(@TraceableField(PARAM_1) String param1, TestObject testObject) {
        // This method is marked as traceable, but TestObject is not annotated for tracing.
        // Verify if tracing is required and complete the implementation accordingly.
        throw new UnsupportedOperationException("nonTraceableObject is not implemented yet.");
    }


    @TraceableMethod
    public void traceableMethodWithEmptyAnnotation(@TraceableField String param, @TraceableObject TestObjectWithoutAnnotationKey testObject) {
        // This method is annotated with @TraceableMethod, but the annotations on its parameters lack keys.
        // Verify whether the annotations should include specific keys for traceability.
        // Either implement the required tracing logic, or throw an exception if this method should not be used yet.
        throw new UnsupportedOperationException("traceableMethodWithEmptyAnnotation is not implemented yet.");
    }


    /**
     * Test object class with multiple traceable fields.
     */
    static class TestObject {

        @TraceableField(MDC_KEY_1)
        private final String field1;

        @TraceableField(MDC_KEY_2)
        private final String field2;

        final String field3;

        public TestObject(final String field1, final String field2, final String field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }
    }

    /**
     * Test object class with multiple traceable fields without annotation key.
     */
    static class TestObjectWithoutAnnotationKey {

        @TraceableField
        private final String field1;

        @TraceableField
        private final String field2;

        private final String field3;

        public TestObjectWithoutAnnotationKey(final String field1, final String field2, final String field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }
    }
}