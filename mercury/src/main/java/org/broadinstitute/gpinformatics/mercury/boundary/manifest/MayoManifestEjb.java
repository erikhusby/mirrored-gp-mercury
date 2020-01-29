package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderData;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure.QuarantinedDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.AouPdoConfigActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
@Stateful
public class MayoManifestEjb {
    private static Log logger = LogFactory.getLog(MayoManifestEjb.class);
    public static final String ACCESSIONED = "Accessioned rack %s with %s samples %s.";
    public static final String ALREADY_ACCESSIONED = "Rack and samples are already accessioned %s.";
    public static final String CANNOT_ADD_ACCESSIONED = "Package receipt cannot add existing rack %s.";
    public static final String CANNOT_ADD_RECEIVED = "Package receipt cannot add rack %s received with %s.";
    public static final String CANNOT_UNRECEIVE = "Package re-receipt cannot un-receive accessioned rack %s.";
    public static final String EXTRA_IN_MANIFEST = "Manifest contains rack %s that was not in the package receipt.";
    public static final String INVALID_MANIFEST = "File %s does not contain a manifest.";
    public static final String IN_QUARANTINED_PKG = "Rack is part of a quarantined package %s.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String MULTIPLE_FILES = "Found multiple files for %s : %s.";
    public static final String NEEDS_ALLOW_UPDATE =
            "%s has already been received. Allow Update must be checked to update it.";
    public static final String NEEDS_MANUAL_LINKING = "You'll need to link a manifest file to this package.";
    public static final String NO_SUCH_FILE = "Cannot find a file for %s.";
    public static final String NO_UPDATE_CHANGES = "No changes were found.";
    public static final String NOT_IN_MANIFEST = "Manifest does not contain %s %s that was in the package receipt.";
    public static final String NOT_LINKED = "Package %s is not linked to a valid manifest.";
    public static final String NOT_RECEIVED = "Package %s has not been received.";
    public static final String ONLY_METADATA_CHANGES =
            "Manifest update cannot remove or change tubes or positions in accessioned rack %s.";
    public static final String NO_VOL_CONC_MASS_CHANGES =
            "Manifest update cannot change tube volume, concentration, or mass: %s.";
    public static final String PACKAGE_UPDATED = "Manifest updated from %s with these changes: %s.";
    public static final String QUARANTINED = "%s %s is quarantined because %s.";
    public static final String RACK_NOT_RECEIVED = "Package containing %s has not been received.";
    public static final String RACK_UPDATED = "rack %s %s";
    public static final String RCT = "Ticket <a id='rctTicketUrl' href=\"%s\">%s</a> was created or updated.";
    public static final String RECEIVED_LINKED = "Package %s is %sreceived and linked to manifest %s.";
    public static final String RECEIVED_QUARANTINED = "Package %s is %sreceived and quarantined because %s.";
    public static final String SAMPLES_UPDATED = "metadata updated for %d samples: %s";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String UNQUARANTINED = "%s was unquarantined.";
    public static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest shows %s.";
    private static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MMM-dd-HHmm");
    // AoU PDO parameter names.
    private final static String[] MAPPING_NAMES = {KeyValueMapping.AOU_PDO_WGS, KeyValueMapping.AOU_PDO_ARRAY};
    private final static int WGS_INDEX = 0;
    private final static int ARRAY_INDEX = 1;
    private static final String OWNER_PARAM = "Owner";
    private static final String WATCHERS_PARAM = "JIRA Watchers";
    private static final String RESEARCH_PROJECT_PARAM = "Research Project Id";
    private static final String PRODUCT_PARAM = "Product Part Number";
    private static final String QUOTE_PARAM = "Quote Id";
    private static final String TEST_NAME = "manifest test_name";
    private static final List<String> EXPECTED_PDO_PARAMS = ImmutableList.of(OWNER_PARAM, WATCHERS_PARAM,
            RESEARCH_PROJECT_PARAM, PRODUCT_PARAM, QUOTE_PARAM, TEST_NAME);

    public static final Map<String, CustomFieldDefinition> JIRA_DEFINITION_MAP =
            new HashMap<String, CustomFieldDefinition>() {{
                put("MaterialTypeCounts", new CustomFieldDefinition("customfield_15660", "Material Type Counts", true));
                put("Racks", new CustomFieldDefinition("customfield_18060", "Rack IDs", false));
                put("ShipmentCondition", new CustomFieldDefinition("customfield_15661", "Shipment Condition", false));
                put("TrackingNumber", new CustomFieldDefinition("customfield_15663", "Tracking Number", false));
                put("KitDeliveryMethod", new CustomFieldDefinition("customfield_13767", "Kit Delivery Method", false));
                put("RequestingPhysician",
                        new CustomFieldDefinition("customfield_16163", "Requesting Physician", false));
                put("ReceiptType", new CustomFieldDefinition("customfield_17360", "Receipt Type", false));
            }};

    private static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    // Assumes every rack is a 96 tube Matrix rack.
    public static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;
    private final static String JIRA_NONE = "None";

    private ManifestSessionDao manifestSessionDao;
    private GoogleBucketDao googleBucketDao;
    private UserBean userBean;
    private LabVesselDao labVesselDao;
    private MercurySampleDao mercurySampleDao;
    private TubeFormationDao tubeFormationDao;
    private JiraService jiraService;
    private QuarantinedDao quarantinedDao;
    private ProductOrderEjb productOrderEjb;
    private AttributeArchetypeDao attributeArchetypeDao;
    private ProductDao productDao;
    private BSPUserList bspUserList;
    private ResearchProjectDao researchProjectDao;

