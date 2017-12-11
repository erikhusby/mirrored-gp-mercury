package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
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
public class DesignationResource implements DesignationUtils.Caller {

    @Inject
    private FlowcellDesignationEjb designationTubeEjb;

    private DesignationUtils designationUtils = new DesignationUtils(this);

    private DesignationBean designationBean;

    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response createDesignationFromTableau(DesignationBean designationBean) throws ResourceException {
        this.designationBean = designationBean;
        designationUtils.applyMultiEdit(DesignationUtils.TARGETABLE_STATUSES, designationTubeEjb);
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

        List<DesignationDto> designationDtos = new ArrayList<>();
        designationDtos.add(designationDto);
        return designationDtos;
    }
}
