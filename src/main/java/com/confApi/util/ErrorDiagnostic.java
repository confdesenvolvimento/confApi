package com.confApi.util;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Bounded exception diagnostics, without serializing request objects. */
public final class ErrorDiagnostic {
    private ErrorDiagnostic() { }
    public static String format(String context, Throwable error) {
        StringBuilder out = new StringBuilder(limit(String.valueOf(context), 900));
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int causes = 0;
        while (error != null && seen.add(error) && causes++ < 6) {
            out.append("\n").append(causes == 1 ? "Erro: " : "Causa: ")
                .append(error.getClass().getName()).append(": ")
                .append(limit(String.valueOf(error.getMessage()), 400));
            int frames = 0;
            for (StackTraceElement frame : error.getStackTrace()) {
                if (frame.getClassName().startsWith("com.conf") && frames++ < 4)
                    out.append("\n  em ").append(frame);
            }
            error = error.getCause();
        }
        return limit(out.toString(), 3200);
    }
    private static String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 15) + "...[truncado]";
    }
}
