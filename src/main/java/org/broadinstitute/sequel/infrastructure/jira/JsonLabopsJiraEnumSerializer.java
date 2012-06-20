package org.broadinstitute.sequel.infrastructure.jira;

import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;


/**
 * When applied to an {@link Enum} with
 * {@link org.codehaus.jackson.map.annotate.JsonSerialize}, will convert any underscores
 * in the {@link Enum} instance name to a blank character during JSON serialization.
 *
 * Also, because labopsjira tickets seem to use a lot of parentheses, we have
 * to deal with this.  We do so by translating "OPENPAREN" to mean "(" and
 * "CLOSEPAREN" to mean ")" in the enum.
 *
 * Note this class does not perform deserialization, nor is there currently a need for that.
 *
 */
public class JsonLabopsJiraEnumSerializer extends JsonSerializer<CreateIssueRequest.Fields.Issuetype> {


    @Override
    /**
     * Replace all underscores in the value with a blank
     */
    public void serialize(CreateIssueRequest.Fields.Issuetype issueType, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeFieldName("name");
        jgen.writeString(issueType.getJiraName());
        jgen.writeEndObject();
    }



}
