package com.apelon.akcds;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.tapi.TerminologyException;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.attribute.DTSPropertyType;
import com.apelon.dts.client.attribute.DTSRole;
import com.apelon.dts.client.attribute.DTSRoleType;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.DTSConcept;
import com.apelon.dts.client.concept.DTSSearchOptions;
import com.apelon.dts.client.concept.OntylogConcept;
import com.apelon.dts.client.namespace.Namespace;

/**
 * Goal which touches a timestamp file.
 *
 * @goal generate-apelon-data
 *
 * @phase process-sources
 */
public class AllDTSToEConcepts
extends AbstractMojo
{

/**
 * Location of the file.
 * @parameter expression="${project.build.directory}"
 * @required
 */
private File outputDirectory;
private File touch;
private ConceptAttributeSetDescriptor csd = ConceptAttributeSetDescriptor.ALL_ATTRIBUTES;
public DbConn dbc;
int conCounter = 0;
int relCounter = 0;
int level = 0;
int namespace_id;
private Namespace ns;
private DataOutputStream dos;
private PrintWriter out;

public void execute()
    throws MojoExecutionException
{
    File f = outputDirectory;

    if ( !f.exists() )
    {
        f.mkdirs();
    }
      touch = new File( f, "NDFRTEConcepts.jbin" );
      try {
    	    dbc =  new DbConn();
    	    dbc.connectDTS();
    	    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));
        	String curDir = System.getProperty("user.dir");
        	File dir = new File(curDir + "\\conn");
    		File file = new File(dir, "written-concepts.txt");
    		OutputStreamWriter out_stream = new OutputStreamWriter(
    				new FileOutputStream(file.getPath()), "UTF8");
    		out = new PrintWriter(new BufferedWriter(out_stream));
    	    namespace_id = dbc.namespace_ID;
    	    ns = dbc.nameQuery.findNamespaceById(namespace_id);
    	    System.out.println("***Connected to: " + dbc.user + "***");
    	    DTSConcept rootCon = new DTSConcept("National Drug File Reference Terminology", namespace_id);
    	    //UUID relRoot = ArchitectonicAuxiliary.Concept.RELATIONSHIP.getPrimoridalUid();
    	    UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();
    	    UUID rootPrimordial = writeNDFRTEConcept(rootCon, touch, null);//create root concept
    	    conCounter++;
    	    UUID NDFRTRelPrimordial = writeAuxEConcept(touch, "NDF-RT Relationship Types", archRoot);
    	    UUID NDFRTPropPrimordial = writeAuxEConcept(touch, "NDF-RT Attributes Types", archRoot);
    	    getAllRelEConceptNames(touch, NDFRTRelPrimordial);
    	    getAllPropEConceptNames(touch, NDFRTPropPrimordial);
    	    createAllConcepts(touch, rootPrimordial);
    	    out.close();
    		dos.flush();
    		dos.close();
      } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
      }

 }

public void createAllConcepts(File file, UUID rootPrimordial) throws Exception{
	String pattern = "*";
	DTSSearchOptions options = new DTSSearchOptions();
	OntylogConcept[] oCons = dbc.searchQuery.findConceptsWithNameMatching(pattern, options);//no property or role values are returned!!
	System.out.println("# of concepts found: " + oCons.length);
	for (int i = 0; i < oCons.length ; i++) {
		DTSConcept dCon = dbc.ontQry.findConceptByCode(oCons[i].getCode(), namespace_id, csd);
		OntylogConcept oCon = (OntylogConcept) dCon;
		DTSRole[] conInferredRoles = oCon.getFetchedRoles();
		OntylogConcept[] parentConcepts = oCon.getFetchedSuperconcepts();
		if (parentConcepts.length == 0 && conInferredRoles.length == 0){
			writeNDFRTEConcept(dCon, touch, rootPrimordial);
		}
		else {
			writeNDFRTEConcept(dCon, oCons[i], touch, parentConcepts, conInferredRoles);	
		}
		conCounter++;
		if ((conCounter % 1000) == 0) {
			System.out.println("Processed: " + conCounter);
			System.out.println("Wrote: " + oCons[i].getName());
		}
	}
}

public void getAllRelEConceptNames(File file, UUID NDFRTRelPrimordial) throws Exception{
	DTSRoleType[] roleTypes = dbc.ontQry.getAllRoleTypes();
	for (int i = 0; i < roleTypes.length; i++){
		String sRoleName = roleTypes[i].getName();
		writeAuxEConcept(file, sRoleName, NDFRTRelPrimordial);
	}
}

