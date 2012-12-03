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
		addPropertyName("MeSH_Name");
		addPropertyName("Print_Name");
		addPropertyName("RxNorm_Name");
		addPropertyName("VA_National_Formulary_Name");
		addPropertyName("Class_Description");
		addPropertyName("MeSH_Definition");
		addPropertyName("Synonym");
		addPropertyName("Display_Name");
	}
	
	@Override
	public String getPropertyFriendlyName(String propertyName)
	{
		if (propertyName.equals("MeSH_Name"))
		{
			return "MeSH Name";
		}
		else if (propertyName.equals("Print_Name"))
		{
			return "Print Name";
		}
		else if (propertyName.equals("RxNorm_Name"))
		{
			return "RxNorm_Name";
		}
		else if (propertyName.equals("VA_National_Formulary_Name"))
		{
			return "VA National Formulary Name";
		}
		else if (propertyName.equals("Class_Description"))
		{
			return "Class Description";
		}
		else if (propertyName.equals("MeSH_Definition"))
		{
			return "MeSH Definition";
		}
		else
		{
			return super.getPropertyFriendlyName(propertyName);
		}
	}
}
