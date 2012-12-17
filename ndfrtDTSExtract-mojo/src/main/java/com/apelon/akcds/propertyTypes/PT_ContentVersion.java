package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;

/**
 * Invented property with special handling for root node in workbench.
 * @author Daniel Armbrust
 */
public class PT_ContentVersion extends BPT_ContentVersion
{
	public PT_ContentVersion(String uuidRoot)
	{
		super(uuidRoot);
		addPropertyName("name");
		addPropertyName("id");
		addPropertyName("code");
		addPropertyName("namespaceId");
		addPropertyName("releaseDate");
		addPropertyName("loaderVersion");
	}
}
