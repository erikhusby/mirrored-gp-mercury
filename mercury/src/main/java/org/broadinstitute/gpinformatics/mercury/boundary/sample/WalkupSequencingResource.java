package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import clover.org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@Path("/Walkup")
@Stateless
public class WalkupSequencingResource {

    public static final String STATUS = "{\"status\": \"success\"}";

    MessageCollection messageCollection = new MessageCollection();

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleKitRequestDao sampleKitRequestDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public String getRun(@QueryParam("runName") String runName) {

        return runName;

    }

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/testJson")
    public String  testJson(WalkUpSequencing walkUpSequencing ) {

        String status = "{\"status\": \"success\"}";
        return status;

    }


    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/postSequenceData")
    public String  getJson(WalkUpSequencing walkUpSequencing ) {

        String errorConditions = "";

        persistData(walkUpSequencing);

        if(!messageCollection.hasErrors()){
            return  STATUS;
        }
        else {
            for(String errors : messageCollection.getErrors()) {
                errorConditions += errors + ",";
            }
            messageCollection.clearAll();
            return  "{\"status\":" + "\"" +  errorConditions.substring(0, errorConditions.length() - 1)  + "\"}";
        }


    }


    private void persistData(WalkUpSequencing walkUpSequencing)
    {

        SampleKitRequest sampleKitRequest = getSampleKit(walkUpSequencing.getEmailAddress());
        SampleInstanceEntity sampleInstanceEntity = getSampleInstanceEntity(walkUpSequencing.getLibraryName(), sampleKitRequest);

        LabVessel labVessel = getLabVessel(walkUpSequencing.getTubeBarcode());
        labVessel.setVolume(new BigDecimal(walkUpSequencing.getVolume()));
        labVessel.setConcentration(new BigDecimal(walkUpSequencing.getConcentration()));
        MercurySample mercurySample = getMercurySample(walkUpSequencing.getLibraryName()+ "_" + walkUpSequencing.getTubeBarcode());

        mercurySample.addLabVessel(labVessel);
        mercurySampleDao.persist(mercurySample);

        sampleInstanceEntity.setSampleLibraryName(walkUpSequencing.getLibraryName());
        sampleInstanceEntity.setReadType(walkUpSequencing.getReadType());
        sampleInstanceEntity.setSubmitDate(walkUpSequencing.getSubmitDate());
        sampleInstanceEntity.setLabName(walkUpSequencing.getLabName());
        sampleInstanceEntity.setIllumina454KitUsed(walkUpSequencing.getIlluminaTech());
        sampleInstanceEntity.setReference(walkUpSequencing.getReference());
        sampleInstanceEntity.setReferenceVersion(new Integer(isNumber(walkUpSequencing.getReferenceVersion())));
        sampleInstanceEntity.setFragmentSize(walkUpSequencing.getFragSizeRange());
        sampleInstanceEntity.setConcentrationUnit(walkUpSequencing.getConcentrationUnit());
        sampleInstanceEntity.setIsPhix(walkUpSequencing.getIsPhix());
        sampleInstanceEntity.setPhixPercentage(new BigDecimal(isNumber(walkUpSequencing.getPhixPercentage())));
        sampleInstanceEntity.setReadLength(new Integer(isNumber(walkUpSequencing.getReadLength())));
        sampleInstanceEntity.setReadLength2(NumberUtils.createInteger(walkUpSequencing.getReadLength2()));
        sampleInstanceEntity.setIndexLength(NumberUtils.createInteger(walkUpSequencing.getIndexLength()));
        sampleInstanceEntity.setIndexLength2(NumberUtils.createInteger(walkUpSequencing.getIndexLength2()));
        sampleInstanceEntity.setLaneQuantity(new Integer(isNumber(walkUpSequencing.getLaneQuantity())));
        sampleInstanceEntity.setComments(walkUpSequencing.getComments());
        sampleInstanceEntity.setEnzyme(walkUpSequencing.getEnzyme());
        sampleInstanceEntity.setFragSizeRange(walkUpSequencing.getFragSizeRange());
        sampleInstanceEntity.setStatus(walkUpSequencing.getStatus());
        sampleInstanceEntity.setReagentDesign(getReagentDesign(walkUpSequencing.getBaitSetName()));
        sampleInstanceEntity.setFlowcellLaneDesignated(walkUpSequencing.getFlowcellLaneDesignated());
        sampleInstanceEntity.setFlowcellDesignation(walkUpSequencing.getFlowcellDesignation());
        sampleInstanceEntity.setLibraryConstructionMethod(walkUpSequencing.getLibraryConstructionMethod());
        sampleInstanceEntity.setQuantificationMethod(walkUpSequencing.getQuantificationMethod());
        sampleInstanceEntity.setUploadDate();

        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setMercurySampleId(mercurySample);
        sampleInstanceEntityDao.persist(sampleInstanceEntity);
        sampleInstanceEntityDao.flush();
        //sampleInstanceEntityDao.clear();
    }


