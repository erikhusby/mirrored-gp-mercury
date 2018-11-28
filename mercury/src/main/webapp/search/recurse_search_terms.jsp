<%@ page contentType="text/html;charset=UTF-8" language="java" import="org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%-- This JSP fragment recurses over a user-defined search instance, rendering form fields.
 The field names will be set by JavaScript in configurable_search.jsp, when the user submits the form --%>

<%--@elvariable id="searchValues" type="java.util.List<org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance.SearchValue>"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
<c:forEach items="${searchValues}" var="searchValue" varStatus="varSatatus">
    <c:set var="uniqueId" value="${searchValue.uniqueId}"/>
    <div class="searchterm" id="${uniqueId}"<c:if test="${searchValue.searchTerm.isRackScanSupported()}"> rackScanSupported="true"</c:if>>
        <%-- Link to remove term--%>
        <c:choose>
            <c:when test="${not actionBean.readOnly and not searchValue.searchTerm.required}">
            <a href="#" onclick="removeTerm(this);return false;"><img class="removeIcon"
                                                                          src="${ctxpath}/images/error.png"
                                                                          alt="Remove term"/></a>
            </c:when>
            <c:otherwise>
                <%-- Blank to keep the required terms lined up vertically --%>
            <label><span class="removeIcon">&nbsp;</span></label>
            </c:otherwise>
        </c:choose>
        <%-- Term name for form submission --%>
        <input type="hidden" name="tbd" value="${searchValue.termName}" id="${uniqueId}_name" class="termname"/>
        <c:if test="${actionBean.readOnly}">
            <input type="hidden" name="tbd" value="${searchValue.valueSetWhenLoaded}"
                   id="${uniqueId}_valueSetWhenLoaded" class="valueSetWhenLoaded"/>
        </c:if>
        <%-- Checkbox to determine whether term appears in search results--%>
        <c:choose>
            <c:when test="${not actionBean.minimal and (not empty searchValue.searchTerm.displayValueExpression or not empty searchValue.searchTerm.displayExpression)}">
                <input type="checkbox" class="displayTerm"
                       name="tbd"  ${searchValue.includeInResults ? 'checked="true"' : ''}/>
            </c:when>
            <c:otherwise>
                <span class="displayTerm">&nbsp;</span>
            </c:otherwise>
        </c:choose>
        <%-- if we're in read only mode, and the term or its parent has a constant value, don't show the term name--%>
        <c:if test="${not(actionBean.readOnly and (not empty searchValue.searchTerm.constantValue or
            not empty searchValue.parent.searchTerm.constantValue))}">
            <%-- Visible Term name --%>
            <label style="text-indent:${searchValue.depth * 15}px">${searchValue.termName}</label>
        </c:if>
        <%-- Comparison operator --%>
        <c:choose>
            <c:when test="${not empty searchValue.searchTerm.sqlRestriction}">
                <%-- Don't display operator for SQL search terms--%>
            </c:when>
            <c:when test="${searchValue.dataType == 'BOOLEAN'}">
                <%-- Only equals is meaningful for booleans --%>
                <span class="termoperatortext">=</span>
                <input type="hidden" name="tbd" value="EQUALS" class="termoperator"/>
            </c:when>
            <c:when test="${searchValue.dataType == 'NOT_NULL'}">
                <span class="termoperatortext">&nbsp;</span>
                <%-- Invisible placeholder --%><input type="hidden" name="tbd" value="NOT_NULL" class="termoperator"/>
            </c:when>
            <c:when test="${not empty searchValue.searchTerm.dependentSearchTerms}">
                <%-- Nodes with children only allow the equals operator --%>
                <%-- if we're in read only mode, and the term has a constant value, don't show the operator--%>
                <c:if test="${not(actionBean.readOnly and not empty searchValue.searchTerm.constantValue)}">
                    <span class="termoperatortext">=</span>
                </c:if>
                <input type="hidden" name="tbd" value="EQUALS" class="termoperator"/>
            </c:when>
            <c:when test="${actionBean.minimal}">
                <%-- Don't display the operator, but we need to submit the value, so use hidden input --%>
                <input type="hidden" name="tbd" value="${searchValue.operator.name}" class="termoperator"/>
            </c:when>
            <c:otherwise>
                <select name="tbd" class="termoperator" onchange="changeOperator(this);">
                    <option value="EQUALS" ${not empty searchValue.operator && searchValue.operator.name == 'EQUALS' ? 'selected' : ''}>
                        =
                    </option>
                    <%-- IN (because of implied midnight) and LIKE are not meaningful for dates --%>
                    <c:if test="${ searchValue.dataType ne 'DATE' and searchValue.dataType ne 'DATE_TIME'}">
                        <option value="IN" ${not empty searchValue.operator && searchValue.operator.name == 'IN' ? 'selected' : ''}>
                            from list
                        </option>
                        <option value="NOT_IN" ${not empty searchValue.operator && searchValue.operator.name == 'NOT_IN' ? 'selected' : ''}>
                            exclude from list
                        </option>
                        <option value="LIKE" ${not empty searchValue.operator && searchValue.operator.name == 'LIKE' ? 'selected' : ''}>
                            wildcard
                        </option>
                    </c:if>
                    <c:if test="${not searchValue.constrainedValuesListDisplayed}">
                        <option value="LESS_THAN" ${not empty searchValue.operator && searchValue.operator.name == 'LESS_THAN' ? 'selected' : ''}>
                            &lt;</option>
                        <option value="GREATER_THAN" ${not empty searchValue.operator && searchValue.operator.name == 'GREATER_THAN' ? 'selected' : ''}>
                            &gt;</option>
                        <option value="BETWEEN" ${not empty searchValue.operator && searchValue.operator.name == 'BETWEEN' ? 'selected' : ''}>
                            between
                        </option>
                    </c:if>
                </select>
            </c:otherwise>
        </c:choose>
        <%-- Term value --%>
        <c:choose>
            <c:when test="${not empty searchValue.searchTerm.sqlRestriction}">
                <%-- Don't display value for SQL search terms --%>
            </c:when>
            <c:when test="${searchValue.dataType == 'NOT_NULL'}">
                <%-- Supply a non-null value (ignored) --%><input type="hidden" name="tbd" value="N/A" id="${uniqueId}_value" class="termvalue"/>
            </c:when>
            <c:when test="${not empty searchValue.children and (not searchValue.constrainedValuesListDisplayed or
                (actionBean.readOnly and searchValue.valueSetWhenLoaded))}">
                <%-- If the term has children, the value cannot be changed --%>
                <input type="hidden" name="tbd" value="${searchValue.values[0]}" id="${uniqueId}_value"
                       class="termvalue"/>
                <span class="termValueConstant">${searchValue.values[0]}</span>
            </c:when>
            <c:when test="${searchValue.constrainedValuesListDisplayed}">
                <!-- Render a drop-down for constrained values -->
                <select name="tbd" id="${uniqueId}_value" class="termvalue"
                <%-- We need to detect changes, in case the user chooses a sub-term, then changes her mind --%>
                ${not empty searchValue.searchTerm.dependentSearchTerms ? 'onchange="changeDependee(this)"' : ''}
                <%-- Multiple select for IN operator --%>
                ${not empty searchValue.operator && (searchValue.operator.name == 'IN' || searchValue.operator.name == 'NOT_IN') ? 'multiple="true"' : ''}>
                    <%-- If not required and not IN, prompt user to choose a value --%>
                    <c:if test="${not searchValue.searchTerm.required and (empty searchValue.operator or searchValue.operator.name == 'EQUALS')}">
                        <option>${actionBean.searchInstance.chooseValue}</option>
                    </c:if>
                    <c:forEach items="${searchValue.constrainedValues}" var="constrainedValue">
                        <option value="${constrainedValue.code}" ${not empty searchValue.values && not empty searchValue.mappedValues[constrainedValue.code] ? 'selected' : ''}>
                            ${constrainedValue.label}
                        </option>
                    </c:forEach>
                </select>
            </c:when>
            <c:when test="${not empty searchValue.searchTerm.constrainedValuesExpression && fn:length(searchValue.constrainedValues) == 0}">
                <%-- If the values are constrained, but the list is empty, display "none", rather than a text box; nothing the user types can be meaningful --%>
                none
            </c:when>
            <c:otherwise>
                <!-- text entry -->
                <c:choose>
                    <c:when test="${not empty searchValue.operator && (searchValue.operator.name == 'IN' || searchValue.operator.name == 'NOT_IN')}">
                        <!-- IN = list of text values -->
                        <textarea rows="4" cols="10" class="termvalue"><c:forEach items="${searchValue.values}"
                                                                                  var="value">${value}<%= '\n' %></c:forEach></textarea>
                        &nbsp;<c:if test="${searchValue.searchTerm.isRackScanSupported()}"><input id="rackScanBtn" name="rackScanBtn" value="Rack Scan" class="btn btn-primary" onclick="startRackScan(this);" type="button"><input type="hidden" id="rackScanData_${uniqueId}" name="rackScanData" value='${searchValue.rackScanData}' class="rackScanData"/></c:if>
                    </c:when>
                    <c:when test="${not empty searchValue.operator && searchValue.operator.name == 'BETWEEN'}">
                        <!-- BETWEEN = Pair of text boxes. Add invented attributes, between1 and between2,
                        so the values can be found by the JavaScript that posts the form -->
                        <input type="text" name="tbd" value="${searchValue.values[0]}"
                               class="termvalue" id="${uniqueId}_value1" between1 dataType="${searchValue.dataType}"/>
                        and
                        <input type="text" name="tbd" value="${searchValue.values[1]}"
                               class="termvalue" id="${uniqueId}_value2" between2 dataType="${searchValue.dataType}"/>
                        <c:if test="${searchValue.dataType eq 'DATE' or searchValue.dataType eq 'DATE_TIME'}">
                            <script type="text/javascript">
                                $j("#${uniqueId}_value1").datepicker();
                                $j("#${uniqueId}_value2").datepicker();
                            </script>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <!-- Single text value -->
                        <input type="text" name="tbd" value="${searchValue.values[0]}"
                               id="${uniqueId}_value" class="termvalue" dataType="${searchValue.dataType}"/>
                        <c:if test="${searchValue.dataType eq 'DATE' or searchValue.dataType eq 'DATE_TIME'}">
                            <script type="text/javascript">
                                $j("#${uniqueId}_value").datepicker();
                            </script>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>
        <%-- If there are sub-terms, render a link to add them, don't bother if value is "none" --%>
        <c:if test="${not empty searchValue.searchTerm.dependentSearchTerms && empty searchValue.children && (empty searchValue.searchTerm.constrainedValuesExpression || fn:length(searchValue.constrainedValues) > 0)}">
            <input type="button" id="addSubTermBtn" class="btn btn-primary" value="Add Sub-Term" onclick="nextTerm(this)" />
        </c:if>
        <%-- Recurse over sub-terms --%>
        <c:set var="searchValues" value="${searchValue.children}" scope="request"/>
        <jsp:include page="recurse_search_terms.jsp"/>
    </div>
    <c:if test="${empty searchValue.parent}">
        <br id="${uniqueId}_br"/>
    </c:if>
</c:forEach>
