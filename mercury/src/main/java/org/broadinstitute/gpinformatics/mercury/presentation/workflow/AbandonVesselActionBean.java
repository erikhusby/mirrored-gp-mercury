package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;

/**
 * Abandon / Un-Abandon vessel / plastic logic.
 *
 */

@UrlBinding(value = "/workflow/AbandonVessel.action")
public class AbandonVesselActionBean  extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/vessel.action";
    public static final String VESSEL_SEARCH = "vesselSearch";
    public static final String UN_ABANDON_VESSEL = "unAbandonVessel";
    public static final String ABANDON_VESSEL = "abandonVessel";

    private static final String SESSION_LIST_PAGE = "/workflow/abandon_vessel.jsp";
    private String abandonComment;
    private String unAbandonComment;
    private String vesselBarcode;
    private String vesselLabel;
    public String barcode;

    MessageCollection messageCollection = new MessageCollection();

    @Inject
    private LabVesselDao labVesselDao;

    @Override
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        getSearchKey();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(VESSEL_SEARCH)
    public Resolution vesselSearch() throws Exception {
        doSearch(SearchActionBean.SearchType.VESSELS_BY_BARCODE);
        orderResults();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(ABANDON_VESSEL)
    public Resolution abandonVessel() throws Exception {
        setSearchKey(vesselBarcode);
        doSearch(SearchActionBean.SearchType.VESSELS_BY_BARCODE);
        for (LabVessel vessel : getFoundVessels()) {
            vessel.setAbandonReason(abandonComment);
            vessel.setAbandonDate(true);
            labVesselDao.persist(vessel);
        }
        messageCollection.addInfo("Vessel: " + vesselBarcode + " Successfully abandoned. " );
        addMessages(messageCollection);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(UN_ABANDON_VESSEL)
    public Resolution unAbandonVessel() throws Exception {

        setSearchKey(vesselLabel);
        doSearch(SearchActionBean.SearchType.VESSELS_BY_BARCODE);
        for (LabVessel vessel : getFoundVessels()) {
            vessel.setAbandonReason(null);
            vessel.setAbandonDate(false);
            labVesselDao.persist(vessel);
        }

        messageCollection.addInfo("Vessel: " + vesselLabel + " Successfully un-abandoned. " );
        addMessages(messageCollection);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    private void orderResults() {
        Map<String, LabVessel> labelToVessel = new HashMap<>();
        for (LabVessel vessel : getFoundVessels()) {
            labelToVessel.put(vessel.getLabel(), vessel);
            setBarcode(vessel.getLabel());
        }
        setFoundVessels(new LinkedHashSet<LabVessel>());
        List<String> searchOrder = cleanInputString(getSearchKey());
        for (String key : searchOrder) {
            LabVessel labVessel = labelToVessel.get(key);
            if (labVessel != null) {
                getFoundVessels().add(labVessel);
            }
        }
    }

    public String getAbandonComment() {
        return abandonComment;
    }

    public void setAbandonComment(String abandonComment) {
        this.abandonComment = abandonComment;
    }

    public String getUnAbandonComment() {
        return unAbandonComment;
    }

    public void setUnAbandonComment(String unAbandonComment) {
        this.unAbandonComment = unAbandonComment;
    }

    public String getVesselBarcode() {
        return vesselBarcode;
    }

    public void setVesselBarcode(String vesselBarcode) {
        this.vesselBarcode = vesselBarcode;
    }

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}