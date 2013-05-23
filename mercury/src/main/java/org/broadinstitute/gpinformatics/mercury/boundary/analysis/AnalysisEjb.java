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
     * Add an aligner.
     *
     * @param alignerName The name of the aligner to create.
     */
    public boolean addAligner(@Nonnull String alignerName) {
        Aligner foundAligner = alignerDao.findByBusinessKey(alignerName);
        if (foundAligner == null) {
            alignerDao.persist(new Aligner(alignerName));
            return true;
        }

        return false;
    }

    /**
     * Remove aligner.
     *
     * @param alignerKeys multiple aligners can be removed at once.
     */
    public void removeAligners(@Nonnull String... alignerKeys) {
        for (String alignerKey : alignerKeys) {
            Aligner aligner = alignerDao.findByBusinessKey(alignerKey);
            if (alignerDao != null) {
                alignerDao.remove(aligner);
            }
        }
    }

    /**
     * Add a new analysis type.
     *
     * @param analysisTypeName The name of the type to create.
     */
    public boolean addAnalysisType(@Nonnull String analysisTypeName) {
        AnalysisType foundType = analysisTypeDao.findByBusinessKey(analysisTypeName);
        if (foundType == null) {
            analysisTypeDao.persist(new AnalysisType(analysisTypeName));
            return true;
        }

        return false;
    }

    /**
     * Remove analysis types.
     *
     * @param analysisTypeKeys multiple analysis types can be removed at once.
     */
    public void removeAnalysisTypes(@Nonnull String... analysisTypeKeys) {
        for (String analysisTypeKey : analysisTypeKeys) {
            AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeKey);
            if (analysisTypeDao != null) {
                analysisTypeDao.remove(analysisType);
            }
        }
    }

    /**
     * Add a new reference sequence.
     */
    public boolean addReferenceSequence(@Nonnull String name, @Nonnull String version, boolean isCurrent) {
        ReferenceSequence referenceSequence = new ReferenceSequence(name, version);
        referenceSequence.setCurrent(isCurrent);

        ReferenceSequence foundSequence = referenceSequenceDao.findByBusinessKey(referenceSequence.getBusinessKey());

        if (foundSequence == null) {
            referenceSequenceDao.persist(referenceSequence);
            return true;
        }

        return false;
    }


    /**
     * Create a new reference sequence with a name and version. It will also ensure that all older versions of the matching sequence name will be set to 'not current'.
     *
     * @param name    String representing the name of the reference sequence.
     * @param version String representing the version of the reference sequence.
     */
    public void addReferenceSequence(@Nonnull String name, @Nonnull String version) {

        // Do a dao call to find all the versions if there are any, loop through and set all the 'isCurrent' to false and then create the new one.
        List<ReferenceSequence> matching = referenceSequenceDao.findAllByName(name);
        for (ReferenceSequence sequence : matching) {
            sequence.setCurrent(false);
        }

        addReferenceSequence(name, version, true);
    }

    /**
     * Remove reference sequence(s).
     *
     * @param referenceSequenceKeys List of reference sequence business keys to be removed.
     */
    public void removeReferenceSequences(@Nonnull String... referenceSequenceKeys) {
        for (String referenceSequenceKey : referenceSequenceKeys) {
            ReferenceSequence referenceSequence = referenceSequenceDao.findByBusinessKey(referenceSequenceKey);
            if (referenceSequence != null) {
                referenceSequenceDao.remove(referenceSequence);
            }
        }
    }

    /**
     * Add a new reagent design.
     *
     * @param name The reagent design name to add.
     * @param type The reagent type.
     */
    public boolean addReagentDesign(@Nonnull String name, @Nonnull ReagentDesign.ReagentType type) {
        ReagentDesign foundDesign = reagentDesignDao.findByBusinessKey(name);
        if (foundDesign == null) {
            ReagentDesign reagentDesign = new ReagentDesign(name, type);
            reagentDesignDao.persist(reagentDesign);
            return true;
        }

        return false;
    }

    /**
     * Remove reagent design.
     *
     * @param reagentDesignKeys multiple reagent designs can be removed at once.
     */
    public void removeReagentDesigns(@Nonnull String... reagentDesignKeys) {
        for (String reagentDesignKey : reagentDesignKeys) {
            ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(reagentDesignKey);
            if (reagentDesign != null) {
                reagentDesignDao.remove(reagentDesign);
            }
        }
    }
}
