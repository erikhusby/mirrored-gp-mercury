package org.broadinstitute.gpinformatics.infrastructure.template;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Encapsulates the mechanism for sending emails.
 */
@Dependent
public class EmailSender implements Serializable {
    private static final long serialVersionUID = -905091780612758760L;

    private static final Log LOG = LogFactory.getLog(EmailSender.class);

    @Resource(mappedName = "java:jboss/mail/Default")
    private Session mailSession;

    /**
     * Send an email in HTML format
     * @param appConfig The configuration for the deployed app.  This determines
     *                  whether email will actually be sent.
     * @param to address
     * @param ccAddrdesses collection of email addresses which should also be CC'ed when the email is sent out.
     * @param subject subject line
     * @param body HTML
     * @param overrideForTest
     * @return null if not configured to send, false if there was a problem sending, or true if send succeeded.
     */
    public Boolean sendHtmlEmail(@Nonnull AppConfig appConfig, String to,
                              Collection<String> ccAddrdesses, String subject, String body, boolean overrideForTest) {
        if (appConfig.shouldSendEmail() || overrideForTest) {
            if (mailSession != null) {
                try {
                    Message message = new MimeMessage(mailSession);
                    message.setFrom(new InternetAddress("gplims@broadinstitute.org"));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
                    message.setRecipients(Message.RecipientType.CC,
                            InternetAddress.parse(StringUtils.join(ccAddrdesses, ",")));
                    message.setSubject(subject);
                    message.setContent(body, "text/html; charset=utf-8");
                    message.setSentDate(new Date());
                    Transport.send(message);
                    return true;

                } catch (MessagingException e) {
                    LOG.error("Failed to send email", e);
                    // Don't rethrow, not fatal
                }
            }
            return false;
        } else {
            return null;
        }
    }
}
