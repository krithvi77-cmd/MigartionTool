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
		response.setContentType("application/json");
	    org.json.JSONArray results = new org.json.JSONArray();
	    
	    try {
	        // Read the array of monitors from the frontend
	        StringBuilder sb = new StringBuilder();
	        String line;
	        try (java.io.BufferedReader reader = request.getReader()) {
	            while ((line = reader.readLine()) != null) sb.append(line);
	        }
	        org.json.JSONArray monitorsToCreate = new org.json.JSONArray(sb.toString());
	        
	        HttpSession session = request.getSession();
	        JSONObject s247 = (JSONObject) session.getAttribute("s247_creds");
	        
	        String token = AuthService.getSite24x7AccessToken(
	            (String)s247.get("client_id"), (String)s247.get("client_secret"), (String)s247.get("refresh_token")
	        );

	        // Migration Loop
	        for (int i = 0; i < monitorsToCreate.length(); i++) {
	            org.json.JSONObject monitor = monitorsToCreate.getJSONObject(i);
	            org.json.JSONObject status = new org.json.JSONObject();
	            status.put("name", monitor.optString("display_name", "Unknown"));

	            try {
	                GenericClient.post("https://www.site24x7.com/api/monitors", 
	                                   Map.of("Authorization", token), 
	                                   monitor.toString());
	                status.put("status", "SUCCESS");
	            } catch (Exception e) {
	                status.put("status", "FAILED");
	                status.put("reason", e.getMessage());
	            }
	            results.put(status);
	        }
	        response.getWriter().write(results.toString());

	    } catch (Exception e) {
	        response.setStatus(500);
	        response.getWriter().write("{\"error\":\"Migration process failed: " + e.getMessage() + "\"}");
	    }
	}
}
