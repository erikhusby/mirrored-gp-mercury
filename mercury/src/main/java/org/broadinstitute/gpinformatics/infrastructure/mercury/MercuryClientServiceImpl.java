package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.*;

@Impl
@Default
@RequestScoped
public class MercuryClientServiceImpl implements MercuryClientService {

    @Inject
    private MercuryClientEjb mercuryClientEjb;

    @Inject
    private Deployment deployment;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private AlignerDao alignerDao;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo) {
        if (deployment != Deployment.PROD) {
            return mercuryClientEjb.addFromProductOrder(pdo);
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo, @Nonnull Collection<ProductOrderSample> samples) {
        if (deployment != Deployment.PROD) {
            return mercuryClientEjb.addFromProductOrder(pdo, samples);
        }
        return Collections.emptyList();
    }

    public Collection<DisplayableItem> getReferenceSequences() {
        List<ReferenceSequence> refSequences = referenceSequenceDao.findAll();
        Collection<DisplayableItem> displayableItems = new ArrayList<DisplayableItem>(refSequences.size());

        for (ReferenceSequence refSequence : refSequences) {
            displayableItems.add(new DisplayableItem(refSequence.getBusinessKey(), refSequence.getName()));
        }
        return displayableItems;
    }

    public Collection<DisplayableItem> getSequenceAligners() {
        List<Aligner> sequenceAligners = alignerDao.findAll();
        Collection<DisplayableItem> displayableItems = new ArrayList<DisplayableItem>(sequenceAligners.size());

        for (Aligner sequenceAligner : sequenceAligners) {
            displayableItems.add(new DisplayableItem(sequenceAligner.getBusinessKey(), sequenceAligner.getName()));
        }
        return displayableItems;
    }

    public Collection<DisplayableItem> getAnalysisTypes() {
        List<AnalysisType> analysisTypes = analysisTypeDao.findAll();
        Collection<DisplayableItem> displayableItems = new ArrayList<DisplayableItem>(analysisTypes.size());

        for (AnalysisType analysisType : analysisTypes) {
            displayableItems.add(new DisplayableItem(analysisType.getBusinessKey(), analysisType.getName()));
        }
        return displayableItems;
    }

    public Collection<DisplayableItem> getReagentDesigns() {
        List<ReagentDesign> reagentDesigns = reagentDesignDao.findAll();
        Collection<DisplayableItem> displayableItems = new ArrayList<DisplayableItem>(reagentDesigns.size());

        for (ReagentDesign reagentDesign : reagentDesigns) {
            displayableItems.add(new DisplayableItem(reagentDesign.getBusinessKey(), reagentDesign.getDesignName()));
        }
        return displayableItems;
    }

}
