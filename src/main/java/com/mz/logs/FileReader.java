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

    private EnvProperties envProperties;

    public static void main(String[] args) throws Exception {
        //(new FileReader()).test();
        (new FileReader()).start();
    }

    private void test() throws Exception {
        (new LogParser()).parseFile("/work/MZ/LogParser/test-req.log",
                "test-service", true, "http://graylog.prod.milezero.com:8080/gelf",true);
    }

    private void start() {
        this.envProperties = EnvUtils.getEnvProperties();
        List<String> failurePaths = new ArrayList<>();
        //List<String> failurePathsAppLog = new ArrayList<>();
        for (String servicePath : envProperties.getAllServicesPath()) {
            String serviceName = ParserUtils.getServiceName(servicePath);
            String path = ParserUtils.getServiceFullPath(servicePath);
            System.out.println(path);
            List<String> files = getLogFiles(path);
            //no log files yet, or app log yet
            if (files == null) {
                failurePaths.add(servicePath);
                continue;
            }
            //app logs created only on exceptions
            /*if(!files.contains("application"))
                failurePathsAppLog.add(servicePath);*/
            System.out.println(files);
            parse(files, serviceName);
        }
        /*if (EnvUtils.isStage(envProperties.getEnvironment())) {
            retryFailures(failurePaths,true);
            retryFailures(failurePathsAppLog,false);
        }*/
    }

    private void retryFailures(List<String> fileNames,boolean parseAllFiles) {
        for (String servicePath : fileNames) {
            RetryWorker worker = new RetryWorker(servicePath,parseAllFiles);
            worker.start();
        }
    }

    private void parse(List<String> files, String serviceName) {
        for (String fileName : files) {
            CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                try {
                    (new LogParser()).parseFile(fileName,
                            serviceName.toLowerCase(),
                            fileName.contains("request"),
                            envProperties.getGrayLogUrl(),
                            true
                            /*!envProperties.getEnabledServicesNoData().contains(serviceName)*/);
                } catch (Exception ex) {
                    ex.printStackTrace();
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
        boolean parseAllFiles;
        public RetryWorker(String servicePath,boolean parseAllFiles) {
            this.servicePath = servicePath;
            this.parseAllFiles=parseAllFiles;
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
                    if(parseAllFiles)
                        parse(files,serviceName);
                    else {
                        parse(files.stream().filter(s->s.contains("application")).collect(Collectors.toList()),
                                serviceName);
                    }
                    break;
                } catch (Exception ex) {
                    System.out.println(" Retry Worker Exception ");
                    ex.printStackTrace();
                }
            }
        }
    }
}