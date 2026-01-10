//package framework;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class Main {
//    public static void main(String[] args) {
//        try {
//            MainEngine engine = new MainEngine();
//
//            Map<String, String> datadogHeaders = new HashMap<>();
//            datadogHeaders.put("DD-API-KEY", "14d4592e16588a42da6646f3e4fd41ed");
//            datadogHeaders.put("DD-APPLICATION-KEY", "4a7e1cc803fe054af1fd160649a3ed261b72eed4");
//
//         
//            Map<String, String> site247Headers = new HashMap<>(); 
//            site247Headers.put("Authorization", "Zoho-oauthtoken 1000.5cd924868cd935a3268e19d9da34b014.f3b30b97499d3e26854d4d1d6924e864");
//
//            String normXml = "src/normalization-rules.xml";
//            String mapXml = "src/site24x7-mappings.xml";
//            String vendor = "datadog";
//            
//            String ddUrl = "https://api.us5.datadoghq.com/api/v1/synthetics/tests";
//            String s247BaseUrl = "https://www.site24x7.in/api";
//
//            System.out.println(">>> Starting Migration Framework...");
//
//            engine.startMigration(
//                normXml, 
//                mapXml, 
//                vendor, 
//                ddUrl, 
//                datadogHeaders, 
//                s247BaseUrl, 
//                site247Headers
//                
//            );
//
//            System.out.println(">>> Migration process finished successfully.");
//
//        } catch (Exception e) {
//            System.err.println(">>> Migration failed!");
//            e.printStackTrace();
//        }
//    }
//}