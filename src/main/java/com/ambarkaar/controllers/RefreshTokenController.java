package com.ambarkaar.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ambarkaar.database.SalesforceTokenDatabase;

@Controller
public class RefreshTokenController{
	
	
	
	public String instanceUrl = null;
	public String refreshToken = null;
	public String signature = null;
	public String scope = null;
	public String idToken = null;
	public String salesforceId = null;
	public String tokenType = null;
	public String issuedAt = null;
	public String accessToken = null;
	public String userId = null;
	
	public InputStream input = null;
	
	public SalesforceTokenDatabase salesforceTokenDatabase;
	public Properties prop;

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	// clientId is 'Consumer Key' in the Remote Access UI
	private static String clientId = "3MVG9d8..z.hDcPIPOIEZ7R2UidmcKgSXNgDPcOR6MsnWAocIeqy3mn6myBEOFXG16ez97tETbA6GxXUN0JyX";
	// clientSecret is 'Consumer Secret' in the Remote Access UI
	private static String clientSecret = "3120272276966716656";
	// This must be identical to 'Callback URL' in the Remote Access UI
	private static String redirectUri = "http://localhost:8090/auth/salesforce/callback";
	private static String environment = "https://login.salesforce.com";
	private String authUrl = null;
	private String tokenUrl = null;
	
