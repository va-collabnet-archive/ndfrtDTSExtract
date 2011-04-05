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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.tapi.TerminologyException;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

import com.apelon.common.log4j.Categories;
import com.apelon.dts.client.DTSException;
import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.attribute.DTSRole;
import com.apelon.dts.client.attribute.DTSRoleType;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.ConceptChild;
import com.apelon.dts.client.concept.DTSConcept;
import com.apelon.dts.client.concept.NavChildContext;
import com.apelon.dts.client.concept.OntylogConcept;
import com.apelon.dts.client.namespace.Namespace;

/**
 * Goal which touches a timestamp file.
 *
 * @goal generate-apelon-data-hierarchy
 *
 * @phase process-sources
 */
public class DTSToEConcepts
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
private UUID[] priorParents = new UUID[20];

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
    	    UUID rootPrimordial = writeNDFRTEConcept(rootCon, touch, null);//create root concept
    	    out.println("***Root Primordial: " + rootPrimordial);
    	    conCounter++;
    	    getAllRelEConceptNames(touch);
    	    createSubroots(touch, rootPrimordial);
    	    out.close();
    		dos.flush();
    		dos.close();
      } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
      }

 }

public void createSubroots(File file, UUID rootPrimordial) throws Exception{
	String[] subrootNames = {"Cellular or Molecular Interactions [MoA]"};
			//"Chemical Ingredients [Chemical/Ingredient]"}; 
			//"Clinical Kinetics [PK]", "Diseases, Manifestations or Physiologic States [Disease/Finding]", "Dose Forms [Dose Form]",
			//"Physiological Effects [PE]", "Therapeutic Categories [TC]", "VA Drug Interactions [VA Drug Interaction]"};
	for (int i = 0; i < subrootNames.length; i++) {
		OntylogConcept con = (OntylogConcept) dbc.ontQry.findConceptByName(subrootNames[i], namespace_id, csd);
		priorParents[0] = writeNDFRTEConcept(con, file, rootPrimordial);//create concept
		NavChildContext nc = dbc.navQry.getNavChildContext(con, csd, ns);
		getChildren(nc, con, file);
		conCounter++;
	}	
}

public void getAllRelEConceptNames(File file) throws Exception{
	DTSRoleType[] roleTypes = dbc.ontQry.getAllRoleTypes();
	for (int i = 0; i < roleTypes.length; i++){
		String sRoleName = roleTypes[i].getName();
		writeRelEConcept(file, sRoleName);
	}
}

public DTSRole[] getInferredERoles(DTSConcept con) throws DTSException{
	OntylogConcept onc = (OntylogConcept) dbc.ontQry.findConceptByName(con.getName(), namespace_id, csd);
	DTSRole[] conInferredRols = onc.getFetchedRoles();
	return conInferredRols;
}

public void getChildren(NavChildContext mc, DTSConcept oc, File file) {
	try {
		ConceptChild[] ccs = mc.getChildren();
		if (ccs.length != 0){
			level++;
		}
		for (int i = 0; i < ccs.length; i++) {
			conCounter++;
			DTSConcept ocdChildCon = dbc.conQry.findConceptById(ccs[i]
					.getId(), namespace_id, csd);
			if ((conCounter % 1000) == 0) {
				System.out.println("Processed: " + conCounter);
				System.out.println("Wrote: " + ocdChildCon.getName());
			}
			if (ccs[i].getHasChildren() == true) {
				NavChildContext nc = dbc.navQry.getNavChildContext(
						ocdChildCon,
						csd, ns);
				priorParents[level] = writeNDFRTEConcept(ocdChildCon, file, priorParents[level-1]);//set previous concept that had children as parent
				getChildren(nc, ocdChildCon, file);
			} else {//leaf concept
				writeNDFRTEConcept(ocdChildCon, file, priorParents[level-1]);//set previous concept that had no children as parent but do not reset the parent primoridal
			}
			if (i + 1 == ccs.length){
				level = level-1;
			}
		}
	} catch (Exception e) {
		Categories.app().error(e);
		e.printStackTrace();
	}
}

