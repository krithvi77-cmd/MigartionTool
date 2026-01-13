package apis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;

import framework.AuthService;
import framework.GenericClient;
import framework.MainEngine;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Servlet implementation class FetchMonitors
 */
@WebServlet("/FetchMonitors")
public class FetchMonitors extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FetchMonitors() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
//		doGet(request, response);
//		response.setContentType("application/json");
//	    try {
//	        JSONObject input = parseRequest(request);
//	        String vendor = (String) input.get("vendor");
//	        
//	        // Retrieve Site24x7 credentials from Session
//	        HttpSession session = request.getSession();
//	        JSONObject s247 = (JSONObject) session.getAttribute("s247_creds");
//	        
//	        if (s247 == null) {
//	            response.setStatus(401);
//	            response.getWriter().write("{\"error\":\"Site24x7 session expired. Please re-authenticate.\"}");
//	            return;
//	        }
//
//	        // 1. Get fresh Token
//	        
//	        long expiry = (long) session.getAttribute("s247_token_expiry");
//	        String token;
//
//	        if (System.currentTimeMillis() < expiry) {
//	            token = (String) session.getAttribute("s247_token"); // Use cached token
//	        } else {
//	            // Token expired! Refresh it
//	        	
//	        	token = AuthService.getSite24x7AccessToken(
//	    	            (String)s247.get("client_id"), (String)s247.get("client_secret"), (String)s247.get("refresh_token")
//	    	        );	            
//	            session.setAttribute("s247_token", token);
//	            session.setAttribute("s247_token_expiry", System.currentTimeMillis() + (3600 * 1000));
//	        }
//	        // 2. Map Vendor Headers (exclude the 'vendor' key)
//	        Map<String, String> srcHeaders = new HashMap<>();
//	        input.forEach((k, v) -> { 
//	            if(!k.equals("vendor")) srcHeaders.put(k, v.toString()); 
//	        });
//
//	        Map<String, String> targetHeaders = Map.of("Authorization", token);
//
//	        // 3. Run "Blind" Engine
//	        MainEngine engine = new MainEngine();
//	        List<Map<String, Object>> readyJson = engine.fetchAndPrepare(
//	            vendor, srcHeaders, 
//	            getServletContext().getRealPath("/WEB-INF/normalization-rules.xml"),
//	            getServletContext().getRealPath("/WEB-INF/site24x7-mappings.xml"),
//	            "https://www.site24x7.in/api", targetHeaders
//	        );
//
//	        response.getWriter().write(new org.json.JSONArray(readyJson).toString());
//	    } catch (Exception e) {
//	        response.setStatus(500);
//	        response.getWriter().write("{\"error\":\"Fetch failed: " + e.getMessage() + "\"}");
//	    }
		response.setContentType("application/json");
	    try {
	        // 1. Parse Input
	        JSONObject input = parseRequest(request);
	        String vendor = (String) input.get("vendor");
	        
	        // 2. Retrieve Session Credentials
	        HttpSession session = request.getSession();
	        JSONObject s247 = (JSONObject) session.getAttribute("s247_creds");
	        
	        if (s247 == null) {
	            response.setStatus(401);
	            writeError(response, "Site24x7 session expired. Please re-authenticate.");
	            return;
	        }

	        // =================================================================================
	        // ✅ STEP 1: LOAD DYNAMIC SETTINGS FROM XML (REPLACES HARDCODING)
	        // =================================================================================
	        String mapXml = getServletContext().getRealPath("/WEB-INF/site24x7-mappings.xml");
	        String normXml = getServletContext().getRealPath("/WEB-INF/normalization-rules.xml");

	        String targetBaseUrl = framework.XmlLoader.getSetting(mapXml, "base-url");
	        String accountsUrl = framework.XmlLoader.getSetting(mapXml, "accounts-url");

	        // 3. Token Management
	        String token;
	        Long expiry = (Long) session.getAttribute("s247_token_expiry");
	        if (expiry != null && System.currentTimeMillis() < expiry) {
	            token = (String) session.getAttribute("s247_token");
	        } else {
	            // ✅ STEP 2: USE DYNAMIC accountsUrl FOR AUTHENTICATION
	            token = AuthService.getSite24x7AccessToken(
	                accountsUrl, 
	                (String)s247.get("client_id"), 
	                (String)s247.get("client_secret"), 
	                (String)s247.get("refresh_token")
	            );
	            session.setAttribute("s247_token", token);
	            session.setAttribute("s247_token_expiry", System.currentTimeMillis() + (3600 * 1000));
	        }

	        // 4. Prepare Headers
	        Map<String, String> srcHeaders = new HashMap<>();
	        input.forEach((k, v) -> { 
	            if(!k.equals("vendor")) srcHeaders.put(k, v.toString()); 
	        });

	        Map<String, String> targetHeaders = Map.of("Authorization", token);

	        // =================================================================================
	        // ✅ STEP 3: USE DYNAMIC targetBaseUrl FOR METADATA FETCHING
	        // =================================================================================
	        
	        // A. Fetch Location Profiles (Replaced hardcoded .in URL)
	        String locUrl = targetBaseUrl + "/location_profiles";
	        String locJson = GenericClient.fetch(locUrl, targetHeaders);
	        List<Map<String, Object>> locations = com.jayway.jsonpath.JsonPath.read(locJson, "$.data");

	        // B. Fetch Notification Profiles (Replaced hardcoded .in URL)
	        String notifUrl = targetBaseUrl + "/notification_profiles";
	        String notifJson = GenericClient.fetch(notifUrl, targetHeaders);
	        List<Map<String, Object>> notifications = com.jayway.jsonpath.JsonPath.read(notifJson, "$.data");

	        // C. Fetch User Groups (Replaced hardcoded .in URL)
	        String groupUrl = targetBaseUrl + "/user_groups";
	        String groupJson = GenericClient.fetch(groupUrl, targetHeaders);
	        List<Map<String, Object>> groups = com.jayway.jsonpath.JsonPath.read(groupJson, "$.data");

	        // D. Run Main Engine for Monitors
	        MainEngine engine = new MainEngine();
	        List<Map<String, Object>> preparedMonitors = engine.fetchAndPrepare(
	            vendor, 
	            srcHeaders, 
	            normXml, 
	            mapXml, 
	            targetBaseUrl, // ✅ PASS DYNAMIC URL TO ENGINE
	            targetHeaders
	        );

	        // 5. Construct Final Wrapper Response
	        org.json.JSONObject finalResponse = new org.json.JSONObject();
	        finalResponse.put("monitors", preparedMonitors);
	        finalResponse.put("locations", locations);
	        finalResponse.put("notifications", notifications);
	        finalResponse.put("groups", groups);

	        response.getWriter().write(finalResponse.toString());

	    } catch (Exception e) {
	        response.setStatus(500);
	        e.printStackTrace(); 
	        writeError(response, "Fetch failed: " + e.getMessage());
	    }
    }

    private JSONObject parseRequest(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return (JSONObject) new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(sb.toString());
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", message);
        response.getWriter().write(errorJson.toString());
    }
}
