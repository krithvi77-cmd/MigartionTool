package framework;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class GenericClient {
    // Reusing a single client is more efficient for connection pooling
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * Standard GET fetch for Vendor APIs and Site24x7 Metadata.
     * Throws generic UNAUTHORIZED for the Servlet to handle UI redirection.
     */
    public static String fetch(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();

        if (headers != null) {
            headers.forEach((k, v) -> {
                System.out.println("DEBUG Header: " + k + " = " + v);
                builder.header(k, v);
            });
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode == 401) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException("API_ERROR_" + statusCode + ": " + response.body());
        }

        return response.body();
    }


    /**
     * Standard POST for final Migration (creating monitors).
     * Used after the user reviews the JSON in the frontend.
     */
    public static void post(String url, Map<String, String> headers, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (headers != null) {
            headers.forEach(builder::header);
        }
        
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        
        int status = response.statusCode();
        System.out.println("[TARGET-POST] Status: " + status);
        
        if (status < 200 || status >= 300) {
             System.err.println("Migration Error Body: " + response.body());
             throw new RuntimeException("POST_FAILED_" + status);
        }
    }

    /**
     * Special POST for OAuth Token Exchange.
     * Params are typically in the URL for Zoho, so body is set to noBody().
     */
    public static String postForToken(String fullUrl, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                // Zoho requires form-urlencoded even for empty body POSTs
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody());

        if (headers != null) {
            headers.forEach(builder::header);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("TOKEN_EXCHANGE_FAILED: " + response.body());
        }

        return response.body();
    }
}