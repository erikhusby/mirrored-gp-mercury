<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt">
    <stripes:layout-component name="content">
        <style type="text/css">
            p, th, td {
                font-size: 12px;
                font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">
            <!-- The hidden variables to pass back to the action bean. -->
            <stripes:hidden name="rackBarcode" value="${actionBean.rackBarcode}"/>
            <c:forEach items="${actionBean.rackScanEntries}" var="mapEntry" varStatus="item">
                <stripes:hidden name="rackScanEntries[${item.index}]" value="${mapEntry}"/>
            </c:forEach>

            <!-- Displays the scan result as a table of positions. -->
            <p>The tubes in rack ${actionBean.rackBarcode}</p>

            <table id="samplesTable" border="2">
                <thead>
                <tr>
                    <th width="20"></th>
                    <c:forEach items="${actionBean.rackColumns}" var="rackColumn">
                        <th width="180">${rackColumn}</th>
                    </c:forEach>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.rackRows}" var="rackRow">
                    <tr>
                        <td><b>${rackRow}</b></td>
                        <c:forEach items="${actionBean.rackColumns}" var="rackColumn">
                            <td>${actionBean.getSampleAt(rackRow, rackColumn)}</td>
                        </c:forEach>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div style="padding-top: 20px;">
                <span>
                    <c:if test="${!actionBean.messageCollection.hasErrors()}">
                        <stripes:submit id="saveBtn" name="saveBtn" value="Save" class="btn btn-primary"
                                        title="Accessions the rack, tubes, and samples."/>
                    </c:if>
                    <span style="margin-left: 20px;">
                        <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>