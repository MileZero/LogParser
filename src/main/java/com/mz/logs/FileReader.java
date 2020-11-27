package com.mz.logs;

import com.mz.logs.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReader {
    EnvProperties envProperties;

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
        envProperties.setAllServicesPath(pathList);
        return envProperties;
    }
    public static void main(String[] args) throws Exception {
        //(new FileReader()).test();
        (new FileReader()).start();
    }

    private void test() throws Exception {
        (new LogParser()).parseFile("/work/MZ/LogParser/test-req-1.log",
                "test-service", true, "http://graylog.stage.milezero.com:8080/gelf");
    }

    private void start() {
        this.envProperties = EnvUtils.getEnvProperties();
        List<String> failurePaths = new ArrayList<>();
        for (String servicePath : envProperties.getAllServicesPath()) {
            String serviceName = ParserUtils.getServiceName(servicePath);
            String path = ParserUtils.getServiceFullPath(servicePath);
            List<String> files = getLogFiles(path);
            if (files == null) {
                failurePaths.add(servicePath);
                continue;
            }
            System.out.println(files);
            parse(files, serviceName);
        }
        if (EnvUtils.isStage(envProperties.getEnvironment())) {
            for (String servicePath : failurePaths) {
                RetryWorker worker = new RetryWorker((servicePath));
                worker.start();
            }
        }
    }

    private void parse(List<String> files, String serviceName) {
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

    private List<String> getLogFiles(String folder) {
        try {
            Stream<Path> pathStream = Files.walk(Paths.get(folder));
            List<String> filesList = pathStream.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            return filesList;
        } catch (Exception ex) {
            System.out.println(" Exception getLogFiles() ");
            //ex.printStackTrace();
        }
        return null;
    }

    class RetryWorker extends Thread {
        String servicePath;
        public RetryWorker(String servicePath) {
            this.servicePath = servicePath;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    String serviceName = ParserUtils.getServiceName(servicePath);
                    String path = ParserUtils.getServiceFullPath(servicePath);
                    List<String> files = getLogFiles(path);
                    if (files == null) {
                        System.out.println(" Log file not yet available for "+path+", sleeping 2m and retrying");
                        Thread.sleep(1000*60*2);
                        continue;
                    }
                    parse(files,serviceName);
                    break;
                } catch (Exception ex) {
                    System.out.println(" Retry Worker Exception ");
                    ex.printStackTrace();
                }
            }
        }
    }
}