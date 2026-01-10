package framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TransformLibrary {

    private static final Map<String, Function<Object, Object>> REGISTRY = new HashMap<>();

    static {
        // 1. Seconds to Minutes with safety check
        REGISTRY.put("seconds-to-minutes", val -> {
            try {
                return (val == null) ? 0 : Integer.parseInt(val.toString()) / 60;
            } catch (Exception e) { return 0; }
        });

        // 2. MS to Seconds
        REGISTRY.put("ms-to-seconds", val -> {
            try {
                return (val == null) ? 0 : Integer.parseInt(val.toString()) / 1000;
            } catch (Exception e) { return 0; }
        });

        // 3. String Prefix
        REGISTRY.put("prefix-test", val -> (val == null) ? "TEST_UNKNOWN" : "TEST_" + val.toString());
        
        // 4. Extract Domain
        REGISTRY.put("extract-domain-from-url", val -> {
            try {
                if (val == null) return "unknown.com";
                String url = val.toString();
                return url.split("//")[1].split("/")[0];
            } catch (Exception e) { return val; } // Return original if logic fails
        });

        // 5. NEW: HTTP Method Normalizer (Datadog 'GET' -> Site24x7 'GET')
        REGISTRY.put("to-uppercase", val -> (val == null) ? "GET" : val.toString().toUpperCase());
    }

    /**
     * Executes the mapping logic based on the function name defined in XML.
     */
    public static Object execute(String functionName, Object value) {
        // If no function name is provided in the XML, return the raw value
        if (functionName == null || functionName.trim().isEmpty()) {
            return value;
        }

        Function<Object, Object> transform = REGISTRY.get(functionName);
        
        if (transform == null) {
            System.err.println("[WARN] Transform function not found: " + functionName);
            return value; 
        }

        return transform.apply(value);
    }
}