package com.api.slack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class SLWebApiClient
{
	private static SSLSocketFactory sslSocketFactory = null;
	private String webApiRootUrl;
	private String apiToken;
	private String proxyHost;
	private int proxyPort;
	public SLWebApiClient(String webApiRootUrl, String apiToken)
	{
		this.webApiRootUrl = webApiRootUrl;
		this.apiToken = apiToken;
	}
	
	public SLWebApiClient(String webApiRootUrl, String apiToken, String proxyHost, int proxyPort)
	{
		this(webApiRootUrl, apiToken);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	
	public boolean authTest()
	{
		String authenticationPath = "/auth.test";
		String urlStr = webApiRootUrl + authenticationPath + "?token=" + apiToken;
		try
		{
			URL url = new URL(urlStr);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			
			int responseCode = connection.getResponseCode();
			if (responseCode == 200)
			{
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
				JSONObject responseJson = new JSONObject(response.toString());
				boolean okStatus = responseJson.getBoolean("ok");
				return okStatus ? true : false;

			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public SLChatPostMessageResult chatPostMessage(String channel_id, String text, boolean asUser, boolean parseFull)
	{
		SLChatPostMessageResult chatPostMessageResult = new SLChatPostMessageResult();
		String userStr = asUser ? "true" : "false";
		String parseFullStr = parseFull ? "true" : "false";
		
		String response = sendWebApiGetRequest(webApiRootUrl, "chat.postMessage", apiToken, "&channel=" + channel_id, "&text=" + text, "&as_user=" + userStr, "&parse=" + parseFullStr);
		if (response != null)
		{
			chatPostMessageResult = extractChatPostMessageResult(response);
		}
		return chatPostMessageResult;
	}
	
	public ArrayList<SLChannel> channelList()
	{
		ArrayList<SLChannel> channelList = new ArrayList<SLChannel>();
		String response = sendWebApiGetRequest(webApiRootUrl, "channels.list", apiToken, "&exclude_archived=1");
		if (response != null)
		{
			channelList = extractChannelList(response);
		}
		return channelList;
	}
	
	public ArrayList<SLChannel> groupList()
	{
		ArrayList<SLChannel> groupList = new ArrayList<SLChannel>();
		String response = sendWebApiGetRequest(webApiRootUrl, "groups.list", apiToken, "&exclude_archived=1");
		if (response != null)
		{
			groupList = extractChannelList(response);
		}
		return groupList;
	}
	
	public ArrayList<SLMessage> channelHistory(String channel_id)
	{
		ArrayList<SLMessage> messageList = new ArrayList<SLMessage>();
		String response = sendWebApiGetRequest(webApiRootUrl, "channels.history", apiToken, "&channel=" + channel_id, "&count=1000");
		if (response != null)
		{
			messageList = extractChannelHistory(response);
			messageList.sort((one, two) -> two.ts.toInstant().compareTo(one.ts.toInstant()));
		}
		return messageList;
	}
	
	public ArrayList<SLMessage> groupHistory(String group_id)
	{
		ArrayList<SLMessage> messageList = new ArrayList<SLMessage>();
		String response = sendWebApiGetRequest(webApiRootUrl, "groups.history", apiToken, "&channel=" + group_id, "&count=1000");
		if (response != null)
		{
			messageList = extractGroupHistory(response);
			messageList.sort((one, two) -> two.ts.toInstant().compareTo(one.ts.toInstant())); 
		}
		return messageList;
	}
	
	public ArrayList<SLUser> userList()
	{
		ArrayList<SLUser> userList = new ArrayList<SLUser>();
		String response = sendWebApiGetRequest(webApiRootUrl, "users.list", apiToken);
		if (response != null)
		{
			userList = extractUserList(response);
		}
		return userList;
	}
	
	private SLChatPostMessageResult extractChatPostMessageResult(String response)
	{
		SLChatPostMessageResult chatPostMessageResult = new SLChatPostMessageResult();
		JSONObject responseJson = new JSONObject(response);
		
		boolean okStatus = responseJson.getBoolean("ok");
		chatPostMessageResult.ok = okStatus;
		if (okStatus)
		{
			chatPostMessageResult.channel_id = SLHelper.extractStringIfExist(responseJson, "channel");
			chatPostMessageResult.ts = SLHelper.dateTimeFromTimestamp(SLHelper.extractStringIfExist(responseJson, "ts"));
			JSONObject messageJson = SLHelper.extractJSONObjectIfExist(responseJson, "message");
			chatPostMessageResult.message.user_id = messageJson.getString("user");
			chatPostMessageResult.message.type = messageJson.getString("type");
			chatPostMessageResult.message.text = messageJson.getString("text");
			chatPostMessageResult.message.ts = SLHelper.dateTimeFromTimestamp(messageJson.getString("ts"));
		}
		return chatPostMessageResult;
	}
	
	private ArrayList<SLChannel> extractChannelList(String response)
	{
		ArrayList<SLChannel> channelList = new ArrayList<SLChannel>();
		JSONArray channels = new JSONObject(response).getJSONArray("channels");
		for (Object obj : channels)
		{
			JSONObject channelJson = (JSONObject) obj;
			SLChannel channel = new SLChannel();
			channel.id = SLHelper.extractStringIfExist(channelJson, "id");
			channel.name = SLHelper.extractStringIfExist(channelJson, "name");
			channel.is_channel = SLHelper.extractBooleanIfExist(channelJson, "is_channel");
			channel.created = SLHelper.extractBigDecimalIfExist(channelJson, "created").longValue();
			channel.creator_id = SLHelper.extractStringIfExist(channelJson, "creator");
			channel.is_archived = SLHelper.extractBooleanIfExist(channelJson, "is_archived");
			
			JSONArray membersData = channelJson.getJSONArray("members");
			for (Object obj2 : membersData)
			{
				String member_id = (String) obj2;
				channel.members_ids.add(member_id);
			}
			
			JSONObject topicData = channelJson.getJSONObject("topic");
			channel.topic.creator_id = topicData.getString("creator");
			channel.topic.value = topicData.getString("value");
			channel.topic.last_set = topicData.getBigInteger("last_set").longValue();
			channelList.add(channel);
		}
		return channelList;
	}
	
	private ArrayList<SLChannel> extractGroupList(String response)
	{
		ArrayList<SLChannel> groupList = new ArrayList<SLChannel>();
		JSONArray channels = new JSONObject(response).getJSONArray("groups");
		for (Object obj : channels)
		{
			JSONObject channelJson = (JSONObject) obj;
			SLChannel channel = new SLChannel();
			channel.id = SLHelper.extractStringIfExist(channelJson, "id");
			channel.name = SLHelper.extractStringIfExist(channelJson, "name");
			channel.is_channel = SLHelper.extractBooleanIfExist(channelJson, "is_channel");
			channel.created = SLHelper.extractBigDecimalIfExist(channelJson, "created").longValue();
			channel.creator_id = SLHelper.extractStringIfExist(channelJson, "creator");
			channel.is_archived = SLHelper.extractBooleanIfExist(channelJson, "is_archived");
			
			JSONArray membersData = channelJson.getJSONArray("members");
			for (Object obj2 : membersData)
			{
				String member_id = (String) obj2;
				channel.members_ids.add(member_id);
			}
			
			JSONObject topicData = channelJson.getJSONObject("topic");
			channel.topic.creator_id = topicData.getString("creator");
			channel.topic.value = topicData.getString("value");
			channel.topic.last_set = topicData.getBigInteger("last_set").longValue();
			groupList.add(channel);
		}
		return groupList;
	}
	
	private ArrayList<SLMessage> extractChannelHistory(String response)
	{
		ArrayList<SLMessage> messageList = new ArrayList<SLMessage>();
		try
		{
			JSONArray messagesJson = new JSONObject(response).getJSONArray("messages");
			for (Object obj : messagesJson)
			{
				JSONObject messageJson = (JSONObject) obj;
				SLMessage message = new SLMessage();
				message.type = SLHelper.extractStringIfExist(messageJson, "type");
				message.text = SLHelper.extractStringIfExist(messageJson, "text");
				message.user_id = SLHelper.extractStringIfExist(messageJson, "user");
				message.upload = SLHelper.extractBooleanIfExist(messageJson, "upload");
				message.ts = SLHelper.dateTimeFromTimestamp(SLHelper.extractStringIfExist(messageJson, "ts"));
				messageList.add(message);
			}
		}
		catch (Exception ex)
		{
			
		}
		return messageList;
	}
	
	private ArrayList<SLMessage> extractGroupHistory(String response)
	{
		ArrayList<SLMessage> messageList = new ArrayList<SLMessage>();
		try 
		{
			JSONArray messagesJson = new JSONObject(response).getJSONArray("messages");
			for (Object obj : messagesJson)
			{
				JSONObject messageJson = (JSONObject) obj;
				SLMessage message = new SLMessage();
				message.type = SLHelper.extractStringIfExist(messageJson, "type");
				message.text = SLHelper.extractStringIfExist(messageJson, "text");
				message.user_id = SLHelper.extractStringIfExist(messageJson, "user");
				message.upload = SLHelper.extractBooleanIfExist(messageJson, "upload");
				message.ts = SLHelper.dateTimeFromTimestamp(SLHelper.extractStringIfExist(messageJson, "ts"));
				messageList.add(message);
			}
		}
		catch (Exception ex)
		{
			
		}
		return messageList;
	}
	
	private ArrayList<SLUser> extractUserList(String response)
	{
		ArrayList<SLUser> userList = new ArrayList<SLUser>();
		JSONArray usersJson = new JSONObject(response).getJSONArray("members");
		for (Object obj : usersJson)
		{
			JSONObject userJson = (JSONObject) obj;
			SLUser user = new SLUser();
			user.id = SLHelper.extractStringIfExist(userJson, "id");
			user.name = SLHelper.extractStringIfExist(userJson, "name");
			user.status = SLHelper.extractStringIfExist(userJson, "status");
			user.color = SLHelper.extractStringIfExist(userJson, "color");
			user.real_name = SLHelper.extractStringIfExist(userJson, "real_name");
			user.tz = SLHelper.extractStringIfExist(userJson, "tz");
			user.tz_label = SLHelper.extractStringIfExist(userJson, "tz_label");
			user.tz_offset = SLHelper.extractBigDecimalIfExist(userJson, "tz_offset").intValue();
			user.is_admin = SLHelper.extractBooleanIfExist(userJson, "is_admin");
			user.is_owner = SLHelper.extractBooleanIfExist(userJson, "is_owner");
			user.is_primary_owner = SLHelper.extractBooleanIfExist(userJson, "is_primary_owner");
			user.is_restricted = SLHelper.extractBooleanIfExist(userJson, "is_restricted");
			user.is_ultra_restricted = SLHelper.extractBooleanIfExist(userJson, "is_ultra_restricted");
			user.is_bot = SLHelper.extractBooleanIfExist(userJson, "is_bot");
			user.has_files = SLHelper.extractBooleanIfExist(userJson, "has_files");
			user.has_2fa = SLHelper.extractBooleanIfExist(userJson, "has_2fa");
			userList.add(user);
		}
		return userList;
	}
	
	public String wrapWithConsoles(String src)
	{
		return "```" + src + "```";
	}
	
	private String sendWebApiGetRequest(String webApiRootUrl, String apiMethod, String apiToken, String... additionalParams)
	{
		StringBuffer urlStrBuf = new StringBuffer();
		urlStrBuf.append(webApiRootUrl);
		urlStrBuf.append("/");
		urlStrBuf.append(apiMethod);
		urlStrBuf.append("?token=");
		urlStrBuf.append(apiToken);
		for (String additionalParam : additionalParams)
		{
			String[] keyAndValue = additionalParam.split("=");
			urlStrBuf.append(keyAndValue[0]);
			urlStrBuf.append("=");
			try
			{
				urlStrBuf.append(URLEncoder.encode(keyAndValue[1], "UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try
		{
			URL url = new URL(urlStrBuf.toString());
			HttpsURLConnection connection = null;
			if (proxyHost != null)
			{
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				connection = (HttpsURLConnection) url.openConnection(proxy);
			}
			else
			{
				connection = (HttpsURLConnection) url.openConnection();
			}
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			if (responseCode == 200)
			{
				StringBuffer response = new StringBuffer();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
				{
					String line = null;
					while ((line = reader.readLine()) != null)
					{
						response.append(line);
					}
				}
				return response.toString();
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
