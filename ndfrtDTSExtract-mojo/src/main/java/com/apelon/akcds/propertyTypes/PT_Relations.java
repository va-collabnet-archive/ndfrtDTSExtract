package com.apelon.akcds.propertyTypes;

import java.util.Arrays;
import java.util.HashSet;
/**
 * Associations from the DTS NDF load.  Roles from DTS are dynamically added to this set at load time.
 * @author Daniel Armbrust
 *
 */
public class PT_Relations extends PropertyType
{
	public PT_Relations(String uuidRoot)
	{
		super(new HashSet<String>(Arrays.asList(new String[] { "Heading_Mapped_To", "Ingredient_1",
				"Ingredient_2", "Product_Component", "Product_Component-2"})), "Relation Types", uuidRoot);
	}
	
	public void addRelation(String relationName)
	{
		addPropertyName(relationName);
	}
}
