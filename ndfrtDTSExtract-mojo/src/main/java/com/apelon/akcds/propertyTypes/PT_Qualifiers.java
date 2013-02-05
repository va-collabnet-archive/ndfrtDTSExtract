package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Qualifiers;

/**
 * Qualifiers from the DTS NDF load which are loaded as nested string annotations within the workbench.
 * @author Daniel Armbrust
 */
public class PT_Qualifiers extends BPT_Qualifiers
{
	public PT_Qualifiers(String uuidRoot)
	{
		super(uuidRoot);
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
