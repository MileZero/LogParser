package com.mz.logs;

import com.mz.logs.utils.DateTimeUtils;
import com.mz.logs.utils.EnvProperties;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReader {
    private String BASE_PATH = "/mnt/nfs-logs/logs/";

    //e.g. "/Tesseract-prod/prod"
    private EnvProperties getEnvProperties() {
        List<String> enabledServicesList = new ArrayList<>();
        EnvProperties envProperties = new EnvProperties();
        try (InputStream input = FileReader.class.getClassLoader().getResourceAsStream("service.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            envProperties = EnvProperties.builder()
                    .environment(prop.getProperty("logparser.environment"))
                    .allServices(prop.getProperty("logparser.enabledServices"))
                    .grayLogUrl(prop.getProperty("graylog.url"))
                    .build();
            for (String str : envProperties.getAllServices().split(","))
                enabledServicesList.add(str);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(" Env:" + envProperties.getEnvironment());
        List<String> pathList = new ArrayList<>();
        for (String serviceName : enabledServicesList) {
            pathList.add("/" + serviceName + "/" + envProperties.getEnvironment());
            System.out.println("/" + serviceName + "/" + envProperties.getEnvironment());
        }
        envProperties.setAllServicesPath(enabledServicesList);
        return envProperties;
    }

    public static void main(String[] args) throws Exception {
        //(new FileReader()).test();
        (new FileReader()).start();
    }

    private void test() throws Exception {
        (new LogParser()).parseFile("/work/MZ/LogParser/test-req-1.log",
                "test-service", true,"http://graylog.prod.milezero.com:8080/gelf");
    }

    private void start() {
        EnvProperties envProperties = getEnvProperties();
        for (String servicePath : envProperties.getAllServicesPath()) {
            String serviceName = servicePath.substring(servicePath.indexOf('/') + 1, servicePath.lastIndexOf('/'));
            LocalDateTime ldt = DateTimeUtils.getDateTimeUTC();
            String hour = ldt.getHour() > 9 ? "" + ldt.getHour() : "0" + ldt.getHour();
            String path =
                    BASE_PATH + ldt.toLocalDate() + "_" + hour + servicePath;
            List<String> files = getLogFiles(path);
            System.out.println(files);
            for (String fileName : files) {
                CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                    try {
                        (new LogParser()).parseFile(fileName,
                                serviceName.toLowerCase(),
                                fileName.contains("request"),
                                envProperties.getGrayLogUrl());
                    } catch (Exception ex) {
                        System.out.println(" Exception in Start() ");
                    }
                });
            }
        }
    }

    private List<String> getLogFiles(String folder) {
        try {
            Stream<Path> pathStream = Files.walk(Paths.get(folder));
            List<String> filesList = pathStream.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            return filesList;
        } catch (IOException ex) {
            System.out.println(" Exception getLogFiles() ");
            ex.printStackTrace();
        }
        return null;
    }

}
