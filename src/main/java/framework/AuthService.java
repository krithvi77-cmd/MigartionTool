package framework;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class AuthService {

    public static String getSite24x7AccessToken(String clientId, String clientSecret, String refreshToken) throws Exception {
        String url = "https://accounts.zoho.com/oauth/v2/token";

        // Wrap each variable in URLEncoder.encode to prevent "bad URL" errors
        String fullUrl = url + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                         + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                         + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                         + "&grant_type=refresh_token";

        // Hit the API
        String response = GenericClient.postForToken(fullUrl, new HashMap<>());

        // Parse response
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        JSONObject json = (JSONObject) parser.parse(response);

        Object token = json.get("access_token");
        if (token == null) {
            throw new RuntimeException("Auth failed: " + response);
        }

        return "Zoho-oauthtoken " + token.toString();
    }
}