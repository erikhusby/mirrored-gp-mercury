/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

@Named
@RequestScoped
public class ReagentsBean {
    @ConversationScoped
    public static class ReagentDesignTableData extends TableData<ReagentDesign> {

    }

    @Inject
    ReagentsBean.ReagentDesignTableData reagentDesignTableData;

//    @ConversationScoped
//    public static class TwoDBarcodedTubeDataTable extends TableData<TwoDBarcodedTube> {
//
//    }
//
//    @Inject
//    ReagentsBean.TwoDBarcodedTubeDataTable twoDBarcodedTubeDataTable;

    private ReagentDesign reagentDesign;

    private String barcode;

    public List<ReagentDesign.ReagentType> getReagentTypes() {
        return Arrays.asList(ReagentDesign.ReagentType.values());
    }

    public List<ReagentDesign> getAllReagentDesigns() {
        return reagentDesignTableData.getValues();
    }

//    public List<TwoDBarcodedTube> getAllTwoDBarcodedTubes() {
//        return twoDBarcodedTubeDataTable.getValues();
//    }

    private void initReagentDesign() {
        reagentDesign = new ReagentDesign();
    }

    public ReagentDesignTableData getReagentDesignTableData() {
        return reagentDesignTableData;
    }

    public void setReagentDesignTableData(ReagentsBean.ReagentDesignTableData reagentDesignTableData) {
        this.reagentDesignTableData = reagentDesignTableData;
    }

    public void setReagentDesignTableData(List<ReagentDesign> tableData) {
        this.reagentDesignTableData.setValues(tableData);
    }
//
//    public TwoDBarcodedTubeDataTable getTwoDBarcodedTubeDataTable() {
//        return twoDBarcodedTubeDataTable;
//    }
//
//    public void setTwoDBarcodedTubeDataTable(TwoDBarcodedTubeDataTable twoDBarcodedTubeDataTable) {
//        this.twoDBarcodedTubeDataTable = twoDBarcodedTubeDataTable;
//    }
//
//    public void setTwoDBarcodedTubeDataTable(List<TwoDBarcodedTube> tableData) {
//        this.twoDBarcodedTubeDataTable.setValues(tableData);
//    }

    public ReagentDesign getReagentDesign() {
        if (reagentDesign == null) {
            initReagentDesign();
        }
        return reagentDesign;
    }

    public void setReagentDesign(ReagentDesign reagentDesign) {
        this.reagentDesign = reagentDesign;
    }


    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String tubeBarcode) {
        this.barcode = tubeBarcode;
    }
}
