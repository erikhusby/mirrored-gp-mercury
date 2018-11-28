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
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handles creating and editing GenotypingChipTypes.
 */
@UrlBinding("/genotyping/chipType.action")
public class GenotypingChipTypeActionBean extends CoreActionBean {

    private static final String CREATE_CHIP_TYPE = CoreActionBean.CREATE + "Chip Type";
    private static final String EDIT_CHIP_TYPE = CoreActionBean.EDIT + "Chip Type";
    private static final String CHIP_TYPE_LIST_PAGE = "/run/genotyping_chip_list.jsp";
    private static final String CHIP_TYPE_EDIT_PAGE = "/run/genotyping_chip_edit.jsp";

    private List<String> chipFamilies;
    private List<GenotypingChip> chipTypes;
    private Map<String, String> attributes = new HashMap<>();
    private Map<String, AttributeDefinition> definitions = null;

    private String chipName;
    private String saveChipName;
    private String chipFamily;
    private String selectedFamily;
    private final String namespace = getClass().getCanonicalName();

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    public GenotypingChipTypeActionBean() {
        super(CREATE_CHIP_TYPE, EDIT_CHIP_TYPE, "");
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        // Populates the Genotyping chip families for the jsp dropdown.
        chipFamilies = new ArrayList<String>(attributeArchetypeDao.findGenotypingChipFamilies());
        Collections.sort(chipFamilies);
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        chipFamily = selectedFamily;
        chipTypes = new ArrayList<>(attributeArchetypeDao.findGenotypingChips(chipFamily));
        Collections.sort(chipTypes, attributeArchetypeDao.BY_ARCHETYPE_NAME);
        chipName = !chipTypes.isEmpty() ? chipTypes.get(0).getArchetypeName() : null;
        return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_CHIP_TYPE);
        if (StringUtils.isBlank(chipName)) {
            return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
        }
        populateAttributes(chipName);
        saveChipName = chipName;
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_CHIP_TYPE);
        if (StringUtils.isBlank(chipName)) {
            return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
        }
        populateAttributes(chipName);
        saveChipName = "";
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            boolean foundChange = false;
            GenotypingChip chip = attributeArchetypeDao.findGenotypingChip(chipFamily, saveChipName);
            if (chip != null) {
                if (isCreating()) {
                    addValidationError("saveChipName", "Chip name is already in use.");
                    return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
                }
            } else {
                if (StringUtils.isBlank(saveChipName)) {
                    addValidationError("saveChipName", "Chip name must not be blank.");
                    return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
                }
                // Adds the new chip type with null valued attributes.
                foundChange = true;
                chip = new GenotypingChip(chipFamily, saveChipName, getDefinitionsMap().values());
            }
            // Updates the values for the displayable attributes. A data fixup is needed to delete or add new
            // attributes and their definitions.
            for (ArchetypeAttribute existingAttribute : chip.getAttributes()) {
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
                // Updates the last modified date attribute.
                chip.setLastModifiedDate();
                attributeArchetypeDao.persist(chip);
                addMessage(chip.getArchetypeName() + " was successfully updated.");
            } else {
                addMessage("No changes found. " + chip.getArchetypeName() + " remains as it was.");
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

    public List<GenotypingChip> getChipTypes() {
        return chipTypes;
    }

    public void setChipTypes(List<GenotypingChip> chipTypes) {
        this.chipTypes = chipTypes;
    }

    public String getChipName() {
        return chipName;
    }

    public void setChipName(String chipName) {
        this.chipName = chipName;
    }

    public String getSaveChipName() {
        return saveChipName;
    }

    public void setSaveChipName(String saveChipName) {
        this.saveChipName = saveChipName;
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

    private void populateAttributes(String name) {
        GenotypingChip chip = attributeArchetypeDao.findGenotypingChip(chipFamily, name);
        if (chip != null) {
            attributes.clear();
            for (ArchetypeAttribute attribute : chip.getAttributes()) {
                AttributeDefinition definition = getDefinitionsMap().get(attribute.getAttributeName());
                if (definition != null && definition.isDisplayable()) {
                    attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
                }
            }
        }
    }

    private Map<String, AttributeDefinition> getDefinitionsMap() {
        if (definitions == null) {
            definitions = attributeArchetypeDao.findGenotypingChipAttributeDefinitions(chipFamily);
        }
        return definitions;
    }

}
