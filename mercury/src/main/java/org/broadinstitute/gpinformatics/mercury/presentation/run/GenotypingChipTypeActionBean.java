/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.run;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handles creating and editing GenotypingChipTypes.
 */
@UrlBinding("/genotyping/chipType.action")
public class GenotypingChipTypeActionBean extends CoreActionBean {

    // Attribute that identifies the genotyping chip family names.
    public static final String GENOTYPING_CHIP_MARKER_ATTRIBUTE = "GenotypingChip_";
    // Attribute for last modified date.
    public static final String LAST_MODIFIED = "LastModifiedDate";

    private static final String CREATE_CHIP_TYPE = CoreActionBean.CREATE + "Chip Type";
    private static final String EDIT_CHIP_TYPE = CoreActionBean.EDIT + "Chip Type";

    private static final String CHIP_TYPE_LIST_PAGE = "/run/genotyping_chip_list.jsp";
    private static final String CHIP_TYPE_EDIT_PAGE = "/run/genotyping_chip_edit.jsp";

    private List<String> chipFamilies;
    private List<AttributeArchetype> chipTypes;
    private Map<String, String> attributes = new HashMap<>();
    private Map<String, AttributeDefinition> definitions = null;

    private String chipName;
    private String selectedChipName;

    private String chipFamily;
    private String selectedFamily;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    public GenotypingChipTypeActionBean() {
        super(CREATE_CHIP_TYPE, EDIT_CHIP_TYPE, "");
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        // Populates the Genotyping chip families for the jsp dropdown.
        chipFamilies = attributeArchetypeDao.findFamiliesIdentifiedByAttribute(GENOTYPING_CHIP_MARKER_ATTRIBUTE,
                GENOTYPING_CHIP_MARKER_ATTRIBUTE);
    }


    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        chipTypes = attributeArchetypeDao.findAllByFamily(selectedFamily);
        chipFamily = selectedFamily;
        return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_CHIP_TYPE);
        if (StringUtils.isBlank(selectedChipName)) {
            return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
        }
        setSubmitString(EDIT_CHIP_TYPE);
        populateAttributes(selectedChipName);
        chipName = selectedChipName;
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_CHIP_TYPE);
        if (StringUtils.isBlank(selectedChipName)) {
            return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
        }
        populateAttributes(selectedChipName);
        chipName = "";
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            boolean foundChange = false;
            AttributeArchetype archetype = attributeArchetypeDao.findByName(chipFamily, chipName);
            if (archetype == null) {
                // Adds the new chip type.
                foundChange = true;
                archetype = new AttributeArchetype(chipFamily, chipName);
                for (AttributeDefinition definition : getDefinitionsMap().values()) {
                    if (!definition.isFamilyAttribute()) {
                        archetype.getAttributes().add(new ArchetypeAttribute(archetype,
                                definition.getAttributeName(), null));
                    }
                }
            }
            // Updates the values for the displayable attributes. A data fixup is needed to delete or add new
            // attributes and their definitions.
            for (ArchetypeAttribute existingAttribute : archetype.getAttributes()) {
                if (getDefinitionsMap().get(existingAttribute.getAttributeName()).isDisplayable()) {
                    String newValue = attributes.get(existingAttribute.getAttributeName());
                    String oldValue = existingAttribute.getAttributeValue();
                    if (oldValue == null && newValue != null || oldValue != null && !oldValue.equals(newValue)) {
                        foundChange = true;
                        existingAttribute.setAttributeValue(newValue);
                    }
                }
            }
            if (foundChange) {
                // Updates the modified date attribute.
                for (ArchetypeAttribute existingAttribute : archetype.getAttributes()) {
                    if (existingAttribute.getAttributeName().equals(LAST_MODIFIED)) {
                        existingAttribute.setAttributeValue(DateUtils.getYYYYMMMDDTime(new Date()));
                    }
                }
                attributeArchetypeDao.persist(archetype);
                addMessage(archetype.getArchetypeName() + " was successfully updated.");
            } else {
                addMessage("No changes found. " + archetype.getArchetypeName() + " remains as it was.");
            }
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }
        return new RedirectResolution(GenotypingChipTypeActionBean.class, LIST_ACTION);
    }

    public List<String> getChipFamilies() {
        return chipFamilies;
    }

    public void setChipFamilies(List<String> chipFamilies) {
        this.chipFamilies = chipFamilies;
    }

    public List<AttributeArchetype> getChipTypes() {
        return chipTypes;
    }

    public void setChipTypes(List<AttributeArchetype> chipTypes) {
        this.chipTypes = chipTypes;
    }

    public String getChipName() {
        return chipName;
    }

    public void setChipName(String chipName) {
        this.chipName = chipName;
    }

    public String getChipFamily() {
        return chipFamily;
    }

    public void setChipFamily(String chipFamily) {
        this.chipFamily = chipFamily;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getSelectedFamily() {
        return selectedFamily;
    }

    public void setSelectedFamily(String selectedFamily) {
        this.selectedFamily = selectedFamily;
    }

    public String getSelectedChipName() {
        return selectedChipName;
    }

    public void setSelectedChipName(String selectedChipName) {
        this.selectedChipName = selectedChipName;
    }

    private void populateAttributes(String name) {
        AttributeArchetype archetype = attributeArchetypeDao.findByName(chipFamily, name);
        if (archetype != null) {
            attributes.clear();
            for (ArchetypeAttribute attribute : archetype.getAttributes()) {
                AttributeDefinition definition = getDefinitionsMap().get(attribute.getAttributeName());
                if (definition != null && definition.isDisplayable()) {
                    attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
                }
            }
        }
    }

    private Map<String, AttributeDefinition> getDefinitionsMap() {
        if (definitions == null) {
            definitions = attributeArchetypeDao.findAttributeDefinitionsByFamily(chipFamily);
        }
        return definitions;
    }

}
