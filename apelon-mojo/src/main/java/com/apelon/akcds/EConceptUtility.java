package com.apelon.akcds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.attribute.TkConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refset.TkRefsetAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refset.str.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;
/**
 * Various constants and methods for building up workbench EConcepts.
 * @author Daniel Armbrust
 */
public class EConceptUtility
{
	private final UUID author_ = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
	private final UUID currentUuid_ = ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid();
	private final UUID path_ = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
	private final UUID preferredTerm_ = ArchitectonicAuxiliary.Concept.PREFERRED_DESCRIPTION_TYPE.getPrimoridalUid();
	private final UUID definingCharacteristic = ArchitectonicAuxiliary.Concept.DEFINING_CHARACTERISTIC.getPrimoridalUid();
	private final UUID notRefinable = ArchitectonicAuxiliary.Concept.NOT_REFINABLE.getPrimoridalUid();
	private final UUID isARel = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
	
	private final String lang_ = "en";
	
	private int relCounter_ = 0;
	private int annotationCounter_ = 0;
	
	private String uuidRoot_;

	public EConceptUtility(String uuidRoot) throws Exception
	{
		this.uuidRoot_ = uuidRoot;
	}

	public EConcept createConcept(UUID primordial, String preferredDescription, long time)
	{
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author_);
		conceptAttributes.setDefined(false);
		conceptAttributes.setPrimordialComponentUuid(primordial);
		conceptAttributes.setStatusUuid(currentUuid_);
		conceptAttributes.setPathUuid(path_);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);
		
		addDescription(concept, preferredTerm_, preferredDescription);

		return concept;
	}
	
	public TkDescription addDescription(EConcept concept, UUID descriptionType, String descriptionValue)
	{
		List<TkDescription> descriptions = concept.getDescriptions();
		if (descriptions == null)
		{
			descriptions = new ArrayList<TkDescription>();
			concept.setDescriptions(descriptions);
		}
		TkDescription description = new TkDescription();
		description.setConceptUuid(concept.getPrimordialUuid());
		description.setLang(lang_);
		description.setPrimordialComponentUuid(UUID.randomUUID());
		description.setTypeUuid(descriptionType);
		description.setText(descriptionValue);
		description.setStatusUuid(currentUuid_);
		description.setAuthorUuid(author_);
		description.setPathUuid(path_);
		description.setTime(System.currentTimeMillis());

		descriptions.add(description);
		return description;
	}
	
	public EIdentifierString addAdditionalIds(EConcept concept, String propTypeName, Object denotation, UUID authorityUUID)
	{
		if (denotation != null)
		{
			List<TkIdentifier> additionalIds = concept.getConceptAttributes().getAdditionalIdComponents();
			if (additionalIds == null)
			{
				additionalIds = new ArrayList<TkIdentifier>();
				concept.getConceptAttributes().setAdditionalIdComponents(additionalIds);
			}

			// create the identifier and add it to the additional ids list
			EIdentifierString cid = new EIdentifierString();
			additionalIds.add(cid);

			// populate the identifier with the usual suspects
			cid.setAuthorityUuid(authorityUUID);
			cid.setPathUuid(path_);
			cid.setStatusUuid(currentUuid_);
			cid.setTime(System.currentTimeMillis());
			// populate the actual value of the identifier
			cid.setDenotation(denotation);
			return cid;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(TkComponent<?> component, String name, String value, UUID refsetUUID)
	{
		List<TkRefsetAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefsetAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefsetStrMember strRefexMember = new TkRefsetStrMember();

			strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			strRefexMember.setStrValue(value);
			strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "annotation:" + annotationCounter_++).getBytes()));
			strRefexMember.setRefsetUuid(refsetUUID);
			strRefexMember.setStatusUuid(currentUuid_);
			strRefexMember.setAuthorUuid(author_);
			strRefexMember.setPathUuid(path_);
			strRefexMember.setTime(System.currentTimeMillis());
			annotations.add(strRefexMember);
			return strRefexMember;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(EConcept concept, String name, String value, UUID refsetUUID)
	{
		TkConceptAttributes conceptAttributes = concept.getConceptAttributes();
		return addAnnotation(conceptAttributes, name, value, refsetUUID);
	}
	
	/**
	 * relationshipPrimoridal is optional - if not provided, the default value of IS_A_REL is used.
	 */
	public TkRelationship addRelationship(EConcept concept, UUID targetPrimordial, UUID relationshipPrimoridal) 
	{
		List<TkRelationship> relationships = concept.getRelationships();
		if (relationships == null)
		{
			relationships = new ArrayList<TkRelationship>();
			concept.setRelationships(relationships);
		}
		 
		TkRelationship rel = new TkRelationship();
		rel.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "rel" + relCounter_++).getBytes()));
		rel.setC1Uuid(concept.getPrimordialUuid());
		rel.setTypeUuid(relationshipPrimoridal == null ? isARel : relationshipPrimoridal);
		rel.setC2Uuid(targetPrimordial);
		rel.setCharacteristicUuid(definingCharacteristic);
		rel.setRefinabilityUuid(notRefinable);
		rel.setStatusUuid(currentUuid_);
		rel.setAuthorUuid(author_);
		rel.setPathUuid(path_);
		rel.setTime(System.currentTimeMillis());
		rel.setRelGroup(0);  

		relationships.add(rel);
		return rel;
	}
	
	public int getCreatedRelCount()
	{
		return relCounter_;
	}
}
