package com.api.slack;

import java.util.ArrayList;
import java.util.List;

public class SLChannel
{
    public String id;
    public String name;
    public boolean is_channel;
    public long created;
    public String creator_id;
    public boolean is_archived;
    public ArrayList<String> members_ids;
    public SLTopic topic;
    public SLPurpose purpose;
	
	public SLChannel()
	{
		id = "";
		name = "";
		is_channel = false;
		created = 0;
		creator_id = "";
		is_archived = false;
		members_ids = new ArrayList<String>();
		topic = new SLTopic();
		purpose = new SLPurpose();
	}
}
