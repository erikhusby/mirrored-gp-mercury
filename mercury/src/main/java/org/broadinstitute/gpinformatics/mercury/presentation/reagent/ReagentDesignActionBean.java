package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collection;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/reagent/design.action")
public class ReagentDesignActionBean extends CoreActionBean {

    private static final String CREATE_DESIGN = CoreActionBean.CREATE + " New Design";
    private static final String EDIT_DESIGN = CoreActionBean.EDIT + " Design: ";

    private static final String REAGENT_LIST_PAGE = "/reagent/list.jsp";
    private static final String REAGENT_CREATE_PAGE = "/reagent/create.jsp";

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @ValidateNestedProperties({
        @Validate(field="designName", required = true, maxlength=255, on={SAVE_ACTION}),
        @Validate(field="targetSetName", required = true, maxlength=2000, on={SAVE_ACTION})
    })
    private ReagentDesign reagentDesign;

    @Validate(required = true, on = {EDIT_ACTION})
    private String businessKey;

    private Collection<ReagentDesign> allReagentDesigns;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {EDIT_ACTION, SAVE_ACTION})
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (!StringUtils.isBlank(businessKey)) {
            reagentDesign = reagentDesignDao.findByBusinessKey(businessKey);
        } else {
            reagentDesign = new ReagentDesign();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allReagentDesigns = reagentDesignDao.findAll();
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void uniqueNameValidation(ValidationErrors errors) {

        if (reagentDesign.getOriginalName() == null) {
            setSubmitString(CREATE_DESIGN);
        } else {
            setSubmitString(EDIT_DESIGN);
        }

        // If we are creating the design or else if the original names do not match, check that the name is not a dupe
        if ((getSubmitString().equals(CREATE_DESIGN)) ||
            (!reagentDesign.getDesignName().equalsIgnoreCase(reagentDesign.getOriginalName()))) {

            // Check if there is an existing research project and error out if it already exists
            ReagentDesign existingDesign = reagentDesignDao.findByBusinessKey(reagentDesign.getDesignName());
            if (existingDesign != null) {
                errors.add("designName", new SimpleError("A reagent already exists with that design name."));
            }
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(REAGENT_LIST_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            reagentDesignDao.persist(reagentDesign);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        addMessage(getSubmitString() + " '" + reagentDesign.getBusinessKey() + "' was successful");
        return new RedirectResolution(ReagentDesignActionBean.class, LIST_ACTION);
    }

    public Collection<ReagentDesign> getAllReagentDesigns() {
        return allReagentDesigns;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public ReagentDesign getReagentDesign() {
        return reagentDesign;
    }

    public void setReagentDesign(ReagentDesign reagentDesign) {
        this.reagentDesign = reagentDesign;
    }
}
