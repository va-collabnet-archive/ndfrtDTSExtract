package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Properties from the DTS ndf load which are treated as alternate IDs within the workbench.
 * @author Daniel Armbrust
 */
public class PT_ContentVersion extends PropertyType
{
	public PT_ContentVersion(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "name", "id", "code", "namespaceId", "releaseDate" })), "Content Version", uuidRoot);
	}
}
