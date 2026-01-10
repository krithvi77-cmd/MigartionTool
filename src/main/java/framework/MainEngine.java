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

        // 1. Load Configurations (Metadata)
        this.typeBridge = XmlLoader.loadTypeBridge(normXml, vendor);
        Map<String, List<Map<String, String>>> mappingRules = XmlLoader.loadMappings(mapXml, vendor);
        Map<String, Map<String, String>> normPaths = XmlLoader.loadNormalizationPaths(normXml, vendor);
        Map<String, String> vendorConfig = XmlLoader.getVendorMonitorConfig(normXml, vendor);

        // 2. Fetch Raw Data using the 'root-path' from XML (e.g., $.tests)
        String rawData = GenericClient.fetch(vendorConfig.get("api"), srcHeaders);
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
                    String endpoint = rule.get("endpoint");
                    
                    // Lazy Load Cache using 'response-path' from XML (e.g., $.data)
                    if (!globalCache.containsKey(endpoint)) {
                        loadCache(targetBaseUrl + endpoint, targetHeaders, endpoint, 
                                  rule.get("matchOn"), rule.get("idField"), rule.get("responsePath"));
                    }

                    if ("true".equals(rule.get("multiValue"))) {
                        // TAKE ALL for lists (e.g. Notifications)
                        finalValue = new ArrayList<>(globalCache.get(endpoint).values());
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