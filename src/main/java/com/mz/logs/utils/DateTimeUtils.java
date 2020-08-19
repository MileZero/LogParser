package com.mz.logs.utils;

import java.time.*;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

	private static final long CPT_BUFFER_TIME_IN_MINUTES = 5;
	
	public static DayOfWeek getDayOfWeek() {
		return getDayOfWeek(Instant.now());
	}

	
	public static DayOfWeek getDayOfWeek(Instant instant) {
		Date date = Date.from(instant);
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		if( (c.get(Calendar.DAY_OF_WEEK)-1) ==0 )
			return DayOfWeek.SUNDAY;
		else
			return DayOfWeek.of(c.get(Calendar.DAY_OF_WEEK)-1);	
	}
	
	public static boolean isSameDay (Instant instant1, Instant instant2) {
		Date d1 = Date.from(instant1);
		Date d2 = Date.from(instant2);
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		c1.setTime(d1);
		c2.setTime(d2);
		if (c1.get(Calendar.DAY_OF_WEEK) == c2.get(Calendar.DAY_OF_WEEK))
			return true;

		return false;
	}
	
	public static LocalTime getLocalTime (Instant instant) {
		Date d1 = Date.from(instant);
		Calendar c1 = Calendar.getInstance();
		c1.setTime(d1);
		return LocalTime.of(c1.get(Calendar.HOUR_OF_DAY), 
				c1.get(Calendar.MINUTE), 
				c1.get(Calendar.SECOND));
	}
	
	public static LocalTime getTimeGMT(LocalTime lt,ZoneId inputZone) {
		if(inputZone==null)
			return lt;
	    ZoneId gmtZone = ZoneId.of("UTC");	    
	    LocalDateTime localT = LocalDateTime.of(LocalDate.now(),lt);	    
	    ZonedDateTime zonedT = ZonedDateTime.of(localT,inputZone);
	    return zonedT.withZoneSameInstant(gmtZone).toLocalTime();
	}
	
	public static LocalDateTime toDateTime_UTC(Instant instant) {
		return LocalDateTime.ofInstant(instant,ZoneOffset.UTC);
	}
	
	public static LocalDateTime getLocal_DateTime(Instant instant,ZoneId inputZone){
		return LocalDateTime.ofInstant(instant,inputZone);
	}
		
	public static LocalDateTime getDateTimeUTC(){
		return LocalDateTime.now(ZoneOffset.UTC);
	}
	
	public static LocalDateTime getStartCptTime(LocalDateTime cptLocalDateTime) {
		return cptLocalDateTime.minusMinutes(CPT_BUFFER_TIME_IN_MINUTES);
	}
	
	public static LocalDateTime getEndCptTime(LocalDateTime cptLocalDateTime) {
		return cptLocalDateTime.plusMinutes(CPT_BUFFER_TIME_IN_MINUTES);
	}
	
	//return date from specified LocalDateTime
	public static LocalDate getDateFromDateTime(LocalDateTime localDateTime) {
		return localDateTime.toLocalDate();
	}
	
	public static LocalTime getTimeFromDateTime(LocalDateTime localDateTime) {
		return localDateTime.toLocalTime();
	}
	
}
