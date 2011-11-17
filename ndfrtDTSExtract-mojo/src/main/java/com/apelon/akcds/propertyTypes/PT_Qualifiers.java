package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Qualifiers from the DTS NDF load which are loaded as nested string annotations within the workbench.
 * @author Daniel Armbrust
 */
public class PT_Qualifiers extends PropertyType
{
	public PT_Qualifiers(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "FILE", "Source", "Strength", "Unit",
				"VA.IEN", "VA_File", "VA_IEN", "VA_Status", "VUID"})), "Qualifier Types", uuidRoot);
	}
}