    /**
     * CDI constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    public MayoManifestEjb() {
    }

    @Inject
    public MayoManifestEjb(ManifestSessionDao manifestSessionDao, GoogleBucketDao googleBucketDao, UserBean userBean,
            LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao, TubeFormationDao tubeFormationDao,
            QuarantinedDao quarantinedDao, JiraService jiraService, Deployment deployment,
            ProductOrderEjb productOrderEjb, AttributeArchetypeDao attributeArchetypeDao, ProductDao productDao,
            BSPUserList bspUserList, ResearchProjectDao researchProjectDao) {

        this.manifestSessionDao = manifestSessionDao;
        this.googleBucketDao = googleBucketDao;
        this.userBean = userBean;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.tubeFormationDao = tubeFormationDao;
        this.quarantinedDao = quarantinedDao;
        this.jiraService = jiraService;
        this.productOrderEjb = productOrderEjb;
        this.attributeArchetypeDao = attributeArchetypeDao;
        this.productDao = productDao;
        this.bspUserList = bspUserList;
        this.researchProjectDao = researchProjectDao;
        // This config is from the yaml file.
        MayoManifestConfig mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    /**
     * Package receipt validation should be comprehensive and must not persist entities since the
     * lab user may choose to cancel the receipt and fix problems first.
     * <p>
     * UI errors should only be given for things that prevent the lab user from doing a receipt-only
     * operation. Manifest parsing, validation, and rack mismatch problems are warnings.
     */
    public void packageReceiptValidation(MayoPackageReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession != null) {
            if (!bean.isAllowUpdate()) {
                messages.addError(NEEDS_ALLOW_UPDATE, packageId);
                return;
            }
        }
        // Package receipt does not support changing existing manifest records from the manifest file.
        // Manifest Update should be used for that.
        List<ManifestRecord> records;
        if (manifestSession == null || StringUtils.isBlank(manifestSession.getManifestFilename())) {
            String filename = searchForFile(bean);
            bean.setFilename(filename);
            // Parses the spreadsheet.
            records = extractManifestRecords(filename, packageId, messages);
            if (!messages.hasErrors() && !messages.hasWarnings() && records.isEmpty() &&
                    StringUtils.isNotBlank(filename)) {
                messages.addWarning(INVALID_MANIFEST, filename);
            }
        } else {
            records = manifestSession.getRecords();
            bean.setFilename(manifestSession.getManifestFilename());
        }
        // Package receipt cannot un-receive an accessioned rack.
        List<String> removedRacks = records.stream().
                map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.RACK_LABEL).getValue()).
                distinct().
                collect(Collectors.toList());
        removedRacks.removeAll(bean.getRackBarcodes());
        String accessionedRemovedRacks = labVesselDao.findByListIdentifiers(removedRacks).stream().
                map(LabVessel::getLabel).sorted().distinct().collect(Collectors.joining(" "));
        if (!accessionedRemovedRacks.isEmpty()) {
            messages.addError(CANNOT_UNRECEIVE, accessionedRemovedRacks);
            return;
        }

        // Warns about rack barcode mismatches.
        validateRackBarcodes(bean.getRackBarcodes(), records, messages);
        // Any errors are turned into warnings since a receipt-only operation does not need to have a manifest.
        messages.addWarning(messages.getErrors());
        messages.getErrors().clear();

        List<String> addedRacks = CollectionUtils.subtract(bean.getRackBarcodes(),
                (manifestSession == null ? Collections.emptyList() : manifestSession.getVesselLabels())).
                stream().sorted().collect(Collectors.toList());
        // Cannot receive an accessioned rack, because the rack's tubes are already defined.
        List<String> accessionedAdds = labVesselDao.findByListIdentifiers(addedRacks).stream().
                map(LabVessel::getLabel).sorted().collect(Collectors.toList());
        if (!accessionedAdds.isEmpty()) {
            messages.addError(CANNOT_ADD_ACCESSIONED, StringUtils.join(accessionedAdds, " "));
        }
        // Cannot receive a rack that has been received by another package. It could cause
        // conflicting manifests for the same rack.
        addedRacks.removeAll(accessionedAdds);
        addedRacks.stream().
                map(rack -> Pair.of(rack, manifestSessionDao.getSessionByVesselLabel(rack))).
                filter(pair -> pair.getRight() != null).
                sorted(Comparator.comparing(Pair::getLeft)).
                forEach(pair -> messages.addError(CANNOT_ADD_RECEIVED, pair.getLeft(),
                        pair.getRight().getSessionPrefix()));

        // If there are no valid manifest records, the lab user will need to manually link a
        // corrected manifest file to this package.
        if (CollectionUtils.isEmpty(records)) {
            messages.addInfo(NEEDS_MANUAL_LINKING, packageId);
        }
    }

    /**
     * Finalizes a package receipt by making a manifest session (with or without records) and an RCT ticket.
     * <p>
     * If the package already has a manifest session, this is a re-receipt and the manifest session is updated
     * including the received rack barcodes.
     */
    public void packageReceipt(MayoPackageReceiptActionBean bean) {
        String packageId = bean.getPackageBarcode();
        // It's validated by the action bean, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(packageId));

        MessageCollection messages = bean.getMessageCollection();
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        ManifestSession previousManifestSession = manifestSession;
        Set<String> previousRacks = new HashSet<>();
        if (manifestSession == null) {
            manifestSession = new ManifestSession(null, packageId, userBean.getBspUser(), false,
                    Collections.emptyList());
            manifestSessionDao.persist(manifestSession);
        } else {
            previousRacks.addAll(manifestSession.getVesselLabels());
            // Replaces the existing received rack barcodes.
            manifestSession.getVesselLabels().clear();
        }
        // Updates the received rack barcodes.
        manifestSession.getVesselLabels().addAll(bean.getRackBarcodes());

        // Uses the filename found in the validation phase and parses manifest into manifest records.
        // Any manifest parsing errors will have already been reported in the UI during the validation
        // phase, though not yet put in the RCT comments. New errors shouldn't happen i.e. the manifest
        // file hasn't changed since the time that the validation page's "continue" button was clicked.
        // If there's a parsing error the manifest session will have no records.
        List<ManifestRecord> records = extractManifestRecords(bean.getFilename(), packageId, messages);
        List<String> rctTicketComments = new ArrayList<>();
        rctTicketComments.addAll(messages.getErrors());
        rctTicketComments.addAll(messages.getWarnings());

        // Existing manifest records do not get replaced by package receipt.
        if (previousManifestSession == null || previousManifestSession.getRecords().isEmpty()) {
            manifestSession.addRecords(records);
            manifestSession.setManifestFilename(bean.getFilename());
        }
        // Updates package quarantine based on presence of manifest records and mismatched rack barcodes.
        boolean mismatchedRacks = validateRackBarcodes(bean.getRackBarcodes(), records, messages);
        // Clears UI messages of any that have been already shown in the validation phase.
        messages.getErrors().clear();
        messages.getWarnings().clear();

        String reReceiptPrefix = StringUtils.isBlank(manifestSession.getReceiptTicket()) ? "" : "re-";
        if (CollectionUtils.isEmpty(manifestSession.getRecords()) || mismatchedRacks) {
            String reason = mismatchedRacks ? Quarantined.RACK_BARCODE_MISMATCH : Quarantined.MISSING_MANIFEST;
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId, reason);
            String msg = String.format(RECEIVED_QUARANTINED, packageId, reReceiptPrefix, reason);
            rctTicketComments.add(msg);
            messages.addInfo(msg);
        } else {
            String msg = String.format(RECEIVED_LINKED, packageId, reReceiptPrefix, bean.getFilename());
            rctTicketComments.add(msg);
            messages.addInfo(msg);
            // Successful re-receipt unquarantines the package.
            if (quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId)) {
                msg = String.format(UNQUARANTINED, packageId);
                rctTicketComments.add(msg);
                messages.addInfo(msg);
            }
        }

        // Quarantines racks based on lab user input. Currently these get unquarantined by either
        // a successful accessioning or if the lab user changes their status in a package re-receipt.
        bean.getQuarantineBarcodeAndReason().forEach((barcode, reason) -> {
            previousRacks.remove(barcode);
            if (StringUtils.isNotBlank(reason)) {
                quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, barcode, reason);
                String msg = String.format(QUARANTINED, "Rack", barcode, reason);
                rctTicketComments.add(msg);
                messages.addInfo(msg);
            }
        });
        // Unquarantines any racks from the previous receipt that were not in the re-receipt.
        previousRacks.forEach(label -> {
            if (quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, label)) {
                String msg = String.format(UNQUARANTINED, label);
                rctTicketComments.add(msg);
                messages.addInfo(msg);
            }
        });

        JiraTicket jiraTicket = makeOrUpdatePackageRct(bean, manifestSession, rctTicketComments);
        if (jiraTicket != null) {
            manifestSessionDao.persist(jiraTicket);
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            manifestSession.setReceiptTicket(jiraTicket.getTicketName());

            messages.addInfo(String.format(RCT, jiraTicket.getBrowserUrl(), jiraTicket.getTicketId()));
        }
    }

    /**
     * The ManifestSession of a previously received package is updated from the records in a manifest file.
     * Three cases are supported:
     * - The received package has no previously linked manifest data.
     * - Manifest data is present but none of the package racks have been accessioned.
     * - Package racks have been accessioned, but the manifest only changes the sample metadata.
     * <p>
     * In each case the manifest file must match the rack barcodes entered at the time of package receipt.
     */
    public void updateManifest(MayoPackageReceiptActionBean bean) {
        // These are validated by the action bean or caller, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(bean.getPackageBarcode()));
        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();

        // A received package always has a manifest session. It may or may not have manifest records.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession == null) {
            messages.addError(NOT_RECEIVED, packageId);
            return;
        }
        // The Allow Updates checkbox must be set to allow updates.
        if (!manifestSession.getRecords().isEmpty() && !bean.isAllowUpdate()) {
            messages.addError(NEEDS_ALLOW_UPDATE, packageId);
            return;
        }

        // Finds a suitable file, or validates the bean's filename if it is present.
        String filename = searchForFile(bean);
        bean.setFilename(filename);
        // Makes the manifest records from the updated manifest file.
        List<ManifestRecord> newRecords = extractManifestRecords(filename, packageId, messages);
        // Warns about rack barcode mismatches.
        boolean mismatchedRacks = validateRackBarcodes(manifestSession.getVesselLabels(), newRecords, messages);

        List<String> rctTicketComments = new ArrayList<>();
        rctTicketComments.addAll(messages.getErrors());
        rctTicketComments.addAll(messages.getWarnings());

        // Reports errors and makes no changes if there are manifest parsing or validation errors.
        if (messages.hasErrors() || CollectionUtils.isEmpty(newRecords)) {
            return;
        }

        // Checks if the new records are the same as the existing ones.
        if (manifestSession.getRecords().size() == newRecords.size() &&
                CollectionUtils.isEqualCollection(
                        manifestSession.getRecords().stream().
                                map(record -> record.getMetadata().stream().
                                        map(Metadata::toString).sorted().
                                        collect(Collectors.joining(","))).
                                sorted().collect(Collectors.toList()),
                        newRecords.stream().
                                map(record -> record.getMetadata().stream().
                                        map(Metadata::toString).sorted().
                                        collect(Collectors.joining(","))).
                                sorted().collect(Collectors.toList()))) {
            messages.addInfo(NO_UPDATE_CHANGES);
            return;
        }

        // Finds the accessioned racks and tubes.
        List<String> oldAndNewRacks = Stream.concat(newRecords.stream(), manifestSession.getRecords().stream()).
                map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.RACK_LABEL).getValue()).
                distinct().
                collect(Collectors.toList());
        List<String> accessionedRacks = labVesselDao.findByListIdentifiers(oldAndNewRacks).stream().
                map(LabVessel::getLabel).
                sorted().
                collect(Collectors.toList());
        List<String> accessionedTubes = newRecords.stream().
                filter(record -> accessionedRacks.contains(record.getValueByKey(Metadata.Key.RACK_LABEL))).
                map(record -> record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE)).
                sorted().
                collect(Collectors.toList());

        // Extracts the changes found in the manifest records.
        Map<MercurySample, Set<Metadata>> metadataDiffs = metadataDiffs(newRecords);
        Map<String, String> rackDiffs = rackDiffs(manifestSession, newRecords);

        // Errors if any accessioned racks are removed or have position or tube changes.
        String accessionedAndChanged = CollectionUtils.intersection(accessionedRacks, rackDiffs.keySet()).
                stream().sorted().collect(Collectors.joining(" "));
        if (StringUtils.isNotBlank(accessionedAndChanged)) {
            messages.addError(ONLY_METADATA_CHANGES, accessionedAndChanged);
            return;
        }

        // Errors if any accessioned sample's volume, concentration, or mass is changed.
        String accessionedAndChangedTubes = metadataDiffs.entrySet().stream().
                filter(mapEntry -> mapEntry.getValue().stream().
                        anyMatch(metadata1 -> metadata1.getKey() == Metadata.Key.VOLUME ||
                                metadata1.getKey() == Metadata.Key.CONCENTRATION ||
                                metadata1.getKey() == Metadata.Key.MASS)).
                map(mapEntry -> mapEntry.getKey().getSampleKey()).
                filter(sampleKey -> accessionedTubes.contains(sampleKey)).
                sorted().
                collect(Collectors.joining(" "));
        if (StringUtils.isNotBlank(accessionedAndChangedTubes)) {
            messages.addError(NO_VOL_CONC_MASS_CHANGES, accessionedAndChangedTubes);
            return;
        }

        // Reports the rack changes and the sample metadata changes in the UI and the RCT ticket.
        List<String> summary = new ArrayList<>();
        if (!metadataDiffs.isEmpty()) {
            summary.add(String.format(SAMPLES_UPDATED, metadataDiffs.keySet().size(),
                    metadataDiffs.keySet().stream().
                            map(MercurySample::getSampleKey).
                            sorted().collect(Collectors.joining(" "))));
        }
        rackDiffs.forEach((rack, description) -> {
            summary.add(String.format(RACK_UPDATED, rack, description));
        });

        String msg = String.format(PACKAGE_UPDATED, filename, StringUtils.join(summary, "; "));
        rctTicketComments.add(msg);
        messages.addInfo(msg);

        // Mismatched rack barcodes or other warnings at this point should quarantine the package.
        // Otherwise unquarantines the package.
        Quarantined quarantinedPkg = quarantinedDao.findItem(Quarantined.ItemSource.MAYO,
                Quarantined.ItemType.PACKAGE, packageId);
        if (messages.hasWarnings() || mismatchedRacks) {
            String reason = mismatchedRacks ? Quarantined.RACK_BARCODE_MISMATCH : Quarantined.MISSING_MANIFEST;
            if (quarantinedPkg == null || !quarantinedPkg.getReason().equals(reason)) {
                quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId,
                        reason);
                String msg1 = String.format(QUARANTINED, "Package", packageId, reason);
                rctTicketComments.add(msg1);
                messages.addInfo(msg1);
            }
        } else {
            quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId);
            String msg1 = String.format(UNQUARANTINED, packageId);
            rctTicketComments.add(msg1);
            messages.addInfo(msg1);
        }

        addRctComment(manifestSession.getReceiptTicket(), messages, rctTicketComments);

        // Updates the MercurySample metadata.
        metadataDiffs.forEach((mercurySample, diffs) -> mercurySample.updateMetadata(diffs));
        // Updates the manifest session.
        manifestSession.getRecords().clear();
        manifestSession.addRecords(newRecords);
        manifestSession.setManifestFilename(filename);
    }

    /**
     * Returns the contents of the Google bucket file given by bean.getFilename() or bean.getPackageId().
     */
    public byte[] download(MayoPackageReceiptActionBean bean) {
        bean.setFilename(searchForFile(bean));
        if (StringUtils.isNotBlank(bean.getFilename())) {
            if (googleBucketDao.exists(bean.getFilename(), bean.getMessageCollection())) {
                return googleBucketDao.download(bean.getFilename(), bean.getMessageCollection());
            } else {
                bean.getMessageCollection().addError(NO_SUCH_FILE, bean.getFilename());
            }
        }
        return null;
    }

    /**
     * If the bean provides an exact filename (including any parent folders and '/' path delimiters)
     * and the file exists, uses that file. If it doesn't exist, the listing of filenames found in the
     * storage bucket is checked to see if any names contain the given match token (partial filename or
     * package barcode). If exactly one filename is found then returns that name. Otherwise generates
     * a message about none found or multiple found.
     */
    public String searchForFile(MayoPackageReceiptActionBean bean) {
        String filename = "";
        if (StringUtils.isNotBlank(bean.getFilename()) &&
                googleBucketDao.exists(bean.getFilename(), bean.getMessageCollection())) {
            filename = bean.getFilename();
        } else {
            String token = StringUtils.isBlank(bean.getFilename()) ? bean.getPackageBarcode() : bean.getFilename();
            List<String> matchingFilenames = googleBucketDao.list(bean.getMessageCollection()).stream().
                    filter(name -> name.contains(token)).
                    sorted().collect(Collectors.toList());
            if (matchingFilenames.isEmpty()) {
                bean.getMessageCollection().addError(NO_SUCH_FILE, token);
            } else if (matchingFilenames.size() > 1) {
                bean.getMessageCollection().addError(MULTIPLE_FILES, token, StringUtils.join(matchingFilenames, "  "));
            } else {
                filename = matchingFilenames.get(0);
            }
        }
        return filename;
    }

    /**
     * Reads a manifest file, extracts its records and validates them.
     *
     * @return list of manifest records from the file (empty if parsing or validation errors were found)
     */
    private List<ManifestRecord> extractManifestRecords(String filename, String packageId, MessageCollection messages) {
        if (StringUtils.isBlank(filename)) {
            return Collections.emptyList();
        }
        // Reads and parses the spreadsheet.
        // Bucket access or csv parsing errors are put in the MessageCollection.
        byte[] spreadsheet = googleBucketDao.download(filename, messages);
        List<List<String>> cellGrid = (spreadsheet == null || spreadsheet.length == 0) ?
                Collections.emptyList() : MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, filename, messages);
        // Makes manifest records from the spreadsheet cell grid.
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, filename, messages);
        if (records.isEmpty()) {
            // The manifest is unacceptable if cannot be parsed into sample records.
            messages.addError(INVALID_MANIFEST, filename);
        } else if (!packageId.equals(records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
            // The manifest is unacceptable if the package doesn't match.
            messages.addError(NOT_IN_MANIFEST, "package", packageId);
            records.clear();
        }
        return records;
    }

    /**
     * Validates the manifest rack barcodes with the received rack barcodes. Generates warnings and returns false
     * if there is a mismatch.
     */
    private boolean validateRackBarcodes(Collection<String> rackBarcodes, List<ManifestRecord> records,
            MessageCollection messages) {

        boolean barcodesMatch = true;
        if (!records.isEmpty()) {
            Set<String> spreadsheetBarcodes = records.stream().
                    map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.RACK_LABEL).getValue()).
                    collect(Collectors.toSet());
            String notInSpreadsheet = CollectionUtils.subtract(rackBarcodes, spreadsheetBarcodes).
                    stream().sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(notInSpreadsheet)) {
                messages.addWarning(NOT_IN_MANIFEST, "rack", notInSpreadsheet);
            }
            String notEntered = CollectionUtils.subtract(spreadsheetBarcodes, rackBarcodes).
                    stream().sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(notEntered)) {
                messages.addWarning(EXTRA_IN_MANIFEST, notEntered);
            }
            barcodesMatch = !StringUtils.isNotBlank(notInSpreadsheet + notEntered);
        }
        return !barcodesMatch;
    }

    /**
     * Extracts the sample metadata differences found in the new manifest records.
     */
    private Map<MercurySample, Set<Metadata>> metadataDiffs(List<ManifestRecord> updateRecords) {
        // Maps the update record's sample name to update sample metadata.
        Multimap<String, Metadata> newMetadataRecords = HashMultimap.create();
        for (ManifestRecord manifestRecord : updateRecords) {
            // Sample name is the tube barcode.
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            newMetadataRecords.putAll(sampleName, manifestRecord.getMetadata());
        }
        // Collects the metadata differences for any MercurySamples (i.e. accessioned samples).
        Map<MercurySample, Set<Metadata>> metadataDiffs = new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(newMetadataRecords.keySet())) {
            Map<Metadata.Key, Metadata> existingData = mercurySample.getMetadata().stream().
                    collect(Collectors.toMap(Metadata::getKey, Function.identity()));
            // Checks if the manifest update wants to add a new metadata type or change an existing value.
            Set<Metadata> diffs = newMetadataRecords.get(mercurySample.getSampleKey()).stream().
                    filter(newData -> !existingData.containsKey(newData.getKey()) ||
                            !newData.getValue().equals(existingData.get(newData.getKey()).getValue())).
                    collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(diffs)) {
                metadataDiffs.put(mercurySample, diffs);
            }
        }
        return metadataDiffs;
    }

    /**
     * Returns a list of racks that have tube or position changes, and an accompanying description.
     */
    private Map<String, String> rackDiffs(ManifestSession manifestSession, List<ManifestRecord> updateRecords) {
        // List of the update record's rack barcode, position, and tube barcode.
        List<Triple<String, String, String>> updateVessels = updateRecords.stream().
                map(record -> Triple.of(
                        record.getValueByKey(Metadata.Key.RACK_LABEL),
                        record.getValueByKey(Metadata.Key.WELL_POSITION),
                        record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE))).
                sorted(Comparator.comparing(triple -> triple.toString("%s %s %s"))).
                collect(Collectors.toList());

        // List of the existing manifest record's rack barcode, position, and tube barcode.
        List<Triple<String, String, String>> existingVessels = manifestSession.getRecords().stream().
                map(record -> Triple.of(
                        record.getValueByKey(Metadata.Key.RACK_LABEL),
                        record.getValueByKey(Metadata.Key.WELL_POSITION),
                        record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE))).
                sorted(Comparator.comparing(triple -> triple.toString("%s %s %s"))).
                collect(Collectors.toList());

        Map<String, String> rackDiffs = new HashMap<>();
        List<String> updateRacks = updateVessels.stream().
                map(Triple::getLeft).sorted().distinct().collect(Collectors.toList());
        List<String> existingRacks = existingVessels.stream().
                map(Triple::getLeft).sorted().distinct().collect(Collectors.toList());
        // Looks for added and removed racks.
        CollectionUtils.subtract(updateRacks, existingRacks).forEach(rack ->
                rackDiffs.put(rack, "was added"));
        CollectionUtils.subtract(existingRacks, updateRacks).forEach(rack ->
                rackDiffs.put(rack, "was removed"));
        // Counts the tube and position changes per rack.
        CollectionUtils.intersection(updateRacks, existingRacks).forEach(rack -> {
            Map<String, String> updateTubeAndPosition = updateVessels.stream().
                    filter(triple -> rack.equals(triple.getLeft())).
                    collect(Collectors.toMap(Triple::getRight, Triple::getMiddle));
            Map<String, String> existingTubeAndPosition = existingVessels.stream().
                    filter(triple -> rack.equals(triple.getLeft())).
                    collect(Collectors.toMap(Triple::getRight, Triple::getMiddle));
            // Counts tube adds and removals and position changes.
            int tubeAdds = CollectionUtils.subtract(
                    updateTubeAndPosition.keySet(), existingTubeAndPosition.keySet()).size();
            int tubeRemovals = CollectionUtils.subtract(
                    existingTubeAndPosition.keySet(), updateTubeAndPosition.keySet()).size();
            long positionChanges = existingTubeAndPosition.entrySet().stream().
                    filter(entry -> !Objects.equal(entry.getValue(), updateTubeAndPosition.get(entry.getKey()))).
                    count();
            List<String> description = new ArrayList<String>() {{
                if (tubeAdds > 0) {
                    add(tubeAdds + " tubes added");
                }
                if (tubeRemovals > 0) {
                    add(tubeRemovals + " tubes removed");
                }
                if (positionChanges > 0) {
                    add(positionChanges + " positions changed");
                }
            }};
            if (!description.isEmpty()) {
                rackDiffs.put(rack, StringUtils.join(description, ", "));
            }
        });
        return rackDiffs;
    }

    /**
     * Validates the rack scan. Neither the rack vessel nor the tubes are
     * expected to exist yet, so it's an error if they do.
     */
    public void validateForAccessioning(MayoSampleReceiptActionBean bean) {
        // The action bean checks that the rack and scan are not empty. These asserts are for unit tests.
        assert (bean.getRackScan() != null && bean.getRackScan().size() > 0);
        assert (StringUtils.isNotBlank(bean.getRackBarcode()));

        MessageCollection messages = bean.getMessageCollection();
        List<String> barcodes = new ArrayList<>();
        // Turns the rack scan into a list of position & sample to pass to and from jsp.
        bean.getRackScanEntries().clear();
        bean.getRackScan().forEach((key, value) -> {
            if (StringUtils.isNotBlank(value)) {
                barcodes.add(value);
                bean.getRackScanEntries().add(StringUtils.join(key, " ", value));
            }
        });
        assert (!barcodes.isEmpty());

        // Rack barcode must be linked to a ManifestSession of a received Mayo package.
        ManifestSession manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
        if (manifestSession == null) {
            messages.addError(RACK_NOT_RECEIVED, bean.getRackBarcode());
        } else if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            messages.addError(NOT_LINKED, manifestSession.getSessionPrefix());
        }

        // Disallows vessels that were already accessioned.
        barcodes.add(0, bean.getRackBarcode());
        String existingVessels = labVesselDao.findByBarcodes(barcodes).entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                map(Map.Entry::getKey).
                sorted().
                collect(Collectors.joining(" "));
        if (!existingVessels.isEmpty()) {
            messages.addError(ALREADY_ACCESSIONED, existingVessels);
        }

        // Rack scan positions must all be valid for the rack type.
        String invalidPositions = CollectionUtils.subtract(
                bean.getRackScan().keySet(),
                Stream.of(DEFAULT_RACK_TYPE.getVesselGeometry().getVesselPositions()).
                        map(VesselPosition::name).
                        collect(Collectors.toList())).
                stream().sorted().collect(Collectors.joining(" "));
        if (!invalidPositions.isEmpty()) {
            messages.addError(UNKNOWN_WELL_SCAN, invalidPositions);
        }
    }

    /**
     * Accessions rack, tubes, samples provided the manifest is found and data matches up.
     */
    public void accession(MayoSampleReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        // Finds the existing manifest session.
        ManifestSession manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
        // This check was already done in validateForAccessioning(). This assert is for unit tests.
        assert (manifestSession != null);
        // If manifest records don't exist it means the package was received without having a manifest file.
        // This is unusable for accessioning.
        if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            messages.addError(NOT_LINKED, manifestSession.getSessionPrefix());
            return;
        }
        // Finds the records for the rack.
        List<ManifestRecord> manifestRecords =
                manifestSession.findRecordsByKey(bean.getRackBarcode(), Metadata.Key.RACK_LABEL);
        if (CollectionUtils.isEmpty(manifestRecords)) {
            messages.addError(NOT_IN_MANIFEST, "rack", bean.getRackBarcode());
            return;
        }
        // Warns if the package is quarantined. This doesn't prevent accessioning if all other data is ok
        // because the quarantine would then be about another rack in the package.
        if (quarantinedDao.findItem(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE,
                manifestSession.getSessionPrefix()) != null) {
            messages.addWarning(IN_QUARANTINED_PKG, manifestSession.getSessionPrefix());
        }

        // Matches the rack scan info against manifest info.
        Map<String, Pair<String, String>> mismatches = new HashMap<>();
        Map<String, String> manifestPositionToTube = new HashMap<>();
        for (ManifestRecord manifestRecord : manifestRecords) {
            String manifestTube = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            String manifestPosition = manifestRecord.getValueByKey(Metadata.Key.WELL_POSITION);
            manifestPositionToTube.put(manifestPosition, manifestTube);
            String rackTube = bean.getRackScan().get(manifestPosition);
            if (!Objects.equal(rackTube, manifestTube)) {
                mismatches.put(manifestPosition, Pair.of(rackTube, manifestTube));
            }
        }
        for (String position : CollectionUtils.subtract(bean.getRackScan().keySet(), manifestPositionToTube.keySet())) {
            String rackTube = bean.getRackScan().get(position);
            mismatches.put(position, Pair.of(rackTube, null));
        }
        List<String> rctMessages = new ArrayList<>();

        // Quarantines the rack if there is a mismatch, and shows the problems.
        if (!mismatches.isEmpty()) {
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, bean.getRackBarcode(),
                    Quarantined.MISMATCH);
            String msg = String.format(QUARANTINED, "Rack", bean.getRackBarcode(), Quarantined.MISMATCH);
            rctMessages.add(msg);
            messages.addInfo(msg);

            mismatches.keySet().stream().sorted().forEach(position -> {
                String rackTube = mismatches.get(position).getLeft();
                String manifestTube = mismatches.get(position).getRight();
                String msg1 = String.format(WRONG_TUBE_IN_POSITION, position,
                        StringUtils.isBlank(rackTube) ? "no tube" : rackTube,
                        StringUtils.isBlank(manifestTube) ? "no tube" : manifestTube);
                rctMessages.add(msg1);
                messages.addError(msg1);
            });

        } else if (StringUtils.isNotBlank(bean.getRackScan().get(VesselPosition.H12.name())) ||
                StringUtils.isNotBlank(bean.getRackScan().get(VesselPosition.G12.name()))) {
            // Quarantines the rack if H12 and G12 are not both empty.
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, bean.getRackBarcode(),
                    Quarantined.H12_G12);
            String msg = String.format(QUARANTINED, "Rack", bean.getRackBarcode(), Quarantined.H12_G12);
            rctMessages.add(msg);
            messages.addError(msg);
        }

        if (!messages.hasErrors()) {
            // Creates tubes.
            Map<VesselPosition, BarcodedTube> vesselPositionToTube = manifestPositionToTube.entrySet().stream().
                    collect(Collectors.toMap(
                            mapEntry -> VesselPosition.getByName(mapEntry.getKey()),
                            mapEntry -> new BarcodedTube(mapEntry.getValue(), DEFAULT_TUBE_TYPE))
                    );
            List<Object> newEntities = new ArrayList<>(vesselPositionToTube.values());

            // Creates the rack and tube formation.
            String digest = TubeFormation.makeDigest(bean.getRackScan().entrySet().stream().
                    map(mapEntry -> Pair.of(VesselPosition.getByName(mapEntry.getKey()), mapEntry.getValue()))
                    .collect(Collectors.toList()));
            TubeFormation tubeFormation = tubeFormationDao.findByDigest(digest);
            if (tubeFormation == null) {
                tubeFormation = new TubeFormation(vesselPositionToTube, DEFAULT_RACK_TYPE);
                newEntities.add(tubeFormation);
            }
            RackOfTubes rack = new RackOfTubes(bean.getRackBarcode(), DEFAULT_RACK_TYPE);
            newEntities.add(rack);
            tubeFormation.addRackOfTubes(rack);
            rack.getTubeFormations().add(tubeFormation);

            // Collects sample info from the manifest.
            Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
            for (ManifestRecord manifestRecord : manifestRecords) {
                String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
                sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());
            }

            // Sets the tube's volume, concentration, and mass from values in the manifest.
            for (BarcodedTube tube : vesselPositionToTube.values()) {
                String sampleName = tube.getLabel();
                for (Metadata metadata : sampleMetadata.get(sampleName)) {
                    if (metadata.getKey() == Metadata.Key.VOLUME) {
                        tube.setVolume(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(metadata.getValue())));
                    } else if (metadata.getKey() == Metadata.Key.CONCENTRATION) {
                        tube.setConcentration(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(metadata.getValue())));
                    } else if (metadata.getKey() == Metadata.Key.MASS) {
                        tube.setMass(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(metadata.getValue())));
                    }
                }
            }

            // Creates mercury samples, all with MetadataSource.MERCURY and metadata from the manifest.
            Set<Metadata> receiptDate = Collections.singleton(Metadata.createMetadata(Metadata.Key.RECEIPT_DATE,
                    manifestSession.getUpdateData().getCreatedDate()));
            for (BarcodedTube tube : vesselPositionToTube.values()) {
                String sampleName = tube.getLabel();
                MercurySample mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.MERCURY);
                mercurySample.updateMetadata(new HashSet<>(sampleMetadata.get(sampleName)));
                mercurySample.addMetadata(receiptDate);
                tube.addSample(mercurySample);
                newEntities.add(mercurySample);
            }

            String msg = String.format(ACCESSIONED, bean.getRackBarcode(),
                    sampleMetadata.keySet().size(),
                    sampleMetadata.keySet().stream().sorted().collect(Collectors.joining(" ")));
            rctMessages.add(msg);
            messages.addInfo(msg);

            boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                    Quarantined.ItemType.RACK, bean.getRackBarcode());
            if (wasQuarantined) {
                String msg1 = String.format(UNQUARANTINED, bean.getRackBarcode());
                rctMessages.add(msg1);
                messages.addInfo(msg1);
            }

            labVesselDao.persistAll(newEntities);
            // The accessioning should persist when the PDO create fails.
            labVesselDao.flush();

            // Makes a PDO if this is the last rack to be accessioned in the package, i.e. all other
            // racks have been accessioned or quarantined.
            Multimap<String, ManifestRecord> rackToRecords =
                    manifestSession.buildMultimapByKey(manifestSession.getRecords(), Metadata.Key.RACK_LABEL);
            Set<String> rackBarcodes = rackToRecords.keySet();
            int rackProcessedCount = 0;
            Set<LabVessel> accessionedTubes = new HashSet<>();
            for (String rackBarcode : rackBarcodes) {
                if (quarantinedDao.findItem(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, rackBarcode)
                        != null) {
                    ++rackProcessedCount;
                } else {
                    Set<String> tubeBarcodes = manifestSession.buildMultimapByKey(rackToRecords.get(rackBarcode),
                            Metadata.Key.BROAD_2D_BARCODE).keySet();
                    List<LabVessel> tubes = labVesselDao.findByListIdentifiers(new ArrayList<>(tubeBarcodes));
                    if (CollectionUtils.isNotEmpty(tubes)) {
                        ++rackProcessedCount;
                        accessionedTubes.addAll(tubes);
                    }
                }
            }
            if (rackProcessedCount == rackBarcodes.size()) {
                // The manifest has already been tested for consistent test_name, so the first found will do.
                String manifestTestName = manifestSession.buildMultimapByKey(rackToRecords.values(),
                        Metadata.Key.PRODUCT_TYPE).keySet().iterator().next();
                makeAouPdo(manifestSession.getSessionPrefix(), manifestTestName, accessionedTubes, messages);
            }
        }
        // Adds comment to the existing RCT.
        addRctComment(manifestSession.getReceiptTicket(), messages, rctMessages);
    }

    /**
     * Runs a step by step credentialing and access of the configured Google bucket.
     * Status is put into info messages for the UI.
     * Puts a listing of all bucket filenames in the bean.
     */
    public void testAccess(MayoAdminActionBean bean) {
        bean.setBucketList(googleBucketDao.test(bean.getMessageCollection()));
    }

    /**
     * Rewrites the credential file.
     */
    public void uploadCredential(MayoAdminActionBean bean) {
        try {
            String jsonContent = StringUtils.join(IOUtils.readLines(bean.getCredentialFileReader()),
                    System.lineSeparator());
            googleBucketDao.uploadCredential(false, jsonContent, bean.getMessageCollection());
        } catch (Exception e) {
            logger.error("Exception while uploading credential file: ", e);
            bean.getMessageCollection().addError("Cannot upload credential file: " + e.toString());
        }
    }

    /**
     * Changes the Google bucket access credential for Mercury's service account.
     * The new credential is written to the credential file that is configured in yaml.
     */
    public void rotateServiceAccountKey(MayoAdminActionBean bean) {
        googleBucketDao.rotateServiceAccountKey(bean.getMessageCollection());
    }

    /**
     * Makes a new RCT in Jira for the Mayo package or updates an existing RCT.
     */
    private JiraTicket makeOrUpdatePackageRct(MayoPackageReceiptActionBean bean,
            @NotNull ManifestSession manifestSession, List<String> comments) {
        String title = bean.getPackageBarcode();
        CustomField titleField = new CustomField(new CustomFieldDefinition("summary", "Summary", true), title);

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(JIRA_DEFINITION_MAP.get("ShipmentCondition"),
                    StringUtils.defaultIfBlank(bean.getShipmentCondition(), " ")));
            add(new CustomField(JIRA_DEFINITION_MAP.get("TrackingNumber"),
                    StringUtils.defaultIfBlank(bean.getTrackingNumber(), " ")));
            // Delivery Method is a Jira selection and does not like to be set if value is "None".
            if (StringUtils.isNotBlank(bean.getDeliveryMethod()) && !JIRA_NONE.equals(bean.getDeliveryMethod())) {
                add(new CustomField(bean.getDeliveryMethod(), JIRA_DEFINITION_MAP.get("KitDeliveryMethod")));
            }
            add(new CustomField(JIRA_DEFINITION_MAP.get("Racks"), bean.getRackBarcodeString()));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"), " "));
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), " "));
        }};

        JiraTicket jiraTicket = null;
        JiraIssue jiraIssue;
        try {
            if (StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
                jiraTicket = labVesselDao.findSingle(JiraTicket.class, JiraTicket_.ticketId,
                        manifestSession.getReceiptTicket());
            }
            if (jiraTicket == null) {
                jiraIssue = jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT,
                        userBean.getLoginUserName(), CreateFields.IssueType.RECEIPT, title, customFieldList);
                // Re-writes the RCT title which for some reason gets ignored in createIssue.
                jiraService.updateIssue(jiraIssue.getKey(), Collections.singleton(titleField));
                jiraTicket = new JiraTicket(jiraService, jiraIssue.getKey());
            } else {
                // If a reference to a previous RCT exists, updates the RCT.
                jiraIssue = jiraService.getIssue(manifestSession.getReceiptTicket());
                customFieldList.add(titleField);
                jiraService.updateIssue(jiraIssue.getKey(), customFieldList);
            }

            // Writes any comments.
            jiraIssue.addComment(StringUtils.join(comments, "\n"));
            return jiraTicket;

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
            return null;
        }
    }

    private void addRctComment(String rctId, MessageCollection messageCollection, List<String> comments) {
        try {
            JiraIssue jiraIssue = jiraService.getIssue(rctId);
            if (jiraIssue == null) {
                messageCollection.addError("Cannot find Jira ticket " + rctId + " and some comments will be lost.");
            } else {
                jiraIssue.addComment(StringUtils.join(comments, "\n"));
            }
        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            messageCollection.addError(JIRA_PROBLEM, e.toString() + " and some RCT comments will be lost.");
        }
    }

    public UserBean getUserBean() {
        return userBean;
    }

    /**
     * Makes a PDO from all accessioned samples in the given package.
     */
    public void makeAouPdo(String packageId, String manifestTestName, Set<LabVessel> accessionedTubes,
            MessageCollection messageCollection) {
        // Excludes tubes that are already in a PDO, to support quarantined racks that get accessioned.
        List<ProductOrderSample> pdoSamples = productDao.findListByList(ProductOrderSample.class,
                ProductOrderSample_.sampleName,
                accessionedTubes.stream().map(LabVessel::getLabel).collect(Collectors.toList()));
        List<String> pdoSampleNames = pdoSamples.stream().
                map(ProductOrderSample::getSampleKey).
                sorted().
                collect(Collectors.toList());
        for (Iterator<LabVessel> iterator = accessionedTubes.iterator(); iterator.hasNext(); ) {
            if (pdoSampleNames.contains(iterator.next().getLabel())) {
                iterator.remove();
            }
        }
        if (!accessionedTubes.isEmpty()) {
            // Determines the product type and the lookup key for attributeArchetype params.
            String wgsKey = attributeArchetypeDao.findKeyValueByKeyAndMappingName(TEST_NAME,
                    KeyValueMapping.AOU_PDO_WGS).getValue();
            String arrayKey = attributeArchetypeDao.findKeyValueByKeyAndMappingName(TEST_NAME,
                    KeyValueMapping.AOU_PDO_ARRAY).getValue();
            String mappingName = manifestTestName.equalsIgnoreCase(wgsKey) ? MAPPING_NAMES[WGS_INDEX] :
                    manifestTestName.equalsIgnoreCase(arrayKey) ? MAPPING_NAMES[ARRAY_INDEX] : null;
            if (mappingName != null) {
                // Fetches the attribute archetype values for the test_name.
                Map<String, String> params = attributeArchetypeDao.findKeyValueMap(mappingName);
                String owner = params.get(OWNER_PARAM).toLowerCase();
                List<String> watchers = Arrays.asList(StringUtils.normalizeSpace(params.get(WATCHERS_PARAM).
                        replaceAll(",", " ")).toLowerCase().split(" "));
                String rpId = params.get(RESEARCH_PROJECT_PARAM);
                String productPartNumber = params.get(PRODUCT_PARAM);
                String quoteId = params.get(QUOTE_PARAM);

                Product product = productDao.findByPartNumber(productPartNumber);
                Date now = new Date();
                String titleType = manifestTestName.equalsIgnoreCase(wgsKey) ? "WGS" : "Array";
                String title = String.format("%s_%s_%s", packageId, titleType, DATE_TIME_FORMAT.format(now));
                // For Mayo the tube barcode is also used as the Broad sample name.
                List<String> accessionedTubeBarcodes = accessionedTubes.stream().
                        map(LabVessel::getLabel).sorted().collect(Collectors.toList());

                ProductOrderData productOrderData = new ProductOrderData();
                productOrderData.setCreatedDate(now);
                productOrderData.setModifiedDate(now);
                productOrderData.setPlacedDate(now);
                productOrderData.setNumberOfSamples(accessionedTubes.size());
                productOrderData.setProduct(productPartNumber);
                productOrderData.setProductName(product.getProductName());
                productOrderData.setQuoteId(quoteId);
                productOrderData.setResearchProjectId(rpId);
                productOrderData.setSamples(accessionedTubeBarcodes);
                productOrderData.setTitle(title);
                productOrderData.setUsername(owner);
                try {
                    ProductOrder productOrder = productOrderEjb.createProductOrder(productOrderData, watchers);
                    productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
                    messageCollection.addInfo("Created Product Order " + productOrder.getBusinessKey() +
                            " for " + accessionedTubeBarcodes.size() + " samples.");
                } catch (Exception e) {
                    logger.error("Failed to make an AoU PDO", e);
                    messageCollection.addError("Failed to make a PDO: " + e.toString());
                }
            }
        }
    }

    /**
     * Validates the AoU PDO parameters.
     */
    public void validateAouPdoParams(List<AouPdoConfigActionBean.Dto> dtos, MessageCollection messageCollection) {
        if (dtos.size() == EXPECTED_PDO_PARAMS.size() &&
                dtos.stream().allMatch(dto -> EXPECTED_PDO_PARAMS.contains(dto.getParamName()))) {

            for (AouPdoConfigActionBean.Dto dto : dtos) {
                switch (dto.getParamName()) {
                case OWNER_PARAM:
                    if (bspUserList.getByUsername(dto.getWgsValue()) == null) {
                        messageCollection.addError("WGS owner '" + dto.getWgsValue() + "' is an unknown user.");
                    }
                    if (bspUserList.getByUsername(dto.getArrayValue()) == null) {
                        messageCollection.addError("Array owner '" + dto.getArrayValue() + "' is an unknown user.");
                    }
                    break;
                case WATCHERS_PARAM:
                    String concatWatchers = (dto.getWgsValue() + " " + dto.getArrayValue()).replaceAll(",", " ").toLowerCase();
                    Stream.of(StringUtils.normalizeSpace(concatWatchers).split(" ")).forEach(watcher -> {
                        if (bspUserList.getByUsername(watcher) == null) {
                            messageCollection.addError("Watcher '" + watcher + "' is an unknown user.");
                        }
                    });
                    break;
                case RESEARCH_PROJECT_PARAM:
                    if (researchProjectDao.findByBusinessKey(dto.getWgsValue()) == null) {
                        messageCollection.addError("WGS research project id '" + dto.getWgsValue() + "' is unknown.");
                    }
                    if (researchProjectDao.findByBusinessKey(dto.getArrayValue()) == null) {
                        messageCollection.addError("Array research project id '" + dto.getArrayValue() + "' is unknown.");
                    }
                    break;
                case PRODUCT_PARAM:
                    if (productDao.findByPartNumber(dto.getWgsValue()) == null) {
                        messageCollection.addError("WGS product '" + dto.getWgsValue() + "' is unknown.");
                    }
                    if (productDao.findByPartNumber(dto.getArrayValue()) == null) {
                        messageCollection.addError("Array product '" + dto.getArrayValue() + "' is unknown.");
                    }
                    break;
                case QUOTE_PARAM:
                    if (StringUtils.isBlank(dto.getWgsValue())) {
                        messageCollection.addError("WGS quote id is blank.");
                    }
                    if (StringUtils.isBlank(dto.getArrayValue())) {
                        messageCollection.addError("Array quote id is blank.");
                    }
                    break;
                case TEST_NAME:
                    if (StringUtils.isBlank(dto.getWgsValue())) {
                        messageCollection.addError("WGS test_name is blank.");
                    }
                    if (StringUtils.isBlank(dto.getArrayValue())) {
                        messageCollection.addError("Array test_name is blank.");
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown AoU PDO parameter '" + dto.getParamName() + "'.");
                }
            }
        } else {
            messageCollection.addError("Parameter names must contain " + StringUtils.join(EXPECTED_PDO_PARAMS, ", "));
        }
    }

    /**
     * Saves changes to the AoU PDO parameters.
     */
    public void saveAouPdoParams(List<AouPdoConfigActionBean.Dto> dtos) {
        for (int i = 0; i < 2; ++i) {
            String mappingName = MAPPING_NAMES[i];
            Map<String, KeyValueMapping> keyValueMap = attributeArchetypeDao.findKeyValueMappings(mappingName).
                    stream().
                    collect(Collectors.toMap(KeyValueMapping::getKey, Function.identity()));
            for (AouPdoConfigActionBean.Dto dto : dtos) {
                String value = (i == 0) ? dto.getWgsValue() : dto.getArrayValue();
                if (keyValueMap.containsKey(dto.getParamName())) {
                    keyValueMap.get(dto.getParamName()).setValue(value);
                } else {
                    throw new RuntimeException("Missing AoU PDO parameter '" + dto.getParamName() + "'.");
                }
            }
            attributeArchetypeDao.persistAll(keyValueMap.values());
        }
    }
}