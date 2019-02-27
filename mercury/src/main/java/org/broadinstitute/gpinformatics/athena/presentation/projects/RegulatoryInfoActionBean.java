package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.projects.RegulatoryInfoEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.analytics.OrspProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding("/projects/regulatoryInfo.action")
public class RegulatoryInfoActionBean extends CoreActionBean {

    public static final String ADD_NEW_REGULATORY_INFO_ACTION = "addNewRegulatoryInfo";
    public static final String ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION = "addRegulatoryInfoToResearchProject";
    public static final String EDIT_REGULATORY_INFO_ACTION = "editRegulatoryInfo";
    public static final String REGULATORY_INFO_QUERY_ACTION = "regulatoryInfoQuery";
    public static final String VALIDATE_TITLE_ACTION = "validateTitle";
    public static final String VIEW_REGULATORY_INFO_ACTION = "viewRegulatoryInfo";

    @Inject
    private OrspProjectDao orspProjectDao;

    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    @Inject
    private RegulatoryInfoEjb regulatoryInfoEjb;

    @Inject
    private ResearchProjectDao researchProjectDao;

    /**
     * The search query.
     */
    private String q;

    private List<RegulatoryInfo> searchResults;

    private OrspProject orspSearchResult;

    private String regulatoryInfoIdentifier;

    private RegulatoryInfo.Type regulatoryInfoType;

    private String regulatoryInfoAlias;

    private Long regulatoryInfoId;

    private String researchProjectKey;

    private ResearchProject researchProject;

    /**
     * Load the ResearchProject for use while rendering the resulting JSP.
     */
    @After(stages = LifecycleStage.EventHandling, on = { REGULATORY_INFO_QUERY_ACTION })
    public void before() {
        researchProjectKey = getContext().getRequest().getParameter("researchProjectKey");
        researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
    }

    /**
     * Handles an AJAX action event to search for regulatory information, case-insensitively, from the value in this.q.
     */
    @HandlesEvent(REGULATORY_INFO_QUERY_ACTION)
    public Resolution queryRegulatoryInfoReturnHtmlSnippet() {
        researchProjectKey = getContext().getRequest().getParameter("researchProjectKey");

        String query = q.trim();

        searchResults = regulatoryInfoDao.findByIdentifier(query);
        if (searchResults.isEmpty()) {
            Optional<OrspProject> orspSearchResults = Optional.ofNullable(orspProjectDao.findByKey(query));
            orspSearchResults.ifPresent(orspProject -> {
                orspSearchResult = orspProject;
                regulatoryInfoType = orspProject.getType();
                regulatoryInfoAlias = orspProject.getName();
            });
        } else {
            final List<String> regulatoryInfoIdentifiers =
                    searchResults.stream().map(RegulatoryInfo::getIdentifier).collect(Collectors.toList());
            final List<OrspProject> orspResults = orspProjectDao.findListByList(regulatoryInfoIdentifiers);
            final Optional<Set<String>> orspResultNames = Optional.ofNullable(orspResults.stream().map(OrspProject::getName).collect(Collectors.toSet()));

            if(orspResultNames.isPresent() && !orspResultNames.get().isEmpty()) {

                searchResults.forEach(regulatoryInfo -> {
                    regulatoryInfo.setUserEdit(orspResultNames.get().contains(regulatoryInfo.getIdentifier()));
                });
            } else {
                searchResults.clear();
            }
        }
        regulatoryInfoIdentifier = query;
        return new ForwardResolution("regulatory_info_dialog_sheet_2.jsp").addParameter("researchProjectKey", researchProjectKey);
    }

    /**
     * Loads an existing regulatory information and pre-populates the regulatory information form.
     *
     * @return a resolution to the regulatory information form
     */
    @HandlesEvent(VIEW_REGULATORY_INFO_ACTION)
    public Resolution viewRegulatoryInfo() {
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        if (regulatoryInfo != null) {
            regulatoryInfoIdentifier = regulatoryInfo.getIdentifier();
            searchResults = regulatoryInfoDao.findByIdentifier(regulatoryInfoIdentifier);
            regulatoryInfoType = regulatoryInfo.getType();
            regulatoryInfoAlias = regulatoryInfo.getName();
        }
        return new ForwardResolution("regulatory_info_form.jsp");
    }

    /**
     * Associates regulatory information with a research project. The RegulatoryInformation is looked up by the value in
     * this.regulatoryInfoId.
     */
    @HandlesEvent(ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION)
    public void addRegulatoryInfoToResearchProject() {
        regulatoryInfoEjb.addRegulatoryInfoToResearchProject(regulatoryInfoId, researchProjectKey);
    }