public void getAllPropEConceptNames(File file, UUID NDFRTPropPrimordial) throws Exception{
	DTSPropertyType[] propType = dbc.ontQry.getAllConceptPropertyTypes();
	for (int i = 0; i < propType.length; i++){
		String sPropName = propType[i].getName();
		writeAuxEConcept(file, sPropName, NDFRTPropPrimordial);
	}
}

public String getPropValue (DTSConcept dc, String sPropType){
	DTSProperty [] props = dc.getFetchedProperties();
	//System.out.println("Number of properties: " + props.length);
	String propValue = null;
	Boolean found = false;
	for (int i = 0; i < props.length; i++) {
		if (props[i].getPropertyType().getName().equals(sPropType)){
			propValue = props[i].getValue();
			//System.out.println("Getting Prop Value for: " + dc.getName() + "Value: " + propValue);
			found = true;
		}
	}
	if (found == false && sPropType.equals("Display_Name")){
		propValue = dc.getName();
	}
	return propValue;
}

public UUID writeNDFRTEConcept(DTSConcept con, OntylogConcept oCon, File file, OntylogConcept[] parents, DTSRole[] infRoles) throws Exception
{
  //System.out.println("Working on concept:" + con.getName());
  long time = System.currentTimeMillis();

  UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
  UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
  UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
  UUID isa = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
  UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
  UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();

  EConcept concept = new EConcept();
  UUID primordial = getPrimoridalUUID(con, "NUI");
  concept.setPrimordialUuid(primordial);
  EConceptAttributes conceptAttributes = new EConceptAttributes();
  conceptAttributes.setAuthorUuid(author);

  conceptAttributes = new EConceptAttributes();
  conceptAttributes.defined = false;
  conceptAttributes.primordialUuid = primordial;
  conceptAttributes.statusUuid = currentUuid;
  conceptAttributes.setPathUuid(path);
  conceptAttributes.setTime(time);
  
  
  /** 
   * TO DO: This needs to be updated to create all concept annotations from applicable concept properties in NDFRT. Not all properties should be concept annotations.
   * For example, alternate ID properties (VUID, NUI etc...) should be entered as such.
   */
  //attempt to add annotations on description
  String [] propTypeNames = {"Level", "Class_Code", "CS_Federal_Schedule", "Severity", "Status", "Strength"};
  addProperties(conceptAttributes, con, propTypeNames);// attempt to add annotations on description
  concept.setConceptAttributes(conceptAttributes);
  
	// get the additional ids list of the attributes
	List<TkIdentifier> additionalIds = conceptAttributes.additionalIds;
	if (additionalIds == null) {
		additionalIds = new ArrayList<TkIdentifier>();
		conceptAttributes.additionalIds = additionalIds;
	}
	
	// create the identifier and add it to the additional ids list
	EIdentifierString cid = new EIdentifierString();
	additionalIds.add(cid);
	
	// populate the identifier with the usual suspects
	cid.setAuthorityUuid(UUID.nameUUIDFromBytes(("com.apelon.akcds:NUI").getBytes()));
	cid.setPathUuid(path);
	cid.setStatusUuid(currentUuid);
	cid.setTime(System.currentTimeMillis());
	// populate the actual value of the identifier
	cid.setDenotation(getPropValue(con, "NUI"));
	
  
  List<TkDescription> descriptions = new ArrayList<TkDescription>();
  TkDescription description = new TkDescription();
  description.setConceptUuid(primordial);
  description.setLang("en");
  description.setPrimordialComponentUuid(UUID.randomUUID());

  description.setTypeUuid(preferredTerm);
  //System.out.println("Getting Description");
  description.text = getPropValue(con, "Display_Name");
  description.setStatusUuid(currentUuid);
  description.setAuthorUuid(author);
  description.setPathUuid(path);
  description.setTime(time);
  descriptions.add(description);
  concept.setDescriptions(descriptions);

  if (conCounter > 0){//create is_a hierarchical relationship
	// get the additional ids list of the attributes
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "NUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "RxNorm_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "FDA_UNII");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "UMLS_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "MeSH_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "MeSH_DUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "SNOMED_CID");

	  
	List<TkRelationship> relationships = new ArrayList<TkRelationship>();
	 for (int i = 0; i < parents.length; i++) {
	    UUID parentPrimordial = getPrimoridalUUID(dbc.thesQuery.findConceptByCode(parents[i].getCode(), namespace_id, csd), "NUI");
	    TkRelationship heirRel = createRelationships(concept, parentPrimordial, relPrimordial);
		relationships.add(heirRel);
	 }
	
    for (int i = 0; i < infRoles.length; i++) {
    	OntylogConcept targetOCon = infRoles[i].getValueConcept();
    	DTSConcept targetDCon = dbc.thesQuery.findConceptByName(targetOCon.getName(), namespace_id, csd);
    	//System.out.println("concept: " + con.getName() + " target: " + targetDCon.getName());
    	TkRelationship roleRel = createRelationships(concept, getPrimoridalUUID(targetDCon, "NUI"), UUID.nameUUIDFromBytes(("com.apelon.akcds:" + infRoles[i].getName()).getBytes()));
    	relationships.add(roleRel);
    }
    
	concept.setRelationships(relationships);
  }

  out.println("Wrote: " + concept);
  concept.writeExternal(dos);
  
  return primordial;
}
public UUID writeNDFRTEConcept(DTSConcept con, File file, UUID parentPrimordial) throws Exception
{

   long time = System.currentTimeMillis();

   UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
   UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
   UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
   UUID isa = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
   UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
   UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();

  EConcept concept = new EConcept();
  UUID primordial = UUID.nameUUIDFromBytes(("com.apelon.akcds:" + getPropValue(con, "NUI")).getBytes());
  concept.setPrimordialUuid(primordial);
  EConceptAttributes conceptAttributes = new EConceptAttributes();
  conceptAttributes.setAuthorUuid(author);

  conceptAttributes = new EConceptAttributes();
  conceptAttributes.defined = false;
  conceptAttributes.primordialUuid = primordial;
  conceptAttributes.statusUuid = currentUuid;
  conceptAttributes.setPathUuid(path);
  conceptAttributes.setTime(time);
  
  
  /** 
   * TO DO: This needs to be updated to create all concept annotations from applicable concept properties in NDFRT. Not all properties should be concept annotations.
   * For example, alternate ID properties (VUID, NUI etc...) should be entered as such.
   */
  //attempt to add annotations on description
  String [] propTypeNames = {"Level", "Class_Code", "CS_Federal_Schedule", "Severity", "Status", "Strength"};
  addProperties(conceptAttributes, con, propTypeNames);
  concept.setConceptAttributes(conceptAttributes);

  List<TkDescription> descriptions = new ArrayList<TkDescription>();
  TkDescription description = new TkDescription();
  description.setConceptUuid(primordial);
  description.setLang("en");
  description.setPrimordialComponentUuid(UUID.randomUUID());

  description.setTypeUuid(preferredTerm);
  //System.out.println("Getting Description");
  description.text = getPropValue(con, "Display_Name");
  description.setStatusUuid(currentUuid);
  description.setAuthorUuid(author);
  description.setPathUuid(path);
  description.setTime(time);
  descriptions.add(description);
  concept.setDescriptions(descriptions);

  if (conCounter > 0){
	// get the additional ids list of the attributes
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "NUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "RxNorm_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "FDA_UNII");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "UMLS_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "MeSH_CUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "MeSH_DUI");
	createAdditionalIds(con, currentUuid, path, conceptAttributes, "SNOMED_CID");
	//create is_a hierarchical relationship  
	List<TkRelationship> relationships = new ArrayList<TkRelationship>();
	TkRelationship heirRel = createRelationships(concept, parentPrimordial, relPrimordial);
	relationships.add(heirRel);
	concept.setRelationships(relationships);
  }

  out.println("Wrote: " + concept);
  concept.writeExternal(dos);
  
  return primordial;
}

