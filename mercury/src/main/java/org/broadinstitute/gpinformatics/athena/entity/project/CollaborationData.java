package org.broadinstitute.gpinformatics.athena.entity.project;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * The object to send in to this web service to create the new collaboration.
 * <p>
 *     The API will send an email address or a domain user id. If both are sent, then the domain user id will be used
 *     as primary. This will send an invitation to the collaborator if they do not already have a portal account. If
 *     there is information in BSP, that will be displayed. If this is a Broad user, LDAP will be used to help with
 *     account creation.
 * </p>
 * <p>
 *     If the user does not have a BSP account, this will create it in BSP as inactive and will tell Mercury to
 *     add the external collaborator to the research project.
 * </p>
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
public class CollaborationData {

    // The name of the collaboration space (mercury will send this from the research project).
    private String name;

    // Information about the collaboration space (mercury will send this from the research project).
    private String description;

    // The research project key.
    private String researchProjectKey;

    /** The user ID for the collaborator */
    private Long collaboratorId;

    /** The User ID for the Project Manager */
    private Long projectManagerId;

    private String emailMessage;

    private Date expirationDate;

    // Empty for XML streaming.
    public CollaborationData() {
    }

    /**
     * For testing purposes.
     *
     * @param name The name
     * @param description The description
     * @param researchProjectKey The research project
     * @param collaboratorId The collaborators identifier
     * @param projectManagerId The identifier of the project manager
     * @param emailMessage The email message to the collaborator
     */
    public CollaborationData(String name, String description, String researchProjectKey, Long collaboratorId,
                             Long projectManagerId, String emailMessage) {
        this.name = name;
        this.description = description;
        this.researchProjectKey = researchProjectKey;
        this.collaboratorId = collaboratorId;
        this.projectManagerId = projectManagerId;
        this.emailMessage = emailMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public Long getCollaboratorId() {
        return collaboratorId;
    }

    public void setCollaboratorId(Long collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    public String getEmailMessage() {
        return emailMessage;
    }

    public void setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
