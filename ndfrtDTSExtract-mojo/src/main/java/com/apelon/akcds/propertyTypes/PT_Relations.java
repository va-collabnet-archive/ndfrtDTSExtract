package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
/**
 * Associations from the DTS NDF load.  Roles from DTS are dynamically added to this set at load time.
 * @author Daniel Armbrust
 *
 */
public class PT_Relations extends BPT_Relations
{
	public PT_Relations()
	{
		super("NDF-RT");
		addProperty("Heading_Mapped_To");
		addProperty("Ingredient_1");
		addProperty("Ingredient_2");
		addProperty("Product_Component");
		addProperty("Product_Component-2");
	}
}
