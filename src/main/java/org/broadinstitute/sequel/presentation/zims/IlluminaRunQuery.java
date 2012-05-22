package org.broadinstitute.sequel.presentation.zims;

import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.sequel.boundary.zims.IlluminaRunService;
import org.broadinstitute.sequel.entity.zims.ZamboniRead;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author breilly
 */
@Named
@ConversationScoped
//@ManagedBean
//@ViewScoped
public class IlluminaRunQuery extends AbstractJsfBean {

    @Inject
    private IlluminaRunService illuminaRunService;

    @Inject @IlluminaRunQueryCache
    private LRUMap<String, ZimsIlluminaRun> runCache;

    @Inject
    private Conversation conversation;

    private List<String> columnNames = new ArrayList<String>();
    private List<ColumnModel> columns = new ArrayList<ColumnModel>();
    private static List<ColumnModel> allColumns = new ArrayList<ColumnModel>();
    private static List<String> allColumnNames = new ArrayList<String>();
    private static Map<String, ColumnModel> columnsByName = new LinkedHashMap<String, ColumnModel>();

    private String runName;
    private ZimsIlluminaRun run;
    private List<ZimsIlluminaChamber> lanes;
    private List<ZamboniRead> reads;
    private ZimsIlluminaChamber selectedLane;

    static {
//        addColumn("Library Name", "library", null);
        addColumn("Project", "project", null);
        addColumn("Initiative", "initiative", null);
        addColumn("Work Request", "workRequestId", null);
        addColumn("Indexing Scheme", null, "library.molecularIndexingScheme.name");
        addColumn("Expected Insert Size", "expectedInsertSize", null);
        addColumn("Analysis Type", "analysisType", null);
        addColumn("Reference Sequence", "referenceSequence", null);
        addColumn("Reference Sequence Version", "referenceSequenceVersion", null);
        addColumn("Sample Alias", "sampleAlias", null);
        addColumn("Collaborator", "sampleCollaborator", null);
        addColumn("Organism", "organism", null);
        addColumn("Species", "species", null);
        addColumn("Strain", "strain", null);
        addColumn("LSID", "lsid", null);
        addColumn("Tissue Type", "tissueType", null);
//        addColumn("Expected Plasmid", "expectedPlasmid", null);
        addColumn("Aligner", "aligner", null);
        addColumn("RRBS Size Range", "rrbsSizeRange", null);
        addColumn("Restriction Enzyme", "restrictionEnzyme", null);
        addColumn("Cell Line", "cellLine", null);
        addColumn("Bait Set", "baitSetName", null);
        addColumn("Individual", "individual", null);
        addColumn("Measured Insert Size", "labMeasuredInsertSize", null);
//        addColumn("Positive Control?", "isPositiveControl", null);
//        addColumn("Negative Control?", "isNegativeControl", null);
        addColumn("Weirdness", "weirdness", null);
        addColumn("Pre-Circularization DNA Size", "preCircularizationDnaSize", null);
        addColumn("Dev Experiment", null, "library.devExperimentData == null ? null : library.devExperimentData.experiment");
        addColumn("Dev Experiment Conditions", null, "illuminaRunQuery.join(library.devExperimentData.conditions)");
        addColumn("GSSR Barcodes", null, "illuminaRunQuery.join(library.gssrBarcodes)");
        addColumn("GSSR Sample Type", "gssrSampleType", null);
        addColumn("Target Lane Coverage", "targetLaneCoverage", null);
//        addColumn("", "", null);
    }

    public static void addColumn(String header, String property, String expression) {
        ColumnModel column = new ColumnModel(header, property, expression);
        allColumns.add(column);
        allColumnNames.add(header);
        columnsByName.put(header, column);
    }

    public IlluminaRunQuery() {
        // use an ArrayList because Mojarra doesn't like to deal with the result of Arrays.asList()
        setColumnNames(new ArrayList<String>(Arrays.asList("Project", "Work Request", "Sample Alias", "GSSR Barcodes")));
    }

    public void query() {
        if (conversation.isTransient()) {
            conversation.begin();
        }

        if (run == null) {
            run = runCache.get(runName);
            if (run == null) {
                run = illuminaRunService.getRun(runName);
                runCache.put(runName, run);
            }

            if (run != null) {
                if (lanes == null) {
                    lanes = new ArrayList<ZimsIlluminaChamber>(run.getLanes());
                    Collections.sort(lanes, new Comparator<ZimsIlluminaChamber>() {
                        @Override
                        public int compare(ZimsIlluminaChamber lane1, ZimsIlluminaChamber lane2) {
                            return lane1.getName().compareTo(lane2.getName());
                        }
                    });
                }

                if (reads == null) {
                    reads = run.getReads();
                }
            }
        }
    }

    public void clearCache() {
        runCache.clear();
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public ZimsIlluminaRun getRun() {
        return run;
    }

    public ZimsIlluminaChamber getSelectedLane() {
        return selectedLane;
    }

    public void setSelectedLane(ZimsIlluminaChamber selectedLane) {
        this.selectedLane = selectedLane;
    }

    public List<ZimsIlluminaChamber> getLanes() {
        return lanes;
    }

    public List<ZamboniRead> getReads() {
        return reads;
    }

    public List<ColumnModel> getAllColumns() {
        return allColumns;
    }

    public List<String> getAllColumnNames() {
        return allColumnNames;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
        columns.clear();
        for (String columnName : columnNames) {
            columns.add(columnsByName.get(columnName));
        }
    }

    public List<ColumnModel> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnModel> columns) {
        this.columns = columns;
    }

    public String eval(String el) {
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        ExpressionFactory expressionFactory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
        ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, "#{" + el + "}", String.class);
        return (String) valueExpression.getValue(elContext);
    }

    public String join(List<String> values) {
        return StringUtils.join(values, ", ");
    }

    public static class ColumnModel implements Serializable {

        private String header;
        private String property;
        private String expression;

        public ColumnModel(String header, String property, String expression) {
            this.header = header;
            this.property = property;
            this.expression = expression;
        }

        public String getHeader() {
            return header;
        }

        public String getProperty() {
            return property;
        }

        public String getExpression() {
            return expression;
        }
    }
}
