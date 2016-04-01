package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean;

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
    public static final String INFINIUM_FAMILY = GenotypingChipTypeActionBean.ATTRIBUTE_DEF_FAMILY_PREFIX + "Infinium";
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
            infiniumRunBean = buildRunBean(chip, vesselPosition);
        } else {
            throw new ResourceException("Barcode is not of expected format", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return infiniumRunBean;
    }

    /**
     * Build a JAXB DTO from a chip and position (a single sample)
     */
    private InfiniumRunBean buildRunBean(LabVessel chip, VesselPosition vesselPosition) {
        if (DATA_PATH == null) {
            AttributeDefinition def =
                    attributeArchetypeDao.findAttributeDefinitionsByFamily(INFINIUM_FAMILY, "data_path");
            if (def != null) {
                DATA_PATH = def.getAttributeFamilyClassFieldValue();
            }
            if (DATA_PATH == null) {
                throw new ResourceException("No configuration for " + INFINIUM_FAMILY + " data_path attribute",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        InfiniumRunBean infiniumRunBean;
        Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
        if (sampleInstancesAtPositionV2.size() == 1) {
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
            SampleData sampleData = sampleDataFetcher.fetchSampleData(
                    sampleInstanceV2.getRootOrEarliestMercurySampleName());
            // todo jmt determine why Arrays samples have no connection between MercurySample and ProductOrderSample
            List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(
                    Collections.singletonList(sampleInstanceV2.getRootOrEarliestMercurySampleName()));

            Set<String> chipTypes = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                String chipType = ProductOrder.genoChipTypeForPart(
                        productOrderSample.getProductOrder().getProduct().getPartNumber());
                if (chipType != null) {
                    chipTypes.add(chipType);
                }
            }
            if (chipTypes.isEmpty()) {
                chipTypes = evaluateAsControl(chip, sampleData);
            }
            if (chipTypes.isEmpty()) {
                throw new ResourceException("Found no chip types", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (chipTypes.size() != 1) {
                throw new ResourceException("Found mix of chip types " + chipTypes,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            String idatPrefix = DATA_PATH + "/" + chip.getLabel() + "_" + vesselPosition.name();
            String chipType = chipTypes.iterator().next();
            AttributeArchetype chipTypeArchetype = attributeArchetypeDao.findByName(INFINIUM_FAMILY, chipType);
            Map<String, String> chipTypeAttributes = chipTypeArchetype.getAttributeMap();
            if (chipTypeArchetype == null || chipTypeAttributes.size() == 0) {
                throw new ResourceException("Found no configuration for " + chipType,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
            infiniumRunBean = new InfiniumRunBean(
                    idatPrefix + "_Red.idat",
                    idatPrefix + "_Grn.idat",
                    chipTypeAttributes.get("norm_manifest_unix"),
                    chipTypeAttributes.get("manifest_location_unix"),
                    chipTypeAttributes.get("cluster_location_unix"),
                    chipTypeAttributes.get("zcall_threshold_unix"),
                    sampleData.getCollaboratorsSampleName(),
                    sampleData.getSampleLsid(),
                    sampleData.getGender());
        } else {
            throw new RuntimeException("Expected 1 sample, found " + sampleInstancesAtPositionV2.size());
        }
        return infiniumRunBean;
    }

    /**
     * No connection to a product was found for a specific sample, so determine if it's a control, then try to
     * get chip type from all samples.
     */
    private Set<String> evaluateAsControl(LabVessel chip, SampleData sampleData) {
        Set<String> chipTypes = new HashSet<>();
        List<Control> controls = controlDao.findAllActive();
        for (Control control : controls) {
            if (control.getCollaboratorParticipantId().equals(sampleData.getCollaboratorParticipantId())) {
                List<String> sampleNames = new ArrayList<>();
                for (SampleInstanceV2 sampleInstanceV2 : chip.getSampleInstancesV2()) {
                     sampleNames.add(sampleInstanceV2.getRootOrEarliestMercurySampleName());
                }
                List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(sampleNames);
                for (ProductOrderSample productOrderSample : productOrderSamples) {
                    String chipType = ProductOrder.genoChipTypeForPart(
                            productOrderSample.getProductOrder().getProduct().getPartNumber());
                    if (chipType != null) {
                        chipTypes.add(chipType);
                    }
                }
                break;
            }
        }
        return chipTypes;
    }
}
