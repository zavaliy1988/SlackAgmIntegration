package com.api.alm;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

class AlmHelper
{	
	public static OffsetDateTime getDateTimeFromString(String dateTimeStr)
	{
		final int ALM_TIME_ZONE_OFFSET = 3; 
		String isoDateTimeStr = dateTimeStr.replace(" ", "T");
		OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.parse(isoDateTimeStr), ZoneOffset.ofHours(ALM_TIME_ZONE_OFFSET));
		return offsetDateTime;
	}
}
