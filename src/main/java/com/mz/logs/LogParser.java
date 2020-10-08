package com.mz.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mz.logs.utils.DataPublisher;
import com.mz.logs.utils.LogFormat;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class LogParser {
    private Map<String,String> requestDataMap = new HashMap<>();
    private DataPublisher dataPublisher = new DataPublisher();

    public void parseFile(String fileName,String serviceName) throws Exception {
        File file = new File(fileName);
        System.out.println("Reading File "+file);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int lineNumber=0;
        while (true) {
            line = br.readLine();
            lineNumber++;
            //System.out.println("Raw Data:"+line);
            if(line==null) {
                //file rotated
                if(!file.exists()) {
                    System.out.println("File Rotated, Closing Parser:");
                    dataPublisher.close();
                    System.exit(0);
                }
                //sleep for 5 seconds and keep trying
                //System.out.println("Pausing:");
                Thread.sleep(1000);
                continue;
            }
            Matcher m = LogFormat.LOG_FILE_PATTERN.matcher(line);
            if (m.find()) {
                gatherData(line,serviceName);
            } else if (line.startsWith(">")) {
                gatherRequestData(line,">");
            }  else if (line.startsWith("<")) {
                gatherRequestData(line,"<");
            } else if (StringUtils.isNotEmpty(line)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> map = mapper.readValue(line, Map.class);
                    requestDataMap.put("data", line);
                    requestDataMap.putAll(map);
                }catch(Exception ex) {
                    //we parse json objects in request, not list of json array thats in response, so just ignore for now
                    //ex.printStackTrace();
                    //System.out.println("Exception Parsing: "+fileName+" : Line number "+lineNumber);
                }
            } else if (StringUtils.isEmpty(line)) {
                dataPublisher.sendPost(requestDataMap);
            }
        }
    }

    private void gatherRequestData(String line, String replaceChar) {
        String[] tokens;
        line = line.replace(replaceChar,"");
        line = line.trim();
        if(line.contains("POST") || line.contains("GET"))
            tokens = line.split(" ");
        else
            tokens = line.split(":");
        if(tokens.length>1)
            requestDataMap.put(tokens[0].trim(),tokens[1].trim());
        else
            requestDataMap.put("response_code",tokens[0].trim());
    }

    private String getJson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gsonObject = gsonBuilder.create();
        String jsonObj = gsonObject.toJson(requestDataMap);
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = prettyGson.toJson(requestDataMap);
        //System.out.println(prettyJson);
        return prettyJson;
    }

    private void gatherData(String line,String serviceName) {
        requestDataMap.clear();
        Matcher m = LogFormat.LOG_FILE_PATTERN.matcher(line);
        if (m.find( )) {
            requestDataMap.put("timestamp",m.group(1));
            requestDataMap.put("thread",m.group(2));
            requestDataMap.put("client_request_id",m.group(3));
            requestDataMap.put("server_request_id",m.group(4));
            requestDataMap.put("service_name",serviceName);
        }else {
            System.out.println("NO MATCH");
        }
    }

}
