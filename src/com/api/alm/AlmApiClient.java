package com.api.alm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.client.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.*;

public class AlmApiClient
{
	private final String LWSSO_COOKIE_KEY_NAME = "LWSSO_COOKIE_KEY";
	private final int PAGE_SIZE = 100;
	private HttpClient httpClient;
	private String proxyHost;
	private int proxyPort;
	private String agmAuthenticateUrl;
	private String agmProjectsUrl;
	private String agmApplicationName;
	private String lwSsoCookieKey;
	
	public AlmApiClient(String agmAuthenticateUrl, String agmProjectsUrl, String agmApplicationName)
	{
		this.agmAuthenticateUrl = agmAuthenticateUrl;
		this.agmProjectsUrl = agmProjectsUrl;
		this.agmApplicationName = agmApplicationName;
	}
	
	public AlmApiClient(String agmAuthenticateUrl, String agmProjectsUrl, String agmApplicationName, String proxyHost, int proxyPort)
	{
		this(agmAuthenticateUrl, agmProjectsUrl, agmApplicationName);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	
	public boolean authenticate(String username, String password) 
	{
		  this.httpClient = CreateHttpClient();
		  HttpPost post = new HttpPost(agmAuthenticateUrl);
	      try
	      {
	    	  if (proxyHost != null)
	    	  {
	    		  HttpHost httpProxy = new HttpHost(proxyHost, proxyPort, "http");
	    		  RequestConfig config = RequestConfig.custom().setProxy(httpProxy).build();
	    		  post.setConfig(config);
	    	  }
	    	  post.setEntity(new StringEntity("LWAP_REQ=&MULTI_DOMAIN_REQ=&hashlink=&urlToken=false&username=" + URLEncoder.encode(username,  "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8")));
	    	  post.addHeader("Content-Type","application/x-www-form-urlencoded");
	    	  HttpResponse resp = httpClient.execute(post);
	      }
	      catch (IOException e)
	      {
	    	  // TODO Auto-generated catch block
	    	  e.printStackTrace();
	      }
	      return true;
	}
	
	public ArrayList<AlmDefect> getDefects(String query)
	{
		String entityType = "defects";
		ArrayList<AlmDefect> defectsList = new ArrayList<AlmDefect>();
		String response = getEntities(agmApplicationName, entityType, PAGE_SIZE, 1, query);
		if (!response.equals(""))
		{
			JSONObject responseJson = new JSONObject(response);
			int totalResults = responseJson.getInt("TotalResults");
			extractDefects(responseJson, defectsList);
			for (int i = 1; i * PAGE_SIZE < totalResults; i++)
			{
				response = getEntities(agmApplicationName, entityType, PAGE_SIZE, i * PAGE_SIZE + 1, query);
				responseJson = new JSONObject(response);
				extractDefects(responseJson, defectsList);
			}
			if (defectsList.size() != totalResults) return new ArrayList<AlmDefect>();
		}
		return defectsList;
	}
	
	private void extractDefects(JSONObject response, List<AlmDefect> defects)
	{
		JSONArray defectsJSON = response.getJSONArray("entities");
		for (Object defectJSON : defectsJSON)
		{
			AlmDefect defect = new AlmDefect();
			JSONArray fields = ((JSONObject) defectJSON).getJSONArray("Fields");
			for (Object field : fields)
			{
				String propertyKey = ((JSONObject) field).getString("Name");
				JSONArray propertyValuesArray = ((JSONObject) field).getJSONArray("values");
				if (propertyValuesArray.length() > 0)
				{
					JSONObject propertyValueObject = propertyValuesArray.getJSONObject(0);
					if (propertyValueObject.length() > 0)
					{
						String propertyValue = propertyValueObject.getString("value");
						String propertyReferenceValue = null;
						if (propertyValueObject.length() > 1) propertyReferenceValue = propertyValueObject.getString("ReferenceValue");
						switch (propertyKey)
						{
							case "id" : defect.id = propertyValue; break;
							case "name" : defect.name = propertyValue; break;
							case "severity" : defect.severity = propertyValue; break;
							case "status" : defect.status = propertyValue; break;
							case "detected-in-rel" : defect.detected_in_rel = propertyReferenceValue; break;
							case "target-rel" : defect.target_rel = propertyReferenceValue; break;
							case "user-16" : defect.user_16_subarea = propertyValue; break;
							case "owner" : defect.owner_dev = propertyValue; break;
							case "detected-by" : defect.detected_by_qa = propertyValue; break;
							case "user-02" : defect.user_02_qalead = propertyValue; break;
							case "dev-comments" :  String[] comments = propertyValue.split("<span style=\"font-size:8pt\"><strong>________________________________________</strong></span>");
												   for (String comment : comments)
												   {
													   comment = comment.replaceAll("\\<.*?>","").replace("&nbsp;", "");
													   int ltIndex = comment.indexOf("&lt;");
													   if (ltIndex > 0)
													   {
														   comment = comment.substring(ltIndex, comment.length());
														   defect.comments.add(comment);
													   }
												   }
												   break;
							case "last-modified" : defect.last_modified = AlmHelper.getDateTimeFromString(propertyValue); break;
							default: break;
						}
					}
				}
			}
			defects.add(defect);
		}
	}
	
	public ArrayList<AlmRequirement> getRequirements(String query)
	{
		String entityType = "requirements";
		ArrayList<AlmRequirement> requirementsList = new ArrayList<AlmRequirement>();
		String response = getEntities(agmApplicationName, entityType, PAGE_SIZE, 1, query);
		if (!response.equals(""))
		{
			JSONObject responseJson = new JSONObject(response);
			int totalResults = responseJson.getInt("TotalResults");
			extractRequirements(responseJson, requirementsList);
			for (int i = 1; i * PAGE_SIZE < totalResults; i++)
			{
				response = getEntities(agmApplicationName, entityType, PAGE_SIZE, i * PAGE_SIZE + 1, query);
				responseJson = new JSONObject(response);
				extractRequirements(responseJson, requirementsList);
			}
			if (requirementsList.size() != totalResults) return new ArrayList<AlmRequirement>();
		}
		return requirementsList;
	}
	
	private void extractRequirements(JSONObject response, ArrayList<AlmRequirement> requirementsList)
	{
		JSONArray requirementsJSON = response.getJSONArray("entities");
		for (Object requierementJSON : requirementsJSON)
		{
			AlmRequirement requirement = new AlmRequirement();
			JSONArray fields = ((JSONObject) requierementJSON).getJSONArray("Fields");
			for (Object field : fields)
			{
				String propertyKey = ((JSONObject) field).getString("Name");
				JSONArray propertyValuesArray = ((JSONObject) field).getJSONArray("values");
				if (propertyValuesArray.length() > 0)
				{
					JSONObject propertyValueObject = propertyValuesArray.getJSONObject(0);
					if (propertyValueObject.length() > 0)
					{
						String propertyValue = propertyValueObject.getString("value");
						String propertyReferenceValue = null;
						if (propertyValueObject.length() > 1) propertyReferenceValue = propertyValueObject.getString("ReferenceValue");
						switch (propertyKey)
						{
							case "id" : requirement.id = propertyValue; break;
							case "name" : requirement.name = propertyValue; break;
							case "target-rel" : requirement.target_release = propertyReferenceValue;
							case "cover-status" : requirement.cover_status = propertyValue; break;
							case "user-04" : requirement.user_04_subarea = propertyValue; break;
							case "owner" : requirement.owner_author = propertyValue; break;
							case "user-03" : requirement.user_03_qalead = propertyValue; break;
							case "last-modified" : requirement.last_modified = AlmHelper.getDateTimeFromString(propertyValue); break;
							default: break;
						}
					}
				}
			}
			requirementsList.add(requirement);
		}
	}

	public ArrayList<AlmRBIRequirement> getRBIRequirements(String query)
	{
		String entityType = "release-backlog-items";
		ArrayList<AlmRBIRequirement> requirementsList = new ArrayList<AlmRBIRequirement>();
		String response = getEntities(agmApplicationName, entityType, PAGE_SIZE, 1, "entity-type[requirement];" + query);
		if (!response.equals(""))
		{
			JSONObject responseJson = new JSONObject(response);
			int totalResults = responseJson.getInt("TotalResults");
			extractRBIRequirements(responseJson, requirementsList);
			for (int i = 1; i * PAGE_SIZE < totalResults; i++)
			{
				response = getEntities(agmApplicationName, entityType, PAGE_SIZE, i * PAGE_SIZE + 1, query);
				responseJson = new JSONObject(response);
				extractRBIRequirements(responseJson, requirementsList);
			}
			if (requirementsList.size() != totalResults) return new ArrayList<AlmRBIRequirement>();
		}
		return requirementsList;
	}
	
	private void extractRBIRequirements(JSONObject response, ArrayList<AlmRBIRequirement> rbiRequirementsList)
	{
		JSONArray rbiRequirementsJSON = response.getJSONArray("entities");
		for (Object rbiRequierementJSON : rbiRequirementsJSON)
		{
			AlmRBIRequirement rbiRequirement = new AlmRBIRequirement();
			JSONArray fields = ((JSONObject) rbiRequierementJSON).getJSONArray("Fields");
			for (Object field : fields)
			{
				String propertyKey = ((JSONObject) field).getString("Name");
				JSONArray propertyValuesArray = ((JSONObject) field).getJSONArray("values");
				if (propertyValuesArray.length() > 0)
				{
					JSONObject propertyValueObject = propertyValuesArray.getJSONObject(0);
					if (propertyValueObject.length() > 0)
					{
						String propertyValue = propertyValueObject.getString("value");
						String propertyReferenceValue = null;
						if (propertyValueObject.length() > 1) propertyReferenceValue = propertyValueObject.getString("ReferenceValue");
						switch (propertyKey)
						{
							case "entity-id" : rbiRequirement.entity_id = propertyValue; break;
							case "id" : rbiRequirement.id = propertyValue; break;
							case "entity-name" : rbiRequirement.entity_name = propertyValue; break;
							case "status" : rbiRequirement.status = propertyValue; break;
							case "owner" : rbiRequirement.owner_dev = propertyValue; break;
							//case "last-modified" : almRBIRequirement.last_modified = AlmHelper.getDateTimeFromString(propertyValue); break;
							default: break;
						}
					}
				}
			}
			rbiRequirementsList.add(rbiRequirement);
		}
	}
	
	public ArrayList<AlmReleaseBacklogItem> getReleaseBacklogItems(String query)
	{
		String entityType = "release-backlog-items";
		ArrayList<AlmReleaseBacklogItem> releaseBacklogItemsList = new ArrayList<AlmReleaseBacklogItem>();
		String response = getEntities(agmApplicationName, entityType, PAGE_SIZE, 1, query);
		if (!response.equals(""))
		{
			JSONObject responseJson = new JSONObject(response);
			int totalResults = responseJson.getInt("TotalResults");
			extractReleaseBacklogItems(responseJson, releaseBacklogItemsList);
			for (int i = 1; i * PAGE_SIZE < totalResults; i++)
			{
				response = getEntities(agmApplicationName, entityType, PAGE_SIZE, i * PAGE_SIZE + 1, query);
				responseJson = new JSONObject(response);
				extractReleaseBacklogItems(responseJson, releaseBacklogItemsList);
			}
			if (releaseBacklogItemsList.size() != totalResults) return new ArrayList<AlmReleaseBacklogItem>();
		}
		return releaseBacklogItemsList;
	}
	
	private void extractReleaseBacklogItems(JSONObject response, List<AlmReleaseBacklogItem> releaseBacklogItemsList)
	{
		JSONArray releaseBacklogItemsJSON = response.getJSONArray("entities");
		for (Object releaseBacklogItemJSON : releaseBacklogItemsJSON)
		{
			AlmReleaseBacklogItem almReleaseBacklogItem = new AlmReleaseBacklogItem();
			JSONArray fields = ((JSONObject) releaseBacklogItemJSON).getJSONArray("Fields");
			for (Object field : fields)
			{
				String propertyKey = ((JSONObject) field).getString("Name");
				JSONArray propertyValuesArray = ((JSONObject) field).getJSONArray("values");
				if (propertyValuesArray.length() > 0)
				{
					JSONObject propertyValueObject = propertyValuesArray.getJSONObject(0);
					if (propertyValueObject.length() > 0)
					{
						String propertyValue = propertyValueObject.getString("value");
						String propertyReferenceValue = null;
						if (propertyValueObject.length() > 1) propertyReferenceValue = propertyValueObject.getString("ReferenceValue");
						switch (propertyKey)
						{
							case "entity-id" : almReleaseBacklogItem.entity_id = propertyValue; break;
							case "entity-type" : almReleaseBacklogItem.entity_type = propertyValue; break;
							case "release-id" : almReleaseBacklogItem.release_id = propertyValue; break;
							case "entity-name" : almReleaseBacklogItem.entity_name = propertyValue; break;
							case "status" : almReleaseBacklogItem.status = propertyValue; break;
							case "id" : almReleaseBacklogItem.id = propertyValue; break;
							case "theme-id" : almReleaseBacklogItem.theme_id = propertyValue; break;
							case "team-id" : almReleaseBacklogItem.team_id = propertyValue; break;
							case "owner" : almReleaseBacklogItem.owner = propertyValue; break;
							case "feature-id" : almReleaseBacklogItem.feature_id = propertyValue; break;
							case "product-id" : almReleaseBacklogItem.product_id = propertyValue; break;
							default: break;
						}
					}
				}
			}
			releaseBacklogItemsList.add(almReleaseBacklogItem);
		}
	}
	
	private String getEntities(String applicationName, String entityType, int pageSize, int startIndex, String queryParameter)
	{
		try
		{
			String urlStr = agmProjectsUrl + "/" + applicationName + "/" + entityType + "?" + "query=" + URLEncoder.encode("{" + queryParameter + "}", "UTF-8") + "&start-index=" + startIndex + "&page-size=" + pageSize;
			URL authenticateUrl = new URL(urlStr);
			HttpsURLConnection connection = null;
			if (proxyHost != null)
			{
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				connection = (HttpsURLConnection) authenticateUrl.openConnection(proxy);
			}
			else
			{
				connection = (HttpsURLConnection) authenticateUrl.openConnection();
			}
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Cookie", this.lwSsoCookieKey);
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
	    	  
			int responseCode = connection.getResponseCode();
			if (responseCode == 200)
			{
				StringBuffer response = new StringBuffer();
				try (BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream())))
				{		
					String line = null;
					while ((line = input.readLine()) != null) 
					{
						response.append(line);
					}
				}
				return response.toString();
			}
		}
		catch (Exception ex)
		{
			
		}
		return "";
	}
	
	private CloseableHttpClient CreateHttpClient()
	{
		CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy() 
		{   
	          public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws org.apache.http.ProtocolException  
	          {
	              boolean isRedirect=false;
	              isRedirect = super.isRedirected(request, response, context);
				  Header[] requestheaders =   response.getAllHeaders();
				  System.out.println("Get LWSSO_COOKE_KEY >>> ");
				  for (Header header : requestheaders)
				  {
					  System.out.println(header.getName()+"-------------------- "+header.getValue());
					  if (header.getName().equalsIgnoreCase("Set-Cookie") && header.getValue().startsWith(LWSSO_COOKIE_KEY_NAME))
					  {
						  lwSsoCookieKey = header.getValue();
						  System.out.println(AlmApiClient.this.LWSSO_COOKIE_KEY_NAME + " = " + lwSsoCookieKey);
					  }
				  }
	              if (!isRedirect) 
	              {
	                int responseCode = response.getStatusLine().getStatusCode();
	                if (responseCode == 301 || responseCode == 302) 
	                {
	                	  Header[] requestheaders3 = response.getAllHeaders();
		  				  for (Header header : requestheaders3)
						  {
							  System.out.println(header.getName()+"-------------------- "+header.getValue());
							  if (header.getName().equalsIgnoreCase("Set-Cookie") && header.getValue().startsWith(LWSSO_COOKIE_KEY_NAME))
							  {
								  lwSsoCookieKey = header.getValue();
								  System.out.println(AlmApiClient.this.LWSSO_COOKIE_KEY_NAME + " = " + lwSsoCookieKey);
							  }
						  }
	                  return true;
	                }
	              }
	              return false;
	            }
	          }).build();
		return httpClient;
	}
}
