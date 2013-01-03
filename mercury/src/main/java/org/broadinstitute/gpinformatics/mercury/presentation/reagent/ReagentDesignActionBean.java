package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
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
        @Validate(field="designName", required = true, maxlength=255, on={"save"}),
        @Validate(field="targetSetName", required = true, maxlength=2000, on={"save"})
    })
    private ReagentDesign reagentDesign;

    @Validate(required = true, on = {"edit"})
    private String businessKey;

    private Collection<ReagentDesign> allReagentDesigns;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"edit"})
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (businessKey != null) {
            reagentDesign = reagentDesignDao.findByBusinessKey(businessKey);
        }
    }

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"save"})
    public void init2() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (businessKey != null) {
            reagentDesign = reagentDesignDao.findByBusinessKey(businessKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allReagentDesigns = reagentDesignDao.findAll();
    }

    @ValidationMethod(on = "save")
    public void uniqueNameValidation(ValidationErrors errors) {

        if (reagentDesign.getOriginalName() == null) {
            setSubmitString(CREATE_DESIGN);
        } else {
            setSubmitString(EDIT_DESIGN);
        }

        // If we are creating the design or else if the original names do not match, check that the name is not a dupe
        if ((getSubmitString() == CREATE_DESIGN) ||
            (!reagentDesign.getDesignName().equalsIgnoreCase(reagentDesign.getOriginalName()))) {

            // Check if there is an existing research project and error out if it already exists
            ReagentDesign existingDesign = reagentDesignDao.findByBusinessKey(reagentDesign.getDesignName());
            if (existingDesign != null) {
                errors.add("designName", new SimpleError(getSubmitString() + " was successful"));
            }
        }
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(REAGENT_LIST_PAGE);
    }

    @HandlesEvent("edit")
    public Resolution edit() {
        setSubmitString(EDIT_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @HandlesEvent("create")
    public Resolution create() {
        setSubmitString(CREATE_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @HandlesEvent("save")
    public Resolution save() {
        try {
            reagentDesignDao.persist(reagentDesign);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Reagent Design \"" + reagentDesign.getBusinessKey() + "\" has been created");
        return new RedirectResolution(ReagentDesignActionBean.class, "list");
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
