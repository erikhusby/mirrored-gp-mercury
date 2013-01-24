package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.testng.annotations.Test;

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
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabBatchDbTest extends ContainerTest {

    @Inject
    private LabBatchDAO labBatchDAO;

    public static final String XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private SimpleDateFormat xmlDateFormat = new SimpleDateFormat(XML_DATE_FORMAT);

    /**
     * Used in test verification, accumulates the events in a chain of transfers
     */
    public static class AccumulateLabEvents implements TransferTraverserCriteria {
        private int hopCount = -1;
        private final List<LabEvent> labEventsList = new ArrayList<LabEvent>();
        /**
         * Avoid infinite loops
         */
        private Set<LabEvent> visitedLabEvents = new HashSet<LabEvent>();


        public List<LabEvent> getLabEventsList() {
            return labEventsList;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getEvent() != null) {
                if (!visitedLabEvents.add(context.getEvent())) {
                    return TraversalControl.StopTraversing;
                }
                if (hopCount > this.hopCount) {
                    this.hopCount = hopCount;
                    labEventsList.add(context.getEvent());

                    // handle incoming branch transfers e.g. IndexedAdapterLigation, BaitAddition
                    // todo jmt recurse for BaitSetup
                    for (LabVessel targetLabVessel : context.getEvent().getTargetLabVessels()) {
                        for (Object o : targetLabVessel.getContainerRole().getSectionTransfersTo()) {
                            SectionTransfer sectionTransfer = (SectionTransfer) o;
                            if (visitedLabEvents.add(sectionTransfer.getLabEvent())) {
                                labEventsList.add(sectionTransfer.getLabEvent());
                            }
                        }
                    }

                    List<LabEvent> inPlaceLabEvents = new ArrayList<LabEvent>();
                    if (context.getLabVessel() == null) {
                        for (LabVessel sourceLabVessel : context.getEvent().getSourceLabVessels()) {
                            inPlaceLabEvents.addAll(sourceLabVessel.getInPlaceEvents());
                        }
                    } else {
                        inPlaceLabEvents.addAll(context.getLabVessel().getInPlaceEvents());
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
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

    }

    @Test(enabled = true)
    public void findMessageFilesForBatch() {
        List<String> lcSets = new ArrayList<String>();
        // PDO-135, C19F5ACXX
        lcSets.add("LCSET-2519");
        // PDO-183, D1JN1ACXX
        lcSets.add("LCSET-2588");

        for (String lcSet : lcSets) {
            LabBatch labBatch = labBatchDAO.findByBusinessKey(lcSet);
            Set<LabVessel> startingLabVessels = labBatch.getStartingLabVessels();
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