    /**
     * Creates a new regulatory information record and adds it to the research project currently being viewed.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(ADD_NEW_REGULATORY_INFO_ACTION)
    public void addNewRegulatoryInfo() {
        regulatoryInfoEjb.createAndAddRegulatoryInfoToResearchProject(regulatoryInfoIdentifier, regulatoryInfoType,
                regulatoryInfoAlias, researchProjectKey);
    }

    /**
     * Edits a regulatory info record, specifically the title (alias, name). The ID of the regulatory info comes from
     * this.regulatoryInfoId and the new title comes from this.regulatoryInfoAlias.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(EDIT_REGULATORY_INFO_ACTION)
    public void editRegulatoryInfo() {
        regulatoryInfoEjb.editRegulatoryInfo(regulatoryInfoId, regulatoryInfoAlias);
    }

    @HandlesEvent(VALIDATE_TITLE_ACTION)
    public Resolution validateTitle() {
        String result = "";
        if (StringUtils.isBlank(regulatoryInfoAlias)) {
            result = "Protocol Title is required.";
        } else if (regulatoryInfoAlias.length() > RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH) {
            result = String.format("Protocol title exceeds maximum length of %d with %d.",
                    RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH, regulatoryInfoAlias.length());
        }
        return createTextResolution(result);
    }

    /**
     * Determines whether or not the given regulatory information is already associated with the current research
     * project. This is used to determine whether an existing regulatory information can be added to a project.
     *
     * @param regulatoryInfo the regulatory info to check
     *
     * @return true if the regulatory info is associated with the research project; false otherwise
     */
    public boolean isRegulatoryInfoInResearchProject(RegulatoryInfo regulatoryInfo) {
        return researchProject.getRegulatoryInfos().contains(regulatoryInfo);
    }

    /**
     * Determines whether or not the regulatory information form represents a new record.
     *
     * @return true if creating a new regulatory information; false otherwise
     */
    public boolean isRegulatoryInformationNew() {
        return regulatoryInfoId == null;
    }

    /**
     * Returns all of the possible regulatory information types. This is used to access the enumeration values because
     * stripes:options-enumeration does not support a "disabled" attribute.
     *
     * @return a collection of all values from the {@link RegulatoryInfo.Type} enum
     */
    public RegulatoryInfo.Type[] getAllTypes() {
        return RegulatoryInfo.Type.values();
    }

    /**
     * Determines whether or not there are any regulatory information types that can be added for the queried
     * identifier. If there is already regulatory information for every type for the queried identifier, the UI should
     * not prompt to create a new record.
     *
     * @return true if the user should be allowed to create new regulatory info; false otherwise
     */
    public boolean isAddRegulatoryInfoAllowed() {
        return searchResults.isEmpty() && orspSearchResult == null;
    }

    /**
     * Determines whether or not a regulatory information of the given type already exists with the queried identifier.
     *
     * @param type the type to check
     *
     * @return true if the type is already in use for the identifier; false otherwise
     */
    public boolean isTypeInUseForIdentifier(RegulatoryInfo.Type type) {
        for (RegulatoryInfo searchResult : searchResults) {
            if (searchResult.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public List<RegulatoryInfo> getSearchResults() {
        return searchResults;
    }

    public OrspProject getOrspSearchResult() {
        return orspSearchResult;
    }

    public String getRegulatoryInfoIdentifier() {
        return regulatoryInfoIdentifier;
    }

    public void setRegulatoryInfoIdentifier(String regulatoryInfoIdentifier) {
        this.regulatoryInfoIdentifier = regulatoryInfoIdentifier;
    }

    public RegulatoryInfo.Type getRegulatoryInfoType() {
        return regulatoryInfoType;
    }

    public void setRegulatoryInfoType(RegulatoryInfo.Type regulatoryInfoType) {
        this.regulatoryInfoType = regulatoryInfoType;
    }

    public String getRegulatoryInfoAlias() {
        return regulatoryInfoAlias;
    }

    public void setRegulatoryInfoAlias(String regulatoryInfoAlias) {
        this.regulatoryInfoAlias = regulatoryInfoAlias;
    }

    public Long getRegulatoryInfoId() {
        return regulatoryInfoId;
    }

    public void setRegulatoryInfoId(Long regulatoryInfoId) {
        this.regulatoryInfoId = regulatoryInfoId;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }
}
