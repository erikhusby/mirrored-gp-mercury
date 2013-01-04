package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/search/plastic.action")
public class SearchPlasticActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/search/search_plastic.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    @Validate(required = true, on = "search")
    private String barcode;

    private List<LabVessel> foundVessels = null;

    @DefaultHandler
    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() throws Exception {
        List<String> barcodeList = Arrays.asList(barcode.trim().split(","));
        foundVessels = labVesselDao.findByListIdentifiers(barcodeList);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }
}
