package com.mz.logs;

import com.mz.logs.utils.EnvProperties;
import com.mz.logs.utils.EnvUtils;
import com.mz.logs.utils.ParserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AppLogParser {

    private EnvProperties envProperties;

    public static void main(String[] args) throws Exception {
        (new AppLogParser()).start();
    }

    private void start() {
        this.envProperties = EnvUtils.getEnvProperties();
        for (String servicePath : envProperties.getAllServicesPath()) {
            String serviceName = ParserUtils.getServiceName(servicePath);
            String path = ParserUtils.getServiceFullPath(servicePath);
            List<String> files = ParserUtils.getLogFiles(path);
            //no log files yet, or app log yet
            if (files == null)
                continue;

            parse(files, serviceName);
        }
    }

    private void parse(List<String> files, String serviceName) {
        CompletableFuture<Void> cf=null;
        for (String fileName : files) {
            if(!fileName.contains("application"))
                continue;
            System.out.println(fileName);
            cf = CompletableFuture.runAsync(() -> {
                try {
                    (new LogParser()).parseAppLogFile(fileName,
                            serviceName.toLowerCase(),
                            envProperties.getGrayLogUrl());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(" Exception in Start() ");
                }
            });
        }
        if(cf!=null)
            cf.join();
    }

}