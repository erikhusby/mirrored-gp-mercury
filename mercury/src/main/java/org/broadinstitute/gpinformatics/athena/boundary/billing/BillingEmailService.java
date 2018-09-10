/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class BillingEmailService {
    private AppConfig appConfig;
    private SapConfig sapConfig;
    private EmailSender emailSender;
    private TemplateEngine templateEngine;

    public BillingEmailService() {
    }

    @Inject
    public BillingEmailService(AppConfig appConfig, SapConfig sapConfig, EmailSender emailSender,
                               TemplateEngine templateEngine) {
        this.appConfig = appConfig;
        this.sapConfig = sapConfig;
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    public void sendReverseBillingEmail(QuoteImportItem quoteImportItem, Set<LedgerEntry> priorBillings) throws
        InformaticsServiceException {
        Map<String, Object> rootMap = new HashMap<>();

        String sapDocuments = priorBillings.stream()
            .map(LedgerEntry::getSapDeliveryDocumentId)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("<br/>"));

        rootMap.put("mercuryOrder", quoteImportItem.getProductOrder().getJiraTicketKey());
        rootMap.put("sapOrderNumber", quoteImportItem.getProductOrder().getSapOrderNumber());
        rootMap.put("sapDeliveryDocuments", sapDocuments);
        rootMap.put("quantity", quoteImportItem.getQuantity());

        String body;
        try {
            body = processTemplate(SapConfig.BILLING_REVERSAL_TEMPLATE, rootMap);
        } catch (RuntimeException e) {
            throw new InformaticsServiceException("Invalid reference in map", e);
        }
        emailSender.sendHtmlEmail(appConfig, sapConfig.getSapSupportEmail(), Collections.emptyList(),
                sapConfig.getSapReverseBillingSubject(), body, true, false);
    }

    protected String processTemplate(String template, Map<String, Object> objectMap) {
        StringWriter stringWriter = new StringWriter();
        templateEngine.processTemplate(template, objectMap, stringWriter);
        return stringWriter.toString();
    }

}
