package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
/**
 * Associations from the DTS NDF load.  Roles from DTS are dynamically added to this set at load time.
 * @author Daniel Armbrust
 *
 */
public class PT_Relations extends BPT_Relations
{
	public PT_Relations(String uuidRoot)
	{
		super(uuidRoot);
		addPropertyName("Heading_Mapped_To");
		addPropertyName("Ingredient_1");
		addPropertyName("Ingredient_2");
		addPropertyName("Product_Component");
		addPropertyName("Product_Component-2");
	}
}
