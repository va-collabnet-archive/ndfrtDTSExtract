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
		super(uuidRoot, "NDF-RT");
		addProperty("MeSH_Name", "MeSH Name", null, false, SYNONYM);
		addProperty("Print_Name", "Print Name", null, false, SYNONYM);
		addProperty("RxNorm_Name", "RxNorm Name", null, false, SYNONYM);
		addProperty("VA_National_Formulary_Name", "VA National Formulary Name", null, false, SYNONYM);
		addProperty("Class_Description", "Class Description", null, false, DEFINITION);
		addProperty("MeSH_Definition", "MeSH Definition", null, false, DEFINITION);
		addProperty("Synonym", SYNONYM);
		addProperty("Display_Name", "Display Name", null, false, FSN);
	}
}