private void createAdditionalIds(DTSConcept con, UUID currentUuid, UUID path,
		EConceptAttributes conceptAttributes, String propTypeName) {
 if (getPropValue(con, propTypeName) != null){
	List<TkIdentifier> additionalIds = conceptAttributes.additionalIds;
	if (additionalIds == null) {
		additionalIds = new ArrayList<TkIdentifier>();
		conceptAttributes.additionalIds = additionalIds;
	}
	
	// create the identifier and add it to the additional ids list
	EIdentifierString cid = new EIdentifierString();
	additionalIds.add(cid);
		
	// populate the identifier with the usual suspects
	cid.setAuthorityUuid(UUID.nameUUIDFromBytes(("com.apelon.akcds:" + propTypeName).getBytes()));
	cid.setPathUuid(path);
	cid.setStatusUuid(currentUuid);
	cid.setTime(System.currentTimeMillis());
	// populate the actual value of the identifier
	cid.setDenotation(getPropValue(con, propTypeName));
 }
}

private UUID getPrimoridalUUID(DTSConcept targetDCon, String propValue) {
	//System.out.println("Inside getPrimordial UUID");
	return UUID.nameUUIDFromBytes(("com.apelon.akcds:" + getPropValue(targetDCon, propValue)).getBytes());
}

