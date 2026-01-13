package apis;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import framework.AuthService;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import static net.minidev.json.parser.JSONParser.MODE_JSON_SIMPLE;

/**
 * Servlet implementation class AuthSite24x7
 */
@WebServlet("/AuthSite24x7")
public class AuthSite24x7 extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AuthSite24x7() {
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
	        
	        // =================================================================================
	        // ✅ STEP 1: LOAD DYNAMIC SETTINGS FROM XML
	        // =================================================================================
	        String mapXml = getServletContext().getRealPath("/WEB-INF/site24x7-mappings.xml");
	        String accountsUrl = framework.XmlLoader.getSetting(mapXml, "accounts-url");

	        // =================================================================================
	        // ✅ STEP 2: PASS 4 PARAMETERS TO AUTH SERVICE (FIXES THE ERROR)
	        // =================================================================================
	        String token = AuthService.getSite24x7AccessToken(
	            accountsUrl, // New parameter
	            (String)input.get("client_id"), 
	            (String)input.get("client_secret"), 
	            (String)input.get("refresh_token")
	        );

	        // 2. Calculate Expiry (Current Time + 3600 seconds)
	        long expiryTime = System.currentTimeMillis() + (3600 * 1000);

	        // 3. Save everything to the Session
	        HttpSession session = request.getSession();
	        
	        session.setAttribute("s247_creds", input); 
	        session.setAttribute("s247_token", token);
	        session.setAttribute("s247_token_expiry", expiryTime);
	        
	        response.getWriter().write("{\"status\":\"authenticated\"}");
	    } catch (Exception e) {
	        response.setStatus(401);
	        response.getWriter().write("{\"error\":\"Auth failed: " + e.getMessage().replace("\"", "'") + "\"}");
	    }
	}

	private JSONObject parseRequest(HttpServletRequest request) throws Exception {
	    StringBuilder sb = new StringBuilder();
	    String line;
	    try (BufferedReader reader = request.getReader()) {
	        while ((line = reader.readLine()) != null) sb.append(line);
	    }
	    JSONParser parser = new JSONParser(MODE_JSON_SIMPLE);
	    return (JSONObject) parser.parse(sb.toString());	
	    }
}
