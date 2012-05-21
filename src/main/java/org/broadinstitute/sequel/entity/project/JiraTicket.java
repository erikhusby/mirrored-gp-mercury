package org.broadinstitute.sequel.entity.project;


import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Entity
public class JiraTicket {

    public static final String TEST_PROJECT_PREFIX = "TP";
    
    public static final String SEQUEL_PROJECT_ISSUE_TYPE = "SequeL Project";
    
    private String ticketName;

    @Id
    private String ticketId;

    @OneToMany(mappedBy = "jiraTicket")
    private Set<Project> projects = new HashSet<Project>();

    @Transient
    private JiraService jiraService;

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

    public Set<Project> getProjects() {
        return projects;
    }
}
