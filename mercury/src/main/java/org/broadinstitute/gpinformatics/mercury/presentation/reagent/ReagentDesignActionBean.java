/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.tokenimporters.ReagentDesignTokenInput;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/reagent/design.action")
public class ReagentDesignActionBean extends CoreActionBean {

    private static final String CREATE_DESIGN = CoreActionBean.CREATE + "New Design";
    private static final String EDIT_DESIGN = CoreActionBean.EDIT + "Design: ";

    private static final String REAGENT_LIST_PAGE = "/reagent/list.jsp";
    private static final String REAGENT_CREATE_PAGE = "/reagent/create.jsp";
    private static final String REAGENT_BARCODE_PAGE = "/reagent/barcode.jsp";

    private static final String SAVE_BARCODE_ACTION = "saveBarcode";
    private static final String ASSIGN_BARCODE_ACTION = "assignBarcode";
    private static final String BARCODE_REAGENT_ACTION = "barcodeReagent";

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @ValidateNestedProperties({
            @Validate(field = "designName", required = true, maxlength = 255, on = {SAVE_ACTION}),
            @Validate(field = "targetSetName", required = true, maxlength = 2000, on = {SAVE_ACTION}),
            @Validate(field = "reagentType", required = true, on = {SAVE_ACTION})
    })
    private ReagentDesign reagentDesign;

    @Validate(required = true, on = {EDIT_ACTION})
    private String businessKey;

    private Collection<ReagentDesign> allReagentDesigns;

    private Map<String, String> barcodeMap;

    @Validate(required = true, on = {SAVE_BARCODE_ACTION})
    private String barcode;

    private List<String> allBarcodes;
    private String q;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (!StringUtils.isBlank(businessKey)) {
            reagentDesign = reagentDesignDao.findByBusinessKey(businessKey);
        } else {
            reagentDesign = new ReagentDesign();
        }

        List<LabVessel> labVessels;
        allBarcodes = new ArrayList<String>();
        allReagentDesigns = reagentDesignDao.findAll();

//        for (ReagentDesign rd : allReagentDesigns) {
//            labVessels = labVesselDao.findByReagent(rd.getBusinessKey());
//            List<String> barcodes = new ArrayList<String>();
//            for (LabVessel labVessel : labVessels) {
//                if (!labVessel.getLabel().isEmpty()) {
//                    barcodes.add(labVessel.getLabel());
//                    allBarcodes.add(labVessel.getLabel());
//                }
//            }
//
//            getBarcodeMap().put(rd.getBusinessKey(), StringUtils.join(barcodes, ", "));
//        }

    }


    @HandlesEvent(ASSIGN_BARCODE_ACTION)
    public Resolution barcode() {
        return new ForwardResolution(REAGENT_BARCODE_PAGE);
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(REAGENT_LIST_PAGE);
    }

    public String getReagentDesignCompleteData() throws Exception {
        if (!StringUtils.isBlank(getBusinessKey())) {
            return ReagentDesignTokenInput
                    .getReagentDesignCompleteData(reagentDesignDao, getBusinessKey());
        }
        return "";
    }

    @HandlesEvent("reagentListAutocomplete")
    public Resolution reagentListAutocomplete() throws Exception {
        return new StreamingResolution("text",
                new StringReader(ReagentDesignTokenInput.getJsonString(reagentDesignDao, getQ())));
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_DESIGN);
        return new ForwardResolution(REAGENT_CREATE_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateReagent(){
        if (isCreating()){
            ReagentDesign d = reagentDesignDao.findByBusinessKey(reagentDesign.getBusinessKey());
            if (d!=null){
                addValidationError("designName", String.format("Name \"%s\" is already in use.",
                        reagentDesign.getBusinessKey()));
            }
        }
    }
    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            reagentDesignDao.persist(reagentDesign);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        addMessage(getSubmitString() + " '" + reagentDesign.getBusinessKey() + "' was successful");
        return new RedirectResolution(ReagentDesignActionBean.class, LIST_ACTION);
    }


    @ValidationMethod(on = {BARCODE_REAGENT_ACTION})
    public void checkBarcodeUsed() {
        List<String> barcodeList = Arrays.asList(barcode.trim().split("\\W"));
        for (String barcodeItem : barcodeList) {
            if (allBarcodes.contains(barcodeItem)) {
                addValidationError("barcode", "Barcode is already in use.");
            }
        }
    }

    @HandlesEvent(BARCODE_REAGENT_ACTION)
    public Resolution barcodeReagent() {
        List<TwoDBarcodedTube> twoDBarcodedTubeList = new ArrayList<TwoDBarcodedTube>();
        String savedChanges = "";

        List<String> barcodeList = Arrays.asList(barcode.trim().split("\\W"));
        savedChanges += reagentDesign.getBusinessKey() + " " + barcodeList.toString() + ")\n";
        for (String barcodeItem : barcodeList) {
            DesignedReagent reagent = new DesignedReagent(reagentDesign);
            TwoDBarcodedTube twoDee = new TwoDBarcodedTube(barcodeItem);
            twoDee.addReagent(reagent);
            twoDBarcodedTubeList.add(twoDee);
        }


        twoDBarcodedTubeDAO.persistAll(twoDBarcodedTubeList);
        addMessage(
                String.format("%s tubes initialized with reagents: %s.", twoDBarcodedTubeList.size(), savedChanges));

        return new RedirectResolution(ReagentDesignActionBean.class, LIST_ACTION)
                .addParameter("businessKey", businessKey);
    }


    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Collection<ReagentDesign> getAllReagentDesigns() {
        return allReagentDesigns;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public ReagentDesign getReagentDesign() {
        return reagentDesign;
    }

    public void setReagentDesign(ReagentDesign reagentDesign) {
        this.reagentDesign = reagentDesign;
    }

    public void setBarcodeMap(Map<String, String> barcodeMap) {
        this.barcodeMap = barcodeMap;
    }

    public Map<String, String> getBarcodeMap() {
        if (barcodeMap == null) {
            barcodeMap = new HashMap<String, String>();
        }

        return barcodeMap;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
