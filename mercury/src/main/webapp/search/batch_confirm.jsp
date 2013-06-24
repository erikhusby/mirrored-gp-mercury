<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Creation Confirmation" sectionTitle="Search">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="form-horizontal">
            <div class="view-control-group control-group">
                <label class="control-label label-form">Batch Ticket</label>

                <div class="controls">
                    <div class="form-value">
                        <stripes:link target="JIRA" id="ticketName"
                                      href="${actionBean.batch.jiraTicket.browserUrl}"
                                      class="external">
                            ${actionBean.batch.jiraTicket.ticketName}
                        </stripes:link></div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Summary</label>

                <div class="controls">
                    <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.summary}</div>
                </div>
            </div>

            <c:if test="${not empty actionBean.batch.jiraTicket.jiraDetails.description}">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Description</label>

                    <div class="controls">
                        <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.description}</div>
                    </div>
                </div>
            </c:if>

            <c:if test="${not empty actionBean.batch.jiraTicket.jiraDetails.dueDate}">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Due Date</label>

                    <div class="controls">
                        <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.dueDate}</div>
                    </div>
                </div>
            </c:if>
        </div>
        <stripes:layout-render name="/vessel/vessel_list.jsp" vessels="${actionBean.batch.startingLabVessels}"
                               bean="${actionBean}" showCheckboxes="false"/>
    </stripes:layout-component>
</stripes:layout-render>
