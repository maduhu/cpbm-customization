<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
 <%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
 <div class="secondlevel_withoutsubmenu">
  <div class="secondlevel_breadcrumb_panel">
     <div class="secondlevel_breadcrumbbox">
       <p><c:out value="${tenantForm.tenant.name}"/></p>
     </div>     
  </div>
</div>

  <tiles:insertDefinition name="warnings"></tiles:insertDefinition> 