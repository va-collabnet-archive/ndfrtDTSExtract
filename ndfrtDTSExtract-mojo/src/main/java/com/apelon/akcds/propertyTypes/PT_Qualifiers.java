package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;

/**
 * Qualifiers from the DTS NDF load which are loaded as nested string annotations within the workbench.
 * @author Daniel Armbrust
 */
public class PT_Qualifiers extends PropertyType
{
	public PT_Qualifiers()
	{
		super("Qualifier Types");
		indexRefsetMembers = true;
		addProperty("FILE");
		addProperty("Source");
		addProperty("Strength");
		addProperty("Unit");
		addProperty("VA.IEN");
		addProperty("VA_File");
		addProperty("VA_IEN");
		addProperty("VA_Status");
		addProperty("VUID");
	}
}
