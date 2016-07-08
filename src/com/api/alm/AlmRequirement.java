package com.api.alm;

import java.time.OffsetDateTime;

public class AlmRequirement
{
	public String id;
	public String target_release;
	public String name;
	public String cover_status;
	public String user_04_subarea;
	public String owner_author;
	public String user_03_qalead;
	public OffsetDateTime last_modified;
	
	public AlmRequirement()
	{
		id = "";
		target_release = "";
		name = "";
		cover_status = "";
		user_04_subarea = "";
		owner_author = "";
		user_03_qalead = "";
		last_modified = OffsetDateTime.MIN;
	}
}
