package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditedRevDto;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@UrlBinding(AuditTrailActionBean.ACTIONBEAN_URL_BINDING)
public class AuditTrailActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/audit/auditTrail.action";
    // Jsp pages
    private static final String AUDIT_TRAIL_LISTING_PAGE = "/audit/trail_list.jsp";
    private static final String AUDIT_TRAIL_ENTRY_PAGE = "/audit/trail_entry.jsp";
    // Events
    private static final String LIST_AUDIT_TRAILS = "listAuditTrails";
    private static final String VIEW_ENTITIES_AT_REV = "viewEntitiesAtRev";

    @Inject
    private AuditReaderDao auditReaderDao;

    private String searchEntityDisplayName = "";
    private String searchUsername = "";
    private Date searchStartDate;
    private Date searchEndDate;
    private String searchStartTime = "00:00";
    private String searchEndTime = "00:00";
    private List<AuditedRevDto> auditTrailList = new ArrayList<>();
    private List<String> usernames;
    // Maps display name to canonical classname.
    private Map<String, String> entityClassnames;
    private List<String> entityDisplayNames;

    public AuditReaderDao getAuditReaderDao() {
        return auditReaderDao;
    }

    public void setAuditReaderDao(AuditReaderDao auditReaderDao) {
        this.auditReaderDao = auditReaderDao;
    }

    public String getSearchEntityDisplayName() {
        return searchEntityDisplayName;
    }

    public void setSearchEntityDisplayName(String searchEntityDisplayName) {
        this.searchEntityDisplayName = searchEntityDisplayName;
    }

    public String getSearchUsername() {
        return searchUsername;
    }

    public void setSearchUsername(String searchUsername) {
        this.searchUsername = searchUsername;
    }

    public Date getSearchStartDate() {
        return searchStartDate;
    }

    public void setSearchStartDate(Date searchStartDate) {
        this.searchStartDate = searchStartDate;
    }

    public Date getSearchEndDate() {
        return searchEndDate;
    }

    public void setSearchEndDate(Date searchEndDate) {
        this.searchEndDate = searchEndDate;
    }

    public String getSearchStartTime() {
        return searchStartTime;
    }

    public void setSearchStartTime(String searchStartTime) {
        this.searchStartTime = searchStartTime;
    }

    public String getSearchEndTime() {
        return searchEndTime;
    }

    public void setSearchEndTime(String searchEndTime) {
        this.searchEndTime = searchEndTime;
    }

    public List<AuditedRevDto> getAuditTrailList() {
        return auditTrailList;
    }

    public void setAuditTrailList(List<AuditedRevDto> auditTrailList) {
        this.auditTrailList = auditTrailList;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public Map<String, String> getEntityClassnames() {
        return entityClassnames;
    }

    public List<String> getEntityDisplayNames() {
        return entityDisplayNames;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @DefaultHandler
    public Resolution selectAuditTrails() {
        setupUserAndClassNames();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private void setupUserAndClassNames() {
        if (usernames == null) {
            usernames = auditReaderDao.getAllAuditUsername();
        }
        if (entityClassnames == null) {
            entityClassnames = new HashMap<>();
            entityDisplayNames = new ArrayList<>();
            for (String classname : ReflectionUtil.getEntityClassnames(ReflectionUtil.getMercuryAthenaClasses())) {
                // Make display names from the ending of canonical classname, but disambiguate it if necessary.
                for (int idx = classname.lastIndexOf('.') - 1; idx >= 0; idx = classname.lastIndexOf('.', idx) - 1) {
                    String displayName = classname.substring(idx);
                    if (!entityDisplayNames.contains(displayName)) {
                        entityDisplayNames.add(displayName);
                        entityClassnames.put(displayName, classname);
                        break;
                    }
                }
            }
            Collections.sort(entityDisplayNames);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(LIST_AUDIT_TRAILS)
    public Resolution listAuditTrails() {
        generateAuditTrailList();
        return new ForwardResolution(AUDIT_TRAIL_LISTING_PAGE);
    }

    private long truncateToSecond(long time) {
        return 1000 * (time / 1000);
    }

    private void generateAuditTrailList() {
        // Search Envers using the user's criteria.
        Collection<Long> revIds = auditReaderDao.fetchAuditIds(truncateToSecond(searchStartDate.getTime()),
                truncateToSecond(searchEndDate.getTime()), searchUsername).keySet();
        auditTrailList = auditReaderDao.fetchAuditedRevs(revIds);

        // Filters out the dtos that don't match the entity type criteria.
        if (StringUtils.isNotBlank(searchEntityDisplayName)) {
            for (Iterator<AuditedRevDto> iter = auditTrailList.iterator(); iter.hasNext(); ) {
                AuditedRevDto dto = iter.next();
                if (!dto.getEntityTypeNames().contains(searchEntityDisplayName)) {
                    iter.remove();
                }
            }
        }
        Collections.sort(auditTrailList, AuditedRevDto.BY_REV_ID);
    }

    @ValidationMethod(on = LIST_AUDIT_TRAILS)
    public void validateSelection() {
        if (searchStartDate == null || searchEndDate == null) {
            addGlobalValidationError("You must select both start and an end date.");
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(VIEW_ENTITIES_AT_REV)
    public Resolution viewEntitiesAtRev() {
        generateAuditTrailEntry();
        return new ForwardResolution(AUDIT_TRAIL_ENTRY_PAGE);
    }

    private void generateAuditTrailEntry() {
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
}
