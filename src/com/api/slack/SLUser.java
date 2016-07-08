package com.api.slack;

public class SLUser
{
    public String id;
    public String name;
    public boolean deleted;
    public String status;
    public String color;
    public String real_name;
    public String tz;
    public String tz_label;
    public int tz_offset;
    //public SLProfile profile;
    public boolean is_admin;
    public boolean is_owner;
    public boolean is_primary_owner;
    public boolean is_restricted;
    public boolean is_ultra_restricted;
    public boolean is_bot;
    public boolean has_files;
    public boolean has_2fa;
    
    public SLUser()
    {
    	id = "";
    	name = "";
    	deleted = false;
    	status = "";
    	color = "";
    	real_name = "";
    	tz = "";
    	tz_label = "";
    	tz_offset = 0;
    	is_admin = false;
    	is_owner = false;
    	is_primary_owner = false;
    	is_restricted = false;
    	is_ultra_restricted = false;
    	is_bot = false;
    	has_files = false;
    	has_2fa = false;
    }
	
}
