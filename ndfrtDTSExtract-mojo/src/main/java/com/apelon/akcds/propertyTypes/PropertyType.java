package com.apelon.akcds.propertyTypes;

import java.util.Set;
import java.util.UUID;

import com.apelon.akcds.counter.UUIDInfo;

/**
 * Abstract base class to help in mapping DTS property types into the workbench data model.
 * 
 * The main purpose of this structure is to keep the UUID generation sane across the various
 * places where UUIDs are needed in the workbench.
 *  
 * @author Daniel Armbrust
 */

public abstract class PropertyType
{
	private String propertyTypeDescription_;
	private Set<String> propertyNames_;
	private String uuidRoot_;
	
	protected PropertyType(Set<String> propertyNames, String propertyTypeDescription, String uuidRoot)
	{
		this.propertyNames_ = propertyNames;
		this.propertyTypeDescription_ = propertyTypeDescription;
		this.uuidRoot_ = uuidRoot;
	}
	
	public UUID getPropertyUUID(String propertyName)
	{
		UUID uuid = UUID.nameUUIDFromBytes((uuidRoot_ + ":" + propertyTypeDescription_ + ":" + propertyName).getBytes());
		UUIDInfo.add(uuid, uuidRoot_ + ":" + propertyTypeDescription_ + ":" + propertyName);
		return uuid;
	}
	
	public UUID getPropertyTypeUUID()
	{
		UUID uuid = UUID.nameUUIDFromBytes((uuidRoot_ + ":" + propertyTypeDescription_).getBytes());
		UUIDInfo.add(uuid, uuidRoot_ + ":" + propertyTypeDescription_);
		return uuid;
	}
	
	public String getPropertyTypeDescription()
	{
		return propertyTypeDescription_;
	}
	
	public Set<String> getPropertyNames()
	{
		return propertyNames_;
	}
	
	public boolean containsProperty(String propertyName)
	{
		return propertyNames_.contains(propertyName);
	}
	
	protected void addPropertyName(String propertyName)
	{
		propertyNames_.add(propertyName);
	}
	
	/**
	 * Default impl just returns what they passed in.  Real implementing classes may choose to override this.
	 */
	public String getPropertyFriendlyName(String propertyName)
	{
		return propertyName;
	}
	
}
