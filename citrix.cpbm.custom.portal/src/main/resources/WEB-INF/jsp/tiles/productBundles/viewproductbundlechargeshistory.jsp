 <%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class="widget_browsergrid_wrapper wsubaction" >
   <div class="widget_details_inlinegrid" >
    <div class="widget_grid inline header widget_navtitlebox">
     <span class="widget_gridicon currentcalendar " style="margin:7px 0 0 5px;"> </span>
      <span  class="title" style="margin-left:5px;width: auto;color:#FFF;">
        <spring:message code="dateonly.format" var="dateonly_format"/>
        <spring:message code="label.bundle.list.ratecard.effective.date"/>
        <c:choose>
          <c:when test="${date != null}">
              <fmt:formatDate value="${date}" pattern="${dateonly_format}"/>
          </c:when>
          <c:otherwise>
              <spring:message code="ui.label.plan.date.not.yet.set"/>
          </c:otherwise>
        </c:choose>
      </span>
    </div>
  
    <tiles:insertAttribute name="chargesdataview"/>

  </div>
</div>