    /**
     * Fix any non numeric inputs from the web service input.
     */
    private String isNumber(String input) {

       if(StringUtils.isNumeric(input)) {
           return input;
       }

       return "0";
    }

    /**
     * Find the current sample kit request or create a new one.
     */
    private SampleKitRequest getSampleKit(String email) {

        if (email == null) {
            messageCollection.addError("email is missing");
            return null;
        }

        //TODO: Is Email a unique enough identifier???
        SampleKitRequest sampleKitRequest;
        sampleKitRequest = sampleKitRequestDao.findByEmail(email);
        if (sampleKitRequest == null) {
            sampleKitRequest = new SampleKitRequest();
            sampleKitRequest.setEmail(email);
        }
        return sampleKitRequest;
    }


    /**
     *  Metadata to be added to the sample.
     */
    private Set<Metadata> getSampleMetaData() {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
        }};
    }

    /**
     * Find the current mercury sample.
     */
    private MercurySample getMercurySample(String sampleId) {

        if (StringUtils.isEmpty(sampleId)) {
            messageCollection.addError("Sample ID is missing");
            return null;
        }

        MercurySample mercurySample = null;
            mercurySample = mercurySampleDao.findBySampleKey(sampleId);
            if (mercurySample == null) {
                mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            }

            //Confirm that material type is has been set for this sample.
            for(Metadata metadata : mercurySample.getMetadata()) {
                if (metadata.getKey() == Metadata.Key.MATERIAL_TYPE) {
                    return mercurySample;
                }
            }

          mercurySample.addMetadata(getSampleMetaData());

        return mercurySample;

    }


    /**
     * Get the Reagent Design
     */
    private ReagentDesign getReagentDesign(String bait) {

        if(StringUtils.isEmpty(bait)) {
            //This is not a required field in walkup sequencing and may be a null input.
            return null;
        }

        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(bait);
        if (reagentDesign == null) {
            messageCollection.addError("No reagent design found for: " + bait);
            return null;
        }

        return reagentDesign;
    }

    /**
     * Find the current lab vessel or create a new one.
     */
    public LabVessel getLabVessel(String barcode) {
        if (StringUtils.isEmpty(barcode)) {
            messageCollection.addError("Tube barcode missing ");
            return null;
        }
        LabVessel labVessel = null;
        labVessel = labVesselDao.findByIdentifier(barcode);

        if (labVessel == null) {
            labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
        }

        return labVessel;

    }


    /**
     * Find the current instance of the sample V2 entity.
     */
    private SampleInstanceEntity getSampleInstanceEntity(String sampleName, SampleKitRequest sampleKitRequest) {
        SampleInstanceEntity sampleInstanceEntity = null;
        if (StringUtils.isEmpty(sampleName)) {
            sampleInstanceEntity = sampleInstanceEntityDao.findByName(sampleName);
        }
        if (sampleInstanceEntity == null) {
            sampleInstanceEntity = new SampleInstanceEntity();
            sampleKitRequestDao.persist(sampleKitRequest);
            sampleInstanceEntity.setSampleKitRequest(sampleKitRequest);
        }
        return sampleInstanceEntity;
    }


    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public void setSampleInstanceEntityDao(SampleInstanceEntityDao sampleInstanceEntityDao) {
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
    }

    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public void  setReagentDesignDao(ReagentDesignDao reagentDesignDao) {
        this.reagentDesignDao = reagentDesignDao;
    }

    public void setSampleKitRequestDao(SampleKitRequestDao sampleKitRequestDao) {
        this.sampleKitRequestDao = sampleKitRequestDao;
    }

}
