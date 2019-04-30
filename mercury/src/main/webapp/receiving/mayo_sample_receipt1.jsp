<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt">
    <stripes:layout-component name="content">

        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <!-- The hidden variables to pass back to the action bean. -->
            <c:if test="${actionBean.vesselGeometry != null}">
                <stripes:hidden name="vesselGeometry" value="${actionBean.vesselGeometry}"/>
            </c:if>

            <!-- Captures the rack barcode. -->
            <div class="control-group" style="padding-left: 75px">
                Rack Barcode:
                <span style="padding-left: 10px">
                    <input type="text" id="rackBarcode" name="rackBarcode" value="${actionBean.rackBarcode}"/>
                </span>
            </div>

            <!-- Button to lookup the manifest file for the rack and display contents. -->
            <c:if test="${actionBean.getManifestCellGrid().isEmpty()}">
                <div style="padding-left: 200px">
                    <stripes:submit id="viewManifestBtn" name="viewManifestBtn" value="View The Manifest File"
                                    class="btn btn-primary"
                                    title="Click for a display of the manifest file that will be used for the rack."/>
                </div>
            </c:if>

            <!-- Manifest file contents. -->
            <c:if test="${!actionBean.getManifestCellGrid().isEmpty()}">
                Filename: ${actionBean.filename}
                <div style="padding-top: 10px;">
                    <table id="manifestCellGrid" border="2">
                        <tbody>
                        <c:forEach items="${actionBean.getManifestCellGrid()}" var="manifestRow">
                            <tr>
                                <c:forEach items="${manifestRow}" var="manifestColumn">
                                    <td align="center">${manifestColumn}</td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>

            <!-- Selectors for the rack scan lab & scanner, and the Scan button. -->
            <div style="padding-top: 20px;">
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

                <div class="controls">
                    <stripes:submit value="Rack Scan" id="scanBtn" name="scanBtn" class="btn btn-primary"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>