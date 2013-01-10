<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Creation Confirmation" sectionTitle="Search">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">


        <div class="view-control-group control-group">
            <label class="control-label label-form">Batch Name</label>

            <div class="controls">
                <div class="form-value">${actionBean.batch.batchName}</div>
            </div>
        </div>
        <div class="view-control-group control-group">
            <label class="control-label label-form">Summary</label>

            <div class="controls">
                <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.summary}</div>
            </div>
        </div>
        <div class="view-control-group control-group">
            <label class="control-label label-form">Description</label>

            <div class="controls">
                <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.description}</div>
            </div>
        </div>
        <div class="view-control-group control-group">
            <label class="control-label label-form">Due Date</label>

            <div class="controls">
                <div class="form-value">${actionBean.batch.jiraTicket.jiraDetails.dueDate}</div>
            </div>
        </div>

        <stripes:layout-render name="/search/vessel_list.jsp" vessels="${actionBean.batch.startingLabVesselsList}" bean="${actionBean}"/>

    </stripes:layout-component>
</stripes:layout-render>
