package org.broadinstitute.gpinformatics.infrastructure.gap;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentId;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.athena.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:27 PM
 */
@Default
public class GenotypingServiceImpl extends AbstractJerseyClientService implements GenotypingService {

    private org.apache.commons.logging.Log logger = LogFactory.getLog(GenotypingServiceImpl.class);
    @Inject
    private GapConnectionParameters gapConnectionParameters;

    @Inject
    private QuoteService quoteService;

    @Inject
    public GenotypingServiceImpl(GapConnectionParameters gapConnectionParameters) {
        this.gapConnectionParameters = gapConnectionParameters;
    }

    public GapExperimentRequest populateQuotes(GapExperimentRequest gapExperimentRequest,
                                               QuoteService quoteService) {
        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();

        if (experimentPlan.getBspQuoteId() != null) {
            Quote quoteBsp = lookupQuoteById(quoteService, experimentPlan.getBspQuoteId());
            gapExperimentRequest.setBspQuote(quoteBsp);
        }
        if (experimentPlan.getGapQuoteId() != null) {
            Quote quoteGap = lookupQuoteById(quoteService, experimentPlan.getGapQuoteId());
            gapExperimentRequest.setGapQuote(quoteGap);
        }
        return gapExperimentRequest;
    }

    public GapExperimentRequest populateGapProduct(GapExperimentRequest gapExperimentRequest) {
        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();

        if (experimentPlan.getProductName() != null) {
            gapExperimentRequest.setTechnologyProduct(new Product(experimentPlan.getProductName()));
        }

        return gapExperimentRequest;
    }


