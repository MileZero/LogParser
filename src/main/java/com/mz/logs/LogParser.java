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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class LogParser {
    private Map<String,String> requestDataMap = new HashMap<>();
    private DataPublisher dataPublisher = new DataPublisher();

    public void parseFile(String fileName) throws Exception {
        File file = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println("Raw Data:"+line);
            Matcher m = LogFormat.LOG_FILE_PATTERN.matcher(line);
            if (m.find()) {
                gatherData(line);
            } else if (line.startsWith(">")) {
                gatherRequestData(line,">");
            }  else if (line.startsWith("<")) {
                gatherRequestData(line,"<");
            } else if (StringUtils.isNotEmpty(line)) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> map = mapper.readValue(line, Map.class);
                requestDataMap.put("payload",line);
                requestDataMap.putAll(map);
            } else if (StringUtils.isEmpty(line)) {
                dataPublisher.sendPost(requestDataMap);
            }
        }
        dataPublisher.close();
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
        System.out.println(prettyJson);
        return prettyJson;
    }

    private void gatherData(String line) {
        requestDataMap.clear();
        Matcher m = LogFormat.LOG_FILE_PATTERN.matcher(line);
        if (m.find( )) {
            requestDataMap.put("timestamp",m.group(1));
            requestDataMap.put("thread",m.group(2));
            requestDataMap.put("client_request_id",m.group(3));
            requestDataMap.put("server_request_id",m.group(4));
        }else {
            System.out.println("NO MATCH");
        }
    }

}
