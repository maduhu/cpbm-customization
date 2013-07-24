<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="dialog_formcontent wizard">
 <span class="helptext">
   <spring:message code="ui.text.message.service.instance.image.message"></spring:message>
 </span>
        <spring:url value="/portal/connector/uploadlogo" var="upload_service_instance_logo_path" htmlEscape="false" /> 
    <form:form commandName="serviceInstanceLogoForm"  cssClass="formPanel"  action="${upload_service_instance_logo_path}"  enctype="multipart/form-data" >
              <ul>
                <li style="margin:10px 0 0 10px;">
                  <form:label path="logo"><spring:message code="label.custom.service.instance.image"/></form:label>
                  <form:input type="file" id="logo" class="logorequired" path="logo" />
                  <div class="main_addnew_formbox_errormsg" id="logoError"></div>
                </li>
              </ul>
          </form:form>
</div>
              