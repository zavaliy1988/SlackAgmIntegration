package com.api.slack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.Response;

import org.json.JSONObject;
import com.neovisionaries.ws.client.*;
import com.neovisionaries.ws.client.WebSocket;

public class SLRuntimeApiClient
{
	private String webApiRootUrl;
	private String apiToken;
	private String proxyHost;
	private int proxyPort;
	private String wsUrlStr;
	private WebSocket webSocket;
	private ArrayList<SLWebSocketListener> listenerList;
	
	public SLRuntimeApiClient(String webApiRootUrl, String apiToken)
	{
		this.webApiRootUrl = webApiRootUrl;
		this.apiToken = apiToken;
		listenerList = new ArrayList<SLWebSocketListener>();
	}
	
	public SLRuntimeApiClient(String webApiRootUrl, String apiToken, String proxyHost, int proxyPort) 
	{
		this(webApiRootUrl, apiToken);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	
	public void addListener(SLWebSocketListener listener)
	{
		listenerList.add(listener);
	}
	
	public void removeListener(SLWebSocketListener listener)
	{
		listenerList.remove(listener);
	}
	
	public boolean authenticate()
	{
		String response = sendAuthenticateRequest();
		if (response != null)
		{
			wsUrlStr = extractWsUrl(response);
			if (!wsUrlStr.equals(""))
			{
				WebSocketFactory factory = new WebSocketFactory();
				ProxySettings set = factory.getProxySettings();
				if (proxyHost != null)
				{
					set.setHost(proxyHost);
					set.setPort(proxyPort);
				}
				
				try
				{
					if (webSocket != null)
					{
						webSocket.sendClose();
						webSocket = null;
					}
					webSocket = factory.createSocket(wsUrlStr);
					webSocket.addListener(new WebSocketAdapter()
					{
						@Override
						public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception
						{
							// TODO Auto-generated method stub
							super.onConnected(websocket, headers);
							for (SLWebSocketListener listener : listenerList)
							{
								listener.onConnected(webSocket);
							}
							System.out.println("Connected");
						}
						
						@Override
						public void onTextMessage(WebSocket websocket, String text) throws Exception
						{
							// TODO Auto-generated method stub
							super.onTextMessage(websocket, text);
							System.out.println("Message = " + text);
							SLRuntimeEventArgs args = extractSLRuntimeEventArgs(text);
							for (SLWebSocketListener listener : listenerList)
							{
								listener.onTextMessage(webSocket, args);
							}
						}
						
						@Override
						public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception
						{
							// TODO Auto-generated method stub
							super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
							System.out.println("Disconnected");
							for (SLWebSocketListener listener : listenerList)
							{
								listener.onDisconnected(websocket);
							}
						}
						
						@Override
						public void onError(WebSocket websocket, WebSocketException cause) throws Exception
						{
							// TODO Auto-generated method stub
							super.onError(websocket, cause);
							System.out.println("Error = " + cause);
							SLExceptionEventArgs args = extractSLExceptionEventArgs(cause);
							for (SLWebSocketListener listener : listenerList)
							{
								listener.onError(websocket, args);
							}
						}
					});
					webSocket.connect();
					return true;
				}
				catch (IOException | WebSocketException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	private String sendAuthenticateRequest()
	{
		StringBuffer urlStrBuf = new StringBuffer();
		urlStrBuf.append(webApiRootUrl);
		urlStrBuf.append("/");
		urlStrBuf.append("rtm.start");
		urlStrBuf.append("?token=");
		urlStrBuf.append(apiToken);

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
	
	private String extractWsUrl(String response)
	{
		JSONObject responseJson = new JSONObject(response);
		return SLHelper.extractStringIfExist(responseJson, "url");
	}
	
	private SLRuntimeEventArgs extractSLRuntimeEventArgs(String webSocketMessageText)
	{
		JSONObject messageTextJson = new JSONObject(webSocketMessageText);
		SLRuntimeEventArgs args = new SLRuntimeEventArgs();
		args.ok = SLHelper.extractBooleanIfExist(messageTextJson, "ok");
		args.type = SLHelper.extractStringIfExist(messageTextJson, "type");
		args.reply_to = SLHelper.extractBigDecimalIfExist(messageTextJson, "reply_to").intValue();
		args.channel = SLHelper.extractStringIfExist(messageTextJson, "channel");
		args.user = SLHelper.extractStringIfExist(messageTextJson, "user");
		args.text = SLHelper.extractStringIfExist(messageTextJson, "text");
		args.ts = SLHelper.dateTimeFromTimestamp(SLHelper.extractStringIfExist(messageTextJson, "ts"));
		args.team = SLHelper.extractStringIfExist(messageTextJson, "team");
		return args;
	}

	private SLExceptionEventArgs extractSLExceptionEventArgs(WebSocketException ex)
	{
		SLExceptionEventArgs args = new SLExceptionEventArgs();
		args.message = ex.getMessage();
		return args;
	}
}
