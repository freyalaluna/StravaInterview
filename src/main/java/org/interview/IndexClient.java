package org.interview;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IndexClient {
    public String url;
    private final HttpClient client;

    public IndexClient(String aUrl) {
        url = aUrl;
        client = HttpClient.newHttpClient();
    }

    public String getUrl(){
        return url;
    }

    public void setUrl(String aUrl){
        url = aUrl;
    }

    // Creates an HTTP GET request for its endpoint, and returns the response
    public String getIndices() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}
