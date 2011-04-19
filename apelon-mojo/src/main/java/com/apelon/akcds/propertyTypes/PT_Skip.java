package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Properties from DTS NDF load which have special handling during the conversion, and should not be loaded
 * the same way that other properties are handled.
 * @author Daniel Armbrust
 */
public class PT_Skip extends PropertyType
{
	public PT_Skip(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "Display_Name" })), "Unloaded Types", uuidRoot);
	}
}
