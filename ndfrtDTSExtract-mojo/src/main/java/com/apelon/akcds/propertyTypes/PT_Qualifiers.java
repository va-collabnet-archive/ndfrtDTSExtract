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
		addPropertyName("FILE");
		addPropertyName("Source");
		addPropertyName("Strength");
		addPropertyName("Unit");
		addPropertyName("VA.IEN");
		addPropertyName("VA_File");
		addPropertyName("VA_IEN");
		addPropertyName("VA_Status");
		addPropertyName("VUID");
	}
}
