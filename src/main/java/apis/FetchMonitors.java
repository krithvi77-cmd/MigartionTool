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
import framework.MainEngine;
import net.minidev.json.JSONObject;

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
		response.setContentType("application/json");
	    try {
	        JSONObject input = parseRequest(request);
	        String vendor = (String) input.get("vendor");
	        
	        // Retrieve Site24x7 credentials from Session
	        HttpSession session = request.getSession();
	        JSONObject s247 = (JSONObject) session.getAttribute("s247_creds");
	        
	        if (s247 == null) {
	            response.setStatus(401);
	            response.getWriter().write("{\"error\":\"Site24x7 session expired. Please re-authenticate.\"}");
	            return;
	        }

	        // 1. Get fresh Token
	        String token = AuthService.getSite24x7AccessToken(
	            (String)s247.get("client_id"), (String)s247.get("client_secret"), (String)s247.get("refresh_token")
	        );

	        // 2. Map Vendor Headers (exclude the 'vendor' key)
	        Map<String, String> srcHeaders = new HashMap<>();
	        input.forEach((k, v) -> { 
	            if(!k.equals("vendor")) srcHeaders.put(k, v.toString()); 
	        });

	        Map<String, String> targetHeaders = Map.of("Authorization", token);

	        // 3. Run "Blind" Engine
	        MainEngine engine = new MainEngine();
	        List<Map<String, Object>> readyJson = engine.fetchAndPrepare(
	            vendor, srcHeaders, 
	            getServletContext().getRealPath("/WEB-INF/normalization.xml"),
	            getServletContext().getRealPath("/WEB-INF/site24x7-mappings.xml"),
	            "https://www.site24x7.com/api", targetHeaders
	        );

	        response.getWriter().write(new org.json.JSONArray(readyJson).toString());
	    } catch (Exception e) {
	        response.setStatus(500);
	        response.getWriter().write("{\"error\":\"Fetch failed: " + e.getMessage() + "\"}");
	    }
	}
	private JSONObject parseRequest(HttpServletRequest request) throws Exception {
	    StringBuilder sb = new StringBuilder();
	    String line;
	    try (java.io.BufferedReader reader = request.getReader()) {
	        while ((line = reader.readLine()) != null) sb.append(line);
	    }
	    return (JSONObject) new net.minidev.json.parser.JSONParser(net.minidev.json.parser.JSONParser.MODE_JSON_SIMPLE).parse(sb.toString());
	}
}