    private static Quote lookupQuoteById(final QuoteService quoteService, final String quoteStr) {
        Quote quote;
        String numericRegex = "[\\d]+";
        boolean quoteIsNumeric = Pattern.matches(numericRegex, quoteStr);
        try {
            if (quoteIsNumeric) {
                quote = quoteService.getQuoteByNumericId(quoteStr);
            } else {
                quote = quoteService.getQuoteByAlphaId(quoteStr);
            }
        } catch (QuoteServerException e) {
            String errSubMsg = (quoteIsNumeric) ? " numeric " : " alphaNumeric ";
            throw new RuntimeException("Could not communicate with quoteServer for quote Id " + quoteStr + " by " + errSubMsg + " id.");
        } catch (QuoteNotFoundException e) {
            String errSubMsg = (quoteIsNumeric) ? " numeric " : " alphaNumeric ";
            throw new RuntimeException("Could not find quote Id " + quoteStr + " by " + errSubMsg + " id.");
        }
        return quote;
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        acceptAllServerCertificates(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, gapConnectionParameters);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public GapExperimentRequest getPlatformRequest(final ExperimentRequestSummary experimentRequestSummary) {
        String expId = experimentRequestSummary.getExperimentId().value;
        GapExperimentRequest gapExperimentRequest = null;

        ExperimentPlan requiredExperimentPlan = new ExperimentPlan();
        requiredExperimentPlan.setId(expId);
        Response response = retrieveExperimentByGXPId(expId, requiredExperimentPlan);

        // Try to transform the GAP ExperimentPlan object into the GapExperimentRequest object
        if ((response != null) && (response.getExperimentPlans() != null)) {
            if (response.getExperimentPlans().size() > 0) {

                if (response.getExperimentPlans().size() > 1) {
                    logger.error("Expected 1 but received " + response.getExperimentPlans().size() +
                            " GAP experiment requests for experiment ID : " + expId + ". Ignoring remaining.");
                }

                ExperimentPlan experimentPlan = response.getExperimentPlans().get(0);
                gapExperimentRequest = new GapExperimentRequest(experimentRequestSummary, experimentPlan);

                gapExperimentRequest = populateQuotes(gapExperimentRequest, quoteService);

                gapExperimentRequest = populateGapProduct(gapExperimentRequest);

            } else {
                String errMsg = "Expected 1 but no GAP experiment request data retrieved from GAP for experiment ID : "
                        + expId;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
        }

        return gapExperimentRequest;
    }

    @Override
    public GapExperimentRequest saveExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException, SubmissionException {
        if ((programMgr == null) || (StringUtils.isBlank(programMgr.getUsername()))) {
            throw new IllegalArgumentException("Username was blank. Need a non-null username to save an experiment request.");
        }
        if ((gapExperimentRequest == null) || (gapExperimentRequest.getExperimentRequestSummary() == null) ||
                (StringUtils.isBlank(gapExperimentRequest.getExperimentRequestSummary().getTitle().name))) {
            throw new IllegalArgumentException("Title was blank. Need a non-null title to save an experiment request.");
        }

        return submitExperimentRequestToPlatform(programMgr, gapExperimentRequest, "DRAFT");
    }

    @Override
    public GapExperimentRequest submitExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException, SubmissionException {

        if ((programMgr == null) || (StringUtils.isBlank(programMgr.getUsername()))) {
            throw new IllegalArgumentException("Username was blank. Need a non-null username to submit an experiment request to GAP.");
        }
        if ((gapExperimentRequest == null) || (gapExperimentRequest.getExperimentRequestSummary() == null) ||
                (StringUtils.isBlank(gapExperimentRequest.getExperimentRequestSummary().getTitle().name))) {
            throw new IllegalArgumentException("Title was blank. Need a non-null title to submit an experiment request to GAP.");
        }

        return submitExperimentRequestToPlatform(programMgr, gapExperimentRequest, "SUBMITTED");

    }

    private GapExperimentRequest submitExperimentRequestToPlatform(final Person programMgr,
                                                                   final GapExperimentRequest gapExperimentRequest,
                                                                   final String status) {
        GapExperimentRequest submittedExperimentRequest = null;

        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();
        experimentPlan.setPlanningStatus(status);
        experimentPlan.setUpdatedBy(programMgr.getUsername().trim());
        experimentPlan.setProgramPm(programMgr.getUsername().trim());

        try {
            String planXmlStr = ObjectMarshaller.marshall(experimentPlan);
            String baseUrl = gapConnectionParameters.getUrl(GapConnectionParameters.GAP_CREATE_EXPERIMENT_URL);
            String fullUrl = baseUrl + "?plan_data=" + encode(planXmlStr);
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource webResource = getJerseyClient().resource(fullUrl);

            try {
                MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
                formData.add("plan_data", planXmlStr);
                ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
                Response response = clientResponse.getEntity(Response.class);

                // Try to transform the GAP ExperimentPlan object into the GapExperimentRequest object
                if ((response != null) && (response.getExperimentPlans() != null)) {
                    boolean submissionSuccessful = (response.getExperimentPlans().size() == 1);

                    if (submissionSuccessful) {
                        ExperimentPlan receivedExperimentPlan = response.getExperimentPlans().get(0);
                        String expId = (receivedExperimentPlan != null) ? receivedExperimentPlan.getId() : "null";

                        ExperimentRequestSummary experimentRequestSummary = gapExperimentRequest.getExperimentRequestSummary();
                        experimentRequestSummary.setExperimentId(new ExperimentId(expId));
//                        experimentRequestSummary.setCreation(new ChangeEvent(receivedExperimentPlan.getDateCreated(),
//                                new Person(receivedExperimentPlan.getCreatedBy(), RoleType.PROGRAM_PM)));
                        experimentRequestSummary.setModification(new ChangeEvent(new Date(), programMgr));
                        experimentRequestSummary.setStatus(new Name(receivedExperimentPlan.getPlanningStatus()));

                        submittedExperimentRequest = new GapExperimentRequest(experimentRequestSummary, receivedExperimentPlan);
                        submittedExperimentRequest = populateQuotes(submittedExperimentRequest, quoteService);
                        submittedExperimentRequest = populateGapProduct(submittedExperimentRequest);

                    } else {
                        logger.error("Expected 1 but received " + response.getExperimentPlans().size() +
                                " GAP experiment requests for experiment titled : " +
                                gapExperimentRequest.getExperimentRequestSummary().getTitle());
                        String gapErrMsg = generateSummaryFromResponse(response.getMessages(), submissionSuccessful);
                        logger.error(gapErrMsg);
                        throw new RuntimeException(gapErrMsg);
                    }
                }
            } catch (UniformInterfaceException e) {
                String errMsg = "Could not submit GAP experiment titled <" + gapExperimentRequest.getExperimentRequestSummary().getTitle() + "> for user " +
                        programMgr.getUsername();
                logger.error(errMsg + " at " + fullUrl);
                throw new RuntimeException(errMsg);
            } catch (ClientHandlerException e) {
                String errMsg = "Could not communicate with GAP server to submit experiment request titled <" +
                        gapExperimentRequest.getExperimentRequestSummary().getTitle() + "> for user " +
                        programMgr.getUsername();
                logger.error(errMsg + " at " + fullUrl);
                throw new RuntimeException(errMsg);
            }
        } catch (Exception exp) {
            if (exp.getMessage().contains("Exception occurred for user " + programMgr.getUsername())) {
                // Can ignore this exception as the user does not exist in in GAP.
                logger.info("User " + programMgr.getUsername() + " not found in GAP. " + exp.getMessage());
            } else {
                logger.error("Exception occurred trying to retrieve experimentRequests from GAP for user " + programMgr.getUsername(), exp);
            }
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for user : " + programMgr.getUsername());
        }
        return submittedExperimentRequest;
    }

    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person user) throws UserNotFoundException {

        List<ExperimentRequestSummary> experimentRequestSummaries = new ArrayList<ExperimentRequestSummary>();

        ExperimentPlan requiredExperimentPlan = new ExperimentPlan();
        requiredExperimentPlan.setProgramPm(user.getUsername());
        Response response = retrieveExperimentByUser(user, requiredExperimentPlan);

        if (response != null) {
            for (ExperimentPlan expPlan : response.getExperimentPlans()) {
                if (expPlan != null) {
                    ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary(
                            expPlan.getExperimentName(), user, expPlan.getDateCreated(),
                            ExperimentType.Genotyping);
                    experimentRequestSummary.setExperimentId(new ExperimentId(expPlan.getId()));
//                    experimentRequestSummary.setCreation(new ChangeEvent(expPlan.getDateCreated(),
//                            new Person(expPlan.getCreatedBy(), RoleType.PROGRAM_PM)));
                    experimentRequestSummary.setModification(new ChangeEvent(expPlan.getProjectStartDate(),
                            new Person(expPlan.getUpdatedBy(), RoleType.PROGRAM_PM)));

                    String status = expPlan.getPlanningStatus();
                    experimentRequestSummary.setStatus(new Name(status));

                    experimentRequestSummary.setResearchProjectId(new Long(expPlan.getResearchProjectId()));
                    experimentRequestSummary.setTitle(new Name(expPlan.getExperimentName()));
                    experimentRequestSummaries.add(experimentRequestSummary);
                }
            }
        }
        return experimentRequestSummaries;
    }

    private Response retrieveExperimentByUser(final Person user, final ExperimentPlan requiredExperimentPlan) throws
            UserNotFoundException {
        final Response response;

        try {
            String planXmlStr = ObjectMarshaller.marshall(requiredExperimentPlan);
            String baseUrl = gapConnectionParameters.getUrl(GapConnectionParameters.GAP_EXPERIMENTS_URL);
            String fullUrl = baseUrl + "?plan_data=" + encode(planXmlStr);
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource resource = getJerseyClient().resource(fullUrl);

            try {
                response = resource.accept(MediaType.APPLICATION_XML).get(Response.class);
            } catch (UniformInterfaceException e) {
                String errMsg;
                ClientResponse clientResponse = e.getResponse();
                Response errResponse = clientResponse.getEntity( Response.class );
                errMsg = getMessagesFromResponse( errResponse );
                if (isNotBlank(errMsg)) {
                    logger.info(errMsg + " at " + baseUrl);
                    throw new UserNotFoundException(errMsg);
                } else {
                    errMsg = "Exception occurred trying to retrieve experimentRequests from GAP for user " + user.getUsername();
                    logger.error(errMsg + " at " + baseUrl, e);
                    throw new RuntimeException(errMsg);
                }
            } catch (ClientHandlerException e) {
                String errMsg = "Could not communicate with GAP server for user " + user.getUsername();
                logger.error(errMsg + " at " + baseUrl, e);
                throw new RuntimeException(errMsg);
            }
        }
        catch (UnsupportedEncodingException exp) {
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for user : " + user.getUsername(), exp);
        }
        return response;
    }

    private String getMessagesFromResponse(final Response response) {
        StringBuilder stringBuilder = new StringBuilder();
        if ((response != null) && (response.getMessages() != null) && (response.getMessages().getMessages() != null) ) {
            for (Message message : response.getMessages().getMessages() ) {
                stringBuilder.append( message.toString() ).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private Response retrieveExperimentByGXPId(final String name, final ExperimentPlan requiredExperimentPlan) {
        final Response response;
        try {

            String planXmlStr = ObjectMarshaller.marshall(requiredExperimentPlan);
            String baseUrl = gapConnectionParameters.getUrl(GapConnectionParameters.GAP_EXPERIMENTS_URL);
            String fullUrl = baseUrl + "?plan_data=" + encode(planXmlStr);
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource resource = getJerseyClient().resource(fullUrl);

            try {
                response = resource.accept(MediaType.APPLICATION_XML).get(Response.class);
            } catch (UniformInterfaceException e) {
                String errMsg = "Could not find GAP experiments for " + name;
                logger.error(errMsg + " at " + baseUrl);
                throw new RuntimeException(errMsg, e);
            } catch (ClientHandlerException e) {
                String errMsg = "Could not communicate with GAP server for " + name;
                logger.error(errMsg + " at " + baseUrl);
                throw new RuntimeException(errMsg, e);
            }

        } catch (Exception exp) {
            logger.error("Exception occurred trying to retrieve experimentRequests from GAP for " + name, exp);
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for : " + name);
        }
        return response;
    }


    @Override
    public Platforms getPlatforms() {
        Platforms platforms;

        String url = gapConnectionParameters.getUrl(GapConnectionParameters.GAP_TECHNOLOGIES_URL);
        logger.info(String.format("url string is '%s'", url));

        try {
            WebResource resource = getJerseyClient().resource(url);
            platforms = resource.accept(MediaType.APPLICATION_XML).get(Platforms.class);
        } catch (UniformInterfaceException e) {
            String errMsg = "Could not find any GAP Platforms";
            logger.error(errMsg + " at " + url);
            throw new RuntimeException(errMsg, e);
        } catch (ClientHandlerException e) {
            String errMsg = "Could not communicate with GAP server to retrieve GAP Platforms";
            logger.error(errMsg + " at " + url);
            throw new RuntimeException(errMsg, e);
        } catch (Exception exp) {
            String errMsg = "Exception occurred trying to retrieve to retrieve GAP Platforms";
            logger.error(errMsg + " at " + url, exp);
            throw new RuntimeException(errMsg, exp);
        }

        return platforms;
    }

    private String encode(String valueStr) throws UnsupportedEncodingException {
        if (valueStr != null) {
            return URLEncoder.encode(valueStr, "UTF-8");
        } else {
            return null;
        }
    }


    private String generateSummaryFromResponse(Messages messages, boolean experimentSubmitted) {
        StringBuilder summarizedResponse = new StringBuilder();

        List<Message> messageList = messages.getMessages();
        if (experimentSubmitted) {
            summarizedResponse.append("Successful submission to GAP");
        } else {
            summarizedResponse.append("Failure occurred while submitting experiment to GAP.");
        }

        if ((messageList != null) && (messageList.size() > 0)) {
            boolean errorsReceived = anyMessagesOfType(MessageType.ERROR, messageList);
            boolean warningsReceived = anyMessagesOfType(MessageType.WARNING, messageList);
            boolean infoReceived = anyMessagesOfType(MessageType.INFO, messageList);

            String errorsStr = (errorsReceived ? "Errors: \n" + getMessagesOfType(MessageType.ERROR, messageList) + "\n" : "");
            String warningsStr = (warningsReceived ? "Warnings: \n" + getMessagesOfType(MessageType.WARNING, messageList) + "\n" : "");
            String infosStr = (infoReceived ? "Alerts: \n" + getMessagesOfType(MessageType.INFO, messageList) + "\n" : "");

            if (experimentSubmitted) {
                if (errorsReceived) {
                    summarizedResponse.append(" but with the following errors.\n");
                    summarizedResponse.append(errorsStr);
                } else {
                    summarizedResponse.append(".\n");
                }
                if (warningsReceived || infoReceived) {
                    summarizedResponse.append("With the following response.\n");
                    summarizedResponse.append(warningsStr);
                    summarizedResponse.append(infosStr);
                }
            } else {
                if (errorsReceived || warningsReceived || infoReceived) {
                    summarizedResponse.append("With the following response.\n");
                    summarizedResponse.append(errorsStr).append(warningsStr).append(infosStr);
                }
            }
        }

        return summarizedResponse.toString();
    }

    private boolean anyMessagesOfType(MessageType messageType, List<Message> messages) {
        boolean result = false;
        for (Message msg : messages) {
            if (msg.getType() == messageType) {
                result = true;
                break;
            }
        }
        return result;
    }

    private String getMessagesOfType(MessageType messageType, List<Message> messages) {
        StringBuilder resultBuffer = new StringBuilder();
        for (Message msg : messages) {
            if (msg.getType() == messageType) {
                String prefix = msg.getField();
                if (isNotBlank(prefix)) {
                    resultBuffer.append(prefix).append(" - ");
                }
                resultBuffer.append(msg.getMessage()).append("\n");
            }
        }
        return resultBuffer.toString();
    }


}



