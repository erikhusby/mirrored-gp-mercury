<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:form beanclass="${actionBean.class.name}">
    <%--<ul>--%>
    <%--<li>--%>
    <%--<input type="checkbox">Select latest bam for each sample if coverage > <stripes:text class="defaultText" name="coverage" maxlength="250"/>--%>
    <%--</li>--%>
    <%--<li>--%>
    <%--<input type="checkbox">Select latest bam for each sample if contamination < <stripes:text class="defaultText" name="contamination" maxlength="250"/>--%>
    <%--</li>--%>
    <%--<li>--%>
    <%--<input type="checkbox">Require fingerpint match--%>
    <%--</li>--%>

    <%--</ul>--%>

    <%--<table>--%>
    <%--<tr>--%>
    <%--<td title="Choose the BioProject to which all samples will be submitted">BioProject:</td>--%>
    <%--<td>--%>
    <%--<stripes:select name="selectedBioProject" onchange="guessBioSamplesForBioProject(this.value)">--%>
    <%--<stripes:option  label="Choose a BioProject" value=""/>--%>
    <%--<stripes:options-collection collection="${actionBean.editResearchProject.bioProjects}"--%>
    <%--value="name" label="name"/>--%>
    <%--</stripes:select>--%>
    <%--</td>--%>
    <%--</tr>--%>
    <%--</table>--%>

    <table class="simple" id="submissionSamples" style="table-layout: fixed;">
        <thead>
        <tr>
            <!-- add data type to big list -->
            <!-- only show latest single file -->
            <%--<th width="20">Choose</th>--%>
            <th width="100">Sample</th>
            <th width="100">BioSample</th>
            <th width="75">Data Type</th>
            <th width="180">PDOs</th>
            <th width="100">Aggregation Project</th>
            <th width="80">File Type</th>
            <th width="20">Version</th>
            <th width="80">Quality Metric</th>
            <th width="20">Contamination</th>
            <th width="20">Fingerprint</th>
            <!-- add # lanes, # lanes blacklisted, notes -->
            <th width="20">Lanes in Aggregation</th>
            <th width="20">Blacklisted Lanes</th>
            <th width="20">Submitted Version</th>
            <th width="20">Current Status</th>
            <th width="20">Status Date</th>

        </tr>
        </thead>
        <tbody>
        <!-- http://localhost:8080/Mercury/projects/project.action?view=&researchProject=RP-13 -->
        <!-- http://localhost:8080/Mercury/projects/project.action?view=&researchProject=RP-356 -->
        <!-- $j('.fileCheckbox').children()[0].checked = true -->
        <!-- $j('.fileCheckbox').data('contamination') -->


        <c:forEach items="${actionBean.submissionSamples}" var="submissionSample">
            <tr>
                <%--<td class="fileCheckbox" style="vertical-align: middle; text-align: left;"--%>
                <%--data-contamination="${submissionSample.contamination}"--%>
                <%--data-coverage="${submissionSample.submissionFile.metrics.qualityMetricValue}">--%>
                <%--<stripes:checkbox title="${submissionSample.submissionFile.label}" class="shiftCheckbox"--%>
                <%--name="selectedFiles"--%>
                <%--value="${submissionSample.submissionFile.label}"/>--%>
                <%--</td>--%>
                <td>${submissionSample.sampleName}</td>
                <td><%--bio-sample--%></td>
                <td>${submissionSample.dataType}</td>
                <td style="padding: 5px;
                                               text-align: center;">
                    <table class="simple" style="table-layout: fixed;">
                        <c:forEach items="${submissionSample.productOrders}" var="pdo">
                            <tr>
                                <td width="100">${pdo.businessKey}</td>
                                <td style="max-width: 100px;
                                                    min-width: 100px;
                                                    overflow: hidden;
                                                    text-overflow: ellipsis;
                                                    white-space: nowrap;"
                                    title="${pdo.product.productName}">
                                    ${pdo.product.productName}</td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
                <td> ${submissionSample.aggregationProject} </td>
                <td> ${submissionSample.fileType} </td>
                <td> ${submissionSample.version}</td>
                <td> ${submissionSample.qualityMetricString}</td>
                <td>${submissionSample.contaminationString}</td>
                <td>${submissionSample.fingerprintLOD.displayString()}</td>
                <td>${fn:length(submissionSample.lanes)}</td>
                <td><%--blacklisted lanes--%></td>
                <td><%--submitted version--%></td>
                <td><%--current status--%></td>
                <td><fmt:formatDate value="${submissionSample.dateCompleted}"/></td>

                <%--<c:if test="${submissionSample.la == 0}">--%>
                <%--<td colspan="11" style="text-align: center;">--%>
                <%--no files available--%>
                <%--</td>--%>
                <%--</c:if>--%>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <%--<button>Submit these files</button>--%>
</stripes:form>
