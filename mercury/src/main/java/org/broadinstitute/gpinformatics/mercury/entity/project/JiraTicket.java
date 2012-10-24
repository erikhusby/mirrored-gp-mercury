package org.broadinstitute.gpinformatics.mercury.entity.project;


import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;

@Entity
@Audited
@Table(schema = "mercury")
public class JiraTicket {

    public static final String TEST_PROJECT_PREFIX = "LCSET";

    private String ticketName;

    @Id
    private String ticketId;

//    @OneToMany(mappedBy = "jiraTicket")
//    private Set<Project> projects = new HashSet<Project>();

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabBatch labBatch;

    @Transient // todo arz make real hibernate relationship
    private JiraService jiraService;

    /*
    SGM -- Doesn't make sense to store the URL.  Can be derived on the front end using the Jira config
     */
    private String browserUrl;

    public JiraTicket() {}
    
    public JiraTicket(JiraService jiraService,
                      String ticketName,
                      String ticketId) {
        if (ticketName == null) {
            throw new NullPointerException("ticketName cannot be null.");
        }
        if (ticketId == null) {
            throw new NullPointerException("ticketId cannot be null.");
        }
        if (jiraService == null) {
             throw new NullPointerException("service cannot be null.");
        }
        this.ticketName = ticketName;
        this.ticketId = ticketId;
        this.jiraService = jiraService;
        this.browserUrl = jiraService.createTicketUrl(ticketName);
    }

    /**
     * Returns the URL to this ticket, to be used
     * in a browser (not the rest url)
     * @return
     */
    public String getBrowserUrl() {
        return browserUrl;
    }

    public String getTicketName() {
        return ticketName;
    }
    
    /**
     * Because we're going to be calling this inline all over the
     * place, in performance-sensitive sections of code like
     * the message processor, we should think about having
     * a separate thread and thread queue to dispatch
     * jira comments
     * @param text
     */
    public void addComment(String text) {
        // todo jmt remove null check after initializing service for entities
        if (jiraService != null) {
            try {
                jiraService.addComment(ticketName,text);
            }
            catch(IOException  e) {
                throw new RuntimeException("Could not log message '" + text + "' to jira ticket " + ticketName + ".  Is the jira server okay?",e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraTicket that = (JiraTicket) o;

        if (ticketId != null ? !ticketId.equals(that.ticketId) : that.ticketId != null) return false;
        if (ticketName != null ? !ticketName.equals(that.ticketName) : that.ticketName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ticketName != null ? ticketName.hashCode() : 0;
        result = 31 * result + (ticketId != null ? ticketId.hashCode() : 0);
        return result;
    }

//    public Set<Project> getProjects() {
//        return projects;
//    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
    }
}
