package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
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
import java.io.File;
import java.io.FileOutputStream;
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

    @Test(enabled = true)
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
                new RegulatoryInfoSelection("PDO-6190", "10-02-0053", RegulatoryInfo.Type.IRB));

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
            Assert.assertNotNull(selectedRegulatoryInfo, "No matching Regulatory Info was found");
            if(StringUtils.isNotBlank(pdoToChange.getSkipRegulatoryReason())) {
                pdoToChange.setSkipRegulatoryReason("");
            }
            pdoToChange.addRegulatoryInfo(selectedRegulatoryInfo);

        }
        productOrderDao.persist(new FixupCommentary("Support-809:  Updated PDOs which did not have the correct Regulatory Info associated with them."));
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
}
