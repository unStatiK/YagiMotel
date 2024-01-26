package org.yagi.motel.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RestClient {
    public static <T> T sendPost(ObjectMapper mapper, HttpPost httpPost, Class<T> clazz) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpPost)) {
            if (response.getCode() == HttpStatus.SC_OK) {
                String clientResponse =
                        new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                return mapper.readValue(clientResponse, clazz);
            }
        }
        return null;
    }

    public static <T> T sendGet(ObjectMapper mapper, HttpGet httpGet, Class<T> clazz) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(httpGet)) {
            if (response.getCode() == HttpStatus.SC_OK) {
                String clientResponse =
                        new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                return mapper.readValue(clientResponse, clazz);
            }
        }
        return null;
    }

    public static HttpPost preparePostRequest(String url, Object request, ObjectMapper mapper) throws JsonProcessingException {
        final HttpPost httpPost = new HttpPost(url);
        StringEntity requestEntity = new StringEntity(
                mapper.writeValueAsString(request),
                ContentType.APPLICATION_JSON, StandardCharsets.UTF_8.name(), false);
        httpPost.setEntity(requestEntity);
        return httpPost;
    }

    public static HttpGet prepareGetRequest(String url) {
        return new HttpGet(url);
    }
}
