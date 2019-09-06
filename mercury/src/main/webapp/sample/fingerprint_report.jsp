<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.FingerprintReportActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Fingerprint Search"
                       sectionTitle="Fingerprint Search">

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
            <div class="control-group">
                <label for="pdoId" class="control-label">PDO-ID</label>
                <div class="controls">
                    <stripes:text name="pdoId" id="controlName"
                                  class="defaultText input-xlarge"
                                  title="Enter a PDO-ID"/>
                </div>
            </div>
            <div class="control-group">
                <div class="control-label">&nbsp;</div>
                <div class="controls actionButtons">
                    <stripes:submit name="search" value="Search"/>
                </div>
            </div>
        </stripes:form>
            <c:if test="${actionBean.showLayout}">
                <stripes:form beanclass="${actionBean.class.name}" id="showScanForm" class="form-horizontal">
                    <stripes:hidden name="participantId" value="${actionBean.participantId}"></stripes:hidden>
                    <stripes:hidden name="sampleId" value="${actionBean.sampleId}"></stripes:hidden>
                    <stripes:hidden name="pdoId" value="${actionBean.pdoId}"></stripes:hidden>

                <div class="row-fluid" style="white-space: nowrap;">
                    <c:set var="prevPtId" value=""></c:set>
                    <c:set var="prevRootId" value=""></c:set>
                    <c:set var="prevAliquotId" value=""></c:set>
                    <ul>
                        <c:forEach items="${actionBean.fingerprints}" var="fingerprint">
                        <c:if test="${prevPtId != fingerprint.mercurySample.sampleData.patientId}">
                        <c:if test="${not empty prevPtId }">
                    </ul>
                    </ul>
                    </ul>
                    </c:if>
                    <li>${fingerprint.mercurySample.sampleData.patientId}</li>
                    <ul>
                        <c:set var="prevPtId"
                               value="${fingerprint.mercurySample.sampleData.patientId}"/>
                        <c:set var="prevRootId" value=""></c:set>
                        <c:set var="prevAliquotId" value=""></c:set>
                        </c:if>
                        <c:if test="${prevRootId != fingerprint.mercurySample.sampleData.rootSample}">
                        <c:if test="${not empty prevRootId }">
                    </ul>
                    </ul>
                    </c:if>
                    <li>${fingerprint.mercurySample.sampleData.rootSample}</li>
                    <ul>
                        <c:set var="prevRootId"
                               value="${fingerprint.mercurySample.sampleData.rootSample}"/>
                        <c:set var="prevAliquotId" value=""></c:set>
                        </c:if>
                        <c:if test="${prevAliquotId != fingerprint.mercurySample.sampleKey}">
                        <c:if test="${not empty prevAliquotId }">
                    </ul>
                    </c:if>
                    <li>${fingerprint.mercurySample.sampleKey}</li>
                    <ul>
                        <c:set var="prevAliquotId"
                               value="${fingerprint.mercurySample.sampleKey}"/>
                        </c:if>
                        <li>Fingerprints:
                                ${actionBean.FormatDate(fingerprint.dateGenerated)},
                                ${fingerprint.platform},
                                ${fingerprint.disposition}

                            <br/>
                            <c:forEach items="${fingerprint.fpGenotypesOrdered}" var="geno">${geno.genotype}</c:forEach>
                        </li>
                        </c:forEach>
                    </ul>
                </div>
                <div class="control-group">
                    <div class="control-label">&nbsp;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="download" value="Download"/>
                    </div>
                </div>
                </stripes:form>
            </c:if>
    </stripes:layout-component>
</stripes:layout-render>

