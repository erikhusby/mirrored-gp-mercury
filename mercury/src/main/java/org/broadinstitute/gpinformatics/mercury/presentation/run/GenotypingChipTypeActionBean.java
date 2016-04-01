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
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * This handles creating and editing GenotypingChipTypes.
 */
@UrlBinding("/genotyping/chipType.action")
public class GenotypingChipTypeActionBean extends CoreActionBean {
    public static final String CHIP_NAME_PARAMETER = "chipName";
    public static final String CHIP_FAMILY_PARAMETER = "chipFamily";

    // Prefix that selects the genotyping chip family names from the attribute_definition database table.
    public static final String ATTRIBUTE_DEF_FAMILY_PREFIX = "GenotypingChip_";

    private static final String CREATE_CHIP_TYPE = CoreActionBean.CREATE + "Chip Type";
    private static final String EDIT_CHIP_TYPE = CoreActionBean.EDIT + "Chip Type";

    private static final String CHIP_TYPE_LIST_PAGE = "/genotyping/chip_type_list.jsp";
    private static final String CHIP_TYPE_CREATE_PAGE = "/genotyping/chip_type_create.jsp";

    private List<String> chipFamilies;
    private List<AttributeArchetype> attributeArchetypes;

    private AttributeArchetype attributeArchetype;

    @Validate(required = true, on = {SAVE_ACTION})
    private String chipName;

    @Validate(required = true, on = {CREATE_ACTION, EDIT_ACTION, SAVE_ACTION})
    private String chipFamily;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    public GenotypingChipTypeActionBean() {
        super(CREATE_CHIP_TYPE, EDIT_CHIP_TYPE, CHIP_NAME_PARAMETER);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        chipFamily = getContext().getRequest().getParameter(CHIP_FAMILY_PARAMETER);
        if (StringUtils.isBlank(chipFamily)) {
            // Populates the GenotypingChip families for the jsp dropdown.
            chipFamilies = attributeArchetypeDao.findFamilyNamesByPrefix(ATTRIBUTE_DEF_FAMILY_PREFIX);
        } else {
            chipName = getContext().getRequest().getParameter(CHIP_NAME_PARAMETER);
            if (StringUtils.isNotBlank(chipName)) {
                // Looks up the chip.
                attributeArchetype = attributeArchetypeDao.findByName(chipFamily, chipName);
            }
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        if (StringUtils.isNotBlank(chipFamily)) {
            attributeArchetypes = attributeArchetypeDao.findAllByFamily(chipFamily);
        } else {
            addMessage("Please select a chip family.");
        }
        return new ForwardResolution(CHIP_TYPE_LIST_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_CHIP_TYPE);
        return new ForwardResolution(CHIP_TYPE_CREATE_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_CHIP_TYPE);
        return new ForwardResolution(CHIP_TYPE_CREATE_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            attributeArchetypeDao.persist(attributeArchetype);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        addMessage(getSubmitString() + " '" + attributeArchetype.getArchetypeName() + "' was successful");
        return new RedirectResolution(GenotypingChipTypeActionBean.class, LIST_ACTION);
    }

}
