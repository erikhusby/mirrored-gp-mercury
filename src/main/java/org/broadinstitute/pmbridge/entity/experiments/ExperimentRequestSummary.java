package org.broadinstitute.pmbridge.entity.experiments;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;

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
    private final PlatformType platformType;
    private Name title;
    private ChangeEvent modification;
    private ExperimentId experimentId;
    private Long researchProjectId = ResearchProject.UNSPECIFIED_ID;
    public static Name DRAFT_STATUS = new Name("DRAFT");
    public static IllegalArgumentException BLANK_CREATOR_EXCEPTION = new IllegalArgumentException("Creator username must not be blank.");
    private Name status = DRAFT_STATUS;


    public ExperimentRequestSummary(Person creator, Date createdDate, PlatformType platformType) {
        if (creator == null || StringUtils.isBlank(creator.getUsername())) {
            throw BLANK_CREATOR_EXCEPTION;
        }
        this.creation = new ChangeEvent(createdDate, creator);
        this.platformType = platformType;
        this.experimentId = new ExperimentId("DRAFT_" + this.creation.date.getTime());
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
    }

    //GETTERS
    public Name getTitle() {
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

    public PlatformType getPlatformType() {
        return platformType;
    }

    public Name getStatus() {
        return status;
    }

    public Long getResearchProjectId() {
        return researchProjectId;
    }

    //SETTERS
    public void setTitle(final Name title) {
        this.title = title;
    }

    public void setExperimentId(final ExperimentId experimentId) {
        this.experimentId = experimentId;
    }

    public void setModification(final ChangeEvent modification) {
        this.modification = modification;
    }

    // Temp setter until we can get the creation date from the summarized pass.
    public void setCreation(final ChangeEvent creation) {
        this.creation = creation;
    }

    public void setStatus(final Name status) {
        this.status = status;
    }

    public void setResearchProjectId(final Long researchProjectId) {
        this.researchProjectId = researchProjectId;
    }
}
