package com.example.evernote;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class ServicesClient {

    private final String baseUrl = System.getenv().getOrDefault("PY_BASE_URL", "http://localhost:8000");
    private final String ocr = "ocr";
    private final String train = "train";
    private final String infer = "infer";
    private final String ocrImages = "ocr_images";

    private final HttpClient http;

    private static final ServicesClient singleton;

    static {
        ServicesClient ls = null;
        try {
            ls = new ServicesClient();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }


    public static ServicesClient getSingleton() {
        return singleton;
    }

    private ServicesClient() throws IOException {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String ocr(byte[] imageBytes) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        body.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"image.png\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        body.write(imageBytes); // deine Bildbytes
        body.write((CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));

        String header = "multipart/form-data; boundary=" + boundary;

        HttpRequest req;
        HttpResponse res = null;

        try {
            req = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + ocr))
                    .header("Content-Type", header)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        String response = null;

        if (res != null) {
            System.out.println(res.statusCode() + " " + res.body());
            response = res.body() == null ? null : res.body().toString();
        }

        return response;
    }

    public void train(int scenario) throws Exception {
        String jsonBody = "{\"scenario\": " + scenario + "}";
        post (train, jsonBody);
    }

    public List<String> infer(int scenario, String csv_path) throws Exception {
        String jsonBody = "{ \"scenario\": " + scenario + ", \"csv_path\": \""  + csv_path + "\"}";
        String body = post (infer, jsonBody);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode tagsNode = mapper.readTree(body).get("tags");
        List<String> tags = new ArrayList<>();

        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode n : tagsNode) {
                tags.add(n.asText());
            }
        }
        return tags;
    }

    public void ocrImages(int images) throws Exception {
        if (images > 0) {
            String jsonBody = "{ \"images\":" + images + "}";
            post(ocrImages, jsonBody);
        }
    }

    private String post(String method, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + method))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

}
