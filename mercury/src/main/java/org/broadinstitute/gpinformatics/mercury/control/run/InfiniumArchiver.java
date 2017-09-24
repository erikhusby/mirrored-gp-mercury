package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Archives Infinium idats and other files at some interval (e.g. 10 days) after the pipeline starter has been called.
 */
public class InfiniumArchiver {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    public List<LabVessel> findChipsToArchive() {
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(
                LabEventType.INFINIUM_AUTOCALL_SOME_STARTED,
                LabEventType.INFINIUM_ARCHIVED);
        Date tenDaysAgo = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 10L);
        List<String> barcodes = new ArrayList<>();
        for (LabVessel labVessel : infiniumChips) {
            barcodes.add(labVessel.getLabel());
        }

        // To avoid running out of memory, break the list into small chunks, and clear the Hibernate session.
        List<Collection<String>> split = BaseSplitter.split(barcodes, 10);
        List<LabVessel> chipsToArchive = new ArrayList<>();
        for (Collection<String> strings : split) {
            List<LabVessel> chips = labVesselDao.findByListIdentifiers(new ArrayList<>(strings));
            for (LabVessel chip : chips) {
                for (LabEvent labEvent : chip.getInPlaceLabEvents()) {
                    if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_ALL_STARTED) {
                        String forwardToGap = LabEventFactory.determineForwardToGap(labEvent, chip, productEjb,
                                attributeArchetypeDao);
                        if (Objects.equals(forwardToGap, "N")) {
                            if (labEvent.getEventDate().before(tenDaysAgo)) {
                                chipsToArchive.add(chip);
                            }
                        }
                        break;
                    }
                }
            }
            labVesselDao.clear();
        }

        return chipsToArchive;
    }

    public void archiveChip(StaticPlate staticPlate) {

    }
}
