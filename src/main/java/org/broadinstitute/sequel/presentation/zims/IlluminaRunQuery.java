package org.broadinstitute.sequel.presentation.zims;

import org.apache.commons.collections15.map.LRUMap;
import org.broadinstitute.sequel.boundary.zims.IlluminaRunService;
import org.broadinstitute.sequel.entity.zims.ZamboniRead;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
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
//@Named
//@ConversationScoped
@ManagedBean
@ViewScoped
public class IlluminaRunQuery extends AbstractJsfBean {

    @Inject
    private IlluminaRunService illuminaRunService;

    @Inject @IlluminaRunQueryCache
    private LRUMap<String, ZimsIlluminaRun> runCache;

//    @Inject
//    private Conversation conversation;

    private List<String> columnNames = new ArrayList<String>();
    private List<ColumnModel> columns = new ArrayList<ColumnModel>();
    private static List<ColumnModel> allColumnss = new ArrayList<ColumnModel>();
    private static Map<String, ColumnModel> allColumns = new LinkedHashMap<String, ColumnModel>();

    private static final String libraryVar = "library";
    private static final String columnVar = "column";

    private String runName;
    private ZimsIlluminaRun run;
    private List<ZimsIlluminaChamber> lanes;
    private List<ZamboniRead> reads;
    private ZimsIlluminaChamber selectedLane;

    static {
        allColumns.put("Library Name", new ColumnModel("Library Name", "library", null));
        allColumns.put("Initiative", new ColumnModel("Initiative", "initiative", null));
        allColumns.put("Project", new ColumnModel("Project", "project", null));
        allColumns.put("Work Request", new ColumnModel("Work Request", "workRequest", null));
        allColumns.put("GSSR Barcode", new ColumnModel("GSSR Barcode", "gssrBarcode", null));
        allColumns.put("Indexing Scheme", new ColumnModel("Indexing Scheme", null, libraryVar + ".indexingScheme.name"));
//        allColumns.add(new ColumnModel("", "", null));
    }

    public IlluminaRunQuery() {
//        columns.add(new ColumnModel("Library Name", "library", null));
//        columns.add(new ColumnModel("Initiative", "initiative", null));
//        columns.add(new ColumnModel("Project", "project", null));
//        columns.add(new ColumnModel("Work Request", "workRequest", null));
//        columns.add(new ColumnModel("GSSR Barcode", "gssrBarcode", null));
//        columns.add(new ColumnModel("Indexing Scheme", null, libraryVar + ".indexingScheme.name"));
//        columns.add(new ColumnModel("", ""));
//        columns.addAll(allColumns);
        setColumnNames(Arrays.asList("Library Name", "Initiative", "Project"));
    }

    public void query() {
//        if (conversation.isTransient()) {
//            conversation.begin();
//        }

        if (run == null) {
            run = runCache.get(runName);
            if (run == null) {
                run = illuminaRunService.getRun(runName);
                runCache.put(runName, run);
            }

            if (run != null) {
                if (lanes == null) {
                    lanes = new ArrayList<ZimsIlluminaChamber>(run.getChambers());
                    Collections.sort(lanes, new Comparator<ZimsIlluminaChamber>() {
                        @Override
                        public int compare(ZimsIlluminaChamber lane1, ZimsIlluminaChamber lane2) {
                            return lane1.getChamberName().compareTo(lane2.getChamberName());
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

    public Map<String, ColumnModel> getAllColumns() {
        return allColumns;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
        columns.clear();
        for (String columnName : columnNames) {
            columns.add(allColumns.get(columnName));
        }
    }

    public List<ColumnModel> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnModel> columns) {
        this.columns = columns;
    }

    public static String getLibraryVar() {
        return libraryVar;
    }

    public String eval(String el) {
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        ExpressionFactory expressionFactory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
        ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, "#{" + el + "}", String.class);
        return (String) valueExpression.getValue(elContext);
    }

    public static String getColumnVar() {
        return columnVar;
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
