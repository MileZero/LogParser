package com.mz.logs;

import com.mz.logs.utils.DateTimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReader {
    private String BASE_PATH="/mnt/nfs-logs/logs/";
    private List<String> SERVICE_PATH_LIST = Arrays.asList(
            "/Tesseract-prod/prod",
            "/switchboard-prod/prod",
            "/Alamo-Prod/prod",
            "/SortationServices-prod/prod",
            "/oegr-prod/prod",
            "/hei-prod/prod",
            "/lmx-prod/prod",
            "/messenger-prod/prod"
    );
    public static void main(String[] args) throws Exception {
        (new FileReader()).start();
        //(new FileReader()).test();
    }

    private void test() throws Exception {
        (new LogParser()).parseFile("/work/MZ/logs/test.log",
                "test-service");
    }

    private void start() {
        for(String servicePath:SERVICE_PATH_LIST) {
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
            ex.printStackTrace();
        }
        return null;
    }

}
