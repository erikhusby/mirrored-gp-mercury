package org.broadinstitute.gpinformatics.mercury.presentation.security;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthorizationService;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collection;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/security/authorizePage.action")
public class AuthorizePageActionBean extends CoreActionBean {
    private static final String CREATE_AUTH = CoreActionBean.CREATE + "New Page Authorization";
    private static final String EDIT_AUTH = CoreActionBean.EDIT + "Page Authorization: ";

    private static final String AUTHORIZE_PAGE_LIST = "/authorize/list.jsp";
    private static final String AUTHORIZE_PAGE_CREATE = "/authorize/create.jsp";

    @Inject
    private AuthorizationService authorizationService;

    private Collection<PageAuthorization> allPageAuthorizations;

    private String originalPagePath = null;

    @ValidateNestedProperties({
        @Validate(field="authorizationId", required = true, maxlength=2000, on={"edit"}),
        @Validate(field="pagePath", required = true, maxlength=2000, on={"save"})
    })
    private PageAuthorization pageAuthorization;

    @After(stages = LifecycleStage.BindingAndValidation, on="list")
    public void setupList() {
        allPageAuthorizations = authorizationService.getAllAuthorizedPages();
    }

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"edit", "save"})
    public void init() {
        String authId = getContext().getRequest().getParameter("pageAuthorization.authorizationId");
        if (authId != null) {
            pageAuthorization = authorizationService.findById(Long.valueOf(authId));
            originalPagePath = pageAuthorization.getPagePath();
        }
    }

    @ValidationMethod(on = "save")
    public void uniqueNameValidation(ValidationErrors errors) {
        // If the there is no original page path, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed
        if ((originalPagePath == null) ||
                (!originalPagePath.equalsIgnoreCase(pageAuthorization.getPagePath()))) {
            // Check if there is an existing research project and error out if it already exists
            PageAuthorization existingAuth = authorizationService.findByPage(pageAuthorization.getPagePath());
            if (existingAuth != null) {
                errors.add("pathName", new SimpleError("This page is already set up for authentication."));
            }
        }
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(AUTHORIZE_PAGE_LIST);
    }

    @HandlesEvent("create")
    public Resolution create() {
        setSubmitString(CREATE_AUTH);
        return new ForwardResolution(AUTHORIZE_PAGE_CREATE);
    }

    @HandlesEvent("edit")
    public Resolution edit() {
        setSubmitString(EDIT_AUTH);
        return new ForwardResolution(AUTHORIZE_PAGE_CREATE);
    }

    @HandlesEvent("save")
    public Resolution save() {
        try {
            if (pageAuthorization.getAuthorizationId() == null) {
                authorizationService.addNewPageAuthorization(
                    pageAuthorization.getPagePath(), pageAuthorization.getRoleList());
            } else {
                authorizationService.addRolesToPage(pageAuthorization.getPagePath(), pageAuthorization.getRoleList());
            }
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Athorization for \"" + pageAuthorization.getPagePath() + "\" has been saved");
        return new RedirectResolution(AuthorizePageActionBean.class, "list");
    }

    public PageAuthorization getPageAuthorization() {
        return pageAuthorization;
    }

    public void setPageAuthorization(PageAuthorization pageAuthorization) {
        this.pageAuthorization = pageAuthorization;
    }

    public Collection<PageAuthorization> getAllPageAuthorizations() {
        return allPageAuthorizations;
    }
}
