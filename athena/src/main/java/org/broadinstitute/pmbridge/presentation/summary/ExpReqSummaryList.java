package org.broadinstitute.pmbridge.presentation.summary;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 6/28/12
 * Time: 9:32 AM
 */

public class ExpReqSummaryList extends ListDataModel<ExperimentRequestSummary> implements SelectableDataModel<ExperimentRequestSummary> {


    @Override
    public Object getRowKey(ExperimentRequestSummary requestSummary) {
        return requestSummary.getExperimentId().value;

    }

    @Override
    public ExperimentRequestSummary getRowData(String rowKey) {
        for (ExperimentRequestSummary summary : (List<ExperimentRequestSummary>) getWrappedData()) {
            if (summary.getExperimentId().getValue().equals(rowKey))
                return summary;
        }

        throw new RuntimeException("Experiment Request Summary " + rowKey + " not found!");
    }
}

