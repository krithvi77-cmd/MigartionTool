package apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import framework.XmlLoader;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Servlet implementation class getvendorname
 */
@WebServlet("/getvendorname")
public class getvendorname extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public getvendorname() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
//		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
		// TODO Auto-generated method stub

		response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");

	    try {
	        // 2. Read Request Body
	        StringBuilder body = new StringBuilder();
	        String line;
	        try (BufferedReader reader = request.getReader()) {
	            while ((line = reader.readLine()) != null) {
	                body.append(line);
	            }
	        }

	        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
	        JSONObject input = (JSONObject) parser.parse(body.toString());

	        String vendorName = (String) input.get("vendor");
	        
	        if (vendorName == null || vendorName.isEmpty()) {
	            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	            return;
	        }

	        // 3. Load Requirements from XML
	        String path = getServletContext().getRealPath("/WEB-INF/normalization-rules.xml");
	        List<String> requirements = XmlLoader.getRequiredCredentials(path, vendorName);

	        // 4. Check if vendor exists
	        if (requirements.isEmpty()) {
	            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	            JSONObject error = new JSONObject();
	            error.put("message", "Vendor not found in configuration.");
	            response.getWriter().write(error.toString());
	            return;
	        }

	        // 5. Send Response (Clean and simple)
	        response.getWriter().write(new JSONArray(requirements).toString());

	    } catch (Exception e) {
	        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        // Important: Use a safe error message format
	        response.getWriter().write("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
	        e.printStackTrace();
	    }
	
	}

}
