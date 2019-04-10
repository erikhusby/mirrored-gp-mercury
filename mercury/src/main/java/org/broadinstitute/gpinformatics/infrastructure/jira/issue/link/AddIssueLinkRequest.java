package org.broadinstitute.gpinformatics.infrastructure.jira.issue.link;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.broadinstitute.gpinformatics.infrastructure.jira.JsonLabopsJiraLinkTypeSerializer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment.GetCommentsResponse;

/**
 * @author Scott Matthews
 *         Date: 10/3/12
 *         Time: 2:51 PM
 */
@JsonPropertyOrder({"type", "inwardIssue", "outwardIssue","comment"})
public class AddIssueLinkRequest {

    private LinkType type;

    private IssueKey inwardIssue;

    private IssueKey outwardIssue;

    private GetCommentsResponse.Comment comment;

    public GetCommentsResponse.Comment getComment() {
        return comment;
    }

    public void setComment(GetCommentsResponse.Comment commentIn) {
        comment = commentIn;
    }

    public LinkType getType() {
        return type;
    }

    public void setType(LinkType typeIn) {
        type = typeIn;
    }

    public IssueKey getInwardIssue() {
        return inwardIssue;
    }

    public void setInwardIssue(IssueKey inwardIssueIn) {
        inwardIssue = inwardIssueIn;
    }

    public void addInwardIssue(String issueIn) {
        inwardIssue = new IssueKey(issueIn);
    }

    public IssueKey getOutwardIssue() {
        return outwardIssue;
    }

    public void setOutwardIssue(IssueKey outwardIssueIn) {
        outwardIssue = outwardIssueIn;
    }

    public void addOutwardIssue(String issueIn) {
        outwardIssue = new IssueKey(issueIn);
    }

    public static AddIssueLinkRequest create(LinkType type,String sourceIssueIn, String targetIssueIn) {
        AddIssueLinkRequest linkRequest = new AddIssueLinkRequest();
        linkRequest.setType(type);
        linkRequest.addInwardIssue(sourceIssueIn);
        linkRequest.addOutwardIssue(targetIssueIn);

        GetCommentsResponse.Comment comment = new GetCommentsResponse.Comment();
        comment.setBody("");

        return linkRequest;
    }

    public static AddIssueLinkRequest create(LinkType type,String sourceIssueIn, String targetIssueIn,
                                             String commentBody, Visibility.Type availabilityType,
                                             Visibility.Value availabilityValue) {
        AddIssueLinkRequest linkRequest = new AddIssueLinkRequest();
        linkRequest.setType(type);
        linkRequest.addInwardIssue(sourceIssueIn);
        linkRequest.addOutwardIssue(targetIssueIn);

        GetCommentsResponse.Comment comment = new GetCommentsResponse.Comment();
        comment.setBody(commentBody);
        comment.setVisibility(Visibility.create(availabilityType, availabilityValue));

        linkRequest.setComment(comment);

        return linkRequest;
    }

    @JsonSerialize(using = JsonLabopsJiraLinkTypeSerializer.class)
    public enum LinkType {

        Cloners("Cloners"),
        Duplicate("Duplicate"),
        Parentage("Parentage"),
        Peripheral("Peripheral"),
        Related("Related"),
        Rework("Rework");
        private String name;

        private LinkType(String nameIn) {
            name = nameIn;
        }

        public String getName() {
            return name;
        }

        public void setName(String nameIn) {
            name = nameIn;
        }
    }

    public class IssueKey {
        private String key;

        public IssueKey(String keyIn) {
            key = keyIn;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String keyIn) {
            key = keyIn;
        }
    }
}
