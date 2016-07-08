package com.api.slack;

public class SLNormalizer 
{
	public SLNormalizer()
	{
		
	}
	
    public static String normalize(String str)
    {
        if (str != null)
        {
            str = str.replace("\"", "'");
            str = str.replace("&", "&amp;");
            str = str.replace("<", "&lt;");
            str = str.replace(">", "&gt;");
            str = str.replace("\n", "\\n");
        }
        return str;
    }

    public static String denormalize(String str)
    {
        if (str != null)
        {
            str = str.replace("<", "");
            str = str.replace(">", "");
            str = str.replace("&lt;", "<");
            str = str.replace("&gt;", ">");
            str = str.replace("&amp;", "%26");
            str = str.replace("#", "%23");
        }
        return str;
    }
}
