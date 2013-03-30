package com.apelon.akcds;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion.BaseContentVersion;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Skip;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;
import com.apelon.akcds.propertyTypes.PT_Attributes;
import com.apelon.akcds.propertyTypes.PT_ContentVersion;
import com.apelon.akcds.propertyTypes.PT_ContentVersion.ContentVersion;
import com.apelon.akcds.propertyTypes.PT_Descriptions;
import com.apelon.akcds.propertyTypes.PT_IDs;
import com.apelon.akcds.propertyTypes.PT_Qualifiers;
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
 * 
 * @goal convert-ndfrt-DTS-to-jbin
 * 
 * @phase process-sources
 */
public class AllDTSToEConcepts extends AbstractMojo
{
	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Loader version number
	 * Use parent because project.version pulls in the version of the data file, which I don't want.
	 * 
	 * @parameter expression="${project.parent.version}"
	 * @required
	 */
	private String loaderVersion;

	/**
	 * Content version number
	 * 
	 * @parameter expression="${project.version}"
	 * @required
	 */
	private String releaseVersion;

	private DataOutputStream dos_;
	private DbConn dbConn_;
	private EConceptUtility conceptUtility_;

	private final String uuidRoot_ = "gov.va.med.term.ndfrt:";

	// Want a specific handle to this one - adhoc usage.
	private final PropertyType contentVersion_ = new PT_ContentVersion(uuidRoot_);

	private final ArrayList<PropertyType> propertyTypes_ = new ArrayList<PropertyType>();

	// These are slightly different than the property types, have special handling - so they are not added to the propertyTypes_ list.
	private final PT_Qualifiers qualifiers_ = new PT_Qualifiers(uuidRoot_);
	private final PT_Relations relations_ = new PT_Relations(uuidRoot_);
	private final PT_RelationQualifier relQualifiers_ = new PT_RelationQualifier(uuidRoot_);

	// Various caches for performance reasons
	private Hashtable<String, String> codeToNUICache_ = new Hashtable<String, String>();
	private Hashtable<String, String> nameToNUICache_ = new Hashtable<String, String>();
	private Hashtable<String, DTSConcept> codeToDTSConceptCache_ = new Hashtable<String, DTSConcept>();
	private Hashtable<String, PropertyType> propertyToPropertyType_ = new Hashtable<String, PropertyType>();

	private EConcept ndfrtRefsetConcept;

	public AllDTSToEConcepts()
	{
		// This could be one nice, neat line of code in the class init section. But maven is broken and can't parse valid java.
		// Sigh. Broken up to appease the maven gods.

		propertyTypes_.add(new PT_IDs(uuidRoot_));
		propertyTypes_.add(new PT_Attributes(uuidRoot_));
		propertyTypes_.add(new PT_Descriptions(uuidRoot_));
		propertyTypes_.add(contentVersion_);
	}

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

