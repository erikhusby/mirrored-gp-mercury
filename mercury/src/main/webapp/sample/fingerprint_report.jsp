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
            <div class="row-fluid">


                <c:set var="prevPtId" value=""></c:set>
                <c:set var="prevRootId" value=""></c:set>
                <c:forEach items="${actionBean.fingerprints}" var="fingerprint">
                <c:choose>
                <c:when test="${prevPtId != fingerprint.mercurySample.sampleData.patientId}">
                <ul>
                    <li>${fingerprint.mercurySample.sampleData.patientId}
                        <ul>
                            <li>${fingerprint.mercurySample.sampleData.rootSample}
                                <ul>
                                    <li>${fingerprint.mercurySample.sampleKey}
                                        <ul>
                                            <li>Fingerprints:
                                                    ${actionBean.FormatDate(fingerprint.dateGenerated)},
                                                    ${fingerprint.platform},
                                                    ${fingerprint.disposition}
                                                <br/>
                                                <c:forEach items="${fingerprint.fpGenotypesOrdered}"
                                                           var="geno">${geno.genotype}</c:forEach>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                </c:when>
                <c:otherwise>
                    <c:choose>
                    <c:when test="${prevRootId != fingerprint.mercurySample.sampleData.rootSample}">
                            <ul>
                                <li>${fingerprint.mercurySample.sampleData.rootSample}
                                    <ul>
                                        <li>${fingerprint.mercurySample.sampleKey}
                                            <ul>
                                                <li>Fingerprints:
                                                        ${actionBean.FormatDate(fingerprint.dateGenerated)},
                                                        ${fingerprint.platform},
                                                        ${fingerprint.disposition}
                                                    <br/>
                                                    <c:forEach items="${fingerprint.fpGenotypesOrdered}"
                                                               var="geno">${geno.genotype}</c:forEach>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                            </c:when>
                            <c:otherwise>
                                <ul>
                                    <li>${fingerprint.mercurySample.sampleKey}
                                        <ul>
                                            <li>Fingerprints:
                                                    ${actionBean.FormatDate(fingerprint.dateGenerated)},
                                                    ${fingerprint.platform},
                                                    ${fingerprint.disposition}
                                                <br/>
                                                <c:forEach
                                                        items="${fingerprint.fpGenotypesOrdered}"
                                                        var="geno">${geno.genotype}</c:forEach>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </c:otherwise>
                        </c:choose>

                                </c:otherwise>
                                                </c:choose>

                                                <c:if test="${prevRootId == fingerprint.mercurySample.sampleData.rootSample}">
                                            </li>
                                        </ul>
                                        </c:if>
                                        <c:if test="${prevPtId == fingerprint.mercurySample.sampleData.patientId}">
                                    </li>
                                </ul>
                                </c:if>
                                    <c:set var="prevPtId"
                                           value="${fingerprint.mercurySample.sampleData.patientId}"/>
                                    <c:set var="prevRootId"
                                           value="${fingerprint.mercurySample.sampleData.rootSample}"/>
                                </c:forEach>

            </div>

        </c:if>
    </stripes:layout-component>
</stripes:layout-render>

<br/>
Participant<br />
&nbsp&nbsp Root Sample<br />
&nbsp&nbsp&nbsp&nbsp Fingerprint Aliquot<br />
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp Fingerprint: date, platform, pass/fail, genotypes