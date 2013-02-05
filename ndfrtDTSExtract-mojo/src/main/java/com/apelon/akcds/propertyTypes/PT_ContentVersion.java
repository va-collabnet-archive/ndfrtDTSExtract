package com.apelon.akcds.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

/**
 * Invented property with special handling for root node in workbench.
 * @author Daniel Armbrust
 */
public class PT_ContentVersion extends BPT_ContentVersion
{
    public enum ContentVersion
    {
        NAME("Name"), ID("ID"), CODE("Code"), NAMESPACE_ID("Namespace ID");

        private Property property;
        private ContentVersion(String niceName)
        {
            // Don't know the owner yet - will be autofilled when we add this to the parent, below.
            property = new Property(null, niceName);
        }

        public Property getProperty()
        {
            return property;
        }
    }
    
	public PT_ContentVersion(String uuidRoot)
	{
		super(uuidRoot);
		for (ContentVersion cv : ContentVersion.values())
        {
            addProperty(cv.getProperty());
        }
	}
}
