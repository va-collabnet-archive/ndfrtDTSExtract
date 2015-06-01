package com.apelon.akcds;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.ConverterBaseMojo;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility.DescriptionType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Skip;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.ValuePropertyPair;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.refex.type_string.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

import com.apelon.akcds.propertyTypes.PT_Annotations;
import com.apelon.akcds.propertyTypes.PT_ContentVersion;
import com.apelon.akcds.propertyTypes.PT_ContentVersion.ContentVersion;
import com.apelon.akcds.propertyTypes.PT_Descriptions;
import com.apelon.akcds.propertyTypes.PT_IDs;
import com.apelon.akcds.propertyTypes.PT_Qualifiers;
import com.apelon.akcds.propertyTypes.PT_Refsets;
import com.apelon.akcds.propertyTypes.PT_RelationQualifier;
import com.apelon.akcds.propertyTypes.PT_Relations;
import com.apelon.dts.client.DTSException;
import com.apelon.dts.client.association.ConceptAssociation;
import com.apelon.dts.client.association.Synonym;
import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.attribute.DTSPropertyType;
import com.apelon.dts.client.attribute.DTSQualifier;
import com.apelon.dts.client.attribute.DTSRole;
import com.apelon.dts.client.attribute.DTSRoleType;
import com.apelon.dts.client.attribute.RoleModifier;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.DTSConcept;
import com.apelon.dts.client.concept.DTSSearchOptions;
import com.apelon.dts.client.concept.OntylogConcept;
import com.apelon.dts.client.namespace.Namespace;

/**
 * 
 * Loader code to connect to a DTS server (specified in dts_conn_params.txt) and load the entire
 * contents into a workbench jbin file.
 * 
 * Paths are typically controlled by maven, however, the main() method has paths configured so that they
 * match what maven does for test purposes.
 */
