package org.broadinstitute.gpinformatics.athena.entity.fixup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfoFixupTest;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;


/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
@Test(groups = TestGroups.FIXUP, singleThreaded = true)
@RequestScoped
public class ProductOrderFixupTest extends Arquillian {

    public ProductOrderFixupTest() {
    }

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ResearchProjectDao projectDao;

    @Inject
    private UserBean userBean;

    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    @Inject
    private UserTransaction utx;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private SAPProductPriceCache productPriceCache;

    @Inject
    private QuoteService quoteService;

    @Inject
    private SAPAccessControlEjb accessController;

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Fixed up data per JIRA ticketN.
     */
    @Test(enabled = false)
    public void fixupGplim123() {
        ProductOrder productOrder = productOrderDao.findById(1001L);
        productOrder.setComments(productOrder.getComments() + " fixup");
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }

    /**
     * Clear the External Plating Addon from PDO-10
     *
     * @throws Exception
     */
    @Test(enabled = false)
    public void clear_addons_for_pdo() throws Exception {
        String jiraKey = "PDO-10";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);

        productOrder.setComments("");
        productOrder.updateAddOnProducts(new ArrayList<Product>());

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }


    /**
     * Helper method to change the owner of a product order.
     *
     * @param newOwnerUsername new owner's username
     * @param orderKeys        list of PDO keys
     */
    private void changePDOOwner(String newOwnerUsername, String... orderKeys) {
        for (BspUser user : bspUserList.find(newOwnerUsername)) {
            if (user.getUsername().equals(newOwnerUsername)) {
                for (String key : orderKeys) {
                    ProductOrder productOrder = productOrderDao.findByBusinessKey(key);
                    productOrder.prepareToSave(user);
                    productOrderDao.persist(productOrder);
                }
                return;
            }
        }

        throw new RuntimeException("No " + newOwnerUsername + " Found!");
    }

    @Test(enabled = false)
    public void reassignPDOsToElizabethNickerson() {
        changePDOOwner("enickers", "PDO-132", "PDO-131", "PDO-130", "PDO-112", "PDO-108", "PDO-107", "PDO-13", "PDO-12",
                "PDO-9");
    }

    @Test(enabled = false)
    public void removeSamplesFromPDO300() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-3SFP5",
                "SM-3SFPA",
                "SM-3SDFV",
                "SM-3SDIE",
                "SM-3SDI2",
                "SM-3SJCZ",
                "SM-3SJCV",
                "SM-3SDIT",
                "SM-3SDIA",
                "SM-3SJCR",
                "SM-3SFP2",
                "SM-3SDTO",
                "SM-3SDT5",
                "SM-3SJD1",
                "SM-3SDIO",
                "SM-3SFPD",
                "SM-3SJCQ",
                "SM-3SJCO",
                "SM-3SJCT",
                "SM-3SDTB",
                "SM-3SJD3",
                "SM-3SFP1",
                "SM-3SFP3",
                "SM-3SFOX");

        String pdo = "PDO-300";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void removeSamplesFromDevTest() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-3DV29",
                "SM-3DV2A",
                "SM-3DV2B",
                "SM-3DV2C",
                "SM-3DV2D",
                "SM-3DV2E",
                "SM-3DV2F");

        String pdo = "PDO-49";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void removeSamplesForGPLIM877() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-1WJOV",
                "SM-1WJOX",
                "SM-1WJPC",
                "SM-1WJPD");

        String pdo = "PDO-388";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void setupOnRiskTestData() {

        String pdo = "PDO-49";
        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        RiskCriterion riskCriterion =
                new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        productOrder.getProduct().getRiskCriteria().add(riskCriterion);
        productDao.persist(productOrder.getProduct());

        // Populate on risk for every other sample
        int count = 0;
        for (ProductOrderSample sample : productOrder.getSamples()) {
            if ((count++ % 2) == 0) {
                RiskItem riskItem = new RiskItem(riskCriterion, "240.0");
                riskItem.setRemark("Bad Concentration found");
                sample.setRiskItems(Collections.singletonList(riskItem));
                productOrderSampleDao.persist(sample);
            }
        }
    }

    @Test(enabled = false)
    public void setPlacedDate() {

        Query query = productOrderDao.getEntityManager().createNativeQuery(
                "SELECT pdo.jira_ticket_key, rev_info.rev_date " +
                "FROM athena.product_order_aud pdo_aud, athena.product_order pdo, mercury.rev_info rev_info " +
                "WHERE " +
                "  pdo.product_order_id = pdo_aud.product_order_id AND " +
                "  pdo_aud.rev = (" +
                "    SELECT MIN(pdo_aud2.rev) " +
                "    FROM athena.product_order_aud pdo_aud2 " +
                "    WHERE " +
                "      pdo_aud2.product_order_id = pdo.product_order_id AND " +
                "      pdo_aud2.jira_ticket_key IS NOT NULL " +
                "  ) AND " +
                "rev_info.rev_info_id = pdo_aud.rev " +
                "ORDER BY pdo.created_date "
        );

        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();

        Map<String, Date> keyToDateMap = new HashMap<>();

        for (Object[] row : resultList) {
            String key = (String) row[0];
            Date date = (Date) row[1];
            keyToDateMap.put(key, date);
        }

        List<ProductOrder> productOrders = productOrderDao.findAll();
        for (ProductOrder productOrder : productOrders) {
            if (keyToDateMap.containsKey(productOrder.getBusinessKey())) {
                productOrder.setPlacedDate(keyToDateMap.get(productOrder.getBusinessKey()));
                productOrderDao.persist(productOrder);
            }
        }
    }

    @Test(enabled = false)
    public void fixupPDOCompleteStatus() throws ProductOrderEjb.NoSuchPDOException, IOException, SAPInterfaceException {
        // Loop through all PDOs and update their status to complete where necessary.  The API can in theory
        // un-complete PDOs but no PDOs in the database should be completed yet.
        List<ProductOrder> orders = productOrderDao.findAll();
        MessageReporter.LogReporter reporter = new MessageReporter.LogReporter(log);
        for (ProductOrder order : orders) {
            productOrderEjb.updateOrderStatus(order.getBusinessKey(), reporter);
        }
    }

    @Test(enabled = false)
    public void fixupGplim2913() throws ProductOrderEjb.NoSuchPDOException, IOException {
        userBean.loginOSUser();
        // Update PDO-2959 So its status matches that of the Jira PDO (Completed)
        // un-complete PDOs but no PDOs in the database should be completed yet.
        String pdoKey = "PDO-2959";

        ProductOrder order = productOrderDao.findByBusinessKey(pdoKey);
        assertThat(order.getOrderStatus(), is(ProductOrder.OrderStatus.Submitted));
        order.updateOrderStatus();
        assertThat(order.getOrderStatus(), is(ProductOrder.OrderStatus.Completed));

        productOrderDao.persist(new FixupCommentary("See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-2913"));
    }

    @Test(enabled = false)
    public void fixupUnplacedPDO() throws ParseException {
        // Fix a case where a PDO was completed but then got changed to Draft. Using the stored values in the audit
        // table to determine the correct values for the PDO. GPLIM-1617.
        ProductOrder order = productOrderDao.findByBusinessKey("Draft-28112");
        if (order != null) {
            order.setJiraTicketKey("PDO-1486");
            order.setOrderStatus(ProductOrder.OrderStatus.Submitted);
            order.setPlacedDate(DateFormat.getDateTimeInstance().parse("Jun 11, 2013 01:43:44 PM"));
            productOrderDao.persist(order);
        }
    }

    private void changeProjectForPdo(String pdoKey, String newProjectKey) throws IOException {
        ProductOrder order = productOrderDao.findByBusinessKey(pdoKey);
        Assert.assertNotNull(order, "Could not find " + pdoKey);

        ResearchProject oldProject = order.getResearchProject();
        ResearchProject newProject = projectDao.findByBusinessKey(newProjectKey);
        Assert.assertNotNull(newProject, "Could not find new research project " + newProjectKey);

        order.setResearchProject(newProject);

        log.info("Updating " + order.getBusinessKey() + "/" + order.getName() + " from project " +
                 oldProject.getBusinessKey() + " to " + newProject.getBusinessKey() + "/" + newProject.getName());

        JiraIssue issue = jiraService.getIssue(pdoKey);

        // Note that this adds the link...BUT YOU MUST DELETE THE PREVIOUS RP LINK MANUALLY!
        issue.addLink(AddIssueLinkRequest.LinkType.Related, newProject.getBusinessKey());

        productOrderDao.persist(order);
    }

    @Test(enabled = false)
    public void changeProjectForPdo() throws Exception {
        changeProjectForPdo("PDO-1621", "RP-317");
    }

    @Test(enabled = false)
    public void changeProjectForPdo_GPLIM_2178() throws Exception {
        String[] pdos = new String[]{
                "PDO-2187",
                "PDO-2039",
                "PDO-2035"};

        for (String pdo : pdos) {
            changeProjectForPdo(pdo, "RP-490");
        }
    }

    @Test(enabled = false)
    public void changeProjectForPdo_GPLIM_2349() throws IOException {
        changeProjectForPdo("PDO-1489", "RP-24");
    }

    @Test(enabled = false)
    public void changePdoName() throws Exception {
        ProductOrder order = productOrderDao.findByBusinessKey("PDO-1928");
        if (order != null) {
            order.setTitle("TB-ARC_CDRCAlland_SKorea_ProductionBatch1");
            productOrderDao.persist(order);
        }
    }

    private void changeJiraKey(String oldKey, String newKey) {
        ProductOrder order = productOrderDao.findByBusinessKey(newKey);
        Assert.assertNull(order, "Should be no " + newKey + " in the database!");
        order = productOrderDao.findByBusinessKey(oldKey);
        order.setJiraTicketKey(newKey);
        productOrderDao.persist(order);
    }

    @Test(enabled = false)
    public void fixupPDOChangeJIRAIssue() {
        // Fix DB error caused by clicking Place Order too many times. We need to update a PDO to use a different
        // JIRA key.
        changeJiraKey("PDO-1043", "PDO-1042");
    }

    @Test(enabled = false)
    public void fixupSampleNames() throws Exception {

        // Renames samples that have some type of non-printable ASCII at the end (e.g. char(160)).
        long[] ids = new long[]{104900, 104901, 104902, 104903, 104905, 104906, 104907, 104908, 104909, 104910, 104911,
                104913, 104915, 104916, 104917, 104918, 104919, 104923, 104924, 104926, 104927, 104930, 104931, 133750,
                133752, 133753, 133756, 133873, 210099, 210893, 234027, 262655, 48110};
        for (long id : ids) {
            ProductOrderSample sample = productOrderSampleDao.findById(ProductOrderSample.class, (Long) id);
            String s1 = sample.getName();
            while (!StringUtils.isAsciiPrintable(s1)) {
                s1 = StringUtils.chop(s1);
            }
            Assert.assertTrue(s1.length() > 0);
            sample.setName(s1);
            productOrderSampleDao.persist(sample);
        }

        // Renames samples that were mistyped.
        Map<Long, String> map = new HashMap<Long, String>() {{
            put((Long) 9181L, "SM-3O76Q");
            put((Long) 9262L, "SM-3NOLL");
            put((Long) 9294L, "SM-3NOYJ");
            put((Long) 9312L, "SM-3NP1A");
            put((Long) 69363L, "SM-3O57J");
            put((Long) 69362L, "SM-3O57I");
        }};
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            ProductOrderSample sample = productOrderSampleDao.findById(ProductOrderSample.class, entry.getKey());
            String s1 = sample.getName();
            sample.setName(entry.getValue());
            productOrderSampleDao.persist(sample);
        }

        // Un-splits two sample names.
        ProductOrderSample sample = productOrderSampleDao.findById(ProductOrderSample.class, (Long) 268485L);
        sample.setName("SM-4M8YQ");
        productOrderSampleDao.persist(sample);

        sample = productOrderSampleDao.findById(ProductOrderSample.class, (Long) 268484L);
        productOrderSampleDao.remove(sample);

        productOrderSampleDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2627() throws ParseException {
        // Complete PDO that was not auto-completed
        ProductOrder order = productOrderDao.findByBusinessKey("PDO-2611");
        if (order != null) {
            order.setOrderStatus(ProductOrder.OrderStatus.Completed);
            productOrderDao.persist(order);
        }
    }

    @Test(enabled = false)
    public void updateQuotesGPLIM2830() throws Exception {
        List<ProductOrder> ordersToUpdate = productOrderDao.findListByBusinessKeys(Arrays.asList("PDO-2693", "PDO-2771",
                "PDO-2686", "PDO-2635"));

        for (ProductOrder productOrder : ordersToUpdate) {
            productOrder.setQuoteId("GP87U");
        }
        productOrderDao.persistAll(ordersToUpdate);
        productOrderDao.flush();

    }

    @Test(enabled = false)
    public void gplim2893ManuallyCompletePDO()
            throws ProductOrderEjb.NoSuchPDOException, IOException, SAPInterfaceException {
        MessageReporter.LogReporter reporter = new MessageReporter.LogReporter(log);
        productOrderEjb.updateOrderStatus("PDO-2635", reporter);
    }

    @Test(enabled = false)
    public void gplim2969UpdatePriceItemType() {
        userBean.loginOSUser();
        Set<String> samples = new HashSet<>();
        samples.add("SM-2CCZM");
        samples.add("SM-2CD1I");
        ProductOrder order = productOrderDao.findByBusinessKey("PDO-3475");

        for (ProductOrderSample productOrderSample : order.getSamples()) {
            if (samples.contains(productOrderSample.getSampleKey())) {
                for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                    if (ledgerEntry.getBillingSession().getBillingSessionId() == 3728) {
                        if (ledgerEntry.getPriceItemType() == LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM) {
                            ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
                            samples.remove(productOrderSample.getSampleKey());
                            System.out.println(
                                    "Updated leger entry " + ledgerEntry.getLedgerId() + " for " + productOrderSample
                                            .getSampleKey());
                        }
                    }
                }
            }
        }

        productOrderDao.flush();
        Assert.assertTrue(samples.isEmpty());
    }

    @Test(enabled = false)
    public void gplim3221ReportProductOrderCompliance() {

        CriteriaQuery<ProductOrder> criteriaQuery = productOrderDao.getEntityManager().getCriteriaBuilder().createQuery(
                ProductOrder.class);
        Root<ProductOrder> root = criteriaQuery.from(ProductOrder.class);

        Calendar malleableCalendar = new GregorianCalendar();
        malleableCalendar.set(Calendar.DAY_OF_MONTH, 1);
        malleableCalendar.add(Calendar.MONTH, -3);

        Date startDate = malleableCalendar.getTime();

        CriteriaBuilder builder = productOrderDao.getEntityManager().getCriteriaBuilder();

        ParameterExpression<Date> rangeStart = builder.parameter(Date.class);

        criteriaQuery.where(builder.lessThanOrEqualTo(rangeStart, root.get(ProductOrder_.createdDate)),
                builder.notEqual(root.get(ProductOrder_.orderStatus), ProductOrder.OrderStatus.Draft));
        criteriaQuery.orderBy(builder.asc(root.get(ProductOrder_.createdDate)),
                builder.asc(root.get(ProductOrder_.skipRegulatoryReason)));

        TypedQuery<ProductOrder> query = productOrderDao.getEntityManager().createQuery(criteriaQuery);

        query.setParameter(rangeStart, startDate);
        Collection<ProductOrder> productOrders = query.getResultList();

        log.info("Found product order records: " + productOrders.size());
        OutputStream outputStream = null;
        try {
            File output = File.createTempFile("compliance_rpt", ".csv");
            outputStream = new FileOutputStream(output);
            log.info("The filestream path is: " + output.getAbsolutePath() + output.getName());
        } catch (IOException e) {
            Assert.fail("Problem opening the output file.");
        }

        PrintStream fileWriter = new PrintStream(outputStream);

        fileWriter.print("PDO key\tProduct order name\t Create Date\tOwner\tOwner Email\tQuote ID\t" +
                         "Reason Regulatory info is not required\tIRB/OSRP Number\tRegulatory Type\n");

        DateFormat creationDate = new SimpleDateFormat("yyyy-MM-dd");
        for (ProductOrder productOrder : productOrders) {
            if (productOrder.getRegulatoryInfos().isEmpty()) {
                fileWriter.print(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        productOrder.getBusinessKey(),
                        productOrder.getName(),
                        creationDate.format(productOrder.getCreatedDate()),
                        bspUserList.getById(productOrder.getCreatedBy()).getFullName(),
                        bspUserList.getById(productOrder.getCreatedBy()).getEmail(),
                        productOrder.getQuoteId(),
                        (StringUtils.isBlank(productOrder.getSkipRegulatoryReason())) ? " " :
                                productOrder.getSkipRegulatoryReason(),
                        " ",
                        " "));

            } else {
                for (RegulatoryInfo regulatoryInfo : productOrder.getRegulatoryInfos()) {

                    fileWriter.print(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                            productOrder.getBusinessKey(),
                            productOrder.getName(),
                            creationDate.format(productOrder.getCreatedDate()),
                            bspUserList.getById(productOrder.getCreatedBy()).getFullName(),
                            bspUserList.getById(productOrder.getCreatedBy()).getEmail(),
                            productOrder.getQuoteId(),
                            (StringUtils.isBlank(productOrder.getSkipRegulatoryReason())) ? " " :
                                    productOrder.getSkipRegulatoryReason(),
                            StringUtils.replaceChars(regulatoryInfo.getIdentifier(), "\t", " "),
                            StringUtils.replaceChars(regulatoryInfo.getType().getName(), "\t", " ")));
                }
            }
        }
        fileWriter.close();
    }

    @Test(enabled = false)
    public void ipi61545ChangeResearchProjectForPDO6074() {
        userBean.loginOSUser();
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-6074");
        ResearchProject researchProject = projectDao.findByBusinessKey("RP-627");
        productOrder.setResearchProject(researchProject);
        productOrderDao.persist(new FixupCommentary("IPI-61545 Change PDO-6074 from RP-623 to RP-627"));
        productOrderDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport859SwitchRegulatoryInfo() {
        userBean.loginOSUser();

        findAndUpdateRegulatoryInfo("Support-859", "pdo_reginfo.support859.update");
    }

    private void findAndUpdateRegulatoryInfo(final String fixupTicket, String pdosToChangeVMOption) {

        // The value of the property reflected in pdosToChangeVMOption must be a VM defined property
        String property = System.getProperty(pdosToChangeVMOption);
        if (property == null) {
            Assert.fail("The filename for the pdo Regulatory info updates is not found");
        }
        File pdostoupdate = new File(property);
        List<String> errors = null;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(pdostoupdate));
            errors = new ArrayList<>();
            String line = null;

            while ((line = reader.readLine()) != null) {
                String[] lineInfo = line.split(",");

                RegulatoryInfoSelection orderToRegInfo = null;
                orderToRegInfo = new RegulatoryInfoSelection(lineInfo[0], lineInfo[1],
                        RegulatoryInfo.Type.valueOf(lineInfo[2]));
                ProductOrder pdoToChange = productOrderDao.findByBusinessKey(orderToRegInfo.getProductOrderKey());

                if(pdoToChange == null) {
                    errors.add(orderToRegInfo.getProductOrderKey() + " was not found");
                    continue;
                }

                RegulatoryInfo selectedRegulatoryInfo = null;
                for (RegulatoryInfo candidate : pdoToChange.getResearchProject().getRegulatoryInfos()) {
                    if (candidate.getIdentifier().equals(orderToRegInfo.getRegulatoryInfoIdentifier()) &&
                        candidate.getType() == orderToRegInfo.getRegulatoryInfoType()) {
                        selectedRegulatoryInfo = candidate;
                        break;
                    }
                }

                if(selectedRegulatoryInfo == null) {
                    errors.add("ORSP candidate " + orderToRegInfo.getRegulatoryInfoIdentifier() +
                               " was not found in any research project for " + orderToRegInfo.getProductOrderKey());
                }

                pdoToChange.setSkipRegulatoryReason(null);
                pdoToChange.addRegulatoryInfo(selectedRegulatoryInfo);
            }

        } catch (IOException e) {
            Assert.fail("Unable to read form the provided file " + property);
        }

        if(CollectionUtils.isNotEmpty(errors)) {
            Assert.fail(StringUtils.join(errors, "\n"));
        }

        productOrderDao.persist(new FixupCommentary(
                fixupTicket + ":  Updated PDOs which did not have the correct Regulatory Info associated with them."));
    }

    @Test(enabled = false)
    public void fixupSupport809SwitchRegulatoryInfo() {

        List<RegulatoryInfoSelection> regulatoryInfoAdditions = Arrays.asList(
                new RegulatoryInfoSelection("PDO-5563", "ORSP-1175", RegulatoryInfo.Type.ORSP_NOT_ENGAGED),
                new RegulatoryInfoSelection("PDO-5569", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5572", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5577", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5655", "ORSP-1175", RegulatoryInfo.Type.ORSP_NOT_ENGAGED),
                new RegulatoryInfoSelection("PDO-5656", "ORSP-1175", RegulatoryInfo.Type.ORSP_NOT_ENGAGED),
                new RegulatoryInfoSelection("PDO-5660", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5913", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5915", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5973", "ORSP-1476", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-6035", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-6265", "orsp-1654",
                        RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH),
                new RegulatoryInfoSelection("PDO-6361", "ORSP-1366", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-6163", "2011P001787", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-6190", "10-02-0053", RegulatoryInfo.Type.IRB),
                new RegulatoryInfoSelection("PDO-5657", "ORSP-1175", RegulatoryInfo.Type.ORSP_NOT_ENGAGED));

        for (RegulatoryInfoSelection orderToRegInfo: regulatoryInfoAdditions) {
            ProductOrder pdoToChange = productOrderDao.findByBusinessKey(orderToRegInfo.getProductOrderKey());

            RegulatoryInfo selectedRegulatoryInfo = null;
            for(RegulatoryInfo candidate: pdoToChange.getResearchProject().getRegulatoryInfos()) {
                if(candidate.getIdentifier().equals(orderToRegInfo.getRegulatoryInfoIdentifier()) &&
                   candidate.getType() == orderToRegInfo.getRegulatoryInfoType()) {
                    selectedRegulatoryInfo = candidate;
                    break;
                }
            }
            pdoToChange.setSkipRegulatoryReason(null);
            pdoToChange.addRegulatoryInfo(selectedRegulatoryInfo);

        }
        productOrderDao.persist(new FixupCommentary(
                "Support-809:  Updated PDOs which did not have the correct Regulatory Info associated with them."));
    }

    /**
     * Clean up the regulatory information records for some PDOs. The currently selected regulatory information is
     * "NHGRI_TCGA", which is not an IRB or ORSP number, however the name mentions two IRB numbers. The goal is to
     * replace the "NHGRI_TCGA" record with two new regulatory information records with ORSP numbers (ORSP-2221 and
     * ORSP-783) referencing the two IRBs. The effective IRBs are not being changed.
     *
     * ORSP-2221 is new. However, ORSP-783's IRB, 1107004579, already has a record under the IRB number instead of the
     * ORSP number. Because ORSP numbers are preferred, the identifier for 1107004579 will be changed to ORSP-783. This
     * is done in {@link RegulatoryInfoFixupTest#gplim3765FixRegulatoryInfoForOrsp783()} which must be run before this
     * fixup test.
     *
     * It is assumed that the research projects for the PDOs being updated (RP-22, RP-608, RP-705) already have
     * associations to ORSP-2221 and ORSP-783.
     */
    @Test(enabled = false)
    public void gplim3765FixRegulatoryInfoForNhgriTcgaPdos() {
        userBean.loginOSUser();

        List<String> pdoIds = Arrays.asList("PDO-6976", "PDO-6977", "PDO-7006", "PDO-7007", "PDO-7070", "PDO-7075");

        List<RegulatoryInfo> regulatoryInfos;
        regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-2221");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp2221 = regulatoryInfos.get(0);
        assertThat(orsp2221.getType(), equalTo(RegulatoryInfo.Type.IRB));

        regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-783");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp783 = regulatoryInfos.get(0);
        assertThat(orsp783.getType(), equalTo(RegulatoryInfo.Type.IRB));

        for (String pdoId : pdoIds) {
            ProductOrder pdo = productOrderDao.findByBusinessKey(pdoId);
            assertThat(pdo.getRegulatoryInfos(), hasSize(1));
            assertThat(pdo.getRegulatoryInfos().iterator().next().getIdentifier(), equalTo("NHGRI_TCGA"));
            assertThat(pdo.getResearchProject().getRegulatoryInfos(), hasItem(orsp2221));
            assertThat(pdo.getResearchProject().getRegulatoryInfos(), hasItem(orsp783));

            pdo.getRegulatoryInfos().clear();
            pdo.addRegulatoryInfo(orsp2221);
            pdo.addRegulatoryInfo(orsp783);
        }

        productOrderDao.persist(new FixupCommentary(
                "GPLIM-3765 Cleaning up regulatory designation records (not changing effective IRBs)"));
    }

    @Test(enabled = false)
    public void gplim4009UnabandonPDOsToPending() {
        userBean.loginOSUser();

        List<String> pdoIds = Arrays.asList("PDO-8013", "PDO-8045");
        for (String pdoId : pdoIds) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoId);
            productOrder.setOrderStatus(ProductOrder.OrderStatus.Pending);
            System.out.println("Updated " + pdoId + " status to pending.");
        }

        productOrderDao.persist(new FixupCommentary("Unabandoning PDO-8013 and PDO-8045 to pending state"));
    }

    @Test(enabled = false)
    public void support1579updatePdo() {
        userBean.loginOSUser();
        String currentPdo = "PDO-8313";
        String newKey = "PDO-8317";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(currentPdo);
        if (productOrder==null){
            throw new RuntimeException(String.format("%s doesn't exist.", currentPdo));
        }
        productOrder.setJiraTicketKey(newKey);
        log.info(String.format("Updated %s to %s", currentPdo, newKey));
        productOrderDao.persist(new FixupCommentary("https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-1579"));
    }

    @Test(enabled = false)
    public void support1573ChangeRegInfoForPdo8181() {
        userBean.loginOSUser();

        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-3342");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp3342 = regulatoryInfos.get(0);

        ProductOrder pdo = productOrderDao.findByBusinessKey("PDO-8181");
        pdo.getRegulatoryInfos().clear();
        pdo.addRegulatoryInfo(orsp3342);

        productOrderDao.persist(
                new FixupCommentary("SUPPORT-1573 Updating reg info for PDO-8181 as requested by Kristina Tracy"));
    }

    @Test(enabled = false)
    public void gplim4755RemoveDuplicateRegulatoryInfo() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-750");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp750 = regulatoryInfos.get(0);

        ProductOrder pdo = productOrderDao.findByBusinessKey("PDO-6148");
        assertThat(pdo.getRegulatoryInfos(), hasSize(2));

        Set<RegulatoryInfo> distinctRegulatoryInfos = new HashSet<>(pdo.getRegulatoryInfos());
        assertThat(distinctRegulatoryInfos, hasSize(1));
        assertThat(orsp750, equalTo(distinctRegulatoryInfos.iterator().next()));

        pdo.getRegulatoryInfos().clear();
        productOrderDao.flush();
        pdo.addRegulatoryInfo(distinctRegulatoryInfos.iterator().next());

        productOrderDao.persist(new FixupCommentary("https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4755"));
        commitTransaction();
    }

    @Test(enabled = false)
    public void gplim4155RemoveUnattachedPDOSamples()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        userBean.loginOSUser();

        /*
         * Use a user transaction for this test because, unfortunately, ProductOrderDao and ProductOrderSampleDao have
         * the default transaction attribute type of REQUIRED instead of explicitly requesting SUPPORTS like most of
         * Mercury's other DAOs. Therefore, every call to a query method on these DAOs will begin and commit a
         * transaction. Using a user transaction allows the whole test to be in one transaction, importantly including
         * the FixupCommentary.
         */
        utx.begin();

        /*
         * PDO-8953 was discovered by users. This was confirmed and 2 other PDOs found with this query:
               select po.JIRA_TICKET_KEY, pos.SAMPLE_POSITION, count(*)
               from athena.PRODUCT_ORDER po
               join athena.PRODUCT_ORDER_SAMPLE pos on po.PRODUCT_ORDER_ID = pos.PRODUCT_ORDER
               where po.JIRA_TICKET_KEY is not null
               group by po.JIRA_TICKET_KEY, pos.SAMPLE_POSITION
               having count(*) > 1
               order by po.JIRA_TICKET_KEY, pos.SAMPLE_POSITION
         */
        String[] orderKeys = new String[]{"PDO-8947", "PDO-8953", "PDO-9075"};

        for (String orderKey : orderKeys) {
            removeUnattachedSamples(orderKey);
        }

        productOrderDao.persist(new FixupCommentary(
                "GPLIM-4155 Removed orphaned ProductOrderSamples for: " + StringUtils.join(orderKeys, ", ")));

        utx.commit();
    }

    @Test(enabled = false)
    public void support1798FixRegulatoryInfo() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        /*
         * From SUPPORT-1798:
         *  Remove ORSP-1525 from RP-867 and replace ORSP-1525 with ORSP-1565 for PDO-5326. ORSP-1525 can also be removed from PDO-5585.
         *  ORSP-1565 should be left associated to RP-867. However, the ORSP-1565 "type" should be changed from "IRB Protocol" to "ORSP Not Engaged."
         */

        // Gather all of the entity instances involved in these changes
        ProductOrder pdo5326 = productOrderDao.findByBusinessKey("PDO-5326");
        ProductOrder pdo5585 = productOrderDao.findByBusinessKey("PDO-5585");
        assertThat(pdo5326.getResearchProject().getBusinessKey(), equalTo("RP-867"));
        assertThat(pdo5585.getResearchProject().getBusinessKey(), equalTo("RP-867"));
        ResearchProject rp867 = pdo5585.getResearchProject();
        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-1525");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp1525 = regulatoryInfos.get(0);
        regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-1565");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo orsp1565 = regulatoryInfos.get(0);

        // Make the changes
        assertThat(pdo5326.getRegulatoryInfos(), hasSize(1));
        assertThat(pdo5326.getRegulatoryInfos(), hasItem(orsp1525));
        pdo5326.getRegulatoryInfos().remove(orsp1525);
        pdo5326.getRegulatoryInfos().add(orsp1565);

        assertThat(pdo5585.getRegulatoryInfos(), hasSize(2));
        assertThat(pdo5585.getRegulatoryInfos(), hasItem(orsp1525));
        assertThat(pdo5585.getRegulatoryInfos(), hasItem(orsp1565));
        pdo5585.getRegulatoryInfos().remove(orsp1525);

        assertThat(rp867.getRegulatoryInfos(), hasSize(2));
        assertThat(rp867.getRegulatoryInfos(), hasItem(orsp1525));
        assertThat(rp867.getRegulatoryInfos(), hasItem(orsp1565));
        rp867.getRegulatoryInfos().remove(orsp1525);

        assertThat(orsp1565.getType(), equalTo(RegulatoryInfo.Type.IRB));
        orsp1565.setType(RegulatoryInfo.Type.ORSP_NOT_ENGAGED);

        productOrderDao
                .persist(new FixupCommentary("SUPPORT-1798 Updated regulatory info for RP-867 and related PDOs"));

        commitTransaction();
    }

    /**
     * Find all samples that reference the given PDO, then remove the ones that the PDO doesn't reference.
     *
     * @param orderKey    the business key of the order to fix
     */
    private void removeUnattachedSamples(String orderKey) {
        ProductOrder order = productOrderDao.findByBusinessKey(orderKey);
        List<ProductOrderSample> samples =
                productOrderSampleDao.findList(ProductOrderSample.class, ProductOrderSample_.productOrder, order);
        samples.removeAll(order.getSamples());
        for (ProductOrderSample sample : samples) {
            sample.remove();
            productOrderSampleDao.remove(sample);
        }
        System.out.println(String.format("Removed %d orphaned ProductOrderSamples for %s", samples.size(), orderKey));
    }

    private static class RegulatoryInfoSelection {
        private String productOrderKey;
        private String regulatoryInfoIdentifier;
        private RegulatoryInfo.Type regulatoryInfoType;


        public RegulatoryInfoSelection(String productOrderKey, String regulatoryInfoIdentifier,
                                       RegulatoryInfo.Type regulatoryInfoType) {
            this.productOrderKey = productOrderKey;
            this.regulatoryInfoIdentifier = regulatoryInfoIdentifier;
            this.regulatoryInfoType = regulatoryInfoType;
        }

        public String getProductOrderKey() {
            return productOrderKey;
        }

        public String getRegulatoryInfoIdentifier() {
            return regulatoryInfoIdentifier;
        }

        public RegulatoryInfo.Type getRegulatoryInfoType() {
            return regulatoryInfoType;
        }
    }

    /**
     * Use a user transaction for these tests because, unfortunately, ProductOrderDao and ProductOrderSampleDao have
     * the default transaction attribute type of REQUIRED instead of explicitly requesting SUPPORTS like most of
     * Mercury's other DAOs. Therefore, every call to a query method on these DAOs will begin and commit a
     * transaction. Using a user transaction allows the whole test to be in one transaction, importantly including
     * the FixupCommentary.
     */
    private void beginTransaction() throws NotSupportedException, SystemException {
        utx.begin();
    }

    private void commitTransaction()
            throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
        utx.commit();
    }

    @Test(enabled = false)
    public void updateSAPRecords() throws Exception {

        userBean.loginOSUser();

        List<ProductOrder> listWithWildcard =
                productOrderDao.findListWithWildcard(ProductOrder.class, "%%", true, ProductOrder_.sapOrderNumber);

        for(ProductOrder orderWithSap:listWithWildcard) {
            if(CollectionUtils.isEmpty(orderWithSap.getSapReferenceOrders())) {
                BigDecimal sampleCount = BigDecimal.ZERO;
                if(orderWithSap.isPriorToSAP1_5()) {
                    sampleCount =
                            SapIntegrationServiceImpl.getSampleCount(orderWithSap, orderWithSap.getProduct(), false);
                }
                SapOrderDetail newDetail = new SapOrderDetail(orderWithSap.getSapOrderNumber(),
                        sampleCount.intValue(),
                        orderWithSap.getQuoteId(), SapIntegrationServiceImpl.determineCompanyCode(orderWithSap).getCompanyCode(),
                        "", "");
                orderWithSap.addSapOrderDetail(newDetail);
            } else {
                SapOrderDetail latestDetail = orderWithSap.latestSapOrderDetail();
                latestDetail.setQuoteId(orderWithSap.getQuoteId());
                latestDetail.setCompanyCode(SapIntegrationServiceImpl.determineCompanyCode(orderWithSap).getCompanyCode());
            }
        }

        productOrderDao.persist(new FixupCommentary("Adding entities for SAP Order Detail to account for new tables"));
    }

    @Test(enabled = false)
    public void gplim4501deleteDraftPdo() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        ProductOrder productOrder = productOrderDao.findByTitle("Health_2000_GWAS");
        Assert.assertNotNull(productOrder);
        Assert.assertNull(productOrder.getJiraTicketKey());
        productOrderDao.remove(productOrder);
        productOrderDao.persist(new FixupCommentary("GPLIM-4501 delete draft pdo"));
        commitTransaction();
    }

    @Test(enabled = false)
    public void gplim4595BackfillInfiniumPdosToOnPremises() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        List<String> arraysPartNumbers = Arrays.asList(
                "P-WG-0053",
                "P-WG-0055",
                "P-WG-0056",
                "P-EX-0021",
                "P-WG-0058",
                "P-WG-0023",
                "P-WG-0028",
                "P-WG-0066",
                "XTNL-GEN-011003",
                "P-WG-0059",
                "XTNL-WES-010210",
                "XTNL-WES-010211",
                "XTNL-GEN-011004",
                "XTNL-GEN-011005",
                "XTNL-WES-010212");
        List<ProductOrder> allProductOrders = productOrderDao.findAll();
        for (ProductOrder productOrder: allProductOrders) {
            if (productOrder.getProduct() != null) {
                if (productOrder.getProduct().getPartNumber() != null) {
                    if (arraysPartNumbers.contains(productOrder.getProduct().getPartNumber())) {
                        productOrder.setPipelineLocation(ProductOrder.PipelineLocation.ON_PREMISES);
                        System.out.println("Updated " + productOrder.getJiraTicketKey() + " Pipeline Location to " +
                                           productOrder.getPipelineLocation());
                    }
                }
            }
        }
        productOrderDao.persist(new FixupCommentary("GPLIM-4595 Updated pipeline location for arrays PDOs to On Prem"));
        commitTransaction();
    }

    @Test(enabled = false)
    public void gplim4823SetOrdersToCompleted() throws Exception {
        userBean.loginOSUser();
        List <String> ordersToComplete = Arrays.asList("PDO-11091","PDO-11492","PDO-11769","PDO-11628","PDO-11629",
                "PDO-11766","PDO-11164","PDO-11721","PDO-11390","PDO-11495","PDO-11519");

        // Update the state of all PDOs affected by this billing session.
        for (String key : ordersToComplete) {
            try {
                // Update the order status using the ProductOrderEjb with a version of the order status update
                // method that does not mark transactions for rollback in the event that JIRA-related RuntimeExceptions
                // are thrown.  It is still possible that this method will throw a checked exception,
                // but these will not mark the transaction for rollback.
                productOrderEjb.updateOrderStatusNoRollback(key);
            } catch (Exception e) {
                // Errors are just logged here because the current user doesn't work with PDOs, and wouldn't
                // be able to resolve these issues.  Exceptions should only occur if a required resource,
                // such as JIRA, is missing.
                log.error("Failed to update PDO status after billing: " + key, e);
            }
        }
        productOrderDao.persist(new FixupCommentary("GPLIM-4823: Updating productOrders to Submitted which were not previously tranistioned as such"));
    }

    @Test(enabled = false)
    public void gplim4954SetOrdersToCompleted() throws Exception {
        userBean.loginOSUser();
        List <String> ordersToComplete = Arrays.asList("PDO-11934",
                "PDO-11935",
                "PDO-11981",
                "PDO-11992",
                "PDO-12215",
                "PDO-12137",
                "PDO-12070",
                "PDO-12111",
                "PDO-12119",
                "PDO-12120",
                "PDO-12150");

        // Update the state of all PDOs affected by this billing session.
        for (String key : ordersToComplete) {
            try {
                // Update the order status using the ProductOrderEjb with a version of the order status update
                // method that does not mark transactions for rollback in the event that JIRA-related RuntimeExceptions
                // are thrown.  It is still possible that this method will throw a checked exception,
                // but these will not mark the transaction for rollback.
                productOrderEjb.updateOrderStatusNoRollback(key);
            } catch (Exception e) {
                // Errors are just logged here because the current user doesn't work with PDOs, and wouldn't
                // be able to resolve these issues.  Exceptions should only occur if a required resource,
                // such as JIRA, is missing.
                log.error("Failed to update PDO status after billing: " + key, e);
            }
        }
        productOrderDao.persist(new FixupCommentary("GPLIM-4954: Updating productOrders to Submitted which were not previously tranistioned as such"));
    }

    @Test(enabled = false)
    public void gplim5054SetOrderToCompleted() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        String pdoToComplete = "PDO-12069";


        productOrderEjb.updateOrderStatus(pdoToComplete, MessageReporter.UNUSED);

        productOrderDao.persist(new FixupCommentary("GPLIM-5054: Updating PDO-12069 to Completed."));
        commitTransaction();
    }

    @Test(enabled = false)
    public void support3427CompleteOrders() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        List<String> pdosToComplete = Arrays.asList("PDO-12890", "PDO-12581", "PDO-12947", "PDO-13129");

        for (String pdoToComplete : pdosToComplete) {
            productOrderEjb.updateOrderStatusNoRollback(pdoToComplete);
        }

        productOrderDao.persist(new FixupCommentary("SUPPORT-3427: Updating " + pdosToComplete + " to Completed."));
        commitTransaction();
    }

    @Test(enabled = false)
    public void support3399UnabandonSamples() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        final String sampleComment = "Accidentally abandoned due to a mis-communication.";
        Set<Long> productOrderSampleIDs = new HashSet<>();
        String pdoTicket = "PDO-13210";

        List<String> sampleKeys = Arrays.asList("SM-BY2PC", "SM-BY2XX", "SM-BY1ZL", "SM-BY3JV", "SM-BY11Y",
                "SM-BY3IW", "SM-BY2Y1", "SM-BY2XF", "SM-BY3JD", "SM-BY2XD", "SM-BY3JQ", "SM-BY2X4", "SM-BY3KQ",
                "SM-BY2KD", "SM-BY3K2", "SM-BY162", "SM-BY3IN", "SM-BY3KA", "SM-BY2XK", "SM-BY2LR", "SM-BY2KR",
                "SM-BY3KC", "SM-BY161");

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoTicket);

        for (ProductOrderSample sample : productOrder.getSamples()) {
            if (sampleKeys.contains(sample.getSampleKey())) {
                productOrderSampleIDs.add(sample.getProductOrderSampleId());
            }
        }

        final MessageReporter testOnly = MessageReporter.UNUSED;

        MessageCollection testCollection = new MessageCollection();

        productOrderEjb.unAbandonSamples(pdoTicket, productOrderSampleIDs, sampleComment, testCollection);
        productOrderEjb.updateOrderStatus(pdoTicket, testOnly);

        final MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.publishProductOrderToSAP(productOrder, messageCollection, false);
        if (messageCollection.hasErrors() || messageCollection.hasWarnings()) {
            Assert.fail("Error occured attempting to update SAP in fixupTest");

        }
        productOrderDao.persist(new FixupCommentary("SUPPORT-3399: unabandonning samples since abandoning "
                                                    + "these came by way of a communication mixup, the public feature "
                                                    + "is removed and the new process would not satisfy this case"));
        commitTransaction();
    }

    @Test(enabled = false)
    public void gplim5364UnabandonSamples() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        final String sampleComment = "Unabandon due to the current status of the samples in the process.";
        Set<Long> productOrderSampleIDs = new HashSet<>();
        String pdoTicket = "PDO-13827";

        List<String> sampleKeys =
                Arrays.asList("SM-GBMDQ", "SM-GBME1", "SM-GBME3", "SM-GBME8", "SM-GBMES", "SM-GBMCE", "SM-GBOBR",
                        "SM-GBMBT", "SM-GBOB7");

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoTicket);

        for (ProductOrderSample sample : productOrder.getSamples()) {
            if (sampleKeys.contains(sample.getSampleKey())) {
                productOrderSampleIDs.add(sample.getProductOrderSampleId());
            }
        }

        final MessageReporter testOnly = MessageReporter.UNUSED;
        final MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.unAbandonSamples(pdoTicket, productOrderSampleIDs, sampleComment, messageCollection);
        productOrderEjb.updateOrderStatus(pdoTicket, testOnly);

        productOrderEjb.publishProductOrderToSAP(productOrder, messageCollection, false);
        if (messageCollection.hasErrors() || messageCollection.hasWarnings()) {
            Assert.fail("Error occured attempting to update SAP in fixupTest");

        }
        productOrderDao.persist(new FixupCommentary("GPLIM-5364:  unabandon due to the current status of the samples in the process."));
        commitTransaction();
    }


    @Test(enabled = false)
    public void support3407UpdatePDOAssociationsOnLCSET11965() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        final ProductOrder oldProductOrder = productOrderDao.findByBusinessKey("PDO-13083");
        final String labBatchBusinessKey = "LCSET-11965";

        final Map<String, List<String>> newPdoToSampleMap = new HashMap<>();

        newPdoToSampleMap.put("PDO-13067", Arrays.asList("SM-F2QYA", "SM-F2QYB", "SM-F2QYC", "SM-F2QYD", "SM-F2QYE", "SM-F2QYF", "SM-F2QYG", "SM-F2QYH"));
        newPdoToSampleMap.put("PDO-13101", Arrays.asList("SM-F2RLI", "SM-F2RLJ", "SM-F2RLK"));
        newPdoToSampleMap.put("PDO-12910", Arrays.asList("SM-G9VS5", "SM-G9VS6", "SM-G9VS7", "SM-G9VS8", "SM-G9VS9", "SM-G9VSA"));

        for (Map.Entry<String, List<String>> newPdoToSampleEntry : newPdoToSampleMap.entrySet()) {

            ProductOrder newProductOrder = productOrderDao.findByBusinessKey(newPdoToSampleEntry.getKey());

            List<BucketEntry> bucketEntries = bucketEntryDao.findByProductOrder(oldProductOrder);
            for (BucketEntry bucketEntry : bucketEntries) {
                for (MercurySample mercurySample : bucketEntry.getLabVessel().getMercurySamples()) {
                    if (newPdoToSampleEntry.getValue().contains(mercurySample.getSampleKey())) {
                        bucketEntry.setProductOrder(newProductOrder);
                    }
                }
            }
        }

        productOrderDao.persist(new FixupCommentary("SUPPORT-3407: changing the PDO association on certain"
                                                    + " samples found in LCSET-11965"));
        commitTransaction();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/PDOsToBeClosed.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-XXXX transitioning pdos to Closed which did not happen during billing
     * PDO-123
     * PDO-456
     */
    @Test(enabled = false)
    public void supportTransitionPdosToClosed() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("PDOsToBeClosed.txt"));
        Assert.assertTrue(CollectionUtils.isNotEmpty(fixupLines), "The file PDOsToBeClosed.txt has no content.");
        String fixupReason = fixupLines.get(0);

        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason is necessary in order to record the fixup.");

        List<String> pdosToComplete = fixupLines.subList(1, fixupLines.size());
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdosToComplete), "No PDOs have been provided to close");

        for (String pdoToComplete : pdosToComplete) {
            productOrderEjb.updateOrderStatusNoRollback(pdoToComplete);
            System.out.println("Updated order status for: " + pdoToComplete);
        }

        productOrderDao.persist(new FixupCommentary(fixupReason ));
        commitTransaction();
    }

    @Test(enabled = false)
    public void support3471AddSampleToSupportData() throws Exception {

        String productOrderString = "PDO-13241";
        ProductOrderSample sampleToAdd = new ProductOrderSample("SM-DAMWL");

        userBean.loginOSUser();
        beginTransaction();


        productOrderEjb.addSamplesNoSap(productOrderString, Collections.singletonList(sampleToAdd),MessageReporter.UNUSED);

        productOrderDao.persist(new FixupCommentary("SUPPORT-3471:  Adding The stock for a control sample since that is what was sequenced in the LCSet" ));
        commitTransaction();

    }

    @Test(enabled=false)
    public void gplim4593BackfillBilledSampleAssociationWithSAPOrders() throws Exception {

        userBean.loginOSUser();
        List<ProductOrder> ordersToUpdate = productOrderDao.findOrdersWithSAPOrdersAndBilledSamples();
        for (ProductOrder productOrder : ordersToUpdate) {
            Set<LedgerEntry> billedLedgerEntries = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                billedLedgerEntries.addAll(productOrderSample.getBilledLedgerItems());
            }

            productOrder.latestSapOrderDetail().addLedgerEntries(billedLedgerEntries);
            System.out.println("Updating association to SAP order " + productOrder.getSapOrderNumber() + " for PDO " +
                               productOrder.getBusinessKey() + " with association to the " + billedLedgerEntries.size() +
                               " ledger entries which have already successfully been billed");
        }

        productOrderDao.persist(new FixupCommentary("GPLIM-4593: Backfilling sap Order Detail with billing Ledger Associations"));
    }

    @Test(enabled=false)
    public void gplim5377MakeCliaPcrFreeWholeGenomeClinical() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        Product pcrFreeProduct = productDao.findByBusinessKey("P-CLA-0008");
        Product germlineCommercial = productDao.findByBusinessKey("P-CLA-0005");
        Product somaticCommercial = productDao.findByBusinessKey("P-CLA-0006");

        List<ProductOrder> pcrFreeOrders = productOrderDao.findList(ProductOrder.class, ProductOrder_.product, pcrFreeProduct);

        pcrFreeProduct.setClinicalProduct(true);
        System.out.println("Set " + pcrFreeProduct.getPartNumber() + " to be a clinical order");
        pcrFreeProduct.setExternalOnlyProduct(false);
        System.out.println("Set " + pcrFreeProduct.getPartNumber() + " to not be an external order");

        germlineCommercial.setExternalOnlyProduct(true);
        somaticCommercial.setExternalOnlyProduct(true);

        for (ProductOrder pcrFreeOrder : pcrFreeOrders) {
            if(pcrFreeOrder.getOrderStatus() != ProductOrder.OrderStatus.Abandoned &&
               pcrFreeOrder.getOrderStatus() != ProductOrder.OrderStatus.Draft) {
                pcrFreeOrder.setClinicalAttestationConfirmed(true);
                System.out.println("Updating " + pcrFreeOrder.getJiraTicketKey() + " to have Clinical attestation set");
            }
        }
        productOrderDao.persist(new FixupCommentary("GPLIM-5377:  Updated PCR-Free Whole Genome product to be a "
                                                    + "clinical product, and set the associated orders for them to "
                                                    + "have the Clinical Attestation Set"));

        commitTransaction();
    }

    @Test(enabled=false)
    public void gplimSupport3853ChangeOrderType() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        ProductOrder orderToUpdate = productOrderDao.findByBusinessKey("PDO-13052");

        orderToUpdate.setOrderType(ProductOrder.OrderAccessType.COMMERCIAL);

        productOrderDao.persist(new FixupCommentary("SUPPORT-3853: updated PDO-13052 to allow it to switch to a commercial order type"));

        commitTransaction();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/PDOSamplesToUnabandon.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-XXXX unabandoning samples in pdo-xxx
     * A reason for unabandoning to go with the unabandon comment
     * PDO-xxx
     * SM-329482
     * SM-2938239
     * ...
     * ...
     */

    @Test(enabled = false)
    public void genericUnAbandonSamples() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("PDOSamplesToUnabandon.txt"));
        Assert.assertTrue(CollectionUtils.isNotEmpty(fixupLines), "The file PDOSamplesToUnabandon.txt has no content.");
        String fixupReason = fixupLines.get(0);
        final String sampleComment = fixupLines.get(1);
        String pdoTicket = fixupLines.get(2);

        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason is necessary in order to record the fixup.");
        Assert.assertTrue(StringUtils.isNotBlank(pdoTicket), "A PDO is necessary to unabandon");

        List<String> samplesToUnabandon = fixupLines.subList(3, fixupLines.size());
        Assert.assertTrue(CollectionUtils.isNotEmpty(samplesToUnabandon), "No Samples have been provided to unabandon");


        Set<Long> productOrderSampleIDs = new HashSet<>();


        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoTicket);

        for (ProductOrderSample sample : productOrder.getSamples()) {
            if (samplesToUnabandon .contains(sample.getSampleKey())) {
                productOrderSampleIDs.add(sample.getProductOrderSampleId());
            }
        }

        final MessageReporter testOnly = MessageReporter.UNUSED;
        final MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.unAbandonSamples(pdoTicket, productOrderSampleIDs, sampleComment, messageCollection);
        productOrderEjb.updateOrderStatus(pdoTicket, testOnly);

        if(productOrder.isSavedInSAP()) {
            productOrderEjb.publishProductOrderToSAP(productOrder, messageCollection, false);
        }
        if (messageCollection.hasErrors() || messageCollection.hasWarnings()) {
            Assert.fail("Error occured attempting to update SAP in fixupTest");

        }
        productOrderDao.persist(new FixupCommentary(fixupReason));
        commitTransaction();
    }


    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/SamplesToAddToClosedPDO.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-XXXX Adding samples to completed pdos PDO-xxx, PDO-xxx1
     * PDO-xxx SM-329482
     * PDO-xxx SM-2938239
     * PDO-xxx1 SM-329482
     * PDO-xxx1 SM-2938239
     * ...
     * ...
     */
    @Test(enabled = false)
    public void addSamplesToClosedOrder() throws Exception {

        userBean.loginOSUser();
        beginTransaction();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("SamplesToAddToClosedPDO.txt"));
        Assert.assertTrue(CollectionUtils.isNotEmpty(fixupLines), "The file SamplesToAddToClosedPDO.txt has no content.");
        final String fixupReason = fixupLines.get(0);
        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason is necessary in order to record the fixup.");

        Multimap<String, ProductOrderSample> samplesToAdd = ArrayListMultimap.create();
        fixupLines.subList(1, fixupLines.size())
                .forEach((pdoSampleString)->samplesToAdd.put(pdoSampleString.split(" ")[0],
                        new ProductOrderSample(pdoSampleString.split(" ")[1])));

        samplesToAdd.asMap().forEach((String pdoKey, Collection<ProductOrderSample> samples) -> {
            ProductOrder pdo = productOrderDao.findByBusinessKey(pdoKey);
            pdo.addSamples(samples);

            pdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            productOrderEjb.attachMercurySamples(new ArrayList<>(samples));

            pdo.prepareToSave(userBean.getBspUser());
            productOrderDao.persist(pdo);
            productOrderEjb.handleSamplesAdded(pdoKey, samples, MessageReporter.UNUSED);

            try {
                productOrderEjb.updateSamples(pdo, samples, MessageReporter.UNUSED, "added");
            } catch (IOException | ProductOrderEjb.NoSuchPDOException | SAPInterfaceException e) {
                Assert.fail();
            }
        });

        productOrderDao.persist(new FixupCommentary(fixupReason));
        commitTransaction();
    }

    @Test(enabled = false)
    public void gplim5824ReOpenPdo() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        String pdoKey = "PDO-16090";

        ProductOrder orderToOpen = productOrderDao.findByBusinessKey(pdoKey);

        orderToOpen.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        System.out.println("Set the status of " +pdoKey + " to be " + orderToOpen.getOrderStatus().getDisplayName());

        productOrderDao.persist(new FixupCommentary("GPLIM-5824: repopening order to allow billing to continue"));
        commitTransaction();
    }

    /**
     *
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/SamplesToAbandon.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     *
     * SUPPORT-XXXX Marking samples as abandoned in PDOs
     * PDO-xxx SM-32948 SM-3344
     * PDO-xxx1 SM-329482 SM-2938239
     * ...
     * @throws Exception
     */
    @Test(enabled = false)
    public void gplim5878ChangeSampleToAbandoned() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("SamplesToAbandon.txt"));
        Assert.assertTrue(CollectionUtils.isNotEmpty(fixupLines), "The file SamplesToAbandon.txt has no content.");
        final String fixupReason = fixupLines.get(0);
        Assert.assertTrue(StringUtils.isNotBlank(fixupReason), "A fixup reason is necessary in order to record the fixup.");

        fixupLines.subList(1, fixupLines.size())
                .forEach(line ->{
                    String pdoKey = line.split(" ", 2)[0];
                    final String samples = line.split(" ", 2)[1];
                    final List<String> sampleList = Arrays.asList(samples.split(" "));
                    try {
                        abandonSamplesForSapOrder(pdoKey, sampleList, fixupReason);
                        System.out.println("Changed the status of " + StringUtils.join(sampleList, ", ") +
                                           " in " + pdoKey + " To abandoned");
                    } catch (ProductOrderEjb.SampleDeliveryStatusChangeException | QuoteNotFoundException |
                            QuoteServerException | InvalidProductException | SAPIntegrationException |
                            IOException | ProductOrderEjb.NoSuchPDOException | SAPInterfaceException e) {
                        Assert.fail(e.getMessage());
                    }
                }
                );

        productOrderDao.persist(new FixupCommentary(fixupReason));
        commitTransaction();
    }

    /**
     *
     * Helper method to assist abandoning
     *
     * @param pdoKey        Business key for the Product Order being updated
     * @param sampleList    List of Samples to be abandoned
     * @param abandonReason   Comment to add as the reason to abandon
     * @throws ProductOrderEjb.SampleDeliveryStatusChangeException
     * @throws QuoteNotFoundException
     * @throws QuoteServerException
     * @throws InvalidProductException
     * @throws SAPIntegrationException
     * @throws IOException
     * @throws ProductOrderEjb.NoSuchPDOException
     * @throws SAPInterfaceException
     */
    public void abandonSamplesForSapOrder(String pdoKey, List<String> sampleList, String abandonReason)
            throws ProductOrderEjb.SampleDeliveryStatusChangeException, QuoteNotFoundException, QuoteServerException,
            InvalidProductException, SAPIntegrationException, IOException, ProductOrderEjb.NoSuchPDOException,
            SAPInterfaceException {

        final ProductOrder orderToModify = productOrderDao.findByBusinessKey(pdoKey);
        final List<ProductOrderSample> samplesToTransition =
                orderToModify.getSamples().stream()
                        .filter(productOrderSample -> sampleList.contains(productOrderSample.getSampleKey()))
                        .collect(Collectors.toList());

        productOrderEjb.transitionSamples(orderToModify,
                EnumSet.of(ProductOrderSample.DeliveryStatus.ABANDONED, ProductOrderSample.DeliveryStatus.NOT_STARTED),
                ProductOrderSample.DeliveryStatus.ABANDONED, samplesToTransition);

        if (orderToModify.isSavedInSAP() && isOrderEligibleForSAP(orderToModify, new Date())) {
            final List<Product> allProductsOrdered = ProductOrder.getAllProductsOrdered(orderToModify);
            Quote quote = orderToModify.getQuote(quoteService);

            final List<String> effectivePricesForProducts = productPriceCache
                    .getEffectivePricesForProducts(allProductsOrdered,orderToModify, quote);

            productOrderEjb.updateOrderInSap(orderToModify, allProductsOrdered, effectivePricesForProducts, new MessageCollection(),
                    CollectionUtils.containsAny(Arrays.asList(
                            ProductOrder.OrderStatus.Abandoned, ProductOrder.OrderStatus.Completed),
                            Collections.singleton(orderToModify.getOrderStatus()))
                    && !orderToModify.isPriorToSAP1_5());
        }

        JiraIssue issue = jiraService.getIssue(orderToModify.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} transitioned samples to status {1}: {2}\n\n{3}",
                productOrderEjb.getUserName(), ProductOrderSample.DeliveryStatus.ABANDONED.getDisplayName(),
                StringUtils.join(ProductOrderSample.getSampleNames(samplesToTransition), ","),
                StringUtils.stripToEmpty(abandonReason)));

        productOrderEjb.updateOrderStatus(orderToModify.getJiraTicketKey(), MessageReporter.UNUSED);
    }

    @Test(enabled = false)
    public void gplim5918_updateIncorrectOrsp() throws Exception {
        userBean.loginOSUser();
        beginTransaction();

        String incorrectOrspIdentifier = "2861";
        String correctOrspIdentifier = "ORSP-2861";

        List<ProductOrder> productOrders = productOrderDao.findOrdersByRegulatoryInfoIdentifier(incorrectOrspIdentifier);
        List<RegulatoryInfo> incorrectRegulatoryInfos = regulatoryInfoDao.findByIdentifier(incorrectOrspIdentifier);
        assertThat(incorrectRegulatoryInfos, hasSize(1));
        RegulatoryInfo incorrectRegulatoryInfo = incorrectRegulatoryInfos.iterator().next();
        List<RegulatoryInfo> replacementRegulatoryInfos = regulatoryInfoDao.findByIdentifier(correctOrspIdentifier);
        assertThat(replacementRegulatoryInfos, hasSize(1));
        RegulatoryInfo replacement = replacementRegulatoryInfos.iterator().next();

        Set<ResearchProject> projectList =
            productOrders.stream().map(ProductOrder::getResearchProject).collect(Collectors.toSet());

        // add replacement regulatoryInfo if it isn't there yet.
        projectList.forEach(researchProject -> {
            if (!researchProject.getRegulatoryInfos().contains(replacement)) {
                researchProject.getRegulatoryInfos().add(replacement);
            }
        });

        // remove incorrect regulatoryInfo and add the replacement.
        productOrders.forEach(productOrder -> {
            assertThat(productOrder.getRegulatoryInfos().remove(incorrectRegulatoryInfo), is(true));
            productOrder.getRegulatoryInfos().add(replacement);
        });

        // remove incorrect regulatoryInfo from the research project.
        projectList.forEach(researchProject -> researchProject.getRegulatoryInfos().remove(incorrectRegulatoryInfo));

        // finally, delete the old regulatoryInfo
        regulatoryInfoDao.remove(incorrectRegulatoryInfo);
        productOrderDao.persist(new FixupCommentary("See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-5918"));
        commitTransaction();
    }

    /**
     * Duplicate of the 'isOrderEligible' check from ProductOrderEjb.  For the purposes of why we are using it, we
     * need to bypass the Quote funding check which is what this version will do.
     * @param editedProductOrder    productOrder against which SAP viability will be validated
     * @param effectiveDate         date by which we are comparing sap order viability
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     * @throws InvalidProductException
     */
    public boolean isOrderEligibleForSAP(ProductOrder editedProductOrder, Date effectiveDate)
            throws QuoteServerException, QuoteNotFoundException, InvalidProductException {
        Quote orderQuote = editedProductOrder.getQuote(quoteService);
        SAPAccessControl accessControl = accessController.getCurrentControlDefinitions();
        boolean eligibilityResult = false;

        Set<AccessItem> priceItemNameList = new HashSet<>();

        final boolean priceItemsValid = productOrderEjb.areProductPricesValid(editedProductOrder, priceItemNameList, orderQuote);

        if(orderQuote != null && accessControl.isEnabled()) {

            eligibilityResult = editedProductOrder.getProduct()!=null &&
                                editedProductOrder.getProduct().getPrimaryPriceItem() != null &&
                                orderQuote != null &&
                                !CollectionUtils.containsAny(accessControl.getDisabledItems(), priceItemNameList) ;
        }

        if(eligibilityResult && !priceItemsValid) {
            throw new InvalidProductException("One of the Price items associated with " +
                                              editedProductOrder.getBusinessKey() + ": " +
                                              editedProductOrder.getName() + " is invalid");
        }
        return eligibilityResult;
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ChangePdoProduct.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-4488
     * PDO-5818 P-EX-0008 P-EX-0016
     * ...
     * The first line is the fixup commentary.  The second and subsequent lines are: the PDO-ID,
     * the old product part number, the new product part number.
     */
    @Test(enabled = false)
    public void fixupSupport4488() throws Exception {
        userBean.loginOSUser();

        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("ChangePdoProduct.txt"));

        for (String line : fixupLines.subList(1, fixupLines.size())) {
            String[] split = line.split("\\s");
            ProductOrder productOrder = productOrderDao.findByBusinessKey(split[0]);
            Assert.assertEquals(productOrder.getProduct().getPartNumber(), split[1]);
            Product product = productDao.findByPartNumber(split[2]);
            System.out.println("Changing " + productOrder.getBusinessKey() + " to " + product.getPartNumber());
            productOrder.setProduct(product);
        }

        productOrderDao.persist(new FixupCommentary(fixupLines.get(0)));
    }

    /**
     * When a PDO is submitted, if auto-bucketing exceeds the transaction timeout and fails, this test can be used to
     * add the PDO samples to the bucket.
     * It reads its parameters from a file, mercury/src/test/resources/testdata/AddPdosSamplesToBucket.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * SUPPORT-5265
     * PDO-17968
     */
    @Test(enabled = false)
    public void fixupSupport5265() throws Exception {
        userBean.loginOSUser();
        beginTransaction();
        List<String> fixupLines = IOUtils.readLines(VarioskanParserTest.getTestResource("AddPdosSamplesToBucket.txt"));
        ProductOrder pdo = productOrderDao.findByBusinessKey(fixupLines.get(1));
        pdo.loadSampleData();
        List<String> messages = new ArrayList<>();
        productOrderEjb.handleSamplesAdded(pdo.getBusinessKey(), pdo.getSamples(), (message, arguments) -> {
            messages.add(message);
            return null;
        });
        if (messages.size() == 1 && messages.get(0).equals("{0} samples have been added to the {1}.")) {
            productOrderDao.persist(new FixupCommentary(fixupLines.get(0)));
            System.out.println("Added samples to bucket for " + pdo.getBusinessKey());
            commitTransaction();
        } else {
            for (String message : messages) {
                System.out.println(message);
            }
            utx.rollback();
        }
    }
}
