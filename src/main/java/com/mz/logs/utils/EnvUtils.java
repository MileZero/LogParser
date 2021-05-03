package com.mz.logs.utils;

import com.mz.logs.FileReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EnvUtils {
    public static String BASE_PATH = "/mnt/nfs-logs/logs/";

    public static EnvProperties getEnvProperties() {
        List<String> enabledServicesList = new ArrayList<>();
        List<String> enabledServicesNoDataList = new ArrayList<>();
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
            for (String str : prop.getProperty("logparser.enabledServicesNoData").split(","))
                enabledServicesNoDataList.add(str);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(" Env:" + envProperties.getEnvironment());
        List<String> pathList = new ArrayList<>();
        for (String serviceName : enabledServicesList) {
            String pathSuffix = envProperties.getEnvironment().equals("prod")?"prod":"staging";
            pathList.add("/" + serviceName + "/" + pathSuffix);
            System.out.println("/" + serviceName + "/" + pathSuffix);
            System.out.println(" Publish Data:" + !enabledServicesNoDataList.contains(serviceName));
        }
        envProperties.setAllServicesPath(pathList);
        envProperties.setEnabledServicesNoData(enabledServicesNoDataList);
        return envProperties;
    }

    public static boolean isStage(String stage) {
        if(stage.equals("stage"))
            return true;

        return false;
    }
}

