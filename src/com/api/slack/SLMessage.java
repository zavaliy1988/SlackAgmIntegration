package com.api.slack;

import java.time.OffsetDateTime;

public class SLMessage
{
    public String type;
    public String subtype;
    public String text;
    public String user_id;
    public boolean upload;
    public OffsetDateTime ts;
	
	public SLMessage()
	{
		type = "";
		subtype = "";
		text = "";
		user_id = "";
		upload = false;
		ts = OffsetDateTime.MIN;
	}
}
