package com.mz.logs;

import com.mz.logs.utils.DateTimeUtils;
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
    private String BASE_PATH="/mnt/nfs-logs/logs/";

    /*@Value("${logparser.environment}")
    private String environment;

    @Value("#{'${logparser.enabledServices}'.split(',')}")
    private List<String> enabledServicesList;*/

    //e.g. "/Tesseract-prod/prod"
    private List<String> getAllServicesPath() {
        String environment="";
        List<String> enabledServicesList=new ArrayList<>();
        try (InputStream input = FileReader.class.getClassLoader().getResourceAsStream("service.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            environment = prop.getProperty("logparser.environment");
            String allServices = prop.getProperty("logparser.enabledServices");
            for(String str:allServices.split(",")) {
                enabledServicesList.add(str);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(" Env:" +environment);
        List<String> pathList = new ArrayList<>();
        for(String serviceName:enabledServicesList) {
            pathList.add("/"+serviceName+"-"+environment+"/"+environment);
            System.out.println("/"+serviceName+"-"+environment+"/"+environment);
        }
        return pathList;
    }

    /*private List<String> SERVICE_PATH_LIST = Arrays.asList(
            "/Tesseract-prod/prod",
            "/switchboard-prod/prod",
            "/Alamo-Prod/prod",
            "/SortationServices-prod/prod",
            "/oegr-prod/prod",
            "/hei-prod/prod",
            "/lmx-prod/prod",
            "/messenger-prod/prod"
    );*/

    public static void main(String[] args) throws Exception {
        (new FileReader()).start();
    }

    private void test() throws Exception {
        (new LogParser()).parseFile("/work/MZ/logs/test.log",
                "test-service");
    }

    private void start() {
        for(String servicePath:getAllServicesPath()) {
            String serviceName = servicePath.substring(servicePath.indexOf('/') + 1, servicePath.lastIndexOf('/'));
            LocalDateTime ldt = DateTimeUtils.getDateTimeUTC();
            String hour = ldt.getHour() > 9 ? "" + ldt.getHour() : "0" + ldt.getHour();
            String path =
                    BASE_PATH + ldt.toLocalDate() + "_" + hour + servicePath;
            List<String> files = getLogFiles(path);
            System.out.println(files);
            for (String fileName : files) {
                //process request logs, we can get to application logs next
                if (fileName.contains("request")) {
                    CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                        try {
                            (new LogParser()).parseFile(fileName, serviceName.toLowerCase());
                        }catch (Exception ex) {
                            System.out.println(" Exception in Start() ");
                        }
                    });
                }
            }
        }
    }

    private List<String> getLogFiles(String folder) {
        try {
            Stream<Path> pathStream = Files.walk(Paths.get(folder));
            List<String> filesList = pathStream.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            return filesList;
        }catch(IOException ex) {
            System.out.println(" Exception getLogFiles() ");
            ex.printStackTrace();
        }
        return null;
    }

}
