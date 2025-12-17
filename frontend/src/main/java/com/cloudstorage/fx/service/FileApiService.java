package com.cloudstorage.fx.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.concurrent.Task;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileApiService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final String BASE_URL = "http://localhost:8080/files"; // Adjust port if needed

    public Task<List<Map<String, String>>> fetchUserFiles(String bucketName) {
        return new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "?bucket=" + bucketName))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse JSON List
                    return new Gson().fromJson(response.body(),
                            new TypeToken<List<Map<String, String>>>(){}.getType());
                }
                return new ArrayList<>();
            }
        };
    }
}