package org.broadinstitute.gpinformatics.infrastructure.experiments;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectId;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.Date;

/**
 * Summarizes an experiment request
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:05 PM
 */
public class ExperimentRequestSummary {

    private ChangeEvent creation;
    private String title;
    private ChangeEvent modification;
    private ExperimentId experimentId;
    private ResearchProjectId researchProjectId;
    public static final String DRAFT_STATUS = "DRAFT";
    public static IllegalArgumentException BLANK_CREATOR_EXCEPTION = new IllegalArgumentException("Creator username must not be blank.");
    private String status = DRAFT_STATUS;
    private ExperimentType experimentType;

    public ExperimentRequestSummary(final String title, Person creator, Date createdDate, ExperimentType experimentType) {
        if (creator == null || StringUtils.isBlank(creator.getLogin())) {
            throw BLANK_CREATOR_EXCEPTION;
        }
        if (createdDate == null) {
            throw new IllegalArgumentException("Creator date must not be null for creator " + creator.getLogin() );
        }
        if ( StringUtils.isBlank( title) ){
            throw new IllegalArgumentException("Experimnet title must not be blank." );
        }

        this.creation = new ChangeEvent(createdDate, creator);
        this.experimentId = new ExperimentId("DRAFT_" + this.creation.date.getTime());
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
        this.experimentType = experimentType;
        this.title = title;

    }

    //GETTERS
    public String getTitle() {
        return title;
    }

    public ChangeEvent getCreation() {
        return creation;
    }

    public ExperimentId getExperimentId() {
        return experimentId;
    }

    public ChangeEvent getModification() {
        return modification;
    }

    public ExperimentType getExperimentType() {
        return experimentType;
    }

    public String getStatus() {
        return status;
    }

    public ResearchProjectId getResearchProjectId() {
        return researchProjectId;
    }

    //SETTERS
    public void setTitle(final String title) {
        this.title = title;
    }

    public void setExperimentId(final ExperimentId experimentId) {
        this.experimentId = experimentId;
    }

    public void setModification(final ChangeEvent modification) {
        this.modification = modification;
    }

    // Temp setter until we can get the creation date from the summarized pass.
//    public void setCreation(final ChangeEvent creation) {
//        this.creation = creation;
//    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setResearchProjectId(final ResearchProjectId researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    public void setExperimentType(final ExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    public Date getModificationDate() {
        return (modification == null ? null : modification.date);
    }

    @Override
    public String toString() {
        return "ExperimentRequestSummary{" +
                "creation=" + creation +
                ", title=" + title +
                ", modification=" + modification +
                ", experimentId=" + experimentId +
                ", researchProjectId=" + researchProjectId.getValue() +
                ", status=" + status +
                ", experimentType=" + experimentType +
                '}';
    }
}
