package com.apelon.akcds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.etypes.EConceptAttributes;
import org.ihtsdo.etypes.EIdentifierString;
import org.ihtsdo.tk.binding.snomed.SnomedMetadataRf2;
import org.ihtsdo.tk.dto.concept.component.TkComponent;
import org.ihtsdo.tk.dto.concept.component.TkRevision;
import org.ihtsdo.tk.dto.concept.component.attribute.TkConceptAttributes;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refex.TkRefexAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_string.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_uuid.TkRefexUuidMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

import com.apelon.akcds.counter.LoadStats;
import com.apelon.akcds.counter.UUIDInfo;
/**
 * Various constants and methods for building up workbench EConcepts.
 * @author Daniel Armbrust
 */
//TODO - this code is copy/paste inheritance from the SPL loader.  Really need to share this code....

public class EConceptUtility
{
	private final UUID author_ = ArchitectonicAuxiliary.Concept.USER.getPrimoridalUid();
	private final UUID statusCurrentUuid_ = SnomedMetadataRf2.ACTIVE_VALUE_RF2.getUuids()[0];
	private final UUID statusRetiredUuid_ = SnomedMetadataRf2.INACTIVE_VALUE_RF2.getUuids()[0];
	private final UUID path_ = ArchitectonicAuxiliary.Concept.SNOMED_CORE.getPrimoridalUid();
	private final UUID synonym_ = SnomedMetadataRf2.SYNONYM_RF2.getUuids()[0];
	private final UUID fullySpecifiedName_ = SnomedMetadataRf2.FULLY_SPECIFIED_NAME_RF2.getUuids()[0];
	private final UUID synonymAcceptable_ = SnomedMetadataRf2.ACCEPTABLE_RF2.getUuids()[0];
	private final UUID synonymPreferred_ = SnomedMetadataRf2.PREFERRED_RF2.getUuids()[0];
	private final UUID usEnRefset_ = SnomedMetadataRf2.US_ENGLISH_REFSET_RF2.getUuids()[0];
	private final UUID definingCharacteristic_ = SnomedMetadataRf2.STATED_RELATIONSHIP_RF2.getUuids()[0];
	private final UUID notRefinable = ArchitectonicAuxiliary.Concept.NOT_REFINABLE.getPrimoridalUid();
	private final UUID isARel = ArchitectonicAuxiliary.Concept.IS_A_REL.getPrimoridalUid();
	private final UUID module_ = TkRevision.unspecifiedModuleUuid;
	
	private final String lang_ = "en";
	
	//Used for making unique UUIDs
	private int relCounter_ = 0;
	private int annotationCounter_ = 0;
	private int descCounter_ = 0;
	private int conceptAnnotationCounter_ = 0;
	
	private LoadStats ls_ = new LoadStats();
	
	private String uuidRoot_;

	public EConceptUtility(String uuidRoot) throws Exception
	{
		this.uuidRoot_ = uuidRoot;
		UUIDInfo.add(isARel, "isA");
		UUIDInfo.add(synonym_, "Display_Name->Synonym");
		UUIDInfo.add(fullySpecifiedName_, "Display_Name->Fully Specified Name");
		UUIDInfo.add(usEnRefset_, "US English Refset");
	}

	public EConcept createConcept(UUID primordial, String preferredDescription, long time)
	{
		return createConcept(primordial, preferredDescription, time, statusCurrentUuid_);
	}

	public EConcept createConcept(UUID primordial, String preferredDescription, long time, UUID status)
	{
		EConcept concept = new EConcept();
		concept.setPrimordialUuid(primordial);
		EConceptAttributes conceptAttributes = new EConceptAttributes();
		conceptAttributes.setAuthorUuid(author_);
		conceptAttributes.setDefined(false);
		conceptAttributes.setPrimordialComponentUuid(primordial);
		conceptAttributes.setStatusUuid(status);
		conceptAttributes.setPathUuid(path_);
		conceptAttributes.setModuleUuid(module_);
		conceptAttributes.setTime(time);
		concept.setConceptAttributes(conceptAttributes);
		
		addSynonym(concept, usEnRefset_, preferredDescription, true);
		addFullySpecifiedName(concept, usEnRefset_, preferredDescription);

		ls_.addConcept();
		return concept;
	}
	
	public TkDescription addSynonym(EConcept concept, UUID languageRefset, String synonym, boolean preferred)
	{
		TkDescription d = addDescription(concept, synonym, synonym_, false);
		addAnnotation(d, (preferred ? synonymPreferred_ : synonymAcceptable_), languageRefset);
		return d;
	}
	
	public TkDescription addFullySpecifiedName(EConcept concept, UUID languageRefset, String fullySpecifiedName)
	{
		TkDescription d = addDescription(concept, fullySpecifiedName, fullySpecifiedName_, false);
		addAnnotation(d, synonymPreferred_, languageRefset);
		return d;
	}
	
