package com.api.agm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import org.json.*;

public class AgmApiClient 
{
	private String agmUrl;
	private String accessToken;
	
	public AgmApiClient(String agmUrl)
	{
		this.agmUrl = agmUrl;
	}
	
	public boolean authenticate() throws IOException
	{
		String agmAuthenticationPath = "/agm/oauth/token";
		String accessTokenKey = "access_token";
		
		URL authenticateUrl = new URL(agmUrl + agmAuthenticationPath);
		HttpsURLConnection connection = (HttpsURLConnection) authenticateUrl.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setDoOutput(true);
		
		String body = "client_id=api_client_758142732_4&client_secret=pBmoJ19nJRhdQhF&grant_type=client_credentials";
		try (DataOutputStream output = new DataOutputStream(connection.getOutputStream()))
		{		
			output.writeBytes(body);
			output.flush();
		}
		
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
			JSONObject responseJson = new JSONObject(response.toString());
			this.accessToken = responseJson.getString(accessTokenKey);
			return true;
		}
		return false;
	}
	
	public void getDefects()
	{
		String defectsPath = "/defects";
		String query = "{product-group-id[1000];release-backlog-item.archive-status[0]}&fields=id,detected-by,creation-time,release-backlog-item.theme-id,release-backlog-item.blocked,description,release-backlog-item.feature-id,release-backlog-item.entity-id,priority,release-backlog-item.status,status,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.release-id,in-bucket,release-backlog-item.watch-id,name,owner,release-backlog-item.team-id,severity,release-backlog-item.linked-entities-info,attachment,release-backlog-item.story-points,release-backlog-item.owner,release-backlog-item.sprint-id";
		try
		{
			sendGetRequest("defects", query, 6, 1);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//"?query=" + "{product-group-id[1000];release-backlog-item.archive-status[0]}&fields=id,detected-by,creation-time,release-backlog-item.theme-id,release-backlog-item.blocked,description,release-backlog-item.feature-id,release-backlog-item.entity-id,priority,release-backlog-item.status,status,release-backlog-item.entity-name,release-backlog-item.entity-type,release-backlog-item.release-id,in-bucket,release-backlog-item.watch-id,name,owner,release-backlog-item.team-id,severity,release-backlog-item.linked-entities-info,attachment,release-backlog-item.story-points,release-backlog-item.owner,release-backlog-item.sprint-id&page-size=6&start-index=1&product-group-id=1000";		
	}
	
	private void sendGetRequest(String entityType, String query, int pageSize, int startIndex) throws IOException
	{
		String urlStr = "https://agilemanager-int.saas.hp.com/agm/api/authentication/sign-in";
		URL url = new URL(urlStr);
		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", "bearer " + this.accessToken);
		
		StringBuffer response = new StringBuffer();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
		{
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				response.append(line);
			}
		}
		
		System.out.println(response.toString());
	}
	
}
