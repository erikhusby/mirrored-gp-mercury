package org.broadinstitute.sequel.presentation.pass;


import org.broadinstitute.sequel.boundary.SummarizedPass;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

public class SummarizedPassDataModel extends ListDataModel<SummarizedPass> implements SelectableDataModel<SummarizedPass> {


    @Override
    public Object getRowKey(SummarizedPass rowPass) {
        return rowPass.getPassNumber();

    }

    @Override
    public SummarizedPass getRowData(String rowKey) {
        for (SummarizedPass pass : (List<SummarizedPass>) getWrappedData()) {
            if (pass.getPassNumber().equals(rowKey))
                return pass;
        }

        throw new RuntimeException("Pass " + rowKey + " not found!");
    }
}
