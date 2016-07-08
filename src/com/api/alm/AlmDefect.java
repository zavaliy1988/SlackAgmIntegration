package com.api.alm;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public class AlmDefect
{
	public String id;
	public String name;
	public String severity;
	public String status;
	public String target_rel;
	public String user_16_subarea;
	public String owner_dev;
	public String detected_by_qa;
	public String user_02_qalead;
	public ArrayList<String> comments;
	public OffsetDateTime last_modified;

	public AlmDefect()
	{
		id = "";
		name = "";
		severity = "";
		status = "";
		user_16_subarea = "";
		owner_dev = "";
		user_02_qalead = "";
		comments = new ArrayList<String>();
		last_modified = OffsetDateTime.MIN;
	}
}
