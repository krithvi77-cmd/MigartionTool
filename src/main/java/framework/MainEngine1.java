package framework;

import org.json.JSONArray;
import org.json.JSONObject;
import com.jayway.jsonpath.JsonPath;
import java.util.*;

public class MainEngine1 {
    private Map<String, Map<String, String>> globalCache = new HashMap<>();
    private Map<String, String> typeBridge = new HashMap<>();

    public List<Map<String, Object>> fetchAndPrepare(
            String vendor, 
            Map<String, String> srcHeaders, 
            String normXml, 
            String mapXml, 
            String targetBaseUrl, 
            Map<String, String> targetHeaders) throws Exception {

        List<Map<String, Object>> preparedList = new ArrayList<>();

        // 1. Load Configurations (Metadata)
        this.typeBridge = XmlLoader.loadTypeBridge(normXml, vendor);
        Map<String, List<Map<String, String>>> mappingRules = XmlLoader.loadMappings(mapXml, vendor);
        Map<String, Map<String, String>> normPaths = XmlLoader.loadNormalizationPaths(normXml, vendor);
        Map<String, String> vendorConfig = XmlLoader.getVendorMonitorConfig(normXml, vendor);

        // 2. Fetch Raw Data using the 'root-path' from XML (e.g., $.tests)
//        String api = vendorConfig.get("api");
//        String endpoint = vendorConfig.get("endpoint");
//
//        if (api == null || api.isEmpty()) {
//            throw new IllegalStateException("Vendor API URL missing for: " + vendor);
//        }
//
//        String rawData = GenericClient.fetch(api + endpoint, srcHeaders);
     // 2. Fetch Raw Data
        String api = vendorConfig.get("api");
        String endpoint = vendorConfig.get("endpoint");

        if (api == null || api.isEmpty()) {
            throw new IllegalStateException("Vendor API URL missing for: " + vendor);
        }

        // ✅ ADD THIS SLASH GUARD LOGIC HERE:
        if (!api.endsWith("/")) {
            api = api + "/";
        }
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }

        if (!api.endsWith("/")) api += "/";
        if (endpoint.startsWith("/")) endpoint = endpoint.substring(1);
        String finalUrl = api + endpoint;

        // Add this print statement so you can see the exact URL in the Eclipse Console
        System.out.println("DEBUG: Final Request URL -> " + finalUrl);
        System.out.println("DEBUG: Fetching URL: " + finalUrl);
        if (srcHeaders != null) {
            srcHeaders.forEach((k, v) -> System.out.println("DEBUG Header: " + k + " = " + v));
        }


        String rawData = GenericClient.fetch(finalUrl, srcHeaders);

        String rootPath = vendorConfig.getOrDefault("rootPath", "$");
        
        // Use JsonPath to find the monitor list dynamically
        Object listObj = JsonPath.read(rawData, rootPath);
        JSONArray monitors = new JSONArray(listObj instanceof List ? (List)listObj : new ArrayList<>());

        // 3. Process Each Monitor
        for (int i = 0; i < monitors.length(); i++) {
            JSONObject sourceJson = monitors.getJSONObject(i);
            String frameworkType = typeBridge.get(sourceJson.optString("type", "api"));

            if (frameworkType == null || !mappingRules.containsKey(frameworkType)) continue;

            Model model = new Model();
            String monitorRawString = sourceJson.toString();

            for (Map<String, String> rule : mappingRules.get(frameworkType)) {
                String targetField = rule.get("target");
                Object finalValue = null;

                if ("api".equals(rule.get("resolve"))) {
                	String lookupEndpoint = rule.get("endpoint");

                    
                    // Lazy Load Cache using 'response-path' from XML (e.g., $.data)
                    if (!globalCache.containsKey(endpoint)) {
//                        loadCache(targetBaseUrl + endpoint, targetHeaders, lookupEndpoint, 
//                                  rule.get("matchOn"), rule.get("idField"), rule.get("responsePath"));
                    	// Fix targetBaseUrl + endpoint concatenation
                    	// Build cache URL for the lookup API
//                    	String cacheUrl = targetBaseUrl;
//                    	if (!cacheUrl.endsWith("/")) cacheUrl += "/";
//
//                    	// Use the endpoint from the rule mapping
//                    	String ruleEndpoint = rule.get("endpoint");
//                    	if (ruleEndpoint.startsWith("/")) ruleEndpoint = ruleEndpoint.substring(1);
//
//                    	cacheUrl += ruleEndpoint;
                    	// Normalize targetBaseUrl and endpoint from XML rule
                    	String ruleEndpoint = rule.get("endpoint"); // endpoint from XML
                    	if (ruleEndpoint == null) ruleEndpoint = "";

                    	String cacheUrl = targetBaseUrl.replaceAll("/$", "") + "/" + ruleEndpoint.replaceAll("^/", "");

                    	System.out.println("DEBUG: Final cache URL -> " + cacheUrl);

                    	loadCache(cacheUrl, targetHeaders, lookupEndpoint, 
                    	          rule.get("matchOn"), rule.get("idField"), rule.get("responsePath"));


                    	System.out.println("DEBUG: Final cache URL -> " + cacheUrl);
                    	if (targetHeaders != null) {
                    	    targetHeaders.forEach((k, v) -> System.out.println("DEBUG Header: " + k + " = " + v));
                    	}

                    	loadCache(cacheUrl, targetHeaders, lookupEndpoint, 
                    	          rule.get("matchOn"), rule.get("idField"), rule.get("responsePath"));


                    }

                    if ("true".equals(rule.get("multiValue"))) {
                        // TAKE ALL for lists (e.g. Notifications)
                        finalValue = new ArrayList<>(globalCache.get(lookupEndpoint).values());
                    } else {
                        // Resolve specific ID
                        String path = normPaths.get(frameworkType).get(rule.get("sourceKey"));
                        String lookupName = String.valueOf(extract(monitorRawString, path, rule.get("default")));
                        finalValue = globalCache.get(endpoint).getOrDefault(lookupName, rule.get("default"));
                    }
                } else {
                    // Direct Mapping + TransformLibrary
                    String path = normPaths.get(frameworkType).get(rule.get("sourceKey"));
                    Object rawVal = extract(monitorRawString, path, rule.get("default"));
                    finalValue = TransformLibrary.execute(rule.get("transform"), rawVal);
                }
                model.addField(targetField, finalValue);
            }
            preparedList.add(model.data);
        }
        return preparedList;
    }

    private Object extract(String json, String path, String def) {
        try {
            if (path == null || path.isEmpty()) return def;
            Object result = JsonPath.read(json, path);
            return (result == null) ? def : result;
        } catch (Exception e) { 
            return def; 
        }
    }

    private void loadCache(String url, Map<String, String> headers, String endpoint, 
                           String nameK, String idK, String respPath) throws Exception {
    	System.out.println("DEBUG: Loading cache URL: " + url);
    	if (headers != null) {
    	    headers.forEach((k, v) -> System.out.println("DEBUG Header: " + k + " = " + v));
    	}

        String resp = GenericClient.fetch(url, headers);
        // Use the responsePath from XML instead of hardcoded ".data"
        List<Map<String, Object>> items = JsonPath.read(resp, respPath);
        
        Map<String, String> subCache = new HashMap<>();
        for (Map<String, Object> item : items) {
            subCache.put(String.valueOf(item.get(nameK)), String.valueOf(item.get(idK)));
        }
        globalCache.put(endpoint, subCache);
    }
}