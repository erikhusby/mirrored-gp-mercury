package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * For importing data from Squid and BSP, creates a batch of tubes
 */
@SuppressWarnings("FeatureEnvy")
@Path("/labbatch")
@Stateful
@RequestScoped
public class LabBatchResource {

    private static final String BSP_BATCH_PREFIX = "BP";

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselFactory labVesselFactory;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private UserBean userBean;

    public LabBatchResource() {
    }

    @POST
    public String createLabBatch(LabBatchBean labBatchBean) {
        userBean.login(labBatchBean.getUsername());
        LabBatch labBatch;
        if (labBatchBean.getParentVesselBean() != null) {
            labBatch = createLabBatchByParentVessel(labBatchBean);
        } else {
            List<String> tubeBarcodes = new ArrayList<>();
            List<MercurySample> mercurySampleKeys = new ArrayList<>();
            for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
                tubeBarcodes.add(tubeBean.getBarcode());
                if (tubeBean.getSampleBarcode() != null) {
                    mercurySampleKeys
                            .add(new MercurySample(tubeBean.getSampleBarcode(), MercurySample.MetadataSource.BSP));
                }
            }

            Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);
            Map<MercurySample, MercurySample> mapSampleToSample =
                    mercurySampleDao.findByMercurySample(mercurySampleKeys);
            labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapSampleToSample);
        }

        if (!labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX)) {
            if (labBatch.getJiraTicket() == null) {
                JiraTicket jiraTicket = new JiraTicket(jiraService, labBatchBean.getBatchId());
                labBatch.setJiraTicket(jiraTicket);
                jiraTicket.setLabBatch(labBatch);
            }
        }
        labBatchDao.persist(labBatch);
        labBatchDao.flush();

        return "Batch persisted";
    }

    /**
     * Build a LabBatch entity from a LabBatchBean with a ParentVesselBean.  If there are plate wells,
     * create bucket entries and associate them with the batch.
     */
    private LabBatch createLabBatchByParentVessel(LabBatchBean labBatchBean) {
        // Process is only interested in the primary vessels
        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(
                Collections.singletonList(labBatchBean.getParentVesselBean()), labBatchBean.getUsername(),
                new Date(), null, MercurySample.MetadataSource.BSP).getLeft();

        // Gather vessels for each PDO (if any)
        Set<LabVessel> labVesselSet = new HashSet<>();
        for (ChildVesselBean childVesselBean : labBatchBean.getParentVesselBean().getChildVesselBeans()) {
            if (labBatchBean.getWorkflowName() != null) {
                if (labVessels.size() == 1 && labVessels.get(0).getType() == LabVessel.ContainerType.STATIC_PLATE) {
                    StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
                    PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(
                            VesselPosition.getByName(childVesselBean.getPosition()));
                    labVesselSet.add(plateWell);
                } else {
                    throw new RuntimeException("Product Orders supported only for plates.");
                }
            }
        }

        if (labVesselSet.isEmpty()) {
            labVesselSet.addAll(labVessels);
        }

        LabBatch labBatch = labBatchDao.findByBusinessKey(labBatchBean.getBatchId());
        if (labBatch == null) {
            labBatch = new LabBatch(labBatchBean.getBatchId(), labVesselSet,
                    labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX) ?
                            LabBatch.LabBatchType.BSP : LabBatch.LabBatchType.WORKFLOW,
                    labBatchBean.getCreatedDate());
        }
        addToBatch(labVesselSet, labBatch, labBatchBean.getWorkflowName(), labBatchBean.getUsername(),
                labBatchBean.getCreatedDate(), bucketEjb);

        return labBatch;
    }

    public static Set<ProductOrder> addToBatch(Set<LabVessel> labVesselSet, LabBatch labBatch, String productFamilyName,
            String username, Date createdDate, BucketEjb bucketEjb) {
        // Create bucket entries (if any) and add to batch
        LabVessel.loadSampleDataForBuckets(labVesselSet);
        ListMultimap<ProductOrder, LabVessel> mapPdoToVessels = ArrayListMultimap.create();
        List<LabVessel> controls = new ArrayList<>();
        for (LabVessel labVessel : labVesselSet) {
            Set<SampleInstanceV2> sampleInstancesV2 = labVessel.getSampleInstancesV2();
            if (sampleInstancesV2.size() == 1) {
                SampleInstanceV2 sampleInstanceV2 = sampleInstancesV2.iterator().next();
                List<ProductOrder> productOrders = new ArrayList<>();
                for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    // For backfill from GAP, the bean specifies the PDO
                    if (productOrderSample.getProductOrder().getBusinessKey().equals(productFamilyName)) {
                        productOrders.add(productOrderSample.getProductOrder());
                    } else {
                        Product product = productOrderSample.getProductOrder().getProduct();
                        if (product.getProductFamily().getName().equals(productFamilyName) ||
                                // Some array / sequencing combo products are in family "Exome"
                                productFamilyName.equals(ProductFamily.WHOLE_GENOME_GENOTYPING) &&
                                        product.getWorkflow().name().contains("INFINIUM")) {
                            if (productOrderSample.getProductOrder().getOrderStatus() == ProductOrder.OrderStatus.Submitted) {
                                productOrders.add(productOrderSample.getProductOrder());
                            }
                        }
                    }
                }
                // Choose most recently submitted PDO
                if (productOrders.size() > 1) {
                    productOrders.sort((o1, o2) -> o2.getPlacedDate().compareTo(o1.getPlacedDate()));
                }
                if (productOrders.isEmpty()) {
                    // assume it's a control
                    controls.add(labVessel);
                } else {
                    mapPdoToVessels.put(productOrders.get(0), labVessel);
                }
            }
        }

        if (!mapPdoToVessels.isEmpty()) {
            // Pick a PDO arbitrarily
            ProductOrder controlPdo = mapPdoToVessels.keySet().iterator().next();
            for (LabVessel control : controls) {
                mapPdoToVessels.put(controlPdo, control);
            }
        }

        int offset = 0;
        for (ProductOrder productOrder : mapPdoToVessels.keySet()) {
            // Remove vessels that have already been bucketed for this PDO
            List<LabVessel> vessels = mapPdoToVessels.get(productOrder);
            List<LabVessel> noBucketEntryVessels = new ArrayList<>();
            for (LabVessel vessel : vessels) {
                boolean found = false;
                for (BucketEntry bucketEntry : vessel.getBucketEntries()) {
                    if (bucketEntry.getProductOrder().equals(productOrder)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    noBucketEntryVessels.add(vessel);
                }
            }

            // Get existing PDOs
            Date localCreatedDate = createdDate;
            if (offset > 0) {
                // Avoid unique constraint on bucket lab events
                localCreatedDate = new Date(createdDate.getTime() + offset);
            }
            Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                    bucketEjb.applyBucketCriteria(noBucketEntryVessels,
                            productOrder, username,
                            ProductWorkflowDefVersion.BucketingSource.LAB_BATCH_WS, localCreatedDate);
            ProductWorkflowDefVersion productWorkflowDefVersion = workflowBucketEntriesPair.getLeft();
            if (productWorkflowDefVersion == null) {
                throw new RuntimeException("No workflow for " + productOrder.getJiraTicketKey());
            }
            labBatch.setWorkflowName(productWorkflowDefVersion.getProductWorkflowDef().getName());
            // todo jmt check that bucket entries count matches lab vessel count?
            bucketEjb.moveFromBucketToBatch(workflowBucketEntriesPair.getRight(), labBatch);
            offset++;
        }
        return mapPdoToVessels.keySet();
    }

    /**
     * DAO-free method to build a LabBatch entity
     *
     * @param labBatchBean       JAXB
     * @param mapBarcodeToTube   from database
     * @param mapBarcodeToSample from database
     *
     * @return entity
     */
    @DaoFree
    public LabBatch buildLabBatch(LabBatchBean labBatchBean, Map<String, BarcodedTube> mapBarcodeToTube,
                                  Map<MercurySample, MercurySample> mapBarcodeToSample) {
        Set<LabVessel> starters = new HashSet<>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(tubeBean.getBarcode());
            if (barcodedTube == null) {
                barcodedTube = new BarcodedTube(tubeBean.getBarcode());
                mapBarcodeToTube.put(tubeBean.getBarcode(), barcodedTube);
            }

            if (tubeBean.getSampleBarcode() != null) {
                MercurySample mercurySampleKey = new MercurySample(tubeBean.getSampleBarcode(),
                        MercurySample.MetadataSource.BSP);
                MercurySample mercurySample = mapBarcodeToSample.get(mercurySampleKey);
                if (mercurySample == null) {
                    mercurySample = mercurySampleKey;
                    mapBarcodeToSample.put(mercurySampleKey, mercurySample);
                }
                barcodedTube.addSample(mercurySample);
            }
            starters.add(barcodedTube);
        }
        return new LabBatch(labBatchBean.getBatchId(), starters,
                labBatchBean.getBatchId().startsWith(BSP_BATCH_PREFIX) ?
                        LabBatch.LabBatchType.BSP : LabBatch.LabBatchType.WORKFLOW);
    }

}
