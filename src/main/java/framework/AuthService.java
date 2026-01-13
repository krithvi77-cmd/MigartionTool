package framework;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class AuthService {

    // ✅ FIXED: Added 'accountsUrl' as the first parameter
    public static String getSite24x7AccessToken(String accountsUrl, String clientId, String clientSecret, String refreshToken) throws Exception {
        
        // ✅ FIXED: Use the dynamic URL from XML instead of hardcoding .in
        String url = accountsUrl + "/oauth/v2/token";

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