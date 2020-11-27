package com.mz.logs.utils;

import java.time.LocalDateTime;

public class ParserUtils {
    public static String getServiceName(String servicePath) {
        return
                servicePath.substring(servicePath.indexOf('/') + 1, servicePath.lastIndexOf('/'));
    }

    public static String getServiceFullPath(String servicePath) {
        LocalDateTime ldt = DateTimeUtils.getDateTimeUTC();
        String hour = ldt.getHour() > 9 ? "" + ldt.getHour() : "0" + ldt.getHour();
        return
                EnvUtils.BASE_PATH + ldt.toLocalDate() + "_" + hour + servicePath;

    }
}
