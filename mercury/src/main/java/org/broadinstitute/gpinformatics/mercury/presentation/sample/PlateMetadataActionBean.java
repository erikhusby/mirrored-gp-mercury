package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleInfo;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.PlateMetadataImportProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RowMetadata;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@UrlBinding(PlateMetadataActionBean.ACTIONBEAN_URL_BINDING)
public class PlateMetadataActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/sample/platemetadata.action";

    public static final String VIEW_PAGE = "/sample/plate_metadata_upload.jsp";

    public static final String UPLOAD_METADATA_ACTION = "uploadMetadata";
    public static final String ADD_TO_PDO_ACTION = "addToPdo";
    public static final String VIEW_UPLOAD_ACTION = "viewUpload";

    private Map<String, List<RowMetadata>> uploadedPlates = new HashMap<>();

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPRestSender bspRestSender;

    @Inject
    private ProductOrderEjb productOrderEjb;

    private static final Log logger = LogFactory.getLog(PlateMetadataActionBean.class);

    @Validate(required = true, on = UPLOAD_METADATA_ACTION)
    private FileBean metadataFile;

    @Validate(required = true, on = UPLOAD_METADATA_ACTION)
    private String plateBarcode;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(UPLOAD_METADATA_ACTION)
    public Resolution uploadManifest() {
        try {
            PlateMetadataImportProcessor processor = new PlateMetadataImportProcessor();
            PoiSpreadsheetParser.processSingleWorksheet(metadataFile.getInputStream(), processor);
            List<RowMetadata> rowRecords = processor.getRowMetadataRecords();
            String manifestPlateBarcode = rowRecords.get(0).getPlateBarcode();
            if (uploadedPlates.containsKey(manifestPlateBarcode)) {
                addGlobalValidationError("Plate Barcode has already been uploaded: " + manifestPlateBarcode);
            } else if (!manifestPlateBarcode.equalsIgnoreCase(plateBarcode)) {
                addGlobalValidationError(String.format("Plate Barcode in field %s doesn't match file %s", plateBarcode,  manifestPlateBarcode));
            } else if (labVesselDao.findByIdentifier(manifestPlateBarcode) != null) {
                addGlobalValidationError("Plate Barcode already exists: " + manifestPlateBarcode);
            } else {
                plateBarcode = "";
                uploadedPlates.put(manifestPlateBarcode, rowRecords);
            }
        } catch (IOException | InvalidFormatException | ValidationException | InformaticsServiceException e) {
            addGlobalValidationError("Unable to upload the metadata file: {2}", e.getMessage());
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = ADD_TO_PDO_ACTION)
    public void validateAddToPdo(ValidationErrors errors) {
        if(uploadedPlates.isEmpty()) {
            errors.add("metadataFile", new SimpleError("Please upload a metadata file first."));
        }
    }

    @HandlesEvent(ADD_TO_PDO_ACTION)
    public Resolution addToPdo() {
        MessageCollection messageCollection = new MessageCollection();
        try {
            Map<String, Set<ProductOrderSample>> mapBySamples =
                    productOrderSampleDao.findMapBySamples(uploadedPlates.keySet());

            Map<ProductOrder, Set<String>> mapProductOrderToPlate = new HashMap<>();
            Map<ProductOrder, Set<ProductOrderSample>> mapPdoToSamples = new HashMap<>();

            for (Map.Entry<String, Set<ProductOrderSample>> entry : mapBySamples.entrySet()) {
                Set<ProductOrder> foundProductOrders = new HashSet<>();
                String barcode = entry.getKey();
                if (entry.getValue().isEmpty()) {
                    messageCollection.addError("Failed to find PDO for: {2}", entry.getKey());
                } else {
                    for (ProductOrderSample productOrderSample : entry.getValue()) {
                        foundProductOrders.add(productOrderSample.getProductOrder());
                    }
                    if (foundProductOrders.size() != 1) {
                        String errPdos = StringUtils.join(foundProductOrders, ",");
                        messageCollection.addError("Found multiple product orders for plate: " + barcode + " " + errPdos);
                    } else {
                        ProductOrder productOrder = foundProductOrders.iterator().next();
                        if (!mapProductOrderToPlate.containsKey(productOrder)) {
                            mapProductOrderToPlate.put(productOrder, new HashSet<>());
                        }
                        mapProductOrderToPlate.get(productOrder).add(barcode);
                        mapPdoToSamples.put(productOrder, entry.getValue());
                    }
                }
            }

            Map<ProductOrder, Set<ProductOrderSample>> mapProductOrderToSamplesToRemove = new HashMap<>();
            Map<ProductOrder, Set<ProductOrderSample>> mapProductOrderToSamplesToAdd = new HashMap<>();
            for (Map.Entry<ProductOrder, Set<String>>entry: mapProductOrderToPlate.entrySet()) {
                ProductOrder productOrder = entry.getKey();

                Map<String, StaticPlate> mapBarcodeToVessel = new HashMap<>();
                Set<LabVessel> labVesselSet = new HashSet<>();
                ListMultimap<ProductOrder, LabVessel> mapPdoToVessels = ArrayListMultimap.create();
                Set<ProductOrderSample> addSamples = new HashSet<>();
                Set<ProductOrderSample> removeSamples = new HashSet<>();

                for (String plateBarcode: entry.getValue()) {
                    List<RowMetadata> rowMetadata = uploadedPlates.get(plateBarcode);

                    Map<String, RowMetadata> mapWellToMetadata =
                            rowMetadata.stream().collect(Collectors.toMap(RowMetadata::getWell, Function.identity()));

                    StaticPlate staticPlate = new StaticPlate(plateBarcode, StaticPlate.PlateType.Eppendorf96);
                    staticPlate.setCreatedOn(new Date());

                    StaticPlate.PlateType plateType = StaticPlate.PlateType.Plate96Well200PCR;

                    // Call BSP to create a new Plate and grab Sample IDs from newly created plate
                    String containerBarcode = bspRestSender.createDisassociatedPlate(plateType.getAutomationName(), "Well200");
                    if (containerBarcode == null) {
                        messageCollection.addError("Failed to create a new plate of SM-IDs in BSP");
                        break;
                    }
                    GetSampleInfo.SampleInfos sampleInfos = bspRestSender.getSampleInfo(containerBarcode);
                    if (sampleInfos == null || sampleInfos.getSampleInfos().isEmpty()) {
                        messageCollection.addError("Failed to list new Sample IDs for " + containerBarcode);
                        break;
                    }

                    // Map found wells to Sample Info from BSP
                    Map<String, GetSampleInfo.SampleInfo> mapWellToSampleInfo = sampleInfos.getSampleInfos().stream()
                            .collect(Collectors.toMap(GetSampleInfo.SampleInfo::getPosition,
                                    Function.identity()));

                    // For each position if theres a well in metadata upload, create a PlateWell with new Mercury Sample
                    // containing the Metadata from the upload and a Material Type of RNA.
                    for (VesselPosition vesselPosition : plateType.getVesselGeometry().getVesselPositions()) {
                        if (mapWellToMetadata.containsKey(vesselPosition.name())) {
                            RowMetadata wellMetadata = mapWellToMetadata.get(vesselPosition.name());
                            PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
                            staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                            mapPdoToVessels.put(productOrder, plateWell);
                            if (mapWellToSampleInfo.containsKey(vesselPosition.name())) {
                                GetSampleInfo.SampleInfo wellSampleInfo =
                                        mapWellToSampleInfo.get(vesselPosition.name());
                                Metadata materialType = new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.RNA.getDisplayName());
                                wellMetadata.getMetadata().add(materialType);
                                MercurySample mercurySample = new MercurySample(wellSampleInfo.getSampleBarcode(),
                                        MercurySample.MetadataSource.MERCURY, true);
                                mercurySample.addMetadata(new HashSet<>(wellMetadata.getMetadata()));
                                plateWell.setVolume(wellMetadata.getVolume());
                                plateWell.getMercurySamples().add(mercurySample);
                                addSamples.add(new ProductOrderSample(wellSampleInfo.getSampleBarcode()));
                                removeSamples.addAll(mapBySamples.get(plateBarcode));
                            } else {
                                messageCollection
                                        .addError("Failed to find well in Sample ID list: " + vesselPosition.name());
                            }
                        }
                    }

                    labVesselSet.add(staticPlate);
                    mapPdoToVessels.put(productOrder, staticPlate);
                }

                if (removeSamples.isEmpty()) {
                    messageCollection.addError("Found no samples to remove from PDO");
                }
                if (addSamples.isEmpty()) {
                    messageCollection.addError("Found no samples to add to a PDO");
                }
                if (!messageCollection.hasErrors()) {
                    labVesselDao.persistAll(labVesselSet);
                    labVesselDao.flush();

                    mapProductOrderToSamplesToRemove.put(productOrder, removeSamples);
                    mapProductOrderToSamplesToAdd.put(productOrder, addSamples);
                }
            }

            if (!messageCollection.hasErrors()) {
                productOrderEjb
                        .addAndRemoveSamplesAndBucket(mapProductOrderToSamplesToAdd, mapProductOrderToSamplesToRemove);
                messageCollection.addInfo("Successfully added Plate Wells to PDOs");
            }
        } catch (Exception e) {
            logger.error("Error attempting to create Single Cell LCSET", e);
            messageCollection.addError(e);
        }

        addMessages(messageCollection);

        return new ForwardResolution(VIEW_PAGE);
    }

    public Map<String, List<RowMetadata>> getUploadedPlates() {
        return uploadedPlates;
    }

    public void setUploadedPlates(
            Map<String, List<RowMetadata>> uploadedPlates) {
        this.uploadedPlates = uploadedPlates;
    }

    public FileBean getMetadataFile() {
        return metadataFile;
    }

    public void setMetadataFile(FileBean metadataFile) {
        this.metadataFile = metadataFile;
    }

    public String getPlateBarcode() {
        return plateBarcode;
    }

    public void setPlateBarcode(String plateBarcode) {
        this.plateBarcode = plateBarcode;
    }
}
