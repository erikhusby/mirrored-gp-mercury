package org.broadinstitute.pmbridge.entity.experiments;

import org.broad.squid.services.TopicService.AbstractPass;
import org.broad.squid.services.TopicService.DirectedPass;
import org.broad.squid.services.TopicService.RNASeqPass;
import org.broad.squid.services.TopicService.WholeGenomePass;
import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.seq.*;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.gap.ExperimentPlan;

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
    private final LocalId localId;
    private final PlatformType platformType;
//    private final ExperimentRequest experimentRequest;
    private Name title;
    private ChangeEvent modification;
    private RemoteId remoteId;
    private Long researchProjectId = ResearchProject.UNSPECIFIED_ID;
    public static Name DRAFT_STATUS = new Name("DRAFT");
    private Name status = DRAFT_STATUS;


    public ExperimentRequestSummary(Person creator, Date createdDate, PlatformType platformType) {
        this.creation = new ChangeEvent( createdDate, creator );
        this.platformType = platformType;
        this.localId = new LocalId( "" + this.creation.date.getTime());
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
    }

    //GETTERS
    public Name getTitle() {
        return title;
    }

    public ChangeEvent getCreation() {
        return creation;
    }

    public RemoteId getRemoteId() {
        return remoteId;
    }

    public LocalId getLocalId() {
        return localId;
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

    public void setRemoteId(final RemoteId remoteId) {
        this.remoteId = remoteId;
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
