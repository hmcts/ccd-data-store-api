package uk.gov.hmcts.ccd.appinsights;

public class CallbackTelemetryThreadContext {

    private static final InheritableThreadLocal<CallbackTelemetryContext> threadLocal = new InheritableThreadLocal<>();

    private CallbackTelemetryThreadContext() {
    }

    public static void setTelemetryContext(CallbackTelemetryContext telemetryContext) {
        threadLocal.set(telemetryContext);
    }

    public static CallbackTelemetryContext getTelemetryContext() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
