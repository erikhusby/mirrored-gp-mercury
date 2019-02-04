<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Manifest Display" sectionTitle="Mayo Manifest Display">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="manifestShowForm" class="form-horizontal">
            <!-- The hidden variables to return to the action bean. -->
            <stripes:hidden name="packageBarcode" value="${actionBean.packageBarcode}"/>
            <stripes:hidden name="rackBarcode" value="${actionBean.rackBarcode}"/>
            <stripes:hidden name="vesselGeometry" value="${actionBean.vesselGeometry}"/>

            <p>Filename: ${actionBean.filename}</p>

            <!-- Displays each sheet of the manifest spreadsheet as cell data in a table. -->
            <c:forEach items="${actionBean.manifestSheetnames}" var="sheet" varStatus="item">
                <div style="padding-top: 10px;">
                    <p>${sheet} Sheet:</p>
                    <table id="${sheet}" border="2">
                        <tbody>
                        <c:forEach items="${actionBean.getManifestArray(sheet)}" var="manifestRow">
                            <tr>
                                <c:forEach items="${manifestRow}" var="manifestColumn">
                                    <td align="center">${manifestColumn}</td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:forEach>

            <!-- The Back button. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="backBtn" name="backBtn" value="Back" class="btn btn-primary"
                                title="Click to return to the samples receipt page."/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>