	@RequestMapping(value = "/auth/salesforce")
	public String authenticate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		try {
			prop = new Properties();
			String filename = "config.properties";
			input = this.getClass().getClassLoader().getResourceAsStream(filename);
			
			if(input==null){
	            System.out.println("Sorry, unable to find " + filename);
			    return null;
			}
			
			prop.load(input);
			String salesforceClientId = request.getParameter("clientId");
			String salesforceClientSecret = request.getParameter("clientSecret");
			
			if(salesforceClientId != null && !salesforceClientId.isEmpty() && salesforceClientSecret != null && !salesforceClientSecret.isEmpty()){
				if((prop.getProperty("clientId").equals(salesforceClientId) && prop.getProperty("clientSecret").equals(salesforceClientSecret))) {
					
					String accessToken = (String) request.getSession().getAttribute(ACCESS_TOKEN);
					
					if(accessToken == null) {
					
						try {
							authUrl = environment
									+ "/services/oauth2/authorize?response_type=code&client_id="
									+ clientId + "&redirect_uri="
									+ URLEncoder.encode(redirectUri, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							throw new ServletException(e);
						}
				
						return "redirect:" + authUrl;
					} else {
						return "redirect:/auth/salesforce/callback";
					}
					
				} else {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Client Id or Client Secret");
					return null;
				}
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "No Client Id, Client Secret or Org Id");
				return null;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return null;
		
	}
	
	@RequestMapping(value = "/refreshToken")
	public void refreshToken(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException  {
		prop = new Properties();
		InputStream input = null;
		
		try {
			String filename = "config.properties";
			input = this.getClass().getClassLoader().getResourceAsStream(filename);
			
			if(input==null){
	            System.out.println("Sorry, unable to find " + filename);
			    return;
			}
			
			prop.load(input);
			String salesforceClientId = request.getParameter("clientId");
			String salesforceClientSecret = request.getParameter("clientSecret");
			String salesforceRefreshToken = request.getParameter("refreshToken");
			
			if(salesforceClientId != null && !salesforceClientId.isEmpty() && salesforceClientSecret != null && !salesforceClientSecret.isEmpty() && salesforceRefreshToken != null && !salesforceRefreshToken.isEmpty()){
				if((prop.getProperty("clientId").equals(salesforceClientId) && prop.getProperty("clientSecret").equals(salesforceClientSecret))) {
					tokenUrl = environment + "/services/oauth2/token";
					
					HttpClient httpclient = new HttpClient();
					
					PostMethod post = new PostMethod(tokenUrl);
					post.addParameter("grant_type", "refresh_token");
					post.addParameter("client_id", clientId);
					post.addParameter("refresh_token", salesforceRefreshToken);
					
					try {
						httpclient.executeMethod(post);

						try {
							JSONObject authResponse = new JSONObject(
									new JSONTokener(new InputStreamReader(
											post.getResponseBodyAsStream())));
							System.out.println("Auth response: "
									+ authResponse.toString(2));

							accessToken = authResponse.getString("access_token");
							
							salesforceTokenDatabase = new SalesforceTokenDatabase();
							
							salesforceTokenDatabase.updateAccessTokenUsingRefreshToken(accessToken, salesforceRefreshToken);
							
							ResultSet resultSet = salesforceTokenDatabase.getByRefreshToken(salesforceRefreshToken);
							
							resultSet.next();
							
							JSONObject jsonObject = this.convertResultSetToJSON(resultSet);
							response.setContentType("application/json");
							PrintWriter out = response.getWriter();
							out.print(jsonObject);
							out.flush();
							
						} catch (JSONException | SQLException e) {
							e.printStackTrace();
							throw new ServletException(e);
						}
					} finally {
						post.releaseConnection();
					}
					
				} else {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Client Id or Client Secret");
				}
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "No Client Id or Client Secret");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	@RequestMapping(value = "/auth/salesforce/callback")
	@ResponseBody
	public String authenticateCallback(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
		accessToken = (String) request.getSession().getAttribute(ACCESS_TOKEN);
		
		if(accessToken == null) {
			instanceUrl = null;
			
			tokenUrl = environment + "/services/oauth2/token";
			
			String code = request.getParameter("code");

			HttpClient httpclient = new HttpClient();

			PostMethod post = new PostMethod(tokenUrl);
			post.addParameter("code", code);
			post.addParameter("grant_type", "authorization_code");
			post.addParameter("client_id", clientId);
			post.addParameter("client_secret", clientSecret);
			post.addParameter("redirect_uri", redirectUri);

			try {
				httpclient.executeMethod(post);

				try {
					JSONObject authResponse = new JSONObject(
							new JSONTokener(new InputStreamReader(
									post.getResponseBodyAsStream())));
					System.out.println("Auth response: "
							+ authResponse.toString(2));

					accessToken = authResponse.getString("access_token");
					instanceUrl = authResponse.getString("instance_url");
					refreshToken = authResponse.getString("refresh_token");
					signature = authResponse.getString("signature");
					scope = authResponse.getString("scope");
					idToken = authResponse.getString("id_token");
					salesforceId = authResponse.getString("id");
					tokenType = authResponse.getString("token_type");
					issuedAt = authResponse.getString("issued_at");
					
					String[] tokens = salesforceId.split("/");
					String orgId = tokens[4];
					String userId = tokens[5];
					
					salesforceTokenDatabase = new SalesforceTokenDatabase();
					
					ResultSet resultSet = salesforceTokenDatabase.getByOrgIdAndUserId(orgId, userId);
					
					if(resultSet.next()) {
						salesforceTokenDatabase.updateAccessTokenUsingRefreshToken(accessToken, refreshToken);
					}else {
						salesforceTokenDatabase.insertIntoDatabase(accessToken, instanceUrl, refreshToken, signature, scope, idToken, salesforceId, tokenType, issuedAt, orgId, userId);
					}
					
					ResultSet newResultSet = salesforceTokenDatabase.getByRefreshToken(refreshToken);
					
					newResultSet.next();

					JSONObject jsonObject = this.convertResultSetToJSON(resultSet);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.print(jsonObject);
					out.flush();
					
				} catch (JSONException | SQLException e) {
					e.printStackTrace();
					throw new ServletException(e);
				}
			} finally {
				post.releaseConnection();
			}
			// Set a session attribute so that other servlets can get the access
			// token
			request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
			
			// We also get the instance URL from the OAuth response, so set it
			// in the session too
			request.getSession().setAttribute(INSTANCE_URL, instanceUrl);
		}

		return "Logged in! <a href=\"/logout\">Log out</a> | "
				+ "<a href=\"/accounts\">Get accounts</a> | "
				+ "Got access token: " + accessToken;
	}
	
	@RequestMapping(value = "/returnToken")
	public String returnToken(HttpServletRequest request,HttpServletResponse response) {
		prop = new Properties();
		InputStream input = null;
		
		try {
			String filename = "config.properties";
			input = this.getClass().getClassLoader().getResourceAsStream(filename);
			
			if(input==null){
	            System.out.println("Sorry, unable to find " + filename);
			    return null;
			}
			
			prop.load(input);
			String salesforceClientId = request.getParameter("clientId");
			String salesforceClientSecret = request.getParameter("clientSecret");
			String salesforceOrgId = request.getParameter("orgId");
			String salesforceUserId = request.getParameter("userId");
			
			if(salesforceClientId != null && !salesforceClientId.isEmpty() && salesforceClientSecret != null && !salesforceClientSecret.isEmpty() && salesforceOrgId != null && !salesforceOrgId.isEmpty()){
				if((prop.getProperty("clientId").equals(salesforceClientId) && prop.getProperty("clientSecret").equals(salesforceClientSecret))) {
					
					salesforceTokenDatabase = new SalesforceTokenDatabase();
					
					try {
						ResultSet resultSet = null;
						int resultSetSize = 0;
						if(userId != null && !userId.isEmpty()) {
							resultSet = salesforceTokenDatabase.getByOrgIdAndUserId(salesforceOrgId, salesforceUserId);
						} else {
							resultSet = salesforceTokenDatabase.getByOrgId(salesforceOrgId);
							resultSetSize = salesforceTokenDatabase.countOrgIdData(salesforceOrgId);
						}
						
						if(!resultSet.next()) {
							response.sendError(HttpServletResponse.SC_NOT_FOUND, "No record found");
						}else if(resultSetSize > 1){
							response.sendError(HttpServletResponse.SC_NOT_FOUND, "Provide User Id");
						}else {
							Date expiryDate = new Date(Long.parseLong(resultSet.getString("issued_at")) * 1000 + 30 * 60 * 1000);
							Date currentDate = new Date();
							if(currentDate.compareTo(expiryDate) > 0) {
								return "redirect:/auth/salesforce/callback";
							}else {
								JSONObject jsonObject = this.convertResultSetToJSON(resultSet);
								response.setContentType("application/json");
								PrintWriter out = response.getWriter();
								out.print(jsonObject);
								out.flush();
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					} 
					
				} else {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Client Id or Client Secret");
				}
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "No Client Id, Client Secret or Org Id");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public JSONObject convertResultSetToJSON(ResultSet resultSet){

		JSONObject obj = new JSONObject();
        try {
				obj.put("access_token", resultSet.getString("access_token"));
				obj.put("refresh_token", resultSet.getString("refresh_token"));
				obj.put("instance_url", resultSet.getString("instance_url"));
				obj.put("org_id", resultSet.getString("org_id"));
				obj.put("user_id", resultSet.getString("user_id"));
		} catch (SQLException | JSONException e) {
			e.printStackTrace();
		}
        return obj;
	}
	
	@RequestMapping(value = "/logout")
	@ResponseBody
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession();
		
		session.removeAttribute(ACCESS_TOKEN);
		session.removeAttribute(INSTANCE_URL);
		
		return "logged out! <a href=\"/auth/salesforce\">login</a>";
	}
}
	