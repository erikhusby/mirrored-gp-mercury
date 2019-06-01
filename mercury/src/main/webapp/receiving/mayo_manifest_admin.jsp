<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Manifest Admin" sectionTitle="Mayo Manifest Admin">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="manifestAdminForm" class="form-horizontal">
            <!-- Tests access to manifest file storage. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="testAccessBtn" name="testAccessBtn" value="Test Bucket Access"
                                class="btn btn-primary"
                                title="Logs the status of step-by-step access to the configured bucket storage."/>

                <!-- After doing the test access, a list of files is obtained and can be downloaded as a txt file. -->
                <c:if test="${fn:length(actionBean.bucketList) > 0}">
                    <c:forEach items="${actionBean.bucketList}" var="filename" varStatus="item">
                        <stripes:hidden name="bucketList[${item.index}]" value="${filename}"/>
                    </c:forEach>

                    <span style="margin-left: 20px;">
                        <stripes:submit id="showBucketListBtn" name="showBucketListBtn" value="Show Filename List"
                                        class="btn btn-primary"
                                        title="Provides a download txt file of all filenames in the bucket."/>
                    </span>
                </c:if>
            </div>

            <!-- Shows files that were read in but were not made into new manifests. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="showFailedFilesListBtn" name="showFailedFilesListBtn" value="Show Failed Files"
                                class="btn btn-primary"
                                title="Makes a download txt file of any filenames that were read in and their content was not a valid manifest."/>
            </div>

            <!-- Loads all new files and makes new manifests out of the valid ones with new package barcodes. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="pullAllFilesBtn" name="pullAllFilesBtn" value="Pull All New Manifest Files"
                                class="btn btn-primary"
                                title="Finds any new manifest files and saves them in Mercury."/>
            </div>

            <!-- Loads the specified file and makes a new manifest if valid and with a new package barcode. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="pullFileBtn" name="pullFileBtn" value="Pull One Manifest File"
                                class="btn btn-primary"
                                title="Reads or re-reads the specified manifest file and saves or updates it in Mercury."/>
                <span style="margin-left: 20px;">
                    Filename:
                    <stripes:text id="filename" name="filename"/>
                </span>
            </div>

            <!-- Displays the contents of a manifest file using the filename. -->
            <div style="padding-top: 20px;">
                <span>
                    <stripes:submit id="viewFileBtn" name="viewFileBtn" value="View A Manifest File"
                                    class="btn btn-primary"
                                    title="Click to display contents of a manifest file."/>
                </span>
            </div>

            <!-- Manifest file contents. -->
            <c:if test="${!actionBean.getManifestCellGrid().isEmpty()}">
                <div style="padding-top: 20px;">
                    <p>Filename: ${actionBean.filename}</p>
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

            <div style="padding-top: 20px; width: 45em;">
                <div style="float:left;">
                    <stripes:submit id="rotateKeyBtn" name="rotateKeyBtn" value="Rotate Service Account Key"
                                    class="btn btn-primary"
                                    title="Click to cause a new Google login key to be generated."/>
                </div>
                <div style="float:right; width: 25em;">
                    <span>
                        <stripes:checkbox id="rotateAcknowledgement" name="rotateAcknowledgement"/>
                        <stripes:label for="rotateAcknowledgement">
                            I acknowledge that rotating (changing) the service account key will affect all other Mercury instances that use this Google storage bucket.
                        </stripes:label>
                    </span>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>