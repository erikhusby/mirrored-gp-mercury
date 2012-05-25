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
    private Name status;
    public static Name DRAFT_STATUS = new Name("DRAFT");


    public ExperimentRequestSummary(Person creator, Date createdDate, PlatformType platformType, String subType) {
        this.creation = new ChangeEvent( createdDate, creator );
        this.platformType = platformType;
        this.localId = new LocalId( "" + this.creation.date.getTime());
        this.modification = new ChangeEvent(new Date(this.creation.date.getTime()), creator);
//        this.experimentRequest = createNewExperimentRequest( platformType, subType );
    }

//    /**
//     * private method to initialize an experiment request member with the appropriates type of experiment request
//     * based on the platform type a subType.
//     */
//    private ExperimentRequest createNewExperimentRequest ( PlatformType platformType, String subType ){
//        ExperimentRequest experimentRequest = null;
//
//        switch (platformType) {
//            case GSP:
//                experimentRequest = createSeqExperimentRequest(subType);
//                break;
//            case GAP:
//                //TODO - defaults for Experiment Plan ??
//                experimentRequest = new GapExperimentRequest(this, new ExperimentPlan());
//                break;
//            default:
//                break;
//        }
//        return experimentRequest;
//    }
//
//
//    private SeqExperimentRequest createSeqExperimentRequest(String subType) {
//        SeqExperimentRequest experimentRequest=null;
//
//        PMBPassType pmbPassType=PMBPassType.convertToEnumElseNull(subType);
//        if (  pmbPassType == null) {
//            throw new IllegalArgumentException("Unsupported Sequencing Pass Type: "+ subType + ". Supported values are : " + PMBPassType.values().toString() );
//        }
//
//        switch (pmbPassType) {
//            //TODO set mandatory defaults for each ??
//            case WG:
//                experimentRequest = new WholeGenomeExperiment(this);
//                break;
//            case DIRECTED:
//                experimentRequest = new HybridSelectionExperiment(this);
//                break;
//            case RNASeq:
//                experimentRequest = new RNASeqExperiment(this);
//                break;
//            default:
//                // in case the enum is extended but not this code.
//                throw new IllegalArgumentException("Unsupported Sequencing Pass Type : " + pmbPassType.name() );
//        }
//
//        return experimentRequest;
//    }



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
