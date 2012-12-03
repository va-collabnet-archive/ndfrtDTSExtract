package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Attributes;

/**
 * Properties from the DTS NDF data which should be expressed as concept attributes in the workbench.
 * @author Daniel Armbrust
 */
public class PT_Attributes extends BPT_Attributes
{
	public PT_Attributes(String uuidRoot)
	{
		super(uuidRoot);
		addPropertyName("Level");
		addPropertyName("Class_Code");
		addPropertyName("CS_Federal_Schedule");
		addPropertyName("Severity");
		addPropertyName("Status");
		addPropertyName("Strength");
		addPropertyName("Units");
		addPropertyName("VANDF_Record");
		addPropertyName("RxNorm_CUI");
		addPropertyName("FDA_UNII");
		addPropertyName("UMLS_CUI");
		addPropertyName("MeSH_CUI");
		addPropertyName("MeSH_DUI");
		addPropertyName("SNOMED_CID");
	}
}
