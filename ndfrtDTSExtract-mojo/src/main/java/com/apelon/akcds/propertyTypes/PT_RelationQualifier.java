package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
/**
 * Role Modifiers from the DTS NDF load.  
 * @author Daniel Armbrust
 *
 */
public class PT_RelationQualifier extends PropertyType
{
	public PT_RelationQualifier(String uuidRoot)
	{
		//This is a little funny.  While the possible role modifiers are:
		//"all", "some", "poss", "notall", "notsome", "somenot", "allnot", "someor", "allor"
		//Those are actually the values, not the names.  So when role qualifiers are added as annotations, 
		//the name of the annotation will be "Relation Qualifier" and the value will be from the above list (but comes from the DTS)
		//I add them here in what would typically be the 'name' list, but the names are not used in the implementation.  I'm only adding
		//them to make them automatically show up in the metadata section.
		super("Relation Qualifiers", uuidRoot);
		addProperty("all");
		addProperty("some");
		addProperty("poss");
		addProperty("notall");
		addProperty("notsome");
		addProperty("somenot");
		addProperty("allnot");
		addProperty("someor");
		addProperty("allor");
	}
}
