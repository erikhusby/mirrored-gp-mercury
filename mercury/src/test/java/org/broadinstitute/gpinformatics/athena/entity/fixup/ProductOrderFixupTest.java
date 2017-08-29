package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
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
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Test(groups = TestGroups.FIXUP)
public class ProductOrderFixupTest extends Arquillian {

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
    private SapIntegrationService sapIntegrationService;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

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
    public void fixupPDOCompleteStatus() throws ProductOrderEjb.NoSuchPDOException, IOException {
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
    public void gplim2893ManuallyCompletePDO() throws ProductOrderEjb.NoSuchPDOException, IOException {
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
                SapOrderDetail newDetail = new SapOrderDetail(orderWithSap.getSapOrderNumber(),
                        SapIntegrationServiceImpl.getSampleCount(orderWithSap, orderWithSap.getProduct()),
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

    @Test(enabled = true)
    public void gplim5054SetOrderToCompleted() throws Exception {
        userBean.loginOSUser();
        String pdoToComplete = "PDO-12069";

        productOrderEjb.updateOrderStatus(pdoToComplete, MessageReporter.UNUSED);

        productOrderDao.persist(new FixupCommentary("GPLIM-5054: Updating PDO-12069 to Completed."));
    }
}
