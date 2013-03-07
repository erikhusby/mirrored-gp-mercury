package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.*;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.common.GroupingIterable;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.inject.Inject;
import java.text.DateFormat;
import java.util.*;

@Impl
public class BSPPlatingRequestServiceImpl extends AbstractJerseyClientService implements BSPPlatingRequestService {

    // not sure these are really going to be constants; they should be true
    // for 96 tube Matrix racks but different size plates are certainly
    // possible
    private final static int PLATE_ROW_COUNT = 8;

    private final static int PLATE_COL_COUNT = 12;

    private final static int PLATE_WELL_COUNT = PLATE_ROW_COUNT * PLATE_COL_COUNT;

    private static final String TECHNOLOGY_SOLEXA = "Solexa";

    @Inject
    private BSPManagerFactory bspManagerFactory;

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private QuoteService quoteService;

    @Inject
    private Log log;
    @Inject
    private JiraTicketDao jiraTicketDao;

    private BSPPlatingRequestOptions defaultPlatingRequestOptions = new BSPPlatingRequestOptions(
            BSPPlatingRequestOptions.HighConcentrationOption.VOLUME_FIRST,
            BSPPlatingRequestOptions.PlatformAndProcess.ILLUMINA_HYBRID_SELECTION_WGS_FRAGMENT_180BP,
            BSPPlatingRequestOptions.PlateType.Matrix96SlotRackSC05,
            BSPPlatingRequestOptions.TubeType.MatrixTubeSC05,
            BSPPlatingRequestOptions.AllowLessThanOne.NO,
            BSPPlatingRequestOptions.CancerProject.NO);


    public BSPPlatingRequestServiceImpl() {
        log = LogFactory.getLog(BSPPlatingRequestServiceImpl.class);
    }

