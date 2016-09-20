package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JAX-RS resource for Infinium genotyping runs.
 */
@Path("/infiniumrun")
@Stateful
@RequestScoped
public class InfiniumRunResource {

    /** Extract barcode, row and column from e.g. 3999595020_R12C02 */
    private static final Pattern BARCODE_PATTERN = Pattern.compile("(\\d*)_(R\\d*)(C\\d*)");
    /** This matches with attribute_definition.attribute_family in the database. */
    public static final String INFINIUM_GROUP = "Infinium";
    private static String DATA_PATH = null;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ControlDao controlDao;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private LabEventDao labEventDao;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public InfiniumRunBean getRun(@QueryParam("chipWellBarcode") String chipWellBarcode) {
        InfiniumRunBean infiniumRunBean;
        Matcher matcher = BARCODE_PATTERN.matcher(chipWellBarcode);
        if (matcher.matches()) {
            String chipBarcode = matcher.group(1);
            String row = matcher.group(2);
            String column = matcher.group(3);
            VesselPosition vesselPosition = VesselPosition.valueOf(row + column);
            LabVessel chip = labVesselDao.findByIdentifier(chipBarcode);
            infiniumRunBean = buildRunBean(chip, vesselPosition, runDate(chip));
        } else {
            throw new ResourceException("Barcode is not of expected format", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return infiniumRunBean;
    }

    private Date runDate(LabVessel chip) {
        for (LabEvent event : chip.getEvents()) {
            if (event.getLabEventType() == LabEventType.INFINIUM_XSTAIN) {
                return event.getEventDate();
            }
        }
        return chip.getLatestEvent().getEventDate();
    }

    /**
     * Build a JAXB DTO from a chip and position (a single sample)
     */
    private InfiniumRunBean buildRunBean(LabVessel chip, VesselPosition vesselPosition, Date effectiveDate) {
        if (DATA_PATH == null) {
            DATA_PATH = attributeArchetypeDao.findChipFamilyAttribute(INFINIUM_GROUP, "data_path");
        }
        if (DATA_PATH == null) {
            throw new ResourceException("No configuration for " + INFINIUM_GROUP +
                                        " data_path attribute", Response.Status.INTERNAL_SERVER_ERROR);
        }
        InfiniumRunBean infiniumRunBean;
        Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
        if (sampleInstancesAtPositionV2.size() == 1) {
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
            SampleData sampleData = sampleDataFetcher.fetchSampleData(
                    sampleInstanceV2.getNearestMercurySampleName());

            List<ProductOrderSample> productOrderSamples;
            Set<String> researchProjectIds;
            Set<ProductOrder> productOrders;
            ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            ProductOrder productOrder = null;
            if (productOrderSample == null) {
                productOrderSamples = productOrderSampleDao.findBySamples(
                        Collections.singletonList(sampleInstanceV2.getRootOrEarliestMercurySampleName()));
                researchProjectIds = findResearchProjectIds(productOrderSamples);
                productOrders = findProductOrders(productOrderSamples);
            } else {
                productOrder = productOrderSample.getProductOrder();
                productOrders = Collections.singleton(productOrder);
                productOrderSamples = Collections.singletonList(productOrderSample);
                researchProjectIds = Collections.singleton(
                        productOrderSample.getProductOrder().getResearchProject().getBusinessKey());
            }
            Set <GenotypingChip> chipTypes = findChipTypes(productOrderSamples, effectiveDate);

            boolean positiveControl = false;
            boolean negativeControl = false;
            Control processControl = null;
            if (chipTypes.isEmpty()) {
                Pair<Control, Set<GenotypingChip>> pair = evaluateAsControl(chip, sampleData, effectiveDate);
                chipTypes = pair.getRight();
                processControl = pair.getLeft();
                if (processControl != null) {
                    if (processControl.getType() == Control.ControlType.POSITIVE) {
                        positiveControl = true;
                    } else if (processControl.getType() == Control.ControlType.NEGATIVE) {
                        negativeControl = true;
                    }
                }
            }
            if (chipTypes.isEmpty()) {
                throw new ResourceException("Found no chip types for " + chip.getLabel() + " on " + effectiveDate,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (chipTypes.size() != 1) {
                throw new ResourceException("Found mix of chip types for " + chip.getLabel() + " on " + effectiveDate,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            // Controls have a null research project id and product order.
            String researchProjectId = null;
            if (processControl == null) {
                if (researchProjectIds.isEmpty()) {
                    throw new ResourceException("Found no research projects", Response.Status.INTERNAL_SERVER_ERROR);
                }
                if (researchProjectIds.size() != 1) {
                    throw new ResourceException("Found mix of research projects " + researchProjectIds, Response.Status.INTERNAL_SERVER_ERROR);
                }
                researchProjectId = researchProjectIds.iterator().next();

                if (productOrders.isEmpty()) {
                    throw new ResourceException("Found no product orders", Response.Status.INTERNAL_SERVER_ERROR);
                }
                productOrder = productOrders.iterator().next();
            }

            String idatPrefix = DATA_PATH + "/" + chip.getLabel() + "/" + chip.getLabel() + "_" + vesselPosition.name();
            GenotypingChip chipType = chipTypes.iterator().next();
            Map<String, String> chipAttributes = chipType.getAttributeMap();
            if (chipType == null || chipAttributes.size() == 0) {
                throw new ResourceException("Found no configuration for " + chipType.getChipName(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            Date startDate = null;
            String scannerName = null;
            for (LabEvent labEvent: chip.getInPlaceLabEvents()) {
                if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_SOME_STARTED) {
                    startDate = labEvent.getEventDate();
                    scannerName = labEvent.getEventLocation();
                }
            }

            String batchName = null;
            if (sampleInstanceV2.getSingleBatch() != null) {
                batchName = sampleInstanceV2.getSingleBatch().getBatchName();
            }
            String productOrderId = null;
            String productName = null;
            String productFamily = null;
            String partNumber = null;
            if (productOrder != null) {
                productOrderId = productOrder.getProductOrderId().toString();
                productName = productOrder.getProduct().getProductName();
                productFamily = productOrder.getProduct().getProductFamily().getName();
                partNumber = productOrder.getProduct().getPartNumber();
            }
            infiniumRunBean = new InfiniumRunBean(
                    idatPrefix + "_Red.idat",
                    idatPrefix + "_Grn.idat",
                    chipAttributes.get("norm_manifest_unix"),
                    chipAttributes.get("manifest_location_unix"),
                    chipAttributes.get("cluster_location_unix"),
                    chipAttributes.get("zcall_threshold_unix"),
                    sampleData.getCollaboratorsSampleName(),
                    sampleData.getSampleLsid(),
                    sampleData.getGender(),
                    sampleData.getPatientId(),
                    researchProjectId,
                    positiveControl,
                    negativeControl,
                    chipAttributes.get("call_rate_threshold"),
                    chipAttributes.get("gender_cluster_file"),
                    sampleData.getCollaboratorParticipantId(),
                    productOrderId,
                    productName,
                    productFamily,
                    partNumber,
                    batchName,
                    startDate,
                    scannerName);
        } else {
            throw new RuntimeException("Expected 1 sample, found " + sampleInstancesAtPositionV2.size());
        }
        return infiniumRunBean;
    }

    /**
     * No connection to a product was found for a specific sample, so determine if it's a control, then try to
     * get chip type from all samples.
     */
    private Pair<Control, Set<GenotypingChip>> evaluateAsControl(LabVessel chip, SampleData sampleData, Date effectiveDate) {
        Set<GenotypingChip> chipTypes = Collections.emptySet();
        List<Control> controls = controlDao.findAllActive();
        Control processControl = null;
        for (Control control : controls) {
            if (control.getCollaboratorParticipantId().equals(sampleData.getCollaboratorParticipantId())) {
                List<String> sampleNames = new ArrayList<>();
                for (SampleInstanceV2 sampleInstanceV2 : chip.getSampleInstancesV2()) {
                     sampleNames.add(sampleInstanceV2.getRootOrEarliestMercurySampleName());
                }
                chipTypes = findChipTypes(productOrderSampleDao.findBySamples(sampleNames), effectiveDate);
                processControl = control;
                break;
            }
        }
        return Pair.of(processControl, chipTypes);
    }

    private Set<GenotypingChip> findChipTypes(List<ProductOrderSample> productOrderSamples, Date effectiveDate) {
        Set<GenotypingChip> chips = new HashSet<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(productOrderSample.getProductOrder(),
                    effectiveDate);
            if (chipFamilyAndName.getLeft() != null && chipFamilyAndName.getRight() != null) {
                GenotypingChip chip = attributeArchetypeDao.findGenotypingChip(chipFamilyAndName.getLeft(),
                        chipFamilyAndName.getRight());
                if (chip == null) {
                    throw new ResourceException("Chip " + chipFamilyAndName.getRight() + " is not configured",
                            Response.Status.INTERNAL_SERVER_ERROR);
                } else {
                    chips.add(chip);
                }
            }
        }
        return chips;
    }

    private Set<String> findResearchProjectIds(List<ProductOrderSample> productOrderSamples) {
        Set<String> researchProjectIds = new HashSet<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            ResearchProject researchProject = productOrderSample.getProductOrder().getResearchProject();
            if (researchProject != null) {
                researchProjectIds.add(researchProject.getJiraTicketKey());
            }
        }
        return researchProjectIds;
    }


    private Set<ProductOrder> findProductOrders(List<ProductOrderSample> productOrderSamples) {
        Set<ProductOrder> productOrders = new HashSet<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            ProductOrder productOrder = productOrderSample.getProductOrder();
            productOrders.add(productOrder);
        }
        return productOrders;
    }
}
