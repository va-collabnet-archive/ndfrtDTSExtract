package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_MemberRefsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_Refsets extends BPT_MemberRefsets
{
	public enum Refsets
	{
		ALL("All NDF-RT Concepts");

		private Property property;

		private Refsets(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_Refsets()
	{
		super("NDF-RT");
		for (Refsets mm : Refsets.values())
		{
			addProperty(mm.getProperty());
		}
	}
}
