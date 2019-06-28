package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FCTJiraFieldFactory extends AbstractBatchJiraFieldFactory {
    public static final String JIRA_CRLF = "\\\\\\\\";
    /**
     * The header columns of the lane info table found in FCT or MISEQ Jira tickets.
     */
    public static String LANE_INFO_HEADER = "||Lane||Loading Vessel||Loading Concentration||LCSET||Product||\n";

    private static final Log logger = LogFactory.getLog(FCTJiraFieldFactory.class);

    public FCTJiraFieldFactory(@Nonnull LabBatch batch, SequencingTemplateFactory sequencingTemplateFactory) {
        super(batch, CreateFields.ProjectType.FCT_PROJECT, sequencingTemplateFactory, null, null);
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {
        Set<CustomField> customFields = new HashSet<>();

        //Old FCT may not have vessel position set and won't have lane info
        for (LabBatchStartingVessel startingVessel : batch.getLabBatchStartingVessels()) {
            if (startingVessel.getVesselPosition() == null) {
                return customFields;
            }
        }

        List<LabBatchStartingVessel> startingVessels = new ArrayList<>(batch.getLabBatchStartingVessels());
        Collections.sort(startingVessels, new Comparator<LabBatchStartingVessel>() {
            @Override
            public int compare(LabBatchStartingVessel startingVessel, LabBatchStartingVessel startingVessel2) {
                return startingVessel.getVesselPosition().compareTo(startingVessel2.getVesselPosition());
            }
        });
        StringBuilder laneInfoBuilder = new StringBuilder();
        laneInfoBuilder.append(LANE_INFO_HEADER);
        for (LabBatchStartingVessel startingVessel : startingVessels) {
            laneInfoBuilder.append(makeJiraFieldRecord(startingVessel));
        }
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.LANE_INFO,
                laneInfoBuilder.toString()));

        // Build read structure String of the form 151x8x8x151
        try {
            SequencingTemplateType sequencingTemplate =
                    sequencingTemplateFactory.getSequencingTemplate(this.batch, false);
            String readStructure = sequencingTemplate.getReadStructure();
            readStructure = readStructure.replaceAll("T", "x");
            readStructure = readStructure.replaceAll("B", "x");
            if (readStructure.endsWith("x")) {
                readStructure = readStructure.substring(0, readStructure.length() - 1);
            }
            customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.READ_STRUCTURE, readStructure));
        } catch (Exception e) {
            logger.error("Failed to build read structure", e);
        }

        return customFields;
    }

    @Override
    public String generateDescription() {
        return null;
    }

    @Override
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        for (LabVessel vessel : batch.getStartingBatchLabVessels()) {
            summary.append(vessel.getLabel());
        }
        return summary.toString();
    }


    /**
     * Parses the laneInfo Jira field. Expects a '|' delimited table, with '\n' row delimiter.
     * Skips the laneInfo table header row. The row order is preserved in the output records.
     */
    public static List<JiraLaneInfo> parseJiraLaneInfo(String jiraField) {
        List<JiraLaneInfo> list = new ArrayList<>();
        String[] lines = jiraField.split("\\|\\n\\|");
        for (String line : lines) {
            if (LANE_INFO_HEADER.startsWith(line)) {
                continue;
            }
            list.add(new JiraLaneInfo(line));
        }
        return list;
    }

    /** Concatentates values into one Jira lane info record including newline at the end. */
    public static String makeJiraFieldRecord(LabBatchStartingVessel startingVessel) {
        return makeJiraFieldRecord(startingVessel.getVesselPosition().name(),
                startingVessel.getLabVessel().getLabel(),
                startingVessel.getConcentration().toString(),
                StringUtils.trimToEmpty(startingVessel.getLinkedLcset()),
                // Puts each product name on its own line in the Jira table cell.
                replaceDtoDelimiter(startingVessel.getProductNames()));
    }

    public static String makeJiraFieldRecord(String... args) {
        return "|" + StringUtils.join(args, "|") + "|\n";
    }

    public static String replaceDtoDelimiter(String dtoString) {
        // Jira wants two backslashes to show a <return> in the Jira table cell.
        return StringUtils.trimToEmpty(dtoString).replaceAll(DesignationDto.DELIMITER, JIRA_CRLF);
    }
}
