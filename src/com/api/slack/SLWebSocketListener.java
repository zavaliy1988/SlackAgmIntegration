package com.api.slack;
import com.neovisionaries.ws.client.*;

public interface SLWebSocketListener 
{
	void onConnected(WebSocket webSocket);
	void onTextMessage(WebSocket webSocket, SLRuntimeEventArgs message);
	void onDisconnected(WebSocket webSocket);
	void onError(WebSocket webSocket, SLExceptionEventArgs err);
}
