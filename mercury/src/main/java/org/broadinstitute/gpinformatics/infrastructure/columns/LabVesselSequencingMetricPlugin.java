package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.SequencingDemultiplexDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.columns.SampleMetadataSequencingMetricPlugin.buildHeaders;

public class LabVesselSequencingMetricPlugin implements ListPlugin {

    private enum VALUE_COLUMN_TYPE {
        SAMPLE_ALIAS("Sample Alias");

        private String displayName;
        private ConfigurableList.Header resultHeader;

        VALUE_COLUMN_TYPE( String displayName ) {
            this.displayName = displayName;
            this.resultHeader = new ConfigurableList.Header(displayName, displayName, "");
        }

        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getResultHeader() {
            return resultHeader;
        }

    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        return null;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        List<ConfigurableList.Header> headers = buildHeaders();
        headers.add(VALUE_COLUMN_TYPE.SAMPLE_ALIAS.getResultHeader());

        SequencingDemultiplexDao sequencingDemultiplexDao = ServiceAccessUtility.getBean(SequencingDemultiplexDao.class);
        IlluminaSequencingRunDao sequencingRunDao = ServiceAccessUtility.getBean(IlluminaSequencingRunDao.class);

        LabVessel labVessel = (LabVessel) entity;

        LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
        labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

        // TODO sample alias is still just SM, but would probably be collab pt id or something along those lines
        Map<String, SampleInstanceV2> mapSampleToSampleInstance = labVessel.getSampleInstancesV2().stream()
                .collect(Collectors.toMap(SampleInstanceV2::getNearestMercurySampleName,
                        Function.identity()));

        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();

        Set<String> runNames = new HashSet<>();
        for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions: eval.getPositions().asMap().entrySet()) {
            if (!OrmUtil.proxySafeIsInstance(labVesselAndPositions.getKey(), IlluminaFlowcell.class)) {
                continue;
            }

            IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(labVesselAndPositions.getKey(), IlluminaFlowcell.class);
            String flowcellBarcode = flowcell.getLabel();

            List<IlluminaSequencingRun> runs = sequencingRunDao.findByFlowcellBarcode(flowcellBarcode);

            List<String> runNamesList =
                    runs.stream().map(IlluminaSequencingRun::getRunName).collect(Collectors.toList());
            runNames.addAll(runNamesList);
        }

        List<DemultiplexSampleMetric> runMetrics = sequencingDemultiplexDao.findByRunName(new ArrayList<>(runNames));

        for (DemultiplexSampleMetric metric: runMetrics) {
            if (!mapSampleToSampleInstance.containsKey(metric.getSampleAlias())) {
                continue;
            }
            List<String> cells = new ArrayList<>();


            List<ConfigurableList.ResultList> nestedTables = new ArrayList<>();
            // Empty nested table for row name
            nestedTables.add(null);

            cells.add(metric.getRunName());
            cells.add(ColumnValueType.DATE.format(metric.getRunDate(), ""));
            cells.add(metric.getFlowcell());
            cells.add(String.valueOf(metric.getLane()));
            cells.add(metric.getAnalysisNode());
            cells.add(String.valueOf(metric.getNumberOfOneMismatchIndexReads()));
            cells.add(String.valueOf(metric.getNumberOfQ30BasesPF()));
            cells.add(String.valueOf(metric.getNumberOfPerfectIndexReads()));
            cells.add(String.valueOf(metric.getNumberOfReads()));
            cells.add(String.valueOf(metric.getNumberOfPerfectReads()));
            long numReadsPF = metric.getNumberOfPerfectIndexReads() + metric.getNumberOfOneMismatchIndexReads();
            double orphanRate = (numReadsPF / (double)metric.getNumberOfReads()) * 100;
            cells.add(String.valueOf(orphanRate));
            cells.add( String.valueOf(metric.getMeanQualityScorePF()));
            cells.add( metric.getSampleAlias());

            ConfigurableList.ResultRow resultRow = new ConfigurableList.ResultRow(null, cells, metric.getRunName() + " " + metric.getLane());
            resultRow.setCellNestedTables(nestedTables);
            resultRows.add(resultRow);
        }

        return new ConfigurableList.ResultList( resultRows, headers, 0, "ASC");
    }
}
