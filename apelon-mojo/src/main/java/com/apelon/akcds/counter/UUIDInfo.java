package com.apelon.akcds.counter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * A Haskish class for keeping track of the source of generated UUIDs.  Helps to debug workbench problems, 
 * and keeps the API cleaner if the EConceptUtility (would have  had to redesign it otherwise, when the load
 * stats were added later)
 *  
 * @author Daniel Armbrust
 */

public class UUIDInfo
{
	//A hack to allow a lookup of UUIDs back to names, for debug purposes, and to keep the API clean
	//for the counter addon
	private static Hashtable<UUID, String> masterUUIDMap_ = new Hashtable<UUID, String>();
	
	public static void add(UUID uuid, String startString) 
	{
		masterUUIDMap_.put(uuid, startString);
	}
	
	public static String getUUIDBaseString(UUID uuid)
	{
		return masterUUIDMap_.get(uuid);
	}
	
	public static String getUUIDBaseStringLastSection(UUID uuid)
	{
		String temp = masterUUIDMap_.get(uuid);
		if (temp != null)
		{
			String[] parts = temp.split(":");
			if (parts != null && parts.length > 0)
			{
				return parts[parts.length - 1];
			}
			return temp;
		}
		return "Unknown";
	}
	
	public static void dump(File file) throws IOException
	{
		BufferedWriter br = new BufferedWriter(new FileWriter(file));
		for (Map.Entry<UUID, String> entry : masterUUIDMap_.entrySet())
		{
			br.write(entry.getKey() + " - " + entry.getValue() + System.getProperty("line.separator"));
		}
		br.close();
	}
}
