package org.broadinstitute.gpinformatics.infrastructure.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.Writer;

/**
 * Encapsulates a template engine, e.g. for generating customized emails.
 * The engine is currently Freemarker.
 */
@ApplicationScoped
public class TemplateEngine {
    /** Should be initialized only once, hence application scope */
    private Configuration configuration = new Configuration();

    @PostConstruct
    public void postConstruct() {
        configuration.setClassForTemplateLoading(getClass(), "/templates");
    }

    /**
     * Combines a template and parameter objects, sending results to a writer.
     * @param templateName name of Freemarker template
     * @param mapNameToObject names referenced in expressions in the template, and the object each name refers to
     * @param writer results of populating template with objects
     */
    public <T> void processTemplate(String templateName, T mapNameToObject, Writer writer) {
        try {
            Template template = configuration.getTemplate(templateName);
            template.process(mapNameToObject, writer);
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
