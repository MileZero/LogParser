package com.mz.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mz.logs.utils.DateTimeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.Min;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReader {
    private String BASE_PATH="/mnt/nfs-logs/logs/";
    private String SERVICE_PATH = "/SortationServices-prod/prod";
    public static void main(String[] args) throws Exception {
        (new FileReader()).start();
        //(new FileReader()).test();
    }

    private void test() throws Exception {
        String serviceName = SERVICE_PATH.substring(SERVICE_PATH.indexOf('/')+1,SERVICE_PATH.lastIndexOf('/'));
        (new LogParser()).parseFile("/work/MZ/logs/test.log",serviceName);
    }

    private String start() throws Exception {
        String serviceName = SERVICE_PATH.substring(SERVICE_PATH.indexOf('/')+1,SERVICE_PATH.lastIndexOf('/'));
        LocalDateTime ldt = DateTimeUtils.getDateTimeUTC();
        String hour = ldt.getHour()>9?""+ldt.getHour():"0"+ldt.getHour();
        String path =
                BASE_PATH+ldt.toLocalDate()+"_"+hour+SERVICE_PATH;
        List<String> files = getLogFiles(path);
        System.out.println(files);
        for(String fileName:files) {
            //process request logs, we can get to application logs next
            if(fileName.contains("request"))
                (new LogParser()).parseFile(fileName,serviceName);
        }
        return path;
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
