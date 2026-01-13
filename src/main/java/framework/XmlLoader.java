package framework;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class XmlLoader {

    /**
     * Loads JsonPaths from normalization.xml 
     * Maps "Normalized Name" -> "Source JsonPath"
     */
    public static Map<String, Map<String, String>> loadNormalizationPaths(String path, String vendorName)
            throws Exception {
        Map<String, Map<String, String>> normMap = new HashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        NodeList types = doc.getElementsByTagName("monitor-type");

        for (int i = 0; i < types.getLength(); i++) {
            Element t = (Element) types.item(i);
            String frameworkType = t.getAttribute("framework-type");
            Map<String, String> fieldPaths = new HashMap<>();

            NodeList fields = t.getElementsByTagName("field");
            for (int j = 0; j < fields.getLength(); j++) {
                Element f = (Element) fields.item(j);
                fieldPaths.put(getTag(f, "normalized-name"), getTag(f, "source-jsonpath"));
            }
            normMap.put(frameworkType, fieldPaths);
        }
        return normMap;
    }

    /**
     * Maps Vendor types to Framework types (e.g., "api" -> "REST_API")
     */
    public static Map<String, String> loadTypeBridge(String path, String vendorName) throws Exception {
        Map<String, String> bridge = new HashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        NodeList vendors = doc.getElementsByTagName("vendor");

        for (int i = 0; i < vendors.getLength(); i++) {
            Element v = (Element) vendors.item(i);
            if (v.getAttribute("name").equalsIgnoreCase(vendorName)) {
                NodeList types = v.getElementsByTagName("monitor-type");
                for (int j = 0; j < types.getLength(); j++) {
                    Element t = (Element) types.item(j);
                    bridge.put(t.getAttribute("vendor-type"), t.getAttribute("framework-type"));
                }
                break;
            }
        }
        return bridge;
    }

    /**
     * Loads Mapping Rules and dynamic Response Paths for API resolution.
     */
    public static Map<String, List<Map<String, String>>> loadMappings(String path, String vendorName) throws Exception {
        Map<String, List<Map<String, String>>> mappings = new HashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        NodeList types = doc.getElementsByTagName("monitor-type");

        for (int i = 0; i < types.getLength(); i++) {
            Element t = (Element) types.item(i);
            String typeName = t.getAttribute("name");
            List<Map<String, String>> fields = new ArrayList<>();
            NodeList fNodes = t.getElementsByTagName("field");

            for (int j = 0; j < fNodes.getLength(); j++) {
                Element f = (Element) fNodes.item(j);
                Map<String, String> m = new HashMap<>();
                
                m.put("target", f.getAttribute("target-field"));
                m.put("type", f.getAttribute("type")); 
                m.put("multiValue", f.getAttribute("type").equals("list") ? "true" : "false");
                m.put("transform", getTag(f, "transform"));

                // Resolution Logic
                m.put("resolve", getTag(f, "resolve-by"));
                
                NodeList endpointNodes = f.getElementsByTagName("lookup-endpoint");
                if (endpointNodes.getLength() > 0) {
                    Element ep = (Element) endpointNodes.item(0);
                    m.put("endpoint", ep.getTextContent().trim());
                    // CRITICAL: Capture the response-path attribute (e.g., "$.data")
                    m.put("responsePath", ep.getAttribute("response-path"));
                }

                m.put("matchOn", getTag(f, "match-on"));
                m.put("idField", getTag(f, "id-field"));
                m.put("default", getTag(f, "default-value"));

                // Vendor-Specific override
                NodeList vendors = f.getElementsByTagName("vendor");
                for (int k = 0; k < vendors.getLength(); k++) {
                    Element v = (Element) vendors.item(k);
                    if (v.getAttribute("name").equalsIgnoreCase(vendorName)) {
                        m.put("sourceKey", getTag(v, "source-key"));
                        String vDef = getTag(v, "default-value");
                        if (!vDef.isEmpty()) m.put("default", vDef);
                    }
                }
                fields.add(m);
            }
            mappings.put(typeName, fields);
        }
        return mappings;
    }

    /**
     * Gets Source API and the JSON Root Path (e.g., "$.tests")
     */
//    public static Map<String, String> getVendorMonitorConfig(String path, String vendorName) throws Exception {
//        Map<String, String> config = new HashMap<>();
//        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
//        NodeList vendors = doc.getElementsByTagName("vendor");
//
//        for (int i = 0; i < vendors.getLength(); i++) {
//            Element v = (Element) vendors.item(i);
//            if (v.getAttribute("name").equalsIgnoreCase(vendorName)) {
//                Element monitors = (Element) v.getElementsByTagName("monitors").item(0);
//                config.put("api", monitors.getAttribute("api"));
//                // CRITICAL: Capture the root-path (e.g., "$.tests")
//                config.put("rootPath", monitors.getAttribute("root-path"));
//                break;
//            }
//        }
//        return config;
//    }
    public static Map<String, String> getVendorMonitorConfig(String path, String vendorName) throws Exception {
        Map<String, String> config = new HashMap<>();

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new File(path));
        doc.getDocumentElement().normalize();

        NodeList vendors = doc.getElementsByTagName("vendor");

        for (int i = 0; i < vendors.getLength(); i++) {
            Element v = (Element) vendors.item(i);

            if (!v.getAttribute("name").equalsIgnoreCase(vendorName)) continue;

            // ✅ READ <api> ELEMENT (FIX)
            Node apiNode = v.getElementsByTagName("api").item(0);
            if (apiNode != null) {
                config.put("api", apiNode.getTextContent().trim());
            }

            // ✅ READ <monitors> ATTRIBUTES
            Element monitors = (Element) v.getElementsByTagName("monitors").item(0);
            if (monitors != null) {
                config.put("endpoint", monitors.getAttribute("endpoint"));
                config.put("rootPath", monitors.getAttribute("root-path"));
            }
            break;
        }
        return config;
    }

    public static List<String> getRequiredCredentials(String path, String vendorName) throws Exception {
        List<String> requirements = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        NodeList vendors = doc.getElementsByTagName("vendor");

        for (int i = 0; i < vendors.getLength(); i++) {
            Element v = (Element) vendors.item(i);
            if (v.getAttribute("name").equalsIgnoreCase(vendorName)) {
                NodeList keys = v.getElementsByTagName("key");
                for (int j = 0; j < keys.getLength(); j++) {
                    requirements.add(((Element) keys.item(j)).getAttribute("id"));
                }
            }
        }
        return requirements;
    }

    private static String getTag(Element e, String tag) {
        NodeList nl = e.getElementsByTagName(tag);
        return (nl.getLength() > 0) ? nl.item(0).getTextContent().trim() : "";
    }
    /**
     * Reads dynamic settings like base-url or accounts-url from the XML.
     */
    public static String getSetting(String path, String tagName) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
        doc.getDocumentElement().normalize();
        
        NodeList settings = doc.getElementsByTagName(tagName);
        if (settings.getLength() > 0) {
            return settings.item(0).getTextContent().trim();
        }
        return ""; 
    }
}