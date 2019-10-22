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
                        <input type="hidden"  name="bucketList[${item.index}]" value="${filename}"/>
                    </c:forEach>

                    <span style="margin-left: 20px;">
                        <stripes:submit id="showBucketListBtn" name="showBucketListBtn" value="Download Filename List"
                                        class="btn btn-primary"
                                        title="Provides a download txt file of all filenames in the bucket."/>
                    </span>
                </c:if>
            </div>

            <div style="padding-top: 20px;">
                <span>
                    <div>
                        <stripes:submit id="rotateKeyBtn" name="rotateKeyBtn" value="Rotate Service Account Key"
                                        class="btn btn-primary"
                                        title="Click to cause a new Google login key to be generated."/>
                    </div>
                    <div style="padding-left: 20px;">
                        <input type="checkbox" id="rotateAcknowledgement" name="rotateAcknowledgement"/>
                        <stripes:label for="rotateAcknowledgement">
                            OK to lock out other Mercury servers using the Google storage bucket.
                        </stripes:label>
                    </div>
                </span>
            </div>

            <div style="padding-top: 20px;">
                <span>
                    <span>
                        <stripes:submit id="uploadCredentialBtn" name="uploadCredentialBtn" value="Upload New Credential"
                                        class="btn btn-primary"
                                        title="Click to upload a new Google bucket credential."/>
                    </span>
                    <span style="padding-left: 20px;">
                        <stripes:file name="credentialFile" id="credentialFile"/>
                    </span>
                </span>
                <div style="padding-left: 20px;">
                    <input type="checkbox" id="uploadCredentialAcknowledgement" name="uploadCredentialAcknowledgement"/>
                    <stripes:label for="uploadCredentialAcknowledgement">
                        OK to lock out other Mercury servers using the Google storage bucket.
                    </stripes:label>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>