    public BSPPlatingRequestServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
        log = LogFactory.getLog(BSPPlatingRequestServiceImpl.class);
    }

    @Override
    public BSPPlatingRequestResult createPlatingRequest(BSPPlatingRequestOptions options, String platformPMLogin,
                                                        String platingRequestName,
                                                        List<SeqWorkRequestAliquot> seqAliquots,
                                                        List<ControlWell> controlWells, String comments,
                                                        String seqTechnology, String humanReadableBarcode
    ) {
        if (seqAliquots == null) {
            throw new IllegalArgumentException("No Stocks passed for plating");
        }

        int totalStockCount = seqAliquots.size() + (controlWells == null ? 0 : controlWells.size());

        if (totalStockCount > 96) {
            throw new IllegalArgumentException("Total Stocks & controls cannot be greater than 96 for plating ");
        }

        return doPlatingRequest(null, options, platformPMLogin, platingRequestName, seqAliquots,
                controlWells, comments, seqTechnology, humanReadableBarcode
        );
    }

    @Override
    public BSPPlatingRequestResult updatePlatingRequest(String platingRequestReceipt, BSPPlatingRequestOptions options,
                                                        String login, List<SeqWorkRequestAliquot> aliquots, List<ControlWell> controlWells) {
        return doPlatingRequest(platingRequestReceipt, options, login, null, aliquots, controlWells, null, null, null);
    }

    /**
     * Submit the plating WR if it has no errors
     *
     * @param bspWorkRequest        The plating WR
     * @param bspWorkRequestManager The BSP work request manager which will handle our submission
     * @return
     */
    private BSPPlatingRequestResult attemptToSubmit(WorkRequest bspWorkRequest, WorkRequestManager bspWorkRequestManager) {

        if (bspWorkRequest == null)
            throw new RuntimeException("Unexpected null BSP plating request!");

        BSPPlatingRequestResult result = new BSPPlatingRequestResult();

        String wrBarcode = bspWorkRequest.getBarCode();

        result.setPlatingRequestReceipt(wrBarcode);
        result.setErrors(bspWorkRequest.getErrors() == null ? null : new ArrayList<String>(bspWorkRequest.getErrors()));
        result.setWarnings(bspWorkRequest.getWarnings() == null ? null : new ArrayList<String>(bspWorkRequest.getWarnings()));
        result.setInfos(bspWorkRequest.getInfo() == null ? null : new ArrayList<String>(bspWorkRequest.getInfo()));

        // assume the worst, that we will ultimately fail submitting this WR
        result.setPlatingRequestSubmitted(false);

        if (bspWorkRequest.getErrors() != null && bspWorkRequest.getErrors().size() > 0) {
            // Errors on work request, do not submit. This is not a hard error,
            // but the PMs will need to sort out the problems before plating can
            // go forward.
            final String message = String.format(
                    "Errors on Squid WR '%s': %s",
                    bspWorkRequest.getWorkRequestName(),
                    bspWorkRequest.getErrors().toString());

            log.warn(message);

            return result;
        }

        final String message = String.format(
                "No errors for BSP plating receipt '%s' created for Squid WR '%s', attempting to submit",
                wrBarcode,
                bspWorkRequest.getWorkRequestName());

        log.info(message);

        // if no errors, proceed with submission
        WorkRequestResponse submissionResponse = bspWorkRequestManager.submit(result.getPlatingRequestReceipt());

        // this REALLY should not happen if we've gotten this far; I've only
        // ever seen this for mismatched client/server jars and we should
        // have found out about that in our initial connection to the WR
        // manager.
        if (submissionResponse == null) {

            final String msg = String.format("Error submitting BSP Plating Work Request '%s'", wrBarcode);

            log.error(msg);
            throw new RuntimeException(msg);
        }

        // log.warn("Skipping submission response check due to bogus BSP errors!");

        if (!submissionResponse.isSuccess()) {

            final String msg = String.format(
                    "Found errors attempting to submit BSP WR %s: %s",
                    wrBarcode,
                    submissionResponse.getErrors().toString());

            log.error(msg);
            throw new RuntimeException(submissionResponse.getErrors().toString());
        }

        // success!
        log.info("Submission successful!");

        result.setPlatingRequestSubmitted(true);
        return result;
    }


    private BSPPlatingRequestResult doPlatingRequest(String bspPlatingBarcode, BSPPlatingRequestOptions options,
                                                     String login, String platingRequestName,
                                                     List<SeqWorkRequestAliquot> seqAliquots, List<ControlWell> controlWells,
                                                     String comments, String seqTechnology, String humanReadableBarcode) {
        final int SAMPLES_PER_PLATE = PLATE_WELL_COUNT - (controlWells == null ? 0 : controlWells.size());

        // TODO Splitterize
        GroupingIterable<SeqWorkRequestAliquot> platesWorthGroupingsOfAliquots =
                new GroupingIterable<SeqWorkRequestAliquot>(SAMPLES_PER_PLATE, seqAliquots);

        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();

        UserManager bspUserManager = bspManagerFactory.createUserManager();

        // this will start being used once Jason clues me in as to how to differentiate plates from tubes
        @SuppressWarnings("unused")
        ContainerManager bspContainerManager = bspManagerFactory.createContainerManager();

        /*
        * Instantiate a new Work Request
        */
        SeqPlatingWorkRequest workRequest;

        // Always make a new plating WR, even on updates!  We don't want any baggage from our
        // older attempts with this plating request receipt
        workRequest = new SeqPlatingWorkRequest();


        // not sure this is necessary
        workRequest.setBarCode(bspPlatingBarcode);

        // do not set the external project id, we don't know our work request id yet

        workRequest.setAllowLessThan1(options.getAllowLessThanOne() == BSPPlatingRequestOptions.AllowLessThanOne.YES);

        workRequest.setHighConcentrationOption(options.getHighConcentrationOption().name());

        workRequest.setCancerProject(options.getCancerProject() == BSPPlatingRequestOptions.CancerProject.YES);
        workRequest.setAllowLessThan1(false);
        workRequest.setAllowLowConcentration(false);

        // the number of plates will be equal to the number of plate's worth groupings
        //workRequest.setExpectedPlateCount((long) platesWorthGroupingsOfAliquots.size());
        workRequest.setExpectedPlateCount(1L);

        String template = "Issued automatically from Squid by %s on %s";
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM);

        // Issued automatically from Squid by susanna on Monday, November 14, 2011 5:08:56 PM
        workRequest.setNotes(String.format(template, login, formatter.format(new Date())));

        // plate numbers are 1-based
        int plateNumber = 1;

        // mapping from 1-based plate numbers to our generated barcodes
        Map<Integer, String> plateNameMap = new HashMap<Integer, String>();
        if (humanReadableBarcode != null && !humanReadableBarcode.isEmpty()) {
            //assumes only 1 plate is created per plating request
            plateNameMap.put(plateNumber, humanReadableBarcode);
        }

        workRequest.setPlateNameMap(plateNameMap);

        workRequest.setPlateType(options.getPlateType().name());
        workRequest.setTubeType(options.getTubeType().name());

        workRequest.setPlatformAndProcess(options.getPlatformAndProcess().name());
        workRequest.setNotificationList(options.getNotificationList());
        workRequest.setFixPlateGaps(options.getFixPlateGaps());
        workRequest.setLabelOption(options.getLabelOption());
        workRequest.setPlatingStyle(options.getPlatingStyle());

        List<SamplePlateInfo> platingInfo = new ArrayList<SamplePlateInfo>();
        workRequest.setPlatingInfo(platingInfo);

        workRequest.setNotes(comments);

        // Work request name must be non-null and unique.  Other than that we don't care about it too much,
        // and we can't use work request id or JIRA id since those don't exist at the time this method is
        // being called
        String workRequestName = null;
        if (platingRequestName != null && !platingRequestName.isEmpty()) {
            workRequestName = platingRequestName;
        } else {
            workRequestName = String.valueOf(System.currentTimeMillis());
        }
        workRequest.setWorkRequestName(workRequestName);

        //TODO .. set something into this ??
        //workRequest.setExternalProjectId(squidWorkRequestId);

        // Trying to leave PI null for now. Per 2011-08-17 meeting PIs can be
        // "various" for the samples.

        // userId = getPrincipalInvestigatorBspUserId(bspUserManager, logins.getPrincipalInvestigator());
        // workRequest.setPrimaryInvestigatorId(userId);

        // hardcoded to an individual, in the long run this should become "various"
        workRequest.setProjectManager(getProgramProjectManagerBspUser(bspUserManager, login));

        String userName = getPlatformProjectManagerBspLogin(bspUserManager, login);
        workRequest.setRequestUser(userName);

        // we don't set the quote for the overall work request since samples can/will be
        // scrambled into LCSETs from various PASSes with differing quotes and then plated
        // to parallel the LCSETs
        Map<String, String> sampleQuoteMap = new HashMap<String, String>();
        workRequest.setSampleQuoteMap(sampleQuoteMap);

        // work request demands 2 of 3 of volume, concentration, and mass to be set at the WR level
        // even if we set volume and concentration on each sample individually
        workRequest.setRequiredConcentration(33.0D);
        workRequest.setRequiredVolume(44.0D);

        //for (SeqWorkRequestAliquot stock : seqAliquots) {
        for (List<SeqWorkRequestAliquot> platesWorthOfAliquots : platesWorthGroupingsOfAliquots) {

            // for the current Tripod use case, there will only be one plate per cart and therefore only one plate
            // per plating request
            // currently force column ordering for Illumina LC and ROW based for everything else
            // have the user specify the ordering ??
            Plateable.Order platingOrder = Plateable.Order.COLUMN; //default
            if (seqTechnology != null && !(TECHNOLOGY_SOLEXA.equalsIgnoreCase(seqTechnology))) {
                platingOrder = Plateable.Order.ROW;
            }

            PlatingArray platingArray = new PlatingArray(seqAliquots, controlWells, platingOrder);

            for (Plateable plateable : platingArray) {

                SamplePlateInfo samplePlateInfo = new SamplePlateInfo(
                        plateable.getSampleId(),
                        plateNumber,
                        // Controls are usually plated to specific positions on every plate,
                        // but regular samples and extra controls we allow BSP to plate
                        // as they see fit, assuming a row major plating order.  Empties to fill
                        // in space before controls at the end of a plate also require specified
                        // positions.
                        plateable.getSpecifiedWell() == null ? null : plateable.getSpecifiedWell().toString(),
                        plateable.getVolume().doubleValue(),
                        plateable.getConcentration().doubleValue());


                platingInfo.add(samplePlateInfo);

                if (plateable.getPlatingQuote() != null)
                    sampleQuoteMap.put(plateable.getSampleId(), plateable.getPlatingQuote());

            }


            plateNumber++;

        }


        /*
        * Create a new Work Request.
        */
        WorkRequestResponse response;

        if (bspPlatingBarcode == null)
            response = bspWorkRequestManager.create(workRequest);
        else
            response = bspWorkRequestManager.update(workRequest);

        String message;

        if (response == null) {
            message = "null response from PlatingRequestManager trying to create/update plating work request " + workRequestName;
            log.error(message);
            throw new RuntimeException(message);
        }

        if (!response.isSuccess()) {
            message = String.format("Errors trying to create/update plating work request %s: %s", workRequestName, response.getErrors().toString());
            log.error(message);
            throw new RuntimeException(message);
        }

        // If we get this far, we have a WorkRequest.  It may be a fatally flawed WorkRequest, but
        // it's a WorkRequest nonetheless.  Now attempt to submit it, assuming it is error-free.
        // (any errors on the work request will be found and flagged in the submission code).
        BSPPlatingRequestResult result = attemptToSubmit(response.getWorkRequest(), bspWorkRequestManager);

        return result;
    }


    private BspUser getProgramProjectManagerBspUser(UserManager bspUserManager, String login) {
        // Value for GSAP PM Id I got from Jason, intended for testing purposes.  I'm using this as a fallback
        // in case the specified GSAP PM isn't found

        Long id = 6947L; //some default that works

        BspUser unknownUser = new BspUser();
        unknownUser.setUserId(id);

        // Just return this id for now, I can't seem to get any real Program PM to work
        //return id;

        BspUser bspUser;
        bspUser = bspUserManager.get(login);

        if (bspUser == null || bspUser.getUsername() == null) {
            // this is not as big of a deal as a missing GSP PM since the GSP PM is the Plating WR owner
            log.warn("Could not find BSP User for GSP PM login '" + login + "'");
            return unknownUser;
        }


        // we currently need to search the list of PMs to make sure this person is a PM
        List<BspUser> projectManagers = bspUserManager.getProjectManagers();

        for (BspUser pm : projectManagers) {
            if (pm.getUserId().equals(bspUser.getUserId())) {
                return pm;
            }
        }

        return unknownUser;
    }

    @SuppressWarnings("unused")
    private Long getPrincipalInvestigatorBspUserId(UserManager bspUserManager, String login) {
        // Value for PI Id I got from Jason, intended for testing purposes.  I'm using this as a fallback
        // in case the specified PI isn't found

        Long id = 7079L;

        // Just return this id for now, I can't seem to get any real PI to work

        return id;

    }


    private String getPlatformProjectManagerBspLogin(UserManager bspUserManager, String ldapLogin) {

        BspUser bspGSPPM = bspUserManager.get(ldapLogin);
        if (bspGSPPM == null) {
            throw new RuntimeException("Could not find BSP User for GSP PM login: '" + ldapLogin + "'");
        }

        return bspGSPPM.getUsername();
    }

    private void checkWorkRequestResponse(WorkRequestResponse workRequestResponse) {
        if (workRequestResponse == null)
            throw new RuntimeException("Null BSP Plating work request response!");

        if (!workRequestResponse.isSuccess())
            throw new RuntimeException("Errors in BSP Plating work request response: " + workRequestResponse.getErrors());
    }


    public void setSquidWorkRequestId(String platingRequestBarcode, long squidWorkRequestId) {
        WorkRequestManager bspWorkRequestManager =
                bspManagerFactory.createWorkRequestManager();

        WorkRequestResponse workRequestResponse = bspWorkRequestManager.get(platingRequestBarcode);

        checkWorkRequestResponse(workRequestResponse);

        SeqPlatingWorkRequest workRequest = (SeqPlatingWorkRequest) workRequestResponse.getWorkRequest();
        workRequest.setExternalProjectId("" + squidWorkRequestId);

        bspWorkRequestManager.update(workRequest);

    }

    public void setLabel(String platingRequestReceipt, int plateIndex, String label) {
        WorkRequestManager bspWorkRequestManager =
                bspManagerFactory.createWorkRequestManager();

        WorkRequestResponse workRequestResponse = bspWorkRequestManager.get(platingRequestReceipt);

        checkWorkRequestResponse(workRequestResponse);

        SeqPlatingWorkRequest workRequest = (SeqPlatingWorkRequest) workRequestResponse.getWorkRequest();
        workRequest.getPlateNameMap().put(plateIndex, label);

        bspWorkRequestManager.update(workRequest);
    }


    public BSPPlatingRequestResult tryAgain(String platingRequestBarcode) {

        WorkRequestManager bspWorkRequestManager =
                bspManagerFactory.createWorkRequestManager();

        // #validate forces BSP to recalculate the errors/warnings/infos on a work request
        WorkRequestResponse workRequestResponse = bspWorkRequestManager.validate(platingRequestBarcode);

        // We do not expect the WorkRequestResponse to have errors, regardless of whether the WorkRequest does or not.
        checkWorkRequestResponse(workRequestResponse);


        return attemptToSubmit(workRequestResponse.getWorkRequest(), bspWorkRequestManager);

    }


    public String generateLinkToBSPPlatingRequestPage(String platingRequestBarcode) {
        return String.format("http://%s:%d/BSP/collection/find.action?barcode=%s",
                bspConfig.getHost(),
                bspConfig.getPort(),
                platingRequestBarcode);
    }

    @Override
    public BSPPlatingRequestOptions getBSPPlatingRequestDefaultOptions() {
        return defaultPlatingRequestOptions;
    }


    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    @Override
    public BSPPlatingRequestResult issueBSPPlatingRequest(BSPPlatingRequestOptions options, List<BSPPlatingRequest> requests,
                                                          List<ControlWell> controlWells, String login, String platingRequestName,
                                                          String comments, String seqTechnology, String label)
            throws Exception {
        //Assert.fail("failed from impl on purpose");

        List<SeqWorkRequestAliquot> bspStocks = new ArrayList<SeqWorkRequestAliquot>();
        BSPPlatingRequestResult result = null;
        for (BSPPlatingRequest request : requests) {
            SeqWorkRequestAliquot aliquot = new SeqWorkRequestAliquot(request.getSampleName(),
                    request.getAliquotParameters().getTargetVolume(),
                    request.getAliquotParameters().getTargetConcentration(),
                    null //request.getAliquotParameters().getProjectPlan().getQuoteDTO(quoteService).getAlphanumericId()
            );

            bspStocks.add(aliquot);
        }

        result = doPlatingRequest(null, options, login, platingRequestName, bspStocks, controlWells, comments, seqTechnology, label);
        return result;
    }


