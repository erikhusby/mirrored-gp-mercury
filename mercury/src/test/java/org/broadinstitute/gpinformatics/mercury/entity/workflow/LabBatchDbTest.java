package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests of the LabBatch entity, using a database
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class LabBatchDbTest extends StubbyContainerTest {

    public LabBatchDbTest(){}

    @Inject
    private LabBatchDao labBatchDao;

    public static final String XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private SimpleDateFormat xmlDateFormat = new SimpleDateFormat(XML_DATE_FORMAT);

    /**
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class AccumulateLabEvents extends TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<LabEvent> labEventsList = new ArrayList<>();
        /**
         * Avoid infinite loops
         */
        private Set<LabEvent> visitedLabEvents = new HashSet<>();


        public List<LabEvent> getLabEventsList() {
            return labEventsList;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();

            if ( contextVesselEvent != null ) {
                // Used for descendants only
                LabEvent contextEvent = contextVesselEvent.getLabEvent();
                if (!visitedLabEvents.add(contextEvent)) {
                    return TraversalControl.StopTraversing;
                }
                if (context.getHopCount() > hopCount) {
                    hopCount = context.getHopCount();
                    labEventsList.add(contextEvent);

                    // handle incoming branch transfers e.g. IndexedAdapterLigation, BaitAddition
                    // todo jmt recurse for BaitSetup
                    for (LabVessel targetLabVessel : contextEvent.getTargetLabVessels()) {
                        for (Object o : targetLabVessel.getContainerRole().getSectionTransfersTo()) {
                            SectionTransfer sectionTransfer = (SectionTransfer) o;
                            if (visitedLabEvents.add(sectionTransfer.getLabEvent())) {
                                labEventsList.add(sectionTransfer.getLabEvent());
                            }
                        }
                    }

                    List<LabEvent> inPlaceLabEvents = new ArrayList<>();
                    LabVessel contextVessel = contextVesselEvent.getSourceLabVessel();
                    if (contextVessel == null) {
                        for (LabVessel sourceLabVessel : contextEvent.getSourceLabVessels()) {
                            inPlaceLabEvents.addAll(sourceLabVessel.getInPlaceEventsWithContainers());
                        }
                    } else {
                        inPlaceLabEvents.addAll(contextVessel.getInPlaceEventsWithContainers());
                    }
                    Collections.sort(inPlaceLabEvents, new Comparator<LabEvent>() {
                        @Override
                        public int compare(LabEvent o1, LabEvent o2) {
                            return o1.getEventDate().compareTo(o2.getEventDate());
                        }
                    });
                    for (LabEvent inPlaceLabEvent : inPlaceLabEvents) {
                        if (visitedLabEvents.add(inPlaceLabEvent)) {
                            labEventsList.add(inPlaceLabEvent);
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

    }

    @Test(enabled = false, groups = TestGroups.STUBBY)
    public void findMessageFilesForBatch() {
        List<String> lcSets = new ArrayList<>();
        // PDO-135, C19F5ACXX
        lcSets.add("LCSET-2519");
        // D1JNDACXX, WR 34532, LCSET-2588, PDO-183
        lcSets.add("LCSET-2588");
        // C1E2VACXX, WR 34473, LCSET-2578, 2 samples from GBM PDO-173, 88 samples from PDO-183, 2 samples Kantoff from PDO-181
        // C1E35ACXX, WR 34131, LCSET-2508, PDO-74
        // C1E3NACXX, WR 34130, LCSET-2507, PDO-74
        // C1EAAACXX, WR 34130, 34131
        // C1EAHACXX, WR 34473, LCSET-2578, ..., WR 34474, LCSET-2582, 76 samples from PDO-183 Black Fan Anemia, 14 samples from PDO-173 GBM, 2 samples from PDO-181 Kantoff Resistance
        // C1EE5ACXX, WR 34474, LCSET-2582, 76 samples from PDO-183 Black Fan Anemia, 14 samples from PDO-173 GBM, 2 samples from PDO-181 Kantoff Resistance
        // C1EK8ACXX, WR 34474, LCSET-2582, 76 samples from PDO-183 Black Fan Anemia, 14 samples from PDO-173 GBM, 2 samples from PDO-181 Kantoff Resistance
        // C1ETHACXX, WR 34033, LCSET-2492, PDO-82 TCGA Bladder Cancer Duplex Redos Batch 195, 235, Request Created - null, Product Order - null PDO-83 TCGA Head and Neck Duplex Redos Batch 107, 188, 190, 215, 54, 83, Work Request Created - null, Product Order - null PDO-84 TCGA Kidney Clear Cell Duplex Redos Batch 32, 68, 69, 70, Work Request Created - null, Product Order - null PDO-86 TCGA Low Grade Glioma Duplex Redos Batch 78, 163, Work Request Created - null, Product Order - null PDO-87 TCGA Lung Adenocarcinoma Duplex Redos Batch 119, 166, 183, 222, 232, 58
        // C1EU9ACXX, WR 34037, 34135
        // D1DUTACXX, WR 34271, LCSET-2519...
        // D1J6MACXX, WR 34190, LCSET-2521, PDO-74
        // D1JP2ACXX, WR 34130, LCSET-2507...
        // D1JPLACXX
        // D1JRRACXX
        // D1JYWACXX
        // D1K4LACXX
        // D1K54ACXX
        // D1K5DACXX
        // D1K7DACXD

        for (String lcSet : lcSets) {
            LabBatch labBatch = labBatchDao.findByBusinessKey(lcSet);
            Set<LabVessel> startingLabVessels = labBatch.getStartingBatchLabVessels();
            // For now, assume all vessels have the same events
            LabVessel labVessel = startingLabVessels.iterator().next();
            AccumulateLabEvents accumulateLabEvents = new AccumulateLabEvents();
            labVessel.evaluateCriteria(accumulateLabEvents, TransferTraverserCriteria.TraversalDirection.Descendants);
            try {
                File inboxDirectory = new File("C:/Temp/seq/lims/bettalims/production/inbox");
                System.out.println("# " + labBatch.getBatchName());
                for (LabEvent labEvent : accumulateLabEvents.getLabEventsList()) {
                    GregorianCalendar gregorianCalendar = new GregorianCalendar();
                    gregorianCalendar.setTime(labEvent.getEventDate());
                    int year = gregorianCalendar.get(Calendar.YEAR);
                    int month = gregorianCalendar.get(Calendar.MONTH) + 1;
                    int day = gregorianCalendar.get(Calendar.DAY_OF_MONTH);
                    int hour = gregorianCalendar.get(Calendar.HOUR_OF_DAY);
                    int minute = gregorianCalendar.get(Calendar.MINUTE);
                    int second = gregorianCalendar.get(Calendar.SECOND);
                    String yearMonthDay = String.format("%d%02d%02d", year, month, day);
                    String dateString = xmlDateFormat.format(labEvent.getEventDate());

                    File dayDirectory = new File(inboxDirectory, yearMonthDay);
                    String[] messageFileList = dayDirectory.list();
                    if (messageFileList == null) {
                        throw new RuntimeException("Failed to find directory " + dayDirectory.getName());
                    }
                    Collections.sort(Arrays.asList(messageFileList));
                    boolean found = false;
                    for (String fileName : messageFileList) {
                        File messageFile = new File(dayDirectory, fileName);
                        String message = FileUtils.readFileToString(messageFile);
                        if (message.contains(labEvent.getLabEventType().getName()) && message.contains(dateString)) {
                            System.out.println("#" + labEvent.getLabEventType().getName());
                            System.out.println(messageFile.getCanonicalPath());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("#Failed to find file for " + labBatch.getBatchName() + " " +
                                           labEvent.getLabEventType().getName() + " " + labEvent.getEventDate());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