@Mojo( name = "convert-ndfrt-DTS-to-jbin", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class AllDTSToEConcepts extends ConverterBaseMojo
{
	private DataOutputStream dos_;
	private DbConn dbConn_;
	private EConceptUtility conceptUtility_;

	private final String ndfrtNamespaceBaseSeed = "gov.va.med.term.ndfrt:";

	// Want a specific handle to this one - adhoc usage.
	private PT_ContentVersion contentVersion_;

	private final ArrayList<PropertyType> propertyTypes_ = new ArrayList<PropertyType>();

	// These are slightly different than the property types, have special handling - so they are not added to the propertyTypes_ list.
	private PT_Qualifiers qualifiers_;
	private PT_Relations relations_;
	private PT_RelationQualifier relQualifiers_;

	// Various caches for performance reasons
	private Hashtable<String, String> codeToNUICache_ = new Hashtable<String, String>();
	private Hashtable<String, String> nameToNUICache_ = new Hashtable<String, String>();
	private Hashtable<String, DTSConcept> codeToDTSConceptCache_ = new Hashtable<String, DTSConcept>();
	private Hashtable<String, PropertyType> propertyToPropertyType_ = new Hashtable<String, PropertyType>();

	private EConcept ndfrtRefsetConcept;
	
	/**
	 * Used for debug. Sets up the same paths that maven would use.... allow the code to be run standalone.
	 */
	public static void main(String[] args) throws Exception
	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		AllDTSToEConcepts ndfConverter = new AllDTSToEConcepts();
		ndfConverter.outputDirectory = new File("../apelon-data/target/");
		ndfConverter.execute();
	}

	@Override
	public void execute() throws MojoExecutionException
	{
		ConsoleUtil.println("NDFRT Processing Begins " + new Date().toString());
		try
		{
			// Set up the output
			if (!outputDirectory.exists())
			{
				outputDirectory.mkdirs();
			}

			// Connect to DTS
			ConsoleUtil.println("Connecting to DTS server");
			dbConn_ = new DbConn();
			dbConn_.connectDTS(inputFileLocation);

			Namespace ns = dbConn_.nameQuery.findNamespaceById(dbConn_.getNamespace());
			ConsoleUtil.println("*** Connected to: " + dbConn_.toString() + " " + ns.toString() + " ***");
			
			File binaryOutputFile = new File(outputDirectory, "ndfrtEConcepts.jbin");
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryOutputFile)));
			conceptUtility_ = new EConceptUtility(ndfrtNamespaceBaseSeed, "NDFRT Path", dos_, ns.getContentVersion().getReleaseDate().getTime());
			
			// Want a specific handle to this one - adhoc usage.
			contentVersion_ = new PT_ContentVersion();
			
			propertyTypes_.add(new PT_IDs());
			propertyTypes_.add(new PT_Annotations());
			propertyTypes_.add(new PT_Descriptions());
			propertyTypes_.add(contentVersion_);
			
			// These are slightly different than the property types, have special handling - so they are not added to the propertyTypes_ list.
			qualifiers_ = new PT_Qualifiers();
			relations_ = new PT_Relations();
			relQualifiers_ = new PT_RelationQualifier();
			PT_Refsets refsets = new PT_Refsets();
			propertyTypes_.add(refsets);

			ConsoleUtil.println("Loading Metadata");

			// Set up a meta-data root concept
			UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
			UUID metaDataRoot = ConverterUUID.createNamespaceUUIDFromString("metadata");
			conceptUtility_.createAndStoreMetaDataConcept(metaDataRoot, "NDF-RT Metadata", false, archRoot, dos_);

			// Load the roles found in DTS into our relations structure
			DTSRoleType[] roleTypes = dbConn_.ontQry.getRoleTypes(dbConn_.getNamespace());
			for (int i = 0; i < roleTypes.length; i++)
			{
				relations_.addProperty(roleTypes[i].getName());
			}

			// Create metadata structures for the qualifiers and relations
			conceptUtility_.loadMetaDataItems(qualifiers_, metaDataRoot, dos_);
			conceptUtility_.loadMetaDataItems(relations_, metaDataRoot, dos_);
			conceptUtility_.loadMetaDataItems(relQualifiers_, metaDataRoot, dos_);

			// And for all of the other property types
			conceptUtility_.loadMetaDataItems(propertyTypes_, metaDataRoot, dos_);

			// Load up the propertyType map for speed, perform basic sanity check
			for (PropertyType pt : propertyTypes_)
			{
				for (String propertyName : pt.getPropertyNames())
				{
					if (propertyToPropertyType_.containsKey(propertyName))
					{
						ConsoleUtil.printErrorln("ERROR: Two different property types each contain " + propertyName);
					}
					propertyToPropertyType_.put(propertyName, pt);
				}
			}

			// validate that we are configured to map all properties properly
			checkForLeftoverPropertyTypes();

			// Create the root concept
			EConcept rootConcept = conceptUtility_.createConcept("NDF-RT");
			conceptUtility_.addDescription(rootConcept, "NDF-RT", DescriptionType.SYNONYM, true, null, null, false);
			conceptUtility_.addDescription(rootConcept, "National Drug File Reference Terminology", DescriptionType.SYNONYM, false, null, null, false);
			conceptUtility_.addDescription(rootConcept, "NDFRT", DescriptionType.SYNONYM, false, null, null, false);
			ConsoleUtil.println("Root concept FSN is 'NDF-RT' and the UUID is " + rootConcept.getPrimordialUuid());
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getName(), ContentVersion.NAME.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getId() + "", ContentVersion.ID.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getCode(), ContentVersion.CODE.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getNamespaceId() + "", ContentVersion.NAMESPACE_ID.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getReleaseDate().toString(), ContentVersion.RELEASE_DATE.getProperty().getUUID(),
					false);
			conceptUtility_.addStringAnnotation(rootConcept, loaderVersion, contentVersion_.LOADER_VERSION.getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, converterResultVersion, contentVersion_.RELEASE.getUUID(), false);

			storeConcept(rootConcept);

			UUID rootPrimordial = rootConcept.getPrimordialUuid();

			// store this later
			ndfrtRefsetConcept = refsets.getConcept(PT_Refsets.Refsets.ALL.getProperty());

			ConsoleUtil.println("");
			ConsoleUtil.println("Metadata summary:");
			for (String s : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println("  " + s);
			}
			conceptUtility_.clearLoadStats();

			// Load the data
			createAllConcepts(rootPrimordial);

			conceptUtility_.storeRefsetConcepts(refsets, dos_);

			ConsoleUtil.println("");
			ConsoleUtil.println("Data Load Summary:");
			for (String s : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println("  " + s);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(outputDirectory, "dtsExtract");
			
			ConsoleUtil.println("NDFRT Processing Completes " + new Date().toString());
			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}
		finally
		{
			if (dos_ != null)
			{
				try
				{
					dos_.flush();
					dos_.close();
				}
				catch (IOException e)
				{
					throw new MojoExecutionException(e.getLocalizedMessage(), e);
				}
			}
		}
	}

	private void createAllConcepts(UUID rootPrimordial) throws Exception
	{
		// Note - to do a quick (partial) load, modify this pattern and/or set a size limit on the options object.
		// The hack code at the end of this class will fix any broken tree that is a result of the partial load.
		String pattern = "*";
		DTSSearchOptions options = new DTSSearchOptions();
		// options.setLimit(50);
		options.setNamespaceId(dbConn_.getNamespace());
		ConsoleUtil.println("Searching for NDF Concepts");
		OntylogConcept[] oCons = dbConn_.searchQuery.findConceptsWithNameMatching(pattern, options);

		ConsoleUtil.println("Found " + oCons.length + " NDF Concept Codes");
		for (int i = 0; i < oCons.length; i++)
		{
			// See if a rel lookup already grabbed this for us.
			DTSConcept dtsConcept = codeToDTSConceptCache_.remove(oCons[i].getCode());
			if (dtsConcept == null)
			{
				dtsConcept = dbConn_.ontQry.findConceptByCode(oCons[i].getCode(), dbConn_.getNamespace(), ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);
			}
			String nui = getPropValue(dtsConcept, "NUI");
			if (nui != null)
			{
				// Populate these caches to avoid an extra trip to the DB during rel loading
				codeToNUICache_.put(dtsConcept.getCode(), nui);
				nameToNUICache_.put(dtsConcept.getName(), nui);
			}
			OntylogConcept oCon = (OntylogConcept) dtsConcept;
			DTSRole[] conInferredRoles = oCon.getFetchedRoles();
			OntylogConcept[] parentConcepts = oCon.getFetchedSuperconcepts();
			if (parentConcepts.length == 0 && conInferredRoles.length == 0)
			{
				writeRootChildConcept(dtsConcept, buildUUIDFromNUI(nui), rootPrimordial);
			}
			else
			{
				writeDeepChildConcept(dtsConcept, buildUUIDFromNUI(nui), parentConcepts, conInferredRoles);
			}
		}

		// ///////////////////////////////////////////////////////////
		/**
		 * This is a hack to load a properly structured tree when the initial
		 * query has been limited in someway that would have left out nodes that
		 * complete the path to the root of the tree.
		 * 
		 * During a normal, full load, this code should not execute.
		 * 
		 * This should probably be removed at some point.
		 * It prints to syserr if it executed.
		 */
		if (codeToDTSConceptCache_.size() > 0)
		{
			ConsoleUtil.printErrorln("WARNING: Hack code adding " + codeToDTSConceptCache_.size());
			ConsoleUtil.printErrorln("This code should NOT be running if you are doing a full load!");
			while (codeToDTSConceptCache_.size() > 0)
			{
				DTSConcept dtsConcept = (DTSConcept) codeToDTSConceptCache_.values().toArray()[0];
				codeToDTSConceptCache_.remove(dtsConcept.getCode());
				String nui = getPropValue(dtsConcept, "NUI");
				if (nui != null)
				{
					// Populate these caches to avoid an extra trip to the DB during rel loading
					codeToNUICache_.put(dtsConcept.getCode(), nui);
					nameToNUICache_.put(dtsConcept.getName(), nui);
				}
				OntylogConcept oCon = (OntylogConcept) dtsConcept;
				DTSRole[] conInferredRoles = oCon.getFetchedRoles();
				OntylogConcept[] parentConcepts = oCon.getFetchedSuperconcepts();
				if (parentConcepts.length == 0 && conInferredRoles.length == 0)
				{
					writeRootChildConcept(dtsConcept, buildUUIDFromNUI(nui), rootPrimordial);
				}
				else
				{
					writeDeepChildConcept(dtsConcept, buildUUIDFromNUI(nui), parentConcepts, conInferredRoles);
				}
			}
		}
		// End of hack code
		// /////////////////////////////////////////
	}

	private void checkForLeftoverPropertyTypes() throws Exception
	{
		DTSPropertyType[] propType = dbConn_.ontQry.getConceptPropertyTypes(dbConn_.getNamespace());
		for (int i = 0; i < propType.length; i++)
		{
			PropertyType pt = propertyToPropertyType_.get(propType[i].getName());
			if (pt == null)
			{
				ConsoleUtil.printErrorln("ERROR:  No mapping for property type " + propType[i].getName());
			}
		}
	}

	private String getPropValue(DTSConcept dc, String sPropType)
	{
		DTSProperty[] props = dc.getFetchedProperties();
		for (int i = 0; i < props.length; i++)
		{
			if (props[i].getPropertyType().getName().equals(sPropType))
			{
				return props[i].getValue();
			}
		}

		// not found...
		if (sPropType.equals("Display_Name"))
		{
			return dc.getName();
		}

		return null;
	}

	/**
	 * Convenience method Used when writing the top level of items found in DTS - tree items with no parents.
	 * Attaches them to the root node invented for NDF.
	 */
	private UUID writeRootChildConcept(DTSConcept dtsConcept, UUID primordial, UUID parentPrimordial) throws Exception
	{
		return writeNDFRTEConcept(dtsConcept, primordial, parentPrimordial, null, null);
	}

	/**
	 * Convenience method used for writing items found at an arbitrary depth in the tree.
	 */
	private UUID writeDeepChildConcept(DTSConcept dtsConcept, UUID primordial, OntylogConcept[] parents, DTSRole[] infRoles) throws Exception
	{
		return writeNDFRTEConcept(dtsConcept, primordial, null, parents, infRoles);
	}

	/**
	 * Write a complete DTSConcept. See the convenience methods, instead.
	 * 
	 * @see AllDTSToEConcepts#writeRootConcept(DTSConcept, UUID)
	 * @see AllDTSToEConcepts#writeRootChildConcept(DTSConcept, UUID, UUID)
	 * @see AllDTSToEConcepts#writeDeepChildConcept(DTSConcept, UUID, OntylogConcept[], DTSRole[])
	 */
	private UUID writeNDFRTEConcept(DTSConcept dtsConcept, UUID primordial, UUID parentPrimordial, OntylogConcept[] parents, DTSRole[] infRoles) throws Exception
	{
		EConcept concept = conceptUtility_.createConcept(primordial);

		ArrayList<ValuePropertyPairExtension> descriptions = new ArrayList<>();
		
		// Property Handling
		for (DTSProperty property : dtsConcept.getFetchedProperties())
		{
			if (property.getValue() != null)
			{
				PropertyType pt = propertyToPropertyType_.get(property.getName());
				if (pt == null)
				{
					ConsoleUtil.printErrorln("ERROR: No property type mapping for the property " + property.getName());
				}
				else
				{
					if (pt instanceof PT_IDs)
					{
						conceptUtility_.addAdditionalIds(concept, property.getValue(), pt.getProperty(property.getName()).getUUID(), false);
					}
					else if (pt instanceof PT_Descriptions)
					{
						descriptions.add(new ValuePropertyPairExtension(property.getValue(), pt.getProperty(property.getName()), property));
					}
					else if (pt instanceof BPT_Skip)
					{
						// noop
					}
					else
					{
						// annotation bucket
						TkRefsetStrMember annotation = conceptUtility_.addStringAnnotation(concept, property.getValue(), pt.getProperty(property.getName()).getUUID(), false);
						DTSQualifier[] qualifiers = property.getFetchedQualifiers();
						for (DTSQualifier qualifier : qualifiers)
						{
							conceptUtility_.addStringAnnotation(annotation, qualifier.getValue(), qualifiers_.getProperty(qualifier.getName()).getUUID(), false);
						}
					}
				}
			}
		}

		// Load the synonyms
		for (Synonym s : dtsConcept.getFetchedSynonyms())
		{
			descriptions.add(new ValuePropertyPairExtension(s.getTerm().getName(),propertyToPropertyType_.get("Synonym").getProperty("Synonym"), null));
		}
		
		//Now that we have gathered all of the description, actually load them.
		List<TkDescription> addedDescriptions = conceptUtility_.addDescriptions(concept, descriptions);
		
		//And then add any qualifiers that are necessary
		for (int i = 0; i < descriptions.size(); i++)
		{
			TkDescription wbDesc = addedDescriptions.get(i);
			ValuePropertyPairExtension vpp = descriptions.get(i);
			if (vpp.getDTSProperty() != null)
			{
				DTSQualifier[] qualifiers = vpp.getDTSProperty().getFetchedQualifiers();
				for (DTSQualifier qualifier : qualifiers)
				{
					conceptUtility_.addStringAnnotation(wbDesc, qualifier.getValue(), qualifiers_.getProperty(qualifier.getName()).getUUID(), false);
				}
			}
		}

		// Load the associations
		for (ConceptAssociation ca : dtsConcept.getFetchedConceptAssociations())
		{
			TkRelationship relationship = conceptUtility_.addRelationship(concept, buildUUIDFromNUI(getNUIForCode(ca.getToConcept().getCode())),
					relations_.getProperty(ca.getAssociationType().getName()).getUUID(), null);

			// And the qualifiers on the association, if any
			DTSQualifier[] qualifiers = ca.getFetchedQualifiers();
			for (DTSQualifier qualifier : qualifiers)
			{
				conceptUtility_.addStringAnnotation(relationship, qualifier.getValue(), qualifiers_.getProperty(qualifier.getName()).getUUID(), false);
			}
		}

		// create the is_a hierarchy if any parents were passed in.

		if (parentPrimordial != null || parents != null)
		{
			// If it has some sort of parent, add the is_a hierarchical relationship
			if (parents != null)
			{
				for (int i = 0; i < parents.length; i++)
				{
					UUID foundParentPrimordial = buildUUIDFromNUI(getNUIForCode(parents[i].getCode()));
					conceptUtility_.addRelationship(concept, foundParentPrimordial);
				}
			}
			else if (parentPrimordial != null)
			{
				conceptUtility_.addRelationship(concept, parentPrimordial);
			}

			// Also load any other roles that were passed in.
			if (infRoles != null)
			{
				for (DTSRole role : infRoles)
				{
					UUID target = buildUUIDFromNUI(getNUIForName(role.getValueConcept().getName()));
					UUID relType = relations_.getProperty(role.getName()).getUUID();
					RoleModifier rm = role.getRoleModifier();
					

					//Need to use the role modifier in the  UUID generation to prevent dupes
					UUID relUUID = ConverterUUID.createNamespaceUUIDFromStrings(concept.getPrimordialUuid().toString(), target.toString(), 
							relType.toString(), (rm == null ? "" : rm.getName()), role.getGroupNum() + "");  
					
					TkRelationship addedRelationship = conceptUtility_.addRelationship(concept, relUUID, target, relType, null, null, role.getGroupNum(), null);

					if (rm != null)
					{
						// See notes in PT_RelationQualifier to understand why the API is used differently in this case.
						conceptUtility_.addStringAnnotation(addedRelationship, rm.getName(), relQualifiers_.getPropertyTypeUUID(), false);
					}
				}
			}
		}

		conceptUtility_.addRefsetMember(ndfrtRefsetConcept, concept.getPrimordialUuid(), null, true, null);

		// Store the final EConcept.
		storeConcept(concept);
		return primordial;
	}

	/**
	 * Utility to help build UUIDs in a consistent manner.
	 */
	private UUID buildUUIDFromNUI(String nui)
	{
		return ConverterUUID.createNamespaceUUIDFromString(nui, true);
	}

	/**
	 * Utility to help build UUIDs in a consistent manner. Queries the DTS server if necessary
	 * to find the nui for the code. Stores the results in the caches for later use.
	 */
	private String getNUIForCode(String code) throws DTSException
	{
		String nui = codeToNUICache_.get(code);
		if (nui == null)
		{
			DTSConcept parentDTSConcept = dbConn_.thesQuery.findConceptByCode(code, dbConn_.getNamespace(), ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);
			nui = getPropValue(parentDTSConcept, "NUI");
			// If we had to look it up, then the main loop hasn't looked it up yet.
			// Cache the entire concept to save a trip later...
			codeToDTSConceptCache_.put(parentDTSConcept.getCode(), parentDTSConcept);
			codeToNUICache_.put(parentDTSConcept.getCode(), nui);
			nameToNUICache_.put(parentDTSConcept.getName(), nui);
		}
		return nui;
	}

	/**
	 * Utility to help build UUIDs in a consistent manner. Queries the DTS server if necessary
	 * to find the nui for the code. Stores the results in the cache for later use.
	 */
	private String getNUIForName(String name) throws DTSException
	{
		String nui = nameToNUICache_.get(name);
		if (nui == null)
		{
			DTSConcept targetDTSConcept = dbConn_.thesQuery.findConceptByName(name, dbConn_.getNamespace(), ConceptAttributeSetDescriptor.ALL_ATTRIBUTES);
			nui = getPropValue(targetDTSConcept, "NUI");
			// If we had to look it up, then the main loop hasn't looked it up yet.
			// Cache the entire concept to save a trip later...
			codeToDTSConceptCache_.put(targetDTSConcept.getCode(), targetDTSConcept);
			codeToNUICache_.put(targetDTSConcept.getCode(), nui);
			nameToNUICache_.put(targetDTSConcept.getName(), nui);
		}
		return nui;
	}

	/**
	 * Write an EConcept out to the jbin file. Updates counters, prints status tics.
	 */
	private void storeConcept(EConcept concept) throws IOException
	{
		concept.writeExternal(dos_);
		int conceptCount = conceptUtility_.getLoadStats().getConceptCount();

		if (conceptCount % 10 == 0)
		{
			ConsoleUtil.showProgress();
		}
		if ((conceptCount % 1000) == 0)
		{
			ConsoleUtil.println("Processed: " + conceptCount + " - just completed " + concept.getDescriptions().get(0).getText());
		}
	}
	
	private class ValuePropertyPairExtension extends ValuePropertyPair
	{
		private DTSProperty dtsProperty_;
		public ValuePropertyPairExtension(String value, Property property, DTSProperty dtsProperty)
		{
			super(value, property);
			dtsProperty_ = dtsProperty;
		}
		
		public DTSProperty getDTSProperty()
		{
			return dtsProperty_;
		}
	}
}
