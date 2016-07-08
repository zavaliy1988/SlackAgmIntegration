package com.api.slack;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransitionRule;

import org.json.JSONObject;

class SLHelper
{
    public static OffsetDateTime dateTimeFromTimestamp(String ts)
    {
        String timestamp = ts.substring(0, ts.indexOf('.'));
        OffsetDateTime initialDateTime = OffsetDateTime.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), ZoneOffset.UTC);
        long timeInterval = 0;
        try
        {
        	timeInterval = Long.valueOf(timestamp);
        	initialDateTime = initialDateTime.plusSeconds(timeInterval);
        	return initialDateTime;
        }
        catch (NumberFormatException ex)
        {
            return OffsetDateTime.MIN;
        }
    }
    
	public static String extractStringIfExist(JSONObject jsonObj, String key)
	{
		String defaultStr = "";
		Object extracted = defaultStr;
		if (jsonObj.has(key))
		{
			extracted = jsonObj.get(key);
		}
		return extracted instanceof String ? (String) extracted : defaultStr;
	}
	
	public static boolean extractBooleanIfExist(JSONObject jsonObj, String key)
	{
		boolean defaultBoolean = false;
		Object extracted = defaultBoolean;
		if (jsonObj.has(key))
		{
			extracted = jsonObj.get(key);
		}
		return extracted instanceof Boolean ? (boolean) extracted : defaultBoolean;
	}
	
	public static BigDecimal extractBigDecimalIfExist(JSONObject jsonObj, String key)
	{
		BigDecimal defaultBigDecimal = BigDecimal.ZERO;
		Object extracted = defaultBigDecimal;
		if (jsonObj.has(key))
		{
			extracted = jsonObj.get(key);
		}
		return extracted instanceof BigDecimal ? (BigDecimal) extracted : defaultBigDecimal;
	}
	
	public static JSONObject extractJSONObjectIfExist(JSONObject jsonObj, String key)
	{
		JSONObject defaultJSONObject = new JSONObject("{}");
		Object extracted = defaultJSONObject;
		if (jsonObj.has(key))
		{
			extracted = jsonObj.get(key);
		}
		return extracted instanceof JSONObject ? (JSONObject) extracted : defaultJSONObject;
	}
}