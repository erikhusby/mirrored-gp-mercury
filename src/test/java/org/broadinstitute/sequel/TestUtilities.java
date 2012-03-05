package org.broadinstitute.sequel;

import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.easymock.EasyMock;

/**
 * Utility methods shared by tests
 */
public class TestUtilities {
    public JiraTicket createMockJiraTicket() {
        JiraTicket ticket = EasyMock.createMock(JiraTicket.class);
        EasyMock.expect(ticket.addComment((String)EasyMock.anyObject())).andReturn(JiraTicket.JiraResponse.OK).atLeastOnce();
        EasyMock.replay(ticket);
        return ticket;
    }
}
