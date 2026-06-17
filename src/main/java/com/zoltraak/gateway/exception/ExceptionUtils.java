package com.zoltraak.gateway.exception;

public class ExceptionUtils {

    public static String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage());
    }
}
