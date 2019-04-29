<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Manifest Admin" sectionTitle="Mayo Manifest Admin">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="manifestAdminForm" class="form-horizontal">
            <!-- Tests access to manifest file storage. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="testAccessBtn" name="testAccessBtn" value="Test Bucket Access"
                                class="btn btn-primary"
                                title="Click to read bucket storage and report status."/>
                <c:if test="${fn:length(actionBean.bucketList) > 0}">
                    <c:forEach items="${actionBean.bucketList}" var="filename" varStatus="item">
                        <stripes:hidden name="bucketList[${item.index}]" value="${filename}"/>
                    </c:forEach>

                    <span style="margin-left: 20px;">
                        <stripes:submit id="showBucketListBtn" name="showBucketListBtn" value="Show Filename List"
                                        class="btn btn-primary"
                                        title="Click to view a sorted list of all filenames in the bucket."/>
                    </span>
                </c:if>
            </div>

            <!-- Shows files that were read in but failed to make manifests. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="showFailedFilesListBtn" name="showFailedFilesListBtn" value="Show Failed Files"
                                class="btn btn-primary"
                                title="Click to get a txt file download of manifest bucket files that were read in but failed to make manifests."/>
            </div>

            <!-- Loads all new manifest files. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="pullAllFilesBtn" name="pullAllFilesBtn" value="Pull All New Manifest Files"
                                class="btn btn-primary"
                                title="Click to find new manifest files and save them in Mercury."/>
            </div>

            <!-- Re-accessions a rack. -->
            <div style="padding-top: 20px;">
                <span>
                    <stripes:submit id="reaccessionBtn" name="reaccessionBtn" value="Re-accession the Rack"
                                    class="btn btn-primary"
                                    title="Click to redo an existing accession using the most recent manifest file."/>
                    <span style="margin-left: 20px;">
                        Rack barcode:
                        <stripes:text id="rackBarcode" name="rackBarcode"/>
                    </span>
                </span>
            </div>

            <!-- Loads a new version of the given manifest file. -->
            <div style="padding-top: 20px;">
                <stripes:submit id="pullFileBtn" name="pullFileBtn" value="Reload and Reprocess A Manifest File"
                                class="btn btn-primary"
                                title="Click to re-read the given manifest file and save it in Mercury."/>
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

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>