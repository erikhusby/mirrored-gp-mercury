package org.broadinstitute.pmbridge.infrastructure.gap;

import clover.org.apache.commons.lang.StringUtils;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.control.AbstractJerseyClientService;
import org.broadinstitute.pmbridge.entity.common.ChangeEvent;
import org.broadinstitute.pmbridge.entity.common.EntityUtils;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.RemoteId;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;
import org.broadinstitute.pmbridge.infrastructure.quote.Quote;
import org.broadinstitute.pmbridge.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.pmbridge.infrastructure.quote.QuoteServerException;
import org.broadinstitute.pmbridge.infrastructure.quote.QuoteService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:27 PM
 */
public class GenotypingServiceImpl  extends AbstractJerseyClientService implements GenotypingService {

    private org.apache.commons.logging.Log logger = LogFactory.getLog(GenotypingServiceImpl.class);
    @Inject private GapConnectionParameters gapConnectionParameters;

    @Inject private QuoteService quoteService;

    @Inject
    public GenotypingServiceImpl(GapConnectionParameters gapConnectionParameters) {
        this.gapConnectionParameters = gapConnectionParameters;
    }

    public GapExperimentRequest populateQuotes(GapExperimentRequest gapExperimentRequest,
                                               QuoteService quoteService) {
        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();

        if ( experimentPlan.getBspQuoteId() != null ) {
            Quote quoteBsp = lookupQuoteById(quoteService, experimentPlan.getBspQuoteId());
            gapExperimentRequest.setBspQuote(quoteBsp);
        }
        if ( experimentPlan.getGapQuoteId() != null ) {
            Quote quoteGap = lookupQuoteById(quoteService, experimentPlan.getGapQuoteId());
            gapExperimentRequest.setGapQuote(quoteGap);
        }
        return  gapExperimentRequest;
    }

    public GapExperimentRequest populateGapProduct(GapExperimentRequest gapExperimentRequest) {
        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();

        if (  experimentPlan.getProductId() != null ) {
            Product product=null;
            Integer gapProductId = experimentPlan.getProductId();
            try {
                product = lookupProductById( gapProductId );
            } catch (ProductNotFoundException e) {
                // Nothing to do here made an effort to translate the productId
                product = new Product("Unknown", "" + gapProductId);
                LogFactory.getLog(GapExperimentRequest.class).error("GAP productId " + gapProductId + " not found.", e );
            }
            gapExperimentRequest.setTechnologyProduct(product);
        }

        return  gapExperimentRequest;
    }


