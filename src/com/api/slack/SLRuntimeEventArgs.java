package com.api.slack;

import java.time.OffsetDateTime;

public class SLRuntimeEventArgs
{
    public String channel;
    public boolean ok;
    public int reply_to;
    public String team;
    public String text;
    public OffsetDateTime ts;
    public String type;
    public String user;
	
	public SLRuntimeEventArgs()
	{
		channel = "";
		ok = false;
		reply_to = 0;
		team = "";
		text = "";
		ts = OffsetDateTime.MIN;
		type = "";
		user = "";
	}
}