private List<TkRefsetAbstractMember<?>> addProperties(TkComponent<?> description, DTSConcept con, String[] propTypeNames) throws IOException, TerminologyException
{
	List<TkRefsetAbstractMember<?>> annotations = new ArrayList<TkRefsetAbstractMember<?>>(); 
	for (int i = 0; i < propTypeNames.length; i++) {
		String propValue = getPropValue(con, propTypeNames[i]);
		if (propValue != null)
		{
			TkRefsetStrMember strRefexMember = new TkRefsetStrMember();
			 
			strRefexMember.setComponentUuid(description.getPrimordialComponentUuid()); 
			strRefexMember.setStrValue(propValue);
			  
			strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes(("com.apelon.akcds:property:" +
					description.getPrimordialComponentUuid().toString() +
					strRefexMember.getStrValue()).getBytes()));
			  
			strRefexMember.setRefsetUuid(UUID.nameUUIDFromBytes(("com.apelon.akcds:" + propTypeNames[i]).getBytes()));
			  
			strRefexMember.setStatusUuid(ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid());
			strRefexMember.setAuthorUuid(ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid());
			strRefexMember.setPathUuid(ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid()); 
			strRefexMember.setTime(System.currentTimeMillis());
			annotations.add(strRefexMember);
		}
	}
	description.setAnnotations(annotations);
	return annotations;
}

public UUID writeAuxEConcept(File file, String name, UUID relParentPrimordial) throws Exception
{
   conCounter++;
   long time = System.currentTimeMillis();

   UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
   UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
   UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
   UUID isa = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
   UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
   UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
   

  EConcept concept = new EConcept();
  UUID primordial = UUID.nameUUIDFromBytes(("com.apelon.akcds:" + name).getBytes());
  concept.setPrimordialUuid(primordial);
  EConceptAttributes conceptAttributes = new EConceptAttributes();
  conceptAttributes.setAuthorUuid(author);

  conceptAttributes = new EConceptAttributes();
  conceptAttributes.defined = false;
  conceptAttributes.primordialUuid = primordial;
  conceptAttributes.statusUuid = currentUuid;
  conceptAttributes.setPathUuid(path);
  conceptAttributes.setTime(time);
  concept.setConceptAttributes(conceptAttributes);

  List<TkDescription> descriptions = new ArrayList<TkDescription>();
  TkDescription description = new TkDescription();
  description.setConceptUuid(primordial);
  description.setLang("en");
  description.setPrimordialComponentUuid(UUID.randomUUID());

  description.setTypeUuid(preferredTerm);
  description.text = name;
  description.setStatusUuid(currentUuid);
  description.setAuthorUuid(author);
  description.setPathUuid(path);
  description.setTime(time);
  descriptions.add(description);
  concept.setDescriptions(descriptions);
  
  List<TkRelationship> relationships = new ArrayList<TkRelationship>();
  TkRelationship heirRel = createRelationships(concept, relParentPrimordial, relPrimordial);
  relationships.add(heirRel);
  concept.setRelationships(relationships);

  out.println("Wrote: " + concept);
  concept.writeExternal(dos);
  
  return primordial;
}

private TkRelationship createRelationships(EConcept eConcept, UUID targetPrimordial, UUID relPrimoridal) throws IOException, TerminologyException {
    relCounter++;
	long time = System.currentTimeMillis();

    UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
    UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
    UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
    
	
	TkRelationship rel = new TkRelationship();
	rel.setPrimordialComponentUuid(UUID.nameUUIDFromBytes(("com.apelon.akcds:rel"+relCounter).getBytes()));
	rel.setC1Uuid(eConcept.getPrimordialUuid());
	rel.setTypeUuid(relPrimoridal);
	rel.setC2Uuid(targetPrimordial);	  
	rel.setCharacteristicUuid(ArchitectonicAuxiliary.Concept.DEFINING_CHARACTERISTIC.getPrimoridalUid());
	rel.setRefinabilityUuid(ArchitectonicAuxiliary.Concept.NOT_REFINABLE.getPrimoridalUid());
	rel.setStatusUuid(currentUuid);
	rel.setAuthorUuid(author);
	rel.setPathUuid(path);
	rel.setTime(time);
	rel.setRelGroup(0);
	
	return rel;
}
}
