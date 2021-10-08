package com.mz.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mz.logs.utils.DataPublisher;
import com.mz.logs.utils.EnvProperties;
import com.mz.logs.utils.LogFormat;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/*
 * 1. Dont include GET responses, too large sometimes
 * 2. Log itself contains partial response only
 * 3. We run on single x3.large box with 32G Mem / 500G diskspace, unnecessary payload will cause burden on elastic search
 * 4. Messages will not get posted to ES
 * 5. Error on graylog: Journal utilization is too high and may go over the limit soon
 */
public class LogParser {
    private Map<String,String> requestDataMap = new HashMap<>();
    private DataPublisher dataPublisher = new DataPublisher();

    public void parseFile(String fileName, String serviceName, boolean requestFile, String grayLogUrl,
                          boolean publishResponseData) throws Exception{
        System.out.println(" In Parse File ");
        if(requestFile)
            parseReqLogFile(fileName,serviceName,grayLogUrl,publishResponseData);
        else
            parseAppLogFile(fileName,serviceName,grayLogUrl);
    }
    public void parseAppLogFile(String fileName,String serviceName,String grayLogUrl) throws Exception {
        File file = new File(fileName);
        //System.out.println("Reading App Log File "+fileName);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String stackTrace="";
        boolean hasStackTrace=false;
        int lineCount=0;
        while (true) {
            line = br.readLine();
            //File Rotated or no data
            if(line==null)
                return;
            if(line.contains(LogFormat.STACK_TRACE_PREFIX)||line.contains(LogFormat.STACK_TRACE_CONTINUE) && lineCount <3 ) {
                hasStackTrace=true;
                requestDataMap.put("service_name",serviceName);
                stackTrace = stackTrace + "\n" + line;
                lineCount++;
            }
            else {
                if(hasStackTrace) {
                    //System.out.println(stackTrace);
                    requestDataMap.put("file_type", "error_logs");
                    requestDataMap.put("stack_trace", stackTrace);
                    dataPublisher.sendPost(requestDataMap,grayLogUrl);
                    stackTrace = "";
                    requestDataMap.clear();
                    hasStackTrace = false;
                    lineCount=0;
                }
            }
        }
    }

    public void parseReqLogFile(String fileName,String serviceName,String grayLogUrl,boolean publishResponseData) throws Exception {
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
            } else if (StringUtils.isNotEmpty(line) && publishResponseData) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> map = mapper.readValue(line, Map.class);
                    requestDataMap.put("data", line);
                    //requestDataMap.putAll(map);
                }catch(Exception ex) {
                    //we parse json objects in request, not list of json array thats in response, so just ignore for now
                    //ex.printStackTrace();
                    requestDataMap.put("data", "not-available");
                    //System.out.println("Exception Parsing: "+fileName+" : Line number "+lineNumber);
                    //System.out.println(ex.getMessage());
                }
            } else if (StringUtils.isEmpty(line)) {
                dataPublisher.sendPost(requestDataMap,grayLogUrl);
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
            requestDataMap.put(tokens[0].trim().toLowerCase(),trimPath(tokens[1].trim()));
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

    //POST http://internal-SortationServices-prod-1528076527.us-west-2.elb.amazonaws.com/SortationServices-war/api/package
    //trim to /api/package
    private String trimPath(String url) {
        if (url.indexOf("/api/")!=-1)
            return url.substring(url.indexOf("/api/"),url.length());

        return url;
    }
}
