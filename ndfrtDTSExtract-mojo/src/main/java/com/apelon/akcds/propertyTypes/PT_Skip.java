package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Skip;

/**
 * Properties from DTS NDF load which have special handling during the conversion, and should not be loaded
 * the same way that other properties are handled.
 * @author Daniel Armbrust
 */
public class PT_Skip extends BPT_Skip
{
	public PT_Skip(String uuidRoot)
	{
		super("Unloaded Types", uuidRoot);
	}
}
