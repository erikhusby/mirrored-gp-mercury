package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is for ...
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/projects/project.action")
public class ResearchProjectActionBean extends CoreActionBean {
    private static final String CREATE = "Create New Product";
    private static final String EDIT = "Edit Product: ";

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private CohortListBean cohortList;

    @Inject
    private FundingListBean fundingList;

    private String businessKey;

    private ResearchProject editResearchProject;

    private String submitString;

    @Inject
    private JiraLink jiraLink;

    /*
     * The search query.
     */
    private String q;

    /**
     * All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped).
     */
    private List<ResearchProject> allResearchProjects;

    /**
     * On demand counts of orders on the project. Map of business key to count value *
     */
    private Map<String, Long> projectOrderCounts;

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allResearchProjects = researchProjectDao.findAllResearchProjects();
    }

    /**
     * Initialize the project with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "save"})
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (businessKey != null) {
            editResearchProject = researchProjectDao.findByBusinessKey(businessKey);
        }
    }

    public Map<String, Long> getResearchProjectCounts() {
        if (projectOrderCounts == null) {
            projectOrderCounts = researchProjectDao.getProjectOrderCounts();
        }

        return projectOrderCounts;
    }

    /**
     * Returns a list of all research project statuses.
     *
     * @return list of all research project statuses
     */
    public List<ResearchProject.Status> getAllProjectStatuses() {
        return Arrays.asList(ResearchProject.Status.values());
    }

    @Default
    public Resolution list() {
        return new ForwardResolution(PROJECT_LIST_PAGE);
    }


    public Resolution create() {
        submitString = CREATE;
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution edit() {
        submitString = EDIT;
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution view() {
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    public String getSumbitString() {
        return submitString;
    }

    public List<ResearchProject> getAllResearchProjects() {
        return allResearchProjects;
    }

    private String getUserListString(Long[] ids) {
        String listString = "";

        if (ids != null) {
            String[] nameList = new String[ids.length];
            int i = 0;
            for (Long id : ids) {
                BspUser user = bspUserList.getById(id);
                nameList[i++] = user.getFirstName() + " " + user.getLastName();
            }

            listString = StringUtils.join(nameList, ", ");
        }

        return listString;
    }

    /**
     * Get a comma separated list of all the project managers for the current project.
     *
     * @return string of the list of project managers
     */
    public String getManagersListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getProjectManagers());
        }
        return listString;
    }

    public String getBroadPIsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getBroadPIs());
        }
        return listString;
    }

    public String getExternalCollaboratorsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getExternalCollaborators());
        }
        return listString;
    }

    public String getScientistsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getScientists());
        }
        return listString;
    }

    public ResearchProject getResearchProject() {
        return editResearchProject;
    }

    /**
     * Get the full name of the creator of the project.
     *
     * @return name of the Creator
     */
    public String getResearchProjectCreatorString() {
        if (editResearchProject == null) {
            return "";
        }
        return bspUserList.getUserFullName(editResearchProject.getCreatedBy());
    }

    public String getCohortsListString() {
        if (editResearchProject == null) {
            return "";
        }
        return cohortList.getCohortListString(editResearchProject.getCohortIds());
    }

    public String getFundingSourcesListString() {
        if (editResearchProject == null) {
            return "";
        }
        return fundingList.getFundingListString(editResearchProject.getFundingIds());
    }

    public String getJiraUrl() {
        if (jiraLink == null) {
            return "";
        }
        return jiraLink.browseUrl();
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    /**
     * Used for AJAX autocomplete (i.e. from create.jsp page).
     *
     * @return JSON list of matching users
     * @throws Exception
     */
    @HandlesEvent("usersAutocomplete")
    public Resolution usersAutocomplete() throws Exception {
        List<BspUser> bspUsers = bspUserList.find(getQ());

        JSONArray itemList = new JSONArray();
        for (BspUser bspUser : bspUsers) {
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken("" + bspUser.getUserId(), fullName, false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }
}
