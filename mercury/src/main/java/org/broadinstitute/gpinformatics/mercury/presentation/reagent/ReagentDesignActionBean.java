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
import net.sourceforge.stripes.validation.ValidationState;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.tokenimporters.ReagentDesignTokenInput;

import javax.inject.Inject;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/reagent/design.action")
public class ReagentDesignActionBean extends CoreActionBean {

    public static final String CREATE_DESIGN = CoreActionBean.CREATE + "Reagent Design";
    private static final String EDIT_DESIGN = CoreActionBean.EDIT + "Reagent Design";
    public static final String DESIGN_PARAMETER = "reagent";

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

    @Inject
    private LabVesselDao labVesselDao;

    public ReagentDesignActionBean() {
        super(CREATE_DESIGN, EDIT_DESIGN, DESIGN_PARAMETER);
    }

    @ValidateNestedProperties({
            @Validate(field = "designName", label = "Design Name", required = true, maxlength = 255,
                    on = {SAVE_ACTION}),
            @Validate(field = "targetSetName", label = "Target Set Name", required = true, maxlength = 2000,
                    on = {SAVE_ACTION})
    })
    private ReagentDesign editReagentDesign;

    @Validate(required = true, on = {EDIT_ACTION})
    private String reagentDesign;

    private Collection<ReagentDesign> allReagentDesigns;

    @Inject
    private ReagentDesignTokenInput reagentDesignTokenInput;

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
        reagentDesign = getContext().getRequest().getParameter("reagentDesign");
        if (!StringUtils.isBlank(reagentDesign)) {
            editReagentDesign = reagentDesignDao.findByBusinessKey(reagentDesign);
        } else {
            editReagentDesign = new ReagentDesign();
        }

        allBarcodes = new ArrayList<>();
        allReagentDesigns = reagentDesignDao.findAll();

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

    @HandlesEvent("reagentListAutocomplete")
    public Resolution reagentListAutocomplete() throws Exception {
        return createTextResolution(reagentDesignTokenInput.getJsonString(getQ()));
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

    @ValidationMethod(on = {SAVE_ACTION,CREATE_ACTION}, when = ValidationState.ALWAYS)
    public void validateReagent() {
        if (isCreating()) {
            if (editReagentDesign.getReagentType() == null) {
                addValidationError("editReagentDesign.reagentType", "Reagent Type is a required field.");
            }

            ReagentDesign d = reagentDesignDao.findByBusinessKey(editReagentDesign.getBusinessKey());
            if (d != null) {
                addValidationError("editReagentDesign.designName", String.format("Name \"%s\" is already in use.",
                        editReagentDesign.getBusinessKey()));
            }
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        try {
            reagentDesignDao.persist(editReagentDesign);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        addMessage(getSubmitString() + " '" + editReagentDesign.getBusinessKey() + "' was successful");
        return new RedirectResolution(ReagentDesignActionBean.class, LIST_ACTION);
    }


    @ValidationMethod(on = {BARCODE_REAGENT_ACTION}, when = ValidationState.ALWAYS)
    public void checkBarcodeUsed() {
        final List<LabVessel> byListIdentifiers = labVesselDao.findByListIdentifiers(barcodeAsList());
        for (LabVessel barcodeItem : byListIdentifiers) {
            addValidationError("barcode", String.format("Barcode \"%s\" is already in use.", barcodeItem.getLabel()));
        }
    }

    @HandlesEvent(BARCODE_REAGENT_ACTION)
    public Resolution barcodeReagent() {
        List<TwoDBarcodedTube> twoDBarcodedTubeList = new ArrayList<>();
        String savedChanges = "";

        savedChanges += editReagentDesign.getBusinessKey() + " " + barcode + ")\n";
        for (String barcodeItem : barcodeAsList()) {
            DesignedReagent reagent = new DesignedReagent(editReagentDesign);
            TwoDBarcodedTube twoDee = new TwoDBarcodedTube(barcodeItem);
            twoDee.addReagent(reagent);
            twoDBarcodedTubeList.add(twoDee);
        }


        twoDBarcodedTubeDAO.persistAll(twoDBarcodedTubeList);
        addMessage(
                String.format("%s tubes initialized with reagents: %s.", twoDBarcodedTubeList.size(), savedChanges));

        return new RedirectResolution(ReagentDesignActionBean.class, LIST_ACTION)
                .addParameter("reagentDesign", reagentDesign);
    }

    private List<String> barcodeAsList() {
        return Arrays.asList(barcode.trim().split("\\W"));
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

    public String getReagentDesign() {
        return reagentDesign;
    }

    public void setReagentDesign(String reagentDesign) {
        this.reagentDesign = reagentDesign;
    }

    public ReagentDesign getEditReagentDesign() {
        return editReagentDesign;
    }

    public void setEditReagentDesign(ReagentDesign editReagentDesign) {
        this.editReagentDesign = editReagentDesign;
    }

    public void setBarcodeMap(Map<String, String> barcodeMap) {
        this.barcodeMap = barcodeMap;
    }

    public Map<String, String> getBarcodeMap() {
        if (barcodeMap == null) {
            barcodeMap = new HashMap<>();
        }

        return barcodeMap;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }

    public ReagentDesignTokenInput getReagentDesignTokenInput() {
        return reagentDesignTokenInput;
    }

    public void setReagentDesignTokenInput(ReagentDesignTokenInput reagentDesignTokenInput) {
        this.reagentDesignTokenInput = reagentDesignTokenInput;
    }
}
