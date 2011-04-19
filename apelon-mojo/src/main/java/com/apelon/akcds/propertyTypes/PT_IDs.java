package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Properties from the DTS ndf load which are treated as alternate IDs within the workbench.
 * @author Daniel Armbrust
 */
public class PT_IDs extends PropertyType
{
	public PT_IDs(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "NUI", "RxNorm_CUI", "FDA_UNII", "UMLS_CUI", "MeSH_CUI", "MeSH_DUI",
				"SNOMED_CID", "VUID" })), "ID Types", uuidRoot);
	}
}