<%--
JSP to allow upload of fingerprinting run.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadFingerprintingRunActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Fingerprinting Run Upload"
                       sectionTitle="Fingerprinting Run Upload" showCreate="false">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="fingerprintingForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="runFile" class="control-label">
                        Fingerprinting Run File
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="runFile" id="runFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload Run" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
        <c:if test="${!actionBean.hasErrors() && actionBean.labMetricRun != null}">
            Type: ${actionBean.labMetricRun.metricType.displayName}
            <br/>
            Run Date: <fmt:formatDate value="${actionBean.labMetricRun.runDate}" pattern="${actionBean.dateTimePattern}"/>
            <br/>
            Run Name: ${actionBean.labMetricRun.runName}
            <c:if test="${not empty actionBean.labMetricRun.metadata}">
                <table class="table simple" id="runTable">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Value</th>
                    </tr>
                    </thead>


                    <c:forEach items="${actionBean.labMetricRun.metadata}" var="metadata">
                        <tr>
                            <td>${metadata.key}</td>
                            <td>${metadata.value}</td>
                        </tr>
                    </c:forEach>
                </table>
            </c:if>
            <c:if test="${not empty actionBean.resultList.resultRows}">
                <div style="margin-top: 50px;">
                    <stripes:layout-render name="/columns/configurable_list.jsp"
                                           entityName="${actionBean.entityName}"
                                           sessionKey="${actionBean.sessionKey}"
                                           columnSetName="${actionBean.columnSetName}"
                                           downloadColumnSets="${actionBean.downloadColumnSets}"
                                           resultList="${actionBean.resultList}"
                                           action="${ctxpath}/search/ConfigurableSearch.action"
                                           downloadViewedColumns="true"
                                           isDbSortAllowed="False"
                                           dbSortPath=""
                                           dataTable="true"
                                           loadDatatable="false"
                                           showJumpToEnd="false"
                    />
                    <stripes:hidden name="labMetricRunId" value="${actionBean.labMetricRun.labMetricRunId}"/>
                </div>
            </c:if>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