	public TkDescription addDescription(EConcept concept, String descriptionValue, UUID descriptionType, boolean retired)
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
		description.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "descr:" + descCounter_++).getBytes()));
		description.setTypeUuid(descriptionType);
		description.setText(descriptionValue);
		description.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
		description.setAuthorUuid(author_);
		description.setPathUuid(path_);
		description.setModuleUuid(module_);
		description.setTime(System.currentTimeMillis());

		descriptions.add(description);
		ls_.addDescription(UUIDInfo.getUUIDBaseStringLastSection(descriptionType));
		return description;
	}
	
	public EIdentifierString addAdditionalIds(EConcept concept, Object denotation, UUID authorityUUID, boolean retired)
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
			cid.setAuthorUuid(author_);
			cid.setPathUuid(path_);
			cid.setModuleUuid(module_);
			cid.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
			cid.setTime(System.currentTimeMillis());
			// populate the actual value of the identifier
			cid.setDenotation(denotation);
			ls_.addId(UUIDInfo.getUUIDBaseStringLastSection(authorityUUID));
			return cid;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(TkComponent<?> component, String value, UUID refsetUUID, boolean retired)
	{
		List<TkRefexAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefexAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefsetStrMember strRefexMember = new TkRefsetStrMember();

			strRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			strRefexMember.setString1(value);
			strRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "annotation:" + annotationCounter_++).getBytes()));
			strRefexMember.setRefsetUuid(refsetUUID);
			strRefexMember.setStatusUuid(retired ? statusRetiredUuid_ : statusCurrentUuid_);
			strRefexMember.setAuthorUuid(author_);
			strRefexMember.setPathUuid(path_);
			strRefexMember.setModuleUuid(module_);
			strRefexMember.setTime(System.currentTimeMillis());
			annotations.add(strRefexMember);
			if (component instanceof TkConceptAttributes)
			{
				ls_.addAnnotation("Concept", UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else if (component instanceof TkRelationship)
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(((TkRelationship) component).getTypeUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else if (component instanceof TkRefsetStrMember)
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(((TkRefsetStrMember) component).getRefexUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(component.getPrimordialComponentUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			return strRefexMember;
		}
		return null;
	}
	
	public TkRefsetStrMember addAnnotation(EConcept concept, String value, UUID refsetUUID, boolean retired)
	{
		TkConceptAttributes conceptAttributes = concept.getConceptAttributes();
		return addAnnotation(conceptAttributes, value, refsetUUID, retired);
	}
	
	public TkRefexUuidMember addAnnotation(TkComponent<?> component, UUID value, UUID refsetUUID)
	{
		List<TkRefexAbstractMember<?>> annotations = component.getAnnotations();

		if (annotations == null)
		{
			annotations = new ArrayList<TkRefexAbstractMember<?>>();
			component.setAnnotations(annotations);
		}

		if (value != null)
		{
			TkRefexUuidMember conceptRefexMember = new TkRefexUuidMember();

			conceptRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			conceptRefexMember.setUuid1(value);
			conceptRefexMember.setPrimordialComponentUuid(UUID.nameUUIDFromBytes((uuidRoot_ + "conceptAnnotation:" + conceptAnnotationCounter_++).getBytes()));
			conceptRefexMember.setRefsetUuid(refsetUUID);
			conceptRefexMember.setStatusUuid(statusCurrentUuid_);
			conceptRefexMember.setAuthorUuid(author_);
			conceptRefexMember.setPathUuid(path_);
			conceptRefexMember.setModuleUuid(module_);
			conceptRefexMember.setTime(System.currentTimeMillis());
			annotations.add(conceptRefexMember);
			if (component instanceof TkConceptAttributes)
			{
				ls_.addAnnotation("Concept", UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else if (component instanceof TkDescription)
			{
				ls_.addAnnotation("Description", UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else if (component instanceof TkRelationship)
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(((TkRelationship) component).getTypeUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else if (component instanceof TkRefsetStrMember)
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(((TkRefsetStrMember) component).getRefexUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			else
			{
				ls_.addAnnotation(UUIDInfo.getUUIDBaseStringLastSection(component.getPrimordialComponentUuid()), UUIDInfo.getUUIDBaseStringLastSection(refsetUUID));
			}
			return conceptRefexMember;
		}
		return null;
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
		rel.setCharacteristicUuid(definingCharacteristic_);
		rel.setRefinabilityUuid(notRefinable);
		rel.setStatusUuid(statusCurrentUuid_);
		rel.setAuthorUuid(author_);
		rel.setPathUuid(path_);
		rel.setModuleUuid(module_);
		rel.setTime(System.currentTimeMillis());
		rel.setRelGroup(0);  

		relationships.add(rel);
		ls_.addRelationship(UUIDInfo.getUUIDBaseStringLastSection(relationshipPrimoridal == null ? isARel : relationshipPrimoridal));
		return rel;
	}
	
	public LoadStats getLoadStats()
	{
		return ls_;
	}
	
	public void clearLoadStats()
	{
		ls_ = new LoadStats();
	}
}
