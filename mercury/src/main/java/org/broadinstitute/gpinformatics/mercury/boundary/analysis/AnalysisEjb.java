package org.broadinstitute.gpinformatics.mercury.boundary.analysis;

import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * This provides all the APIs for working with the information that is managed by the pipeline
 * team and then applied to products or projects.
 */
@Stateful
@RequestScoped
public class AnalysisEjb {

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private AlignerDao alignerDao;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    /**
     * Add a aligner.
     *
     * @param alignerName The name of the aligner to create
     */
    public void addAligner(@Nonnull String alignerName) {
        alignerDao.persist(new Aligner(alignerName));
    }

    /**
     * Remove aligner.
     *
     * @param aligners multiple aligners can be removed at once.
     */
    public void removeAligner(@Nonnull Aligner aligners) {
        alignerDao.remove(aligners);
    }

    /**
     * Add a new analysis type.
     *
     * @param analysisTypeName The name of the type to create.
     */
    public void addAnalysisType(@Nonnull String analysisTypeName) {
        analysisTypeDao.persist(new AnalysisType(analysisTypeName));
    }

    /**
     * Remove analysis types.
     *
     * @param analysisTypes multiple analysis types can be removed at once.
     */
    public void removeAnalysisTypes(@Nonnull Collection<AnalysisType> analysisTypes) {
        analysisTypeDao.remove(analysisTypes);
    }

    /**
     * Add a new reference sequence.
     */
    public void addReferenceSequence(@Nonnull String name, @Nonnull String version, boolean isCurrent) {
        ReferenceSequence referenceSequence = new ReferenceSequence(name, version);
        referenceSequence.setCurrent(isCurrent);
        referenceSequenceDao.persist(referenceSequence);
    }


    /**
     * Create a new reference sequence with a name and version. It will also ensure that all older versions of the matching sequence name will be set to 'not current'.
     *
     * @param name    String representing the name of the reference sequence.
     * @param version String representing the version of the reference sequence.
     */
    public void addReferenceSequence(@Nonnull String name, @Nonnull String version) {

        //Do a dao call to find all the versions if there are any, loop thru and set all the 'isCurrent' to false and then create the new one.
        List<ReferenceSequence> matching = referenceSequenceDao.findAllByName(name);
        for (ReferenceSequence sequence : matching) {
            sequence.setCurrent(false);
            // do we need to update the version too?..
//            referenceSequenceDao.persist(sequence); // Would it be better to create a method to set the reference sequence?
        }

        addReferenceSequence(name, version, true);
    }

    /**
     * Remove reference sequence(s).
     *
     * @param referenceSequences Collection of reference sequences to be removed.
     */
    public void removeReferenceSequences(@Nonnull Collection<ReferenceSequence> referenceSequences) {
        referenceSequenceDao.remove(referenceSequences);
    }

    /**
     * Remove reference sequence.
     *
     * @param referenceSequence Reference sequence instance to be removed.
     */
    public void removeReferenceSequence(@Nonnull ReferenceSequence referenceSequence) {
        referenceSequenceDao.remove(referenceSequence);
    }

    /**
     * Add a new reagent design.
     *
     * @param name The reagent design name to add.
     * @param type The reagent type.
     */
    public void addReagentDesign(@Nonnull String name, @Nonnull ReagentDesign.ReagentType type) {
        ReagentDesign foundDesign = reagentDesignDao.findByBusinessKey(name);
        if (foundDesign == null) {
            ReagentDesign reagentDesign = new ReagentDesign(name, type);
            reagentDesignDao.persist(reagentDesign);
        }
    }

    /**
     * Remove reagent design.
     *
     * @param reagentDesigns multiple reagent designs can be removed at once.
     */
    public void removeReagentDesign(@Nonnull Collection<ReagentDesign> reagentDesigns) {
        reagentDesignDao.remove(reagentDesigns);
    }
}
