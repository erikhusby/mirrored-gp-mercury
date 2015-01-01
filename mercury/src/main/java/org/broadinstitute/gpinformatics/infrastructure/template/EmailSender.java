package org.broadinstitute.gpinformatics.infrastructure.template;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates the mechanism for sending emails.
 */
public class EmailSender implements Serializable {
    private static final long serialVersionUID = -905091780612758760L;

    private static final Log LOG = LogFactory.getLog(EmailSender.class);

    @Resource(mappedName = "java:/mail/broadsmtp")
    private Session mailSession;

    /**
     * Send an email in HTML format
     * @param appConfig The configuration for the deployed app.  This determines
     *                  whether email will actually be sent.
     * @param to address
     * @param subject subject line
     * @param body HTML
     */
    public void sendHtmlEmail(@Nonnull AppConfig appConfig, String to, String subject, String body) {
        if (appConfig.shouldSendEmail()) {
            if (mailSession != null) {
                try {
                    Message message = new MimeMessage(mailSession);
                    message.setFrom(new InternetAddress("gplims@broadinstitute.org"));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
                    message.setSubject(subject);
                    message.setContent(body, "text/html; charset=utf-8");
                    message.setSentDate(new Date());
                    Transport.send(message);
                } catch (MessagingException e) {
                    LOG.error("Failed to send email", e);
                    // Don't rethrow, not fatal
                }
            }
        }
    }

}
