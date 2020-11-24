package com.mz.logs.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataPublisher {
    private CloseableHttpClient httpClient = HttpClients.createDefault();
    public void sendPost(Map<String,String> requestDataMap,String grayLogUrl) {
        try {
            requestDataMap.put("short_message", UUID.randomUUID().toString());
            HttpPost httpPost = new HttpPost(grayLogUrl);
            String json = getJson(requestDataMap);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            //assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
            System.out.println(" Posting to " +grayLogUrl);
            System.out.println(" Posted "+json);
        }catch (Exception ex) {
           System.out.println(" Write failed ");
        }
    }

    private String getJson(Map<String,String> requestDataMap) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gsonObject = gsonBuilder.create();
        String jsonObj = gsonObject.toJson(requestDataMap);
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = prettyGson.toJson(requestDataMap);
        //System.out.println(prettyJson);
        return prettyJson;
    }

    public void close() {
        try {
            httpClient.close();
        }catch (IOException ex) {
            System.out.println(" Exception closing connection () ");
        }
    }
}
