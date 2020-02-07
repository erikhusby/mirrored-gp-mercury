<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib prefix="s" uri="http://stripes.sourceforge.net/stripes-dynattr.tld" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.FingerprintMatrixActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Fingerprint Matrix"
                       sectionTitle="Fingerprint Matrix">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal sapn6">
            <div class="control-group">
                <label for="participantId" class="control-label">PT-ID</label>
                <div class="controls">
                    <stripes:textarea name="participantId" id="controlName"
                                      class="defaultText input-xlarge"
                                      title="Enter PT-ID(s)"/>
                </div>
            </div>
            <div class="control-group">
                <label for="sampleId" class="control-label">SM-ID</label>
                <div class="controls">
                    <stripes:textarea name="sampleId" id="controlName"
                                      class="defaultText input-xlarge"
                                      title="Enter SM-ID(s)"/>
                </div>
            </div>
            <label for="platforms" class="control-label">Platform(s)</label>
            <div class="controls">
               <s:select name="platforms" multiple="true" size="4">
                   <s:options-enumeration enum="org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint.Platform"/>
               </s:select>
            </div>
            <div class="control-group">
                <div class="control-label">&nbsp;</div>
                <div class="controls actionButtons">
                   <br>
                    <stripes:submit name="downloadMatrix" value="Download Matrix"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>

