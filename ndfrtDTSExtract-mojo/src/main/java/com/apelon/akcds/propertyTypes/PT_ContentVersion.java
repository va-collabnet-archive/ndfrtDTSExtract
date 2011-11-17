package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Invented property with special handling for root node in workbench.
 * @author Daniel Armbrust
 */
public class PT_ContentVersion extends PropertyType
{
	public PT_ContentVersion(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "name", "id", "code", "namespaceId", "releaseDate" })), "Content Version", uuidRoot);
	}
}
