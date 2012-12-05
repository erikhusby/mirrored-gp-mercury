package org.broadinstitute.gpinformatics.infrastructure.gap;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.infrastructure.UserNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.experiments.ExperimentId;
import org.broadinstitute.gpinformatics.infrastructure.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.infrastructure.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:27 PM
 */
@Impl
public class GenotypingServiceImpl extends AbstractJerseyClientService implements GenotypingService {


    enum Endpoint {

        PLATFORMS("get_experiment_platforms"),
        USERS("auth/get_users_by_role?role_name=Project%20Manager&domain=GAP"),
        EXPERIMENTS("get_experiment_plan"),
        TECHNOLOGIES("get_experiment_platforms"),
        CREATE_EXPERIMENT("create_experiment_plan");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }
    }


    private String url( Endpoint endpoint ) {

        return gapConfig.getUrl() + "/ws/project_management/" + endpoint.getSuffixUrl();
    }


    private org.apache.commons.logging.Log logger = LogFactory.getLog(GenotypingServiceImpl.class);


    @Inject
    private GAPConfig gapConfig;

    @Inject
    private QuoteService quoteService;

    public GenotypingServiceImpl() {}


    public GenotypingServiceImpl( GAPConfig gapConfig ) {
        this.gapConfig = gapConfig;
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
        specifyHttpAuthCredentials(client, gapConfig);
        forceResponseMimeTypes(client, MediaType.APPLICATION_XML_TYPE);
    }



    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final String user) throws UserNotFoundException {

        List<ExperimentRequestSummary> experimentRequestSummaries = new ArrayList<ExperimentRequestSummary>();

        ExperimentPlan requiredExperimentPlan = new ExperimentPlan();
        requiredExperimentPlan.setProgramPm(user);
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
                            expPlan.getUpdatedBy()));

                    String status = expPlan.getPlanningStatus();
                    experimentRequestSummary.setStatus(status);

                    experimentRequestSummary.setResearchProjectId(expPlan.getResearchProjectId());
                    experimentRequestSummary.setTitle(expPlan.getExperimentName());
                    experimentRequestSummaries.add(experimentRequestSummary);
                }
            }
        }
        return experimentRequestSummaries;
    }

    private Response retrieveExperimentByUser(final String user, final ExperimentPlan requiredExperimentPlan) throws
            UserNotFoundException {
        final Response response;

        try {
            String planXmlStr = ObjectMarshaller.marshall(requiredExperimentPlan);
            String baseUrl = url(Endpoint.EXPERIMENTS);
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
                    errMsg = "Exception occurred trying to retrieve experimentRequests from GAP for user " + user;
                    logger.error(errMsg + " at " + baseUrl, e);
                    throw new RuntimeException(errMsg);
                }
            } catch (ClientHandlerException e) {
                String errMsg = "Could not communicate with GAP server for user " + user;
                logger.error(errMsg + " at " + baseUrl, e);
                throw new RuntimeException(errMsg);
            }
        }
        catch (UnsupportedEncodingException exp) {
            throw new RuntimeException("Problem retrieving Gap Experiments from GAP for user : " + user, exp);
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
            String baseUrl = url(Endpoint.EXPERIMENTS);
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

        String url = url( Endpoint.TECHNOLOGIES );
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



