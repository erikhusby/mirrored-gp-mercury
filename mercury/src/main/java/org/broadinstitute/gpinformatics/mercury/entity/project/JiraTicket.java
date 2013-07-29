package org.broadinstitute.gpinformatics.mercury.entity.project;


import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.IOException;
import java.util.regex.Pattern;

@Entity
@Audited
@Table(schema = "mercury")
public class JiraTicket {

    /**
     * Real JIRA tickets IDs for PDOs have a "PDO-" prefix followed by digits.  Draft PDOs don't have a ticket ID,
     * Graphene tests have "PDO-" followed by arbitrary text.  There are groups to capture both the prefix and
     * the issue number.
     */
    public static final Pattern PATTERN = Pattern.compile("^([A-Z]+)-([\\d]+)$");

    public static final int PATTERN_GROUP_PREFIX = 1;

    public static final int PATTERN_GROUP_NUMBER = 2;

    private String ticketName;

    @Id
    private String ticketId;

//    @OneToMany(mappedBy = "jiraTicket")
//    private Set<Project> projects = new HashSet<Project>();

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabBatch labBatch;

    /*
    SGM -- Doesn't make sense to store the URL.  Can be derived on the front end using the Jira config
     */
    private String browserUrl;

    @Transient
    private JiraService jiraService;

    JiraTicket() {
        jiraService = ServiceAccessUtility.getBean(JiraService.class);
    }

    public JiraTicket(@Nonnull JiraService jiraService, @Nonnull String ticketId) {
        if (ticketId == null) {
            throw new NullPointerException("ticketId cannot be null.");
        }
        this.ticketId = ticketId;
        this.ticketName = ticketId;
        this.jiraService = jiraService;
        this.browserUrl = this.jiraService.createTicketUrl(ticketName);
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

    public String getTicketId() {
        return ticketId;
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
        try {
            jiraService.addComment(ticketName,text);
        } catch(IOException e) {
            throw new RuntimeException("Could not log message '" + text + "' to jira ticket " + ticketName + ".  Is the jira server okay?",e);
        }
    }


    public JiraIssue getJiraDetails() throws IOException{
        return jiraService.getIssue(ticketId);
    }

    /**
     * addWatcher allows a user to add a user as a watcher of the Jira ticket associated with this product order
     *
     * @param personLoginId Broad User Id
     * @throws IOException
     */
    public void addWatcher(String personLoginId) throws IOException {
        jiraService.addWatcher(ticketName, personLoginId);
    }

    /**
     * addLink allows a user to link this the jira ticket associated with this product order with another Jira Ticket
     *
     * @param targetTicketKey Unique Jira Key of the Jira ticket to which this product order's Jira Ticket will be
     *                       linked
     * @throws IOException
     */
    public void addJiraLink (String targetTicketKey) throws IOException {
        jiraService.addLink( AddIssueLinkRequest.LinkType.Related ,ticketName, targetTicketKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraTicket that = (JiraTicket) o;

        if (ticketId != null ? !ticketId.equals(that.getTicketId()) : that.getTicketId() != null) return false;
        if (ticketName != null ? !ticketName.equals(that.getTicketName()) : that.getTicketName() != null) return false;

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