    private static Quote lookupQuoteById(final QuoteService quoteService, final String quoteStr) {
        Quote quote = null;
        String numericRegex = "[\\d]+";
        boolean quoteIsNumeric = Pattern.matches(numericRegex, quoteStr);
        try {
            if ( quoteIsNumeric ) {
                quote = quoteService.getQuoteByNumericId( quoteStr );
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
    public GapExperimentRequest getPlatformRequest(final  ExperimentRequestSummary experimentRequestSummary) {
        String expId = experimentRequestSummary.getRemoteId().value;
        GapExperimentRequest gapExperimentRequest = null;
        Response response = null;

        ExperimentPlan requiredExperimentPlan = new ExperimentPlan();
        requiredExperimentPlan.setId(expId);
        response = retrieveExperimentByGXPId(expId, requiredExperimentPlan);

        // Try to transform the GAP ExperimentPlan object into the GapExperimentRequest object
        if ((response != null) && (response.getExperimentPlans() != null)) {
            if (response.getExperimentPlans().size() > 0) {

                if (response.getExperimentPlans().size() > 1) {
                    logger.error("Expected 1 but received " + response.getExperimentPlans().size() +
                            " GAP experiment requests for experiment ID : " + expId + ". Ignoring remaining.");
                }

                ExperimentPlan experimentPlan = response.getExperimentPlans().get(0);
                gapExperimentRequest = new GapExperimentRequest(experimentRequestSummary, experimentPlan );

                gapExperimentRequest = populateQuotes(gapExperimentRequest, quoteService);

                gapExperimentRequest = populateGapProduct(gapExperimentRequest);

                //TODO hmc -Set a version string for Gap since they don't use a version number - use a date.

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
        if (( programMgr == null ) || (StringUtils.isBlank(programMgr.getUsername())) ) {
            throw new IllegalArgumentException("Username was blank. Need a non-null username to save an experiment request.");
        }
        if (( gapExperimentRequest == null ) || ( gapExperimentRequest.getExperimentRequestSummary() == null ) ||
                (StringUtils.isBlank( gapExperimentRequest.getExperimentRequestSummary().getTitle().name)) ) {
            throw new IllegalArgumentException("Title was blank. Need a non-null title to save an experiment request.");
        }

        return submitExperimentRequestToPlatform(programMgr, gapExperimentRequest, "DRAFT");
    }

    @Override
    public GapExperimentRequest submitExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException, SubmissionException {

        if (( programMgr == null ) || (StringUtils.isBlank(programMgr.getUsername())) ) {
            throw new IllegalArgumentException("Username was blank. Need a non-null username to submit an experiment request to GAP.");
        }
        if (( gapExperimentRequest == null ) || ( gapExperimentRequest.getExperimentRequestSummary() == null ) ||
                (StringUtils.isBlank( gapExperimentRequest.getExperimentRequestSummary().getTitle().name)) ) {
            throw new IllegalArgumentException("Title was blank. Need a non-null title to submit an experiment request to GAP.");
        }

        return submitExperimentRequestToPlatform(programMgr, gapExperimentRequest, "SUBMITTED");

    }

    private GapExperimentRequest submitExperimentRequestToPlatform(final Person programMgr,
                                                                   final GapExperimentRequest gapExperimentRequest,
                                                                   final String status ) {
        GapExperimentRequest submittedExperimentRequest = null;
        Response response = null;

        ExperimentPlan experimentPlan = gapExperimentRequest.getExperimentPlanDTO();
        experimentPlan.setPlanningStatus(status);
        experimentPlan.setUpdatedBy(programMgr.getUsername().trim());
        experimentPlan.setProgramPm(programMgr.getUsername().trim());

        try {
            String planXmlStr = ObjectMarshaller.marshall(experimentPlan);
            String baseUrl =  gapConnectionParameters.getUrl( GapConnectionParameters.GAP_CREATE_EXPERIMENT_URL );
            String fullUrl = baseUrl +  "?plan_data=" + encode( planXmlStr );
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource webResource = getJerseyClient().resource(fullUrl);

            try
            {
                MultivaluedMap formData = new MultivaluedMapImpl();
                formData.add("plan_data", planXmlStr);
                ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
                response = clientResponse.getEntity(Response.class);

                // Try to transform the GAP ExperimentPlan object into the GapExperimentRequest object
                if ((response != null) && (response.getExperimentPlans() != null)) {
                    boolean submissionSuccessful = (response.getExperimentPlans().size() == 1);
                    String expId = null;

                    if ( submissionSuccessful ) {
                        ExperimentPlan receivedExperimentPlan = response.getExperimentPlans().get(0);
                        expId = (receivedExperimentPlan != null) ? receivedExperimentPlan.getId() : "null";

                        ExperimentRequestSummary experimentRequestSummary = gapExperimentRequest.getExperimentRequestSummary();
                        experimentRequestSummary.setRemoteId( new RemoteId( expId ) );
                        experimentRequestSummary.setCreation( new ChangeEvent(receivedExperimentPlan.getDateCreated(),
                                new Person(receivedExperimentPlan.getCreatedBy(), RoleType.PROGRAM_PM)) );
                        experimentRequestSummary.setModification(new ChangeEvent( new Date(), programMgr));
                        experimentRequestSummary.setStatus( new Name(receivedExperimentPlan.getPlanningStatus()) );

                        submittedExperimentRequest = new GapExperimentRequest(experimentRequestSummary, receivedExperimentPlan );
                        submittedExperimentRequest = populateQuotes(submittedExperimentRequest, quoteService);
                        submittedExperimentRequest = populateGapProduct(submittedExperimentRequest);

                        //TODO hmc -Set a version string for Gap since they don't use a version number - use a date.

                    } else {
                        logger.error("Expected 1 but received " + response.getExperimentPlans().size() +
                                " GAP experiment requests for experimen titled : " +
                                gapExperimentRequest.getExperimentRequestSummary().getTitle() );
                        String gapErrMsg = generateSummaryFromResponse( response.getMessages(), submissionSuccessful );
                        logger.error(gapErrMsg);
                        throw new RuntimeException(gapErrMsg);
                    }
                }
            }
            catch(UniformInterfaceException e)
            {
                String errMsg = "Could not submit GAP experiment titled <" + gapExperimentRequest.getExperimentRequestSummary().getTitle() + "> for user " +
                        programMgr.getUsername();
                logger.error( errMsg + " at " + fullUrl );
                throw new RuntimeException( errMsg );
            }
            catch(ClientHandlerException e)
            {
                String errMsg = "Could not communicate with GAP server to submit experiment request titled <" +
                        gapExperimentRequest.getExperimentRequestSummary().getTitle() + "> for user " +
                        programMgr.getUsername();
                logger.error( errMsg + " at " + fullUrl );
                throw new RuntimeException( errMsg );
            }
        } catch (Exception exp) {
            if (exp.getMessage().contains("Exception occurred for user " + programMgr.getUsername() )) {
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
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person user) {

        List<ExperimentRequestSummary> experimentRequestSummaries = new ArrayList<ExperimentRequestSummary>();
        Response response = null;

        ExperimentPlan requiredExperimentPlan = new ExperimentPlan();
        requiredExperimentPlan.setProgramPm(user.getUsername());
        response = retrieveExperimentByUser(user, requiredExperimentPlan);

        if (response != null) {
            for (ExperimentPlan expPlan : response.getExperimentPlans()) {
                if (expPlan != null) {
                    ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary (
                            user, expPlan.getDateCreated(),
                            PlatformType.GAP, "");
                    experimentRequestSummary.setRemoteId( new RemoteId(expPlan.getId()) );
                    experimentRequestSummary.setCreation( new ChangeEvent( expPlan.getDateCreated(),
                            new Person( expPlan.getCreatedBy(), RoleType.PROGRAM_PM ) ) );
                    experimentRequestSummary.setModification(new ChangeEvent(expPlan.getProjectStartDate(),
                            new Person(expPlan.getUpdatedBy(), RoleType.PROGRAM_PM ) ) );

                    String status = expPlan.getPlanningStatus();
                    experimentRequestSummary.setStatus(new Name(status));

                    experimentRequestSummary.setResearchProjectId(new Long( expPlan.getResearchProjectId()));
                    experimentRequestSummary.setTitle(new Name(expPlan.getExperimentName()));
                    experimentRequestSummaries.add(experimentRequestSummary);
                }
            }
        }
        return experimentRequestSummaries;
    }

    private Response retrieveExperimentByUser(final Person user, final ExperimentPlan requiredExperimentPlan) {
        final Response response;
        try {

            String planXmlStr = ObjectMarshaller.marshall(requiredExperimentPlan);
            String baseUrl =  gapConnectionParameters.getUrl( GapConnectionParameters.GAP_EXPERIMENTS_URL );
            String fullUrl = baseUrl +  "?plan_data=" + encode( planXmlStr );
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource resource = getJerseyClient().resource(fullUrl);

            try
            {
                response = resource.accept(MediaType.APPLICATION_XML).get(Response.class);
            }
            catch(UniformInterfaceException e)
            {
                String errMsg = "Could not find GAP experiments for user " + user.getUsername();
                logger.error( errMsg + " at " + baseUrl );
                throw new RuntimeException( errMsg );
            }
            catch(ClientHandlerException e)
            {
                String errMsg = "Could not communicate with GAP server for user " + user.getUsername();
                logger.error( errMsg + " at " + baseUrl );
                throw new RuntimeException( errMsg );
            }

        } catch (Exception exp) {
            if (exp.getMessage().contains("No users found for name ")) {
                // Can ignore this exception as the user does not exist in in GAP.
                logger.info("User " + user.getUsername() + " not found in GAP. " + exp.getMessage());
            } else {
                logger.error("Exception occurred trying to retrieve experimentRequests from GAP for user " + user.getUsername(), exp);
            }
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for user : " + user.getUsername());
        }
        return response;
    }


    private Response retrieveExperimentByGXPId(final String name, final ExperimentPlan requiredExperimentPlan) {
        final Response response;
        try {

            String planXmlStr = ObjectMarshaller.marshall(requiredExperimentPlan);
            String baseUrl =  gapConnectionParameters.getUrl( GapConnectionParameters.GAP_EXPERIMENTS_URL );
            String fullUrl = baseUrl +  "?plan_data=" + encode( planXmlStr );
            logger.info(String.format("url string is '%s'", fullUrl));

            WebResource resource = getJerseyClient().resource(fullUrl);

            try
            {
                response = resource.accept(MediaType.APPLICATION_XML).get(Response.class);
            }
            catch(UniformInterfaceException e)
            {
                String errMsg = "Could not find GAP experiments for " + name;
                logger.error( errMsg + " at " + baseUrl );
                throw new RuntimeException( errMsg , e);
            }
            catch(ClientHandlerException e)
            {
                String errMsg = "Could not communicate with GAP server for " + name;
                logger.error( errMsg + " at " + baseUrl );
                throw new RuntimeException( errMsg , e );
            }

        } catch (Exception exp) {
            logger.error("Exception occurred trying to retrieve experimentRequests from GAP for " + name, exp);
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for : " + name);
        }
        return response;
    }


    @Override
    public Product lookupProductById(final Integer productId) throws ProductNotFoundException {
        final Product result;

        String url =  gapConnectionParameters.getUrl( GapConnectionParameters.GAP_TECHNOLOGIES_URL );
        logger.info(String.format("url string is '%s'", url));
        WebResource resource = getJerseyClient().resource(url);

        try {
            Platforms platforms = resource.accept(MediaType.APPLICATION_XML).get(Platforms.class);
            // Traverse the platform tree of products to find a match.
            // The lists of platforms and products are pretty small lists.
            if ((platforms != null) && (platforms.getPlatforms() != null) && platforms.getPlatforms().size() > 0 ) {
                for ( Platform platform : platforms.getPlatforms() ) {
                    if (( platform != null ) && (platform.getProducts() != null )
                            && (platform.getProducts().getProducts() != null) &&  (platform.getProducts().getProducts().size() >0) ) {
                        for ( Product product : platform.getProducts().getProducts() ) {
                            if (( product != null ) && (product.getId() != null) && product.getId().trim().equals("" + productId.intValue()) ) {
                                result = product;
                                return result;
                            }
                        }
                    }
                }
            }
        } catch(UniformInterfaceException e) {
            String errMsg = "Could not find GAP technologies for product id : " + productId.intValue();
            logger.error( errMsg + " at " + url );
            throw new RuntimeException( errMsg , e);
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with GAP server for technology chip product id " + productId.intValue();
            logger.error( errMsg + " at " + url );
            throw new RuntimeException( errMsg , e );
        } catch (Exception exp) {
            String errMsg = "Exception occurred trying to retrieve GAP technology for product id : " + productId.intValue();
            logger.error(errMsg + " at " + url , exp);
            throw new RuntimeException( errMsg , exp);
        }

        String errMsg = "Could not find GAP technologies for product id : " + productId.intValue();
        logger.error( errMsg + " at " + url );
        throw new ProductNotFoundException( errMsg );
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
            if (msg.getType().equals(messageType)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private String getMessagesOfType(MessageType messageType, List<Message> messages) {
        StringBuffer resultBuffer = new StringBuffer();
        for (Message msg : messages) {
            if (msg.getType().equals(messageType)) {
                String prefix = msg.getField();
                if (org.apache.commons.lang.StringUtils.isNotBlank(prefix)) {
                    resultBuffer.append(prefix).append(" - ");
                }
                resultBuffer.append(msg.getMessage() + "\n");
            }
        }
        return resultBuffer.toString();
    }



}



