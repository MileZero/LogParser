package com.mz.logs.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static List<String> getLogFiles(String folder) {
        try {
            Stream<Path> pathStream = Files.walk(Paths.get(folder));
            List<String> filesList = pathStream.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            return filesList;
        } catch (Exception ex) {
            System.out.println(" Exception getLogFiles() ");
            ex.printStackTrace();
        }
        return null;
    }

}
