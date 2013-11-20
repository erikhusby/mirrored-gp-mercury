<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Creation Confirmation" sectionTitle="Search">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#accordion").accordion({  collapsible:true, active:false, heightStyle:"content", autoHeight:false});
                $j("#accordion").show();
            });
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
        <div id="accordion" style="display:none;" class="accordion">
            <c:forEach items="${actionBean.batch.startingBatchLabVessels}" var="vessel" varStatus="status">
                <div style="padding-left: 30px;padding-bottom: 2px">
                    <stripes:layout-render name="/vessel/vessel_info_header.jsp" bean="${actionBean}"
                                           vessel="${vessel}"/>
                </div>

                <div id="vesselList-${vessel.labCentricName}">
                    <div>
                        <stripes:layout-render name="/vessel/vessel_sample_list.jsp" vessel="${vessel}"
                                               index="${status.count}" bean="${actionBean}"/>
                    </div>
                </div>
            </c:forEach>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
