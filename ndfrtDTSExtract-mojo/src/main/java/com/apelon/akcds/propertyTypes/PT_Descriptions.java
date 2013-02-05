package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Descriptions;
/**
 * properties from the DTS ndf load which should be expressed as descriptions in the workbench.
 * 
 * Also includes Synonym - which is not represented as a property in DTS, but is treated the same
 * in the workbench.
 * 
 * @author Daniel Armbrust
 *
 */
public class PT_Descriptions extends BPT_Descriptions
{
	public PT_Descriptions(String uuidRoot)
	{
		super(uuidRoot);
		addProperty("MeSH_Name", "MeSH Name");
		addProperty("Print_Name", "Print Name");
		addProperty("RxNorm_Name");
		addProperty("VA_National_Formulary_Name", "VA National Formulary Name");
		addProperty("Class_Description", "Class Description");
		addProperty("MeSH_Definition", "MeSH Definition");
		addProperty("Synonym");
		addProperty("Display_Name");
	}
}
