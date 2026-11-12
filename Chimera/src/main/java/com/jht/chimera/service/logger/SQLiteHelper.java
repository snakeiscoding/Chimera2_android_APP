package com.jht.chimera.service.logger;

import android.database.sqlite.SQLiteCursor;

import java.util.Calendar;
import java.util.Locale;

public class SQLiteHelper
{
	
	public static String dateToString( Calendar date )
	{
		if( date == null )
			return "";
		return 
				String.format(
						Locale.ENGLISH,
						"%d-%02d-%02d %02d:%02d:%02d", 
						date.get(Calendar.YEAR), 
						date.get(Calendar.MONTH), 
						date.get(Calendar.DAY_OF_MONTH), 
						date.get(Calendar.HOUR_OF_DAY), 
						date.get(Calendar.MINUTE), 
						date.get(Calendar.SECOND)
				);
		
	}
	
	public static Calendar stringToDate( String value )
	{
		Calendar date = Calendar.getInstance();
		
		if( value == null ) return date;
		
		String[] dateParts = value.split(" ");
		
		if( dateParts.length != 2 ) return date;
		
		String[] dayParts = dateParts[0].split( "-" );
		if( dayParts.length != 3 ) return date;
		
		String[] timeParts = dateParts[1].split( ":" );
		if( timeParts.length != 3 ) return date;
		date.set( 
				Integer.parseInt( dayParts[0] ), 
				Integer.parseInt( dayParts[1] ),  
				Integer.parseInt( dayParts[2] ), 
				Integer.parseInt( timeParts[0] ), 
				Integer.parseInt( timeParts[1] ), 
				Integer.parseInt( timeParts[2] ) 
		);
		
		return date;
		
	}
	
	public static Calendar getDateByColName( SQLiteCursor cursor, String col )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return Calendar.getInstance();
		return stringToDate( cursor.getString( index ) );
	}
	
	
	public static byte[] getBlobByColName( SQLiteCursor cursor, String col )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return null;
		return cursor.getBlob( index );
	}
	
	public static double getDoubleByColName( SQLiteCursor cursor, String col, double defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getDouble( index );
	}

	public static float getFloatByColName( SQLiteCursor cursor, String col, float defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getFloat( index );
	}
	
	public static int getIntByColName( SQLiteCursor cursor, String col, int defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getInt( index );
	}
	
	public static long getLongByColName( SQLiteCursor cursor, String col, long defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getLong( index );
	}
	
	public static short getShortByColName( SQLiteCursor cursor, String col, short defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getShort( index );
	}
	
	public static String getStringByColName( SQLiteCursor cursor, String col, String defaultValue )
	{
		int index = cursor.getColumnIndex(col);
		if( index == -1 ) return defaultValue;
		return cursor.getString( index );
	}

}
