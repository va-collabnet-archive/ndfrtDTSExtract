package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Annotations;

/**
 * Properties from the DTS NDF data which should be expressed as concept attributes in the workbench.
 * @author Daniel Armbrust
 */
public class PT_Annotations extends BPT_Annotations
{
	public PT_Annotations()
	{
		super();
		addProperty("Level");
		addProperty("Class_Code");
		addProperty("CS_Federal_Schedule");
		addProperty("Severity");
		addProperty("Status");
		addProperty("Strength");
		addProperty("Units");
		addProperty("VANDF_Record");
		addProperty("RxNorm_CUI");
		addProperty("FDA_UNII");
		addProperty("UMLS_CUI");
		addProperty("MeSH_CUI");
		addProperty("MeSH_DUI");
		addProperty("SNOMED_CID");
		addProperty("Value_Set");
		addProperty("SNOMED_Parent_CID");
		addProperty("SNOMED_Child_CID");
	}
}