/*
    public BSPPlatingReceipt issueBSPPlatingRequestNew(BSPPlatingRequestOptions options, List<BSPPlatingRequest> requests,
                                                          List<ControlWell> controlWells, String login, String platingRequestName,
                                                          String comments, String seqTechnology, String label)
            throws Exception {
        List<SeqWorkRequestAliquot> bspStocks = new ArrayList<SeqWorkRequestAliquot>();
        BSPPlatingReceipt receipt = null;
        for (BSPPlatingRequest request : requests) {
            SeqWorkRequestAliquot aliquot = new SeqWorkRequestAliquot(request.getSampleName(),
                    request.getAliquotParameters().getTargetVolume(),
                    request.getAliquotParameters().getTargetConcentration(),
                    request.getAliquotParameters().getProjectPlan().getQuoteDTO(quoteService).getAlphanumericId()
            );

            bspStocks.add(aliquot);
        }

        BSPPlatingRequestResult result = doPlatingRequest(null, options, login, platingRequestName, bspStocks, controlWells, comments, seqTechnology, label);
        //TODO .. depending on the result..if BSP WR is atleast created.. build receipt
        if (result.getErrors() == null || result.getErrors().isEmpty()) {
            //BSP work request was created
            receipt = new BSPPlatingReceipt(result.getPlatingRequestReceipt());
            receipt.getPlatingRequests().addAll(requests);
        }
        return receipt;
    }
*/


}
