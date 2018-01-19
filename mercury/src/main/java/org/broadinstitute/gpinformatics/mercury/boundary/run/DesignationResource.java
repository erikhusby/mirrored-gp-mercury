package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Web service for designations.  Initially called from Tableau Pooling Calculator.
 */
@Path("/designation")
@Stateful
@RequestScoped
public class DesignationResource {

    @Inject
    private FlowcellDesignationEjb designationTubeEjb;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    private class UtilsClient implements DesignationUtils.Caller {
        private DesignationUtils designationUtils = new DesignationUtils(this);
        private DesignationBean designationBean;

        UtilsClient(DesignationBean designationBean) {
            this.designationBean = designationBean;
        }

        public List<Pair<DesignationDto, FlowcellDesignation>> run() {
            return designationUtils.applyMultiEdit(DesignationUtils.TARGETABLE_STATUSES, designationTubeEjb);
        }

        @Override
        public DesignationDto getMultiEdit() {
            return new DesignationDto();
        }

        @Override
        public void setMultiEdit(DesignationDto dto) {
        }

        @Override
        public List<DesignationDto> getDtos() {
            DesignationDto designationDto = new DesignationDto();
            designationDto.setBarcode(designationBean.getTubeBarcode());
            designationDto.setNumberLanes(designationBean.getNumLanes());
            designationDto.setSelected(true);
            designationDto.setStatus(FlowcellDesignation.Status.QUEUED);
            designationDto.setPoolTest(false);
            designationDto.setPairedEndRead(true);

            List<DesignationDto> designationDtos = new ArrayList<>();
            designationDtos.add(designationDto);
            return designationDtos;
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public String createDesignationFromTableau(DesignationBean designationBean) {
        BspUser bspUser = bspUserList.getByUsername(designationBean.getUserId());
        if (bspUser == null) {
            throw new ResourceException("Failed to find user " + designationBean.getUserId(),
                    Response.Status.BAD_REQUEST);
        }
        userBean.login(designationBean.getUserId());
        // todo jmt check user group?
        UtilsClient utilsClient = new UtilsClient(designationBean);
        List<Pair<DesignationDto, FlowcellDesignation>> pairs = utilsClient.run();
        pairs.get(0).getRight().setPoolingCalculatorParams(String.format(
                "PoolTestFlowcell=%s;Lane=%s;TargetSize=%d;TargetCoverage=%d;LaneYield=%d;SeqPenalty=%s",
                designationBean.getPoolTestFlowcell(), designationBean.getPoolTestFlowcellLane(),
                designationBean.getTargetSize(), designationBean.getTargetCoverage(),
                designationBean.getLaneYield(), designationBean.getSeqPenalty()));
        illuminaFlowcellDao.flush();
        return "success";
    }

}
