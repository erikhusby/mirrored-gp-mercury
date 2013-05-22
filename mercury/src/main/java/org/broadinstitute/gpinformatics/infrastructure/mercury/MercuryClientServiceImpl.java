package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Nameable;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


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

    private Collection<DisplayableItem> makeDisplayableItemCollection(List<? extends BusinessKeyable> items) {
        Collection<DisplayableItem> displayableItems = new ArrayList<DisplayableItem>(items.size());

        for (BusinessKeyable item : items) {
            displayableItems.add(new DisplayableItem(item.getBusinessKey(), item.getName()));
        }
        return displayableItems;
    }

    @Override
    public Collection<DisplayableItem> getReferenceSequences() {
        return makeDisplayableItemCollection(referenceSequenceDao.findAllCurrent());
    }

    @Override
    public Collection<DisplayableItem> getSequenceAligners() {
        return makeDisplayableItemCollection(alignerDao.findAll());
    }

    @Override
    public Collection<DisplayableItem> getAnalysisTypes() {
        return makeDisplayableItemCollection(analysisTypeDao.findAll());
    }

    @Override
    public Collection<DisplayableItem> getReagentDesigns() {
        return makeDisplayableItemCollection(reagentDesignDao.findAll());
    }

    private DisplayableItem getDisplayableItemInfo(String businessKey, BusinessKeyFinder dao) {
        BusinessKeyable object = dao.findByBusinessKey(businessKey);
        if (object == null) {
            // Object of that business key was not found.
            return null;
        }

        DisplayableItem displayableItem = new DisplayableItem(object.getBusinessKey(), object.getName());

        return displayableItem;
    }

    @Override
    public DisplayableItem getAnalysisType(String businessKey) {
        return getDisplayableItemInfo(businessKey, analysisTypeDao);
    }

    @Override
    public DisplayableItem getSequenceAligner(String businessKey) {
        return getDisplayableItemInfo(businessKey, alignerDao);
    }

    @Override
    public DisplayableItem getReagentDesign(String businessKey) {
        return getDisplayableItemInfo(businessKey, reagentDesignDao);
    }

    @Override
    public DisplayableItem getReferenceSequence(String businessKey) {
        return getDisplayableItemInfo(businessKey, referenceSequenceDao);
    }
}
