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
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handles creating and editing GenotypingChipTypes.
 */
@UrlBinding("/genotyping/chipType.action")
public class GenotypingChipTypeActionBean extends CoreActionBean {
    public static final String CHIP_NAME_PARAMETER = "chipName";
    public static final String CHIP_FAMILY_PARAMETER = "chipFamily";

    // Attribute that identifies the genotyping chip family names.
    public static final String GENOTYPING_CHIP_MARKER_ATTRIBUTE = "GenotypingChip_";

    private static final String CREATE_CHIP_TYPE = CoreActionBean.CREATE + "Chip Type";
    private static final String EDIT_CHIP_TYPE = CoreActionBean.EDIT + "Chip Type";

    private static final String CHIP_TYPE_LIST_PAGE = "/run/genotyping_chip_list.jsp";
    private static final String CHIP_TYPE_EDIT_PAGE = "/run/genotyping_chip_edit.jsp";

    private List<String> chipFamilies;
    private List<AttributeArchetype> chipTypes;
    private List<ArchetypeAttribute> attributes;
    private Map<String, AttributeDefinition> attributeDefinitions;
    private AttributeArchetype archetype;
    private String chipRadio;

    @Validate(required = true, on = {SAVE_ACTION})
    private String chipName;

//    @Validate(required = true, on = {CREATE_ACTION, EDIT_ACTION, SAVE_ACTION})
    private String chipFamily;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    public GenotypingChipTypeActionBean() {
        super(CREATE_CHIP_TYPE, EDIT_CHIP_TYPE, CHIP_NAME_PARAMETER);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        // Populates the Genotyping chip families for the jsp dropdown.
        chipFamilies = attributeArchetypeDao.findFamiliesIdentifiedByAttribute(GENOTYPING_CHIP_MARKER_ATTRIBUTE,
                GENOTYPING_CHIP_MARKER_ATTRIBUTE);
        // As a convenience, pre-select when there's only one choice.
        if (chipFamilies.size() == 1) {
            chipFamily = chipFamilies.get(0);
        }
        if (!StringUtils.isBlank(chipFamily)) {
            attributeDefinitions = new HashMap<>();
            for (AttributeDefinition def : attributeArchetypeDao.findAttributeDefinitionsByFamily(chipFamily)) {
                attributeDefinitions.put(def.getAttributeName(), def);
            }
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        if (StringUtils.isNotBlank(chipFamily)) {
            chipTypes = attributeArchetypeDao.findAllByFamily(chipFamily);
            if (chipTypes.size() == 0) {
                addMessage("No chips found for chip family '" + chipFamily + "'");
            }
        } else {
            addMessage("Please select a chip family.");
        }
        return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        extractAttributes(chipRadio);
        chipName = chipRadio;
        setSubmitString(EDIT_CHIP_TYPE);
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        extractAttributes(chipRadio);
        chipName = "";
        setSubmitString(CREATE_CHIP_TYPE);
        return new ForwardResolution(CHIP_TYPE_EDIT_PAGE);
    }

    private void extractAttributes(String archetypeName) {
        if (StringUtils.isNotBlank(archetypeName)) {
            archetype = attributeArchetypeDao.findByName(chipFamily, archetypeName);
            if (archetype != null) {
                attributes = new ArrayList<>();
                for (ArchetypeAttribute attribute : archetype.getAttributes()) {
                    if (attributeDefinitions.get(attribute.getAttributeName()).isDisplayedInUi()) {
                        attributes.add(new ArchetypeAttribute(null, attribute.getAttributeName(),
                                attribute.getAttributeValue()));
                    }
                }
            }
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            // Determines if there are any changes. If so, creates a new Archetype version
            // with its own attributes and leaves the existing archetype and attributes as-is.
            // Attribute names cannot be added or deleted here. That requires a data fixup.
            Map<String, String> attributeMap = new HashMap<>();
            for (ArchetypeAttribute attribute : attributes) {
                attributeMap.put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
            boolean foundChange = (attributeArchetypeDao.createArchetypeVersion(archetype.getAttributeFamily(),
                    archetype.getArchetypeName(), attributeMap) != null);
            addMessage(getSubmitString() + " " + archetype.getArchetypeName() +
                       (foundChange? " created a new chip version." : " leaves the chip unchanged."));
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

    public void setChipTypes(
            List<AttributeArchetype> chipTypes) {
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

    public List<ArchetypeAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ArchetypeAttribute> attributes) {
        this.attributes = attributes;
    }

    public String getChipRadio() {
        return chipRadio;
    }

    public void setChipRadio(String chipRadio) {
        this.chipRadio = chipRadio;
    }
}
