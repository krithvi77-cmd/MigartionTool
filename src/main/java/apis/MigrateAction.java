package apis;

import java.io.BufferedReader;
import java.io.IOException;
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
import net.minidev.json.JSONObject;

/**
 * Servlet implementation class MigrateAction
 */
@WebServlet("/MigrateAction")
public class MigrateAction extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MigrateAction() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
//		doGet(request, response);
		response.setContentType("application/json");
	    org.json.JSONArray results = new org.json.JSONArray();

	    try {
	        // 1. Read the array of monitors from the frontend
	        StringBuilder sb = new StringBuilder();
	        String line;
	        try (java.io.BufferedReader reader = request.getReader()) {
	            while ((line = reader.readLine()) != null)
	                sb.append(line);
	        }
	        org.json.JSONArray monitorsToCreate = new org.json.JSONArray(sb.toString());

	        // 2. Retrieve Session Credentials
	        HttpSession session = request.getSession();
	        JSONObject s247 = (JSONObject) session.getAttribute("s247_creds");

	        if (s247 == null) {
	            response.setStatus(401);
	            response.getWriter().write("{\"error\":\"Session expired. Please re-authenticate.\"}");
	            return;
	        }

	        // =================================================================================
	        // ✅ STEP 1: LOAD DYNAMIC SETTINGS FROM XML (REPLACES HARDCODING)
	        // =================================================================================
	        String mapXml = getServletContext().getRealPath("/WEB-INF/site24x7-mappings.xml");
	        String targetBaseUrl = framework.XmlLoader.getSetting(mapXml, "base-url");
	        String accountsUrl = framework.XmlLoader.getSetting(mapXml, "accounts-url");

	        // 3. SMART TOKEN CHECK
	        String token;
	        Long expiry = (Long) session.getAttribute("s247_token_expiry");

	        if (expiry != null && System.currentTimeMillis() < expiry) {
	            token = (String) session.getAttribute("s247_token");
	        } else {
	            // ✅ FIXED: Added 'accountsUrl' as the first parameter to fix the compilation error
	            token = AuthService.getSite24x7AccessToken(
	                    accountsUrl,
	                    (String) s247.get("client_id"),
	                    (String) s247.get("client_secret"), 
	                    (String) s247.get("refresh_token")
	            );
	            session.setAttribute("s247_token", token);
	            session.setAttribute("s247_token_expiry", System.currentTimeMillis() + (3600 * 1000));
	        }

	        // 4. Migration Loop
	        // ✅ FIXED: Use targetBaseUrl + "/monitors" instead of hardcoded .in URL
	        String migrationUrl = targetBaseUrl.replaceAll("/$", "") + "/monitors";

	        for (int i = 0; i < monitorsToCreate.length(); i++) {
	            org.json.JSONObject monitor = monitorsToCreate.getJSONObject(i);
	            org.json.JSONObject status = new org.json.JSONObject();
	            status.put("name", monitor.optString("display_name", "Unknown"));

	            try {
	                // ✅ FIXED: Post to dynamic migrationUrl
	                GenericClient.post(migrationUrl, Map.of("Authorization", token),
	                        monitor.toString());
	                status.put("status", "SUCCESS");
	            } catch (Exception e) {
	                status.put("status", "FAILED");
	                status.put("reason", e.getMessage().replace("\"", "'"));
	            }
	            results.put(status);
	        }

	        response.getWriter().write(results.toString());

	    } catch (Exception e) {
	        response.setStatus(500);
	        response.getWriter()
	                .write("{\"error\":\"Migration failed: " + e.getMessage().replace("\"", "'") + "\"}");
	    }
	}
}
