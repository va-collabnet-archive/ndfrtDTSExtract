package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Properties from the DTS NDF data which should be expressed as concept attributes in the workbench.
 * @author Daniel Armbrust
 */
public class PT_Attributes extends PropertyType
{
	public PT_Attributes(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "Level", "Class_Code",
			"CS_Federal_Schedule", "Severity", "Status", "Strength", "Units", "VANDF_Record"  })), "Attribute Types", uuidRoot);
	}
}
