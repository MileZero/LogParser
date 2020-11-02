package com.mz.logs.utils;

import java.util.regex.Pattern;

public class LogFormat {
    //2020-04-29 10:00:00,558
    public static String datePattern = "(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})";
    public static String threadNumber = ".(\\d{3})";
    public static String clientRequestId = "\\s*(\\w*-\\w*-\\w*-\\w*-\\w*)";
    public static String serverRequestId = "\\s*(\\w*-\\w*-\\w*-\\w*-\\w*)";
    public static String msgReceived = "*\\s-\\s*(w*)";
    public static Pattern LOG_FILE_PATTERN = Pattern.compile(datePattern + threadNumber + clientRequestId + serverRequestId + msgReceived);
    public static String STACK_TRACE_PREFIX="Exception";
    public static String STACK_TRACE_CONTINUE="at";
}