			File binaryOutputFile = new File(outputDirectory, "ndfrtEConcepts.jbin");
			dos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryOutputFile)));
			conceptUtility_ = new EConceptUtility(uuidRoot_, "NDFRT Path", dos_);

			// Connect to DTS
			dbConn_ = new DbConn();
			dbConn_.connectDTS(new File(new File(outputDirectory.getParentFile().getParentFile(), "conn"), "dts_conn_params.txt"));

			Namespace ns = dbConn_.nameQuery.findNamespaceById(dbConn_.getNamespace());
			ConsoleUtil.println("*** Connected to: " + dbConn_.toString() + " " + ns.toString() + " ***");

			ConsoleUtil.println("Loading Metadata");

			// Set up a meta-data root concept
			UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
			UUID metaDataRoot = ConverterUUID.nameUUIDFromBytes((uuidRoot_ + ":metadata").getBytes());
			conceptUtility_.createAndStoreMetaDataConcept(metaDataRoot, "NDF-RT Metadata", archRoot, dos_);

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
			EConcept rootConcept = conceptUtility_.createConcept(ConverterUUID.nameUUIDFromBytes((uuidRoot_ + ":root").getBytes()),
					"National Drug File Reference Terminology");
			conceptUtility_.addSynonym(rootConcept, "NDF-RT", true, null);
			conceptUtility_.addSynonym(rootConcept, "NDFRT", false, null);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getName(), ContentVersion.NAME.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getId() + "", ContentVersion.ID.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getCode(), ContentVersion.CODE.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getNamespaceId() + "", ContentVersion.NAMESPACE_ID.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, ns.getContentVersion().getReleaseDate().toString(), ContentVersion.RELEASE_DATE.getProperty().getUUID(),
					false);
			conceptUtility_.addStringAnnotation(rootConcept, loaderVersion, BaseContentVersion.LOADER_VERSION.getProperty().getUUID(), false);
			conceptUtility_.addStringAnnotation(rootConcept, releaseVersion, BaseContentVersion.RELEASE.getProperty().getUUID(), false);

			storeConcept(rootConcept);

			UUID rootPrimordial = rootConcept.getPrimordialUuid();

			EConcept vaRefsetsConcept = conceptUtility_.createVARefsetRootConcept();
			storeConcept(vaRefsetsConcept);

			// store this later
			ndfrtRefsetConcept = conceptUtility_.createConcept("All NDF-RT Concepts", vaRefsetsConcept.getPrimordialUuid());

			ConsoleUtil.println("");
			ConsoleUtil.println("Metadata summary:");
			for (String s : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println("  " + s);
			}
			conceptUtility_.clearLoadStats();

			// Load the data
			createAllConcepts(rootPrimordial);

			ndfrtRefsetConcept.writeExternal(dos_);

			ConsoleUtil.println("");
			ConsoleUtil.println("Data Load Summary:");
			for (String s : conceptUtility_.getLoadStats().getSummary())
			{
				ConsoleUtil.println("  " + s);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(new File(outputDirectory, "uuidDebugMap.txt"));

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
		EConcept concept = conceptUtility_.createConcept(primordial, getPropValue(dtsConcept, "Display_Name"));

		// Property Handling
		for (DTSProperty property : dtsConcept.getFetchedProperties())
		{
			if (property.getValue() != null)
			{
				TkComponent<?> annotableAddedItem = null;

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
						annotableAddedItem = conceptUtility_.addDescription(concept, property.getValue(), pt.getProperty(property.getName()).getUUID(), false);
					}
					else if (pt instanceof BPT_Skip)
					{
						// noop
					}
					else
					{
						// annotation bucket
						annotableAddedItem = conceptUtility_.addStringAnnotation(concept, property.getValue(), pt.getProperty(property.getName()).getUUID(), false);
					}
				}

				// Any qualifiers that need to be added to the property?

				DTSQualifier[] qualifiers = property.getFetchedQualifiers();
				if (annotableAddedItem == null && qualifiers.length > 0)
				{
					ConsoleUtil.printErrorln("ERROR: Design flaw - qualifier found on type that was loaded as non-qualifiable!");
				}
				if (annotableAddedItem != null)
				{
					for (DTSQualifier qualifier : qualifiers)
					{
						conceptUtility_.addStringAnnotation(annotableAddedItem, qualifier.getValue(), qualifiers_.getProperty(qualifier.getName()).getUUID(), false);
					}
				}
			}
		}

		// Load the synonyms
		for (Synonym s : dtsConcept.getFetchedSynonyms())
		{
			conceptUtility_.addDescription(concept, s.getTerm().getName(), propertyToPropertyType_.get("Synonym").getProperty("Synonym").getUUID(), false);
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
					conceptUtility_.addRelationship(concept, foundParentPrimordial, null, null);
				}
			}
			else if (parentPrimordial != null)
			{
				conceptUtility_.addRelationship(concept, parentPrimordial, null, null);
			}

			// Also load any other roles that were passed in.
			if (infRoles != null)
			{
				for (DTSRole role : infRoles)
				{
					TkRelationship addedRelationship = conceptUtility_.addRelationship(concept, buildUUIDFromNUI(getNUIForName(role.getValueConcept().getName())),
							relations_.getProperty(role.getName()).getUUID(), null);

					RoleModifier rm = role.getRoleModifier();
					if (rm != null)
					{
						// See notes in PT_RelationQualifier to understand why the API is used differently in this case.
						conceptUtility_.addStringAnnotation(addedRelationship, rm.getName(), relQualifiers_.getPropertyTypeUUID(), false);
					}
				}
			}
		}

		conceptUtility_.addRefsetMember(ndfrtRefsetConcept, concept.getPrimordialUuid(), true, null);

		// Store the final EConcept.
		storeConcept(concept);
		return primordial;
	}

	/**
	 * Utility to help build UUIDs in a consistent manner.
	 */
	private UUID buildUUIDFromNUI(String nui)
	{
		return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + ":" + nui).getBytes());
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
}
