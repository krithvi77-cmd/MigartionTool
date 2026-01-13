package framework;

import org.json.JSONArray;
import org.json.JSONObject;
import com.jayway.jsonpath.JsonPath;
import java.util.*;

public class MainEngine {

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

        // 1. Load Configurations
        this.typeBridge = XmlLoader.loadTypeBridge(normXml, vendor);
        Map<String, List<Map<String, String>>> mappingRules = XmlLoader.loadMappings(mapXml, vendor);
        Map<String, Map<String, String>> normPaths = XmlLoader.loadNormalizationPaths(normXml, vendor);
        Map<String, String> vendorConfig = XmlLoader.getVendorMonitorConfig(normXml, vendor);

        // 2. Fetch Raw Data
        String api = vendorConfig.get("api");
        String endpoint = vendorConfig.get("endpoint");

        if (api == null || api.isEmpty()) {
            throw new IllegalStateException("Vendor API URL missing for: " + vendor);
        }

        api = api.replaceAll("/$", "");
        endpoint = (endpoint == null) ? "" : endpoint.replaceAll("^/", "");
        String finalUrl = api + (endpoint.isEmpty() ? "" : "/" + endpoint);

        System.out.println("DEBUG: Final Request URL -> " + finalUrl);
        String rawData = GenericClient.fetch(finalUrl, srcHeaders);

        String rootPath = vendorConfig.getOrDefault("rootPath", "$");
        Object listObj = JsonPath.read(rawData, rootPath);
        JSONArray monitors = new JSONArray(listObj instanceof List ? (List<?>) listObj : new ArrayList<>());

        // 3. Process Each Monitor
        for (int i = 0; i < monitors.length(); i++) {
            JSONObject sourceJson = monitors.getJSONObject(i);
            String sourceType = sourceJson.optString("type", "api");
            String frameworkType = typeBridge.get(sourceType);

            if (frameworkType == null || !mappingRules.containsKey(frameworkType)) continue;

            Model model = new Model();
            String monitorRawString = sourceJson.toString();

            for (Map<String, String> rule : mappingRules.get(frameworkType)) {
                String targetField = rule.get("target");
                Object finalValue = null;

                if ("api".equalsIgnoreCase(rule.get("resolve"))) {
                    // Resolution Logic
                    String lookupEndpoint = rule.get("endpoint");
                    lookupEndpoint = (lookupEndpoint == null) ? "" : lookupEndpoint;

                    String cacheUrl = targetBaseUrl.replaceAll("/$", "") + "/" + lookupEndpoint.replaceAll("^/", "");

                    // Lazy Load Cache
                    if (!globalCache.containsKey(lookupEndpoint)) {
                        // FIX 1: Pass 'null' for filterType to load ALL profiles
                        // FIX 2: Pass 'false' for singleValue to load ALL profiles
                        loadCache(cacheUrl, targetHeaders, lookupEndpoint,
                                  rule.get("matchOn"), rule.get("idField"), rule.get("responsePath"),
                                  null, false);
                    }

                    if ("list".equalsIgnoreCase(rule.get("type"))) {
                        finalValue = new ArrayList<>(globalCache.get(lookupEndpoint).values());
                    } else {
                        // Single value lookup
                        String path = normPaths.get(frameworkType).get(rule.get("sourceKey"));
                        // XML default is "RESTAPI". Cache should map "RESTAPI" -> "540...001"
                        String lookupName = String.valueOf(extract(monitorRawString, path, rule.get("default")));
                        finalValue = globalCache.get(lookupEndpoint).getOrDefault(lookupName, rule.get("default"));
                    }
                } else {
                    // Direct Mapping
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

    private void loadCache(
            String url,
            Map<String, String> headers,
            String endpoint,
            String nameK,
            String idK,
            String respPath,
            String filterType,
            boolean singleValue) throws Exception { // singleValue param is kept for signature but ignored logic

        System.out.println("DEBUG: Loading cache URL: " + url);
        String resp = GenericClient.fetch(url, headers);
        List<Map<String, Object>> items = JsonPath.read(resp, respPath);

        Map<String, String> subCache = new HashMap<>();
        for (Map<String, Object> item : items) {
            // FIX: Only filter if filterType is explicitly provided (which we set to null now)
            if (filterType != null && !filterType.equalsIgnoreCase(String.valueOf(item.get("type")))) {
                continue;
            }
            
            String key = String.valueOf(item.get(nameK));
            String val = String.valueOf(item.get(idK));
            
            subCache.put(key, val);
            // FIX: Removed "if (singleValue) break;" so we load the entire list
        }
        
        System.out.println("DEBUG: Cache loaded for " + endpoint + " size=" + subCache.size());
        globalCache.put(endpoint, subCache);
    }
}