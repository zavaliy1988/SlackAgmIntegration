package com.api.slack;

import java.time.OffsetDateTime;

public class SLChatPostMessageResult
{
    public boolean ok;
    public String channel_id;
    public OffsetDateTime ts;
    public SLMessage message;
	
	public SLChatPostMessageResult()
	{
		ok = false;
		channel_id = "";
		ts = OffsetDateTime.MIN;
		message = new SLMessage();
	}
}