public String getPropValue (DTSConcept oc, String sPropType){
	DTSProperty [] props = oc.getFetchedProperties();
	String propValue = null;
	Boolean found = false;
	for (int i = 0; i < props.length; i++) {
		if (props[i].getPropertyType().getName().equals(sPropType)){
			propValue = props[i].getValue();
			found = true;
		}
	}
	if (found == false && sPropType.equals("Display_Name")){
		propValue = oc.getName();
	}
	//System.out.println("Getting Prop Value for: " + oc.getName() + "Value: " + propValue);
	return propValue;
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
  concept.setConceptAttributes(conceptAttributes);

  List<TkDescription> descriptions = new ArrayList<TkDescription>();
  TkDescription description = new TkDescription();
  description.setConceptUuid(primordial);
  description.setLang("en");
  description.setPrimordialComponentUuid(UUID.randomUUID());

  description.setTypeUuid(preferredTerm);
  description.text = getPropValue(con, "Display_Name");
  description.setStatusUuid(currentUuid);
  description.setAuthorUuid(author);
  description.setPathUuid(path);
  description.setTime(time);
  descriptions.add(description);
  concept.setDescriptions(descriptions);

/*  List<TkRefsetAbstractMember<?>> annotations = new ArrayList<TkRefsetAbstractMember<?>>();
  TkRefsetStrMember strRefexMember = new TkRefsetStrMember();
  
  strRefexMember.setComponentUuid(description.getPrimordialComponentUuid());
  strRefexMember.setStrValue("Abilify Attribute");

  strRefexMember.setPrimordialComponentUuid(
          UUID.nameUUIDFromBytes(("com.apelon.akcds:1" +
          description.getPrimordialComponentUuid().toString() +
         strRefexMember.getStrValue()).getBytes()));

  strRefexMember.setRefsetUuid(RefsetAuxiliary.Concept.REFSET_IDENTITY.getPrimoridalUid());

  strRefexMember.setStatusUuid(currentUuid);
  strRefexMember.setAuthorUuid(author);
  strRefexMember.setPathUuid(path);
  strRefexMember.setTime(time);
  annotations.add(strRefexMember);

  description.setAnnotations(annotations);*/

  if (conCounter > 0){//create is_a hierarchical relationship
	List<TkRelationship> relationships = new ArrayList<TkRelationship>();
	TkRelationship heirRel = createRelationships(concept, parentPrimordial, relPrimordial);
	relationships.add(heirRel);
    DTSRole[] inferredRoles = getInferredERoles(con);
    for (int i = 0; i < inferredRoles.length; i++) {
    	OntylogConcept targetOCon = inferredRoles[i].getValueConcept();
    	DTSConcept targetDCon = dbc.thesQuery.findConceptByName(targetOCon.getName(), namespace_id, csd);
    	//System.out.println("concept: " + con.getName() + " target: " + targetDCon.getName());
    	TkRelationship roleRel = createRelationships(concept, UUID.nameUUIDFromBytes(("com.apelon.akcds:" + getPropValue(targetDCon, "NUI")).getBytes()), UUID.nameUUIDFromBytes(("com.apelon.akcds:" + inferredRoles[i].getName()).getBytes()));
    	relationships.add(roleRel);
    }
    
	concept.setRelationships(relationships);
  }

  out.println("Wrote: " + concept);
  concept.writeExternal(dos);
  
  return primordial;
}

public UUID writeRelEConcept(File file, String relName) throws Exception
{
   conCounter++;
   long time = System.currentTimeMillis();

   UUID currentUuid = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
   UUID path = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
   UUID preferredTerm = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
   UUID isa = ArchitectonicAuxiliary.Concept.IS_TERM_OF.getPrimoridalUid();
   UUID author = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
   UUID relPrimordial = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
   UUID archRoot = ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid();

  EConcept concept = new EConcept();
  UUID primordial = UUID.nameUUIDFromBytes(("com.apelon.akcds:" + relName).getBytes());
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
  description.text = relName;
  description.setStatusUuid(currentUuid);
  description.setAuthorUuid(author);
  description.setPathUuid(path);
  description.setTime(time);
  descriptions.add(description);
  concept.setDescriptions(descriptions);
  
  createRelationships(concept, archRoot, relPrimordial);

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
