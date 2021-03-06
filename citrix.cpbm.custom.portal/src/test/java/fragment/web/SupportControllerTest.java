/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package fragment.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.support.SessionStatus;

import web.WebTestsBase;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import com.citrix.cpbm.portal.fragment.controllers.SupportController;
import com.vmops.internal.service.PrivilegeService;
import com.vmops.model.AccountType;
import com.vmops.model.Tenant;
import com.vmops.model.TenantHandle;
import com.vmops.model.Ticket;
import com.vmops.model.Ticket.Category;
import com.vmops.model.Ticket.TicketStatus;
import com.vmops.model.TicketComment;
import com.vmops.model.User;
import com.vmops.model.UserHandle;
import com.vmops.service.ChannelService;
import com.vmops.service.SupportService;
import com.vmops.utils.DateUtils;
import com.vmops.web.forms.TicketCommentForm;
import com.vmops.web.forms.TicketForm;

public class SupportControllerTest extends WebTestsBase {

  ModelMap map;

  SessionStatus status;

  @Autowired
  SupportController controller;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private SupportService supportService;

  @Autowired
  private PrivilegeService privilegeService;

  private MockHttpServletRequest request;

  private Tenant tenant;

  private User user;

  private MockHttpSession httpSession;

  @Before
  public void init() {
    map = new ModelMap();
    status = new MockSessionStatus();
    request = new MockHttpServletRequest();
    tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType(), new Date());
    tenant.setSourceChannel(channelService.getDefaultServiceProviderChannel());
    user = createTestUserInTenant(tenant);
    tenant.setOwner(user);
    tenantDAO.save(tenant);

    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);
    List<User> users = new ArrayList<User>();
    users.add(user);
    supportService.list(0, 0, listTicketStatus, users, "", "", new HashMap<String, String>()).clear();
    createTestTicket(5, tenant, user);
    httpSession = new MockHttpSession();
  }

  @Override
  protected Tenant createTestTenant(AccountType type, Date createdAt) {
    Tenant tenant = new Tenant("Acme Corp " + random.nextInt(), type, getRootUser(), randomAddress(), true,
        currencyValueDAO.findByCurrencyCode("USD"), getPortalUser());
    tenant.setMaxUsers(tenant.getAccountType().getMaxUsers());
    tenant.setAccountId(accountNumberService.next());

    tenant.setCreatedAt(createdAt);
    tenantDAO.save(tenant);
    privilegeService.newTenant(tenant);
    TenantHandle tenantHandle = new TenantHandle();
    tenantHandle.setHandle("Test" + System.currentTimeMillis());
    tenantHandle.setServiceInstanceUuid("003fa8ee-fba3-467f-a517-fd806dae8a80");
    tenantHandle.setTenant(tenant);
    tenant.addHandle(tenantHandle);
    tenantDAO.merge(tenant);
    return tenant;
  }

  @Override
  protected User createTestUserInTenant(Tenant tenant) {
    User user = new User("test", "user", "test@test.com", VALID_USER + random.nextInt(), VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, tenant, userProfile, getPortalUser());
    userDAO.save(user);
    UserHandle userHandle = new UserHandle();
    userHandle.setHandle("Test" + System.currentTimeMillis());
    userHandle.setServiceInstanceUuid("003fa8ee-fba3-467f-a517-fd806dae8a80");
    userHandle.setUser(user);
    user.addHandle(userHandle);
    userDAO.merge(user);
    privilegeService.newUser(user);
    tenant.getUsers().add(user);
    return user;
  }

  @Test
  public void testSupportRouting() throws Exception {
    logger.debug("Testing routing....Started");
    DispatcherTestServlet servlet = this.getServletInstance();
    Method expected = locateMethod(controller.getClass(), "listTickets", new Class[] {
        Tenant.class, String.class, String.class, Boolean.TYPE, String.class, String.class, String.class, int.class,
        String.class, ModelMap.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/support/tickets"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "createTicket", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/support/tickets/create"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "createTicket", new Class[] {
        String.class, HttpServletRequest.class, TicketForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/support/tickets/create"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "viewTicket", new Class[] {
        Tenant.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/support/tickets/view"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "createNewComment", new Class[] {
        Tenant.class, TicketCommentForm.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/support/tickets/{ticketId}/comment"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "editTicket", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, String.class, HttpServletRequest.class,
        TicketForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/support/tickets/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "closeTicket", new Class[] {
        Tenant.class, String.class, HttpServletRequest.class, TicketForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/support/tickets/close"));
    Assert.assertEquals(expected, handler);

    logger.debug("Testing routing.... Done");
  }

  @Test
  public void testCreateTicket() throws Exception {
    Ticket ticket = new Ticket();
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");
    ticket.setCategory(Category.WEB);
    ticket.setOwner(user);
    TicketForm form = new TicketForm(ticket);
    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, form, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
  }

  @Test
  public void testListTicketsAsRoot() throws Exception {
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTickets(systemTenant, tenant.getUuid(), "All", false, "", "", "", 1, null, map);

    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());
  }

  @Test
  public void testListTicketsAsMasterUser() throws Exception {
    Tenant otherTenant = tenantDAO.find(2L);
    User otherMasterUser = otherTenant.getOwner();
    userDAO.save(otherMasterUser);
    asUser(otherMasterUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    createTestTicket(3, otherTenant, otherMasterUser);
    asUser(user);
    systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTickets(systemTenant, tenant.getUuid(), "All", false, "", "", "", 1, null, map);

    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());

    asUser(otherMasterUser);
    responseAttribute.clear();
    view = controller.listTickets(systemTenant, tenant.getUuid(), "All", false, "", "", "", 1, null, map);
    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));

    @SuppressWarnings("unchecked")
    List<String> list1 = (List<String>) map.get("tickets");
    Assert.assertEquals(3, list1.size());
  }

  @Test
  public void testListTicketsAsRootOtherTenant() throws Exception {
    Tenant otherTenant = tenantDAO.find(2L);
    User otherMasterUser = otherTenant.getOwner();
    userDAO.save(otherMasterUser);
    asUser(otherMasterUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    createTestTicket(3, otherTenant, otherMasterUser);
    asUser(getRootUser());
    systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTickets(systemTenant, tenant.getUuid(), "All", false, "", "", "", 1, null, map);

    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());

    systemTenant = controller.getCurrentUser().getTenant();
    view = controller.listTickets(systemTenant, otherTenant.getUuid(), "All", false, "", "", "", 1, null, map);
    responseAttribute.clear();
    responseAttribute.put("queryLocator", "xyz");
    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));

    @SuppressWarnings("unchecked")
    List<String> list1 = (List<String>) map.get("tickets");
    Assert.assertEquals(6, list1.size());
  }

  @Test
  public void testListTicketsPageAsNormalUser() throws Exception {
    User normalUser = createTestUserInTenant(tenant);
    asUser(normalUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTickets(systemTenant, tenant.getUuid(), "All", false, "", "", "", 1, null, map);
    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());
  }

  @Test
  public void testListTicketsPageAsRoot() throws Exception {
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTicketsPage(systemTenant, tenant.getUuid(), "All", false, "", "", "", map, request);
    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());
  }

  @Test
  public void testListTicketsPageAsMasterUser() throws Exception {
    Tenant otherTenant = tenantDAO.find(2L);
    User otherMasterUser = otherTenant.getOwner();
    userDAO.save(otherMasterUser);
    asUser(otherMasterUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    createTestTicket(3, otherTenant, otherMasterUser);
    asUser(user);
    systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTicketsPage(systemTenant, tenant.getUuid(), "All", false, "", "", "", map, request);
    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());

    asUser(otherMasterUser);
    responseAttribute.clear();
    view = controller.listTicketsPage(systemTenant, tenant.getUuid(), "All", false, "", "", "", map, request);
    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));

    @SuppressWarnings("unchecked")
    List<String> list1 = (List<String>) map.get("tickets");
    Assert.assertEquals(9, list1.size());
  }

  @Test
  public void testListTicketsPageAsRootOtherTenant() throws Exception {
    Tenant otherTenant = tenantDAO.find(2L);
    User otherMasterUser = otherTenant.getOwner();
    userDAO.save(otherMasterUser);
    asUser(otherMasterUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    createTestTicket(3, otherTenant, otherMasterUser);
    asUser(getRootUser());
    systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTicketsPage(systemTenant, tenant.getUuid(), "All", false, "", "", "", map, request);
    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());

    systemTenant = controller.getCurrentUser().getTenant();
    view = controller.listTicketsPage(systemTenant, otherTenant.getUuid(), "All", false, "", "", "", map, request);
    responseAttribute.clear();
    responseAttribute.put("queryLocator", "xyz");
    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));

    @SuppressWarnings("unchecked")
    List<String> list1 = (List<String>) map.get("tickets");
    Assert.assertEquals(12, list1.size());
  }

  @Test
  public void testListTicketsAsNormalUser() throws Exception {
    User normalUser = createTestUserInTenant(tenant);
    normalUser.setProfile(profileDAO.find(10L));
    userDAO.save(normalUser);
    asUser(normalUser);
    Tenant systemTenant = controller.getCurrentUser().getTenant();
    String view = controller.listTicketsPage(systemTenant, tenant.getUuid(), "All", false, "", "", "", map, request);
    List<TicketStatus> listTicketStatus = new ArrayList<Ticket.TicketStatus>();
    listTicketStatus.add(TicketStatus.NEW);
    listTicketStatus.add(TicketStatus.CLOSED);
    listTicketStatus.add(TicketStatus.ESCALATED);
    listTicketStatus.add(TicketStatus.WORKING);

    List<User> users = new ArrayList<User>();
    users.add(user);
    Map<String, String> responseAttribute = new HashMap<String, String>();
    responseAttribute.put("queryLocator", "xyz");

    List<Ticket> tickets = supportService.list(0, 0, listTicketStatus, users, "", "", responseAttribute);

    Assert.assertEquals("support.tickets.list", view);
    Assert.assertTrue(map.containsKey("tickets"));
    Assert.assertTrue(map.containsValue(tickets));

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) map.get("tickets");
    Assert.assertEquals(5, list.size());
  }

  @Test
  public void testCreateTicketAsNormalUser() throws Exception {
    User normalUser = createTestUserInTenant(tenant);
    asUser(normalUser);
    String view = controller.createTicket(normalUser.getTenant().getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(normalUser, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());
  }

  @Test
  public void testCreateTicketAsRootForNormalUser() throws Exception {
    asRoot();
    tenantDAO.save(tenant);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    map.clear();
    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());
  }

  @Test
  public void testViewTicket() throws Exception {
    asUser(user);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());

    map.clear();
  }

  @Test
  public void testViewTicketAsOtherUser() throws Exception {
    asUser(user);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());

    map.clear();
  }

  @Test
  public void testAddComment() throws Exception {
    asUser(user);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());

    map.clear();

    List<TicketComment> comments = supportService.listComments(ticket, user);
    Assert.assertNull(comments);

  }

  @Test
  public void testCloseTicket() throws Exception {
    asUser(user);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());

    map.clear();

    asRoot();
    ticket.setStatus(TicketStatus.CLOSED);
    ticket.setUpdatedAt(new Date());
    ticket.setUpdatedBy(user);
    supportService.update(ticket, user);
    Assert.assertNotNull(ticket);
    Assert.assertEquals("Success", "Success");

    map.clear();
  }

  @Test
  public void testEditTicket() throws Exception {
    asUser(user);
    String view = controller.createTicket(tenant.getUuid(), map);
    Assert.assertEquals("support.tickets.create", view);
    TicketForm ticketForm = (TicketForm) map.get("createTicketForm");
    Assert.assertNotNull(ticketForm);
    Assert.assertNull(ticketForm.getTicket());

    Ticket ticket = new Ticket();
    ticketForm.setTicket(ticket);
    ticket.setDescription("test desc");
    ticket.setSubject("test sub");

    Ticket newTicket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    Assert.assertEquals(ticket.getSubject(), newTicket.getSubject());
    Assert.assertEquals(user, newTicket.getOwner());
    Assert.assertNotNull(newTicket.getCaseNumber());

    map.clear();
  }

  private void createTestTicket(int count, Tenant tenant, User user) {
    for (int i = 0; i < count; i++) {
      supportService.create("base test sub " + i, "base test desc " + i, "web", user);
    }
  }

  /*
   * Description: Test view product Author: Avinash ReviewedBy:
   */

  @Test
  public void testViewTicket1() throws Exception {
    asUser(user);
    ModelMap map = new ModelMap();
    TicketCommentForm ticketCommentForm = new TicketCommentForm();
    TicketComment comment = new TicketComment();
    ticketCommentForm.setComment(comment);
    Ticket ticket = new Ticket();
    TicketForm ticketForm = new TicketForm();
    ticketForm.setTicket(ticket);

    ticket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    comment.setParentId(ticket.getUuid());
    controller.createNewComment(tenant, ticketCommentForm, ticket.getCaseNumber(), tenant.getParam(), map);

    String view = controller.viewTicket(tenant, ticket.getCaseNumber(), tenant.getParam(), map);
    Assert.assertNotNull(map);
    Assert.assertEquals(map.get("ticket"), ticket);
    TicketForm ticketForm1 = (TicketForm) map.get("ticketForm");
    Assert.assertEquals(ticketForm1.getTicket(), ticket);
    Assert.assertNotNull(map.get("ticketcomments"));

    Assert.assertNotNull(map.get("ticketCommentForm"));
    Assert.assertEquals(view, new String("support.tickets.view"));
    view = controller.viewTicket(tenant, null, tenant.getParam(), map);
    Assert.assertNotNull(map.get("statuses"));

  }

  @Test
  public void testAddNewComment() throws Exception {
    asUser(user);
    ModelMap map = new ModelMap();
    TicketCommentForm ticketCommentForm = new TicketCommentForm();
    TicketComment comment = new TicketComment();
    ticketCommentForm.setComment(comment);
    Ticket ticket = new Ticket();
    TicketForm ticketForm = new TicketForm();
    ticketForm.setTicket(ticket);

    ticket = controller.createTicket(tenant.getUuid(), request, ticketForm, map);
    comment.setParentId(ticket.getUuid());
    controller.createNewComment(tenant, ticketCommentForm, ticket.getCaseNumber(), tenant.getParam(), map);
    String view = controller.viewTicket(tenant, ticket.getCaseNumber(), tenant.getParam(), map);
    Assert.assertNotNull(map);
    Assert.assertEquals(map.get("ticket"), ticket);
    ticketForm = (TicketForm) map.get("ticketForm");
    Assert.assertEquals(ticketForm.getTicket(), ticket);
    Assert.assertNotNull(map.get("ticketcomments"));

    Assert.assertNotNull(map.get("ticketCommentForm"));
    Assert.assertEquals(view, new String("support.tickets.view"));
    view = controller.viewTicket(tenant, null, tenant.getParam(), map);
    Assert.assertNotNull(map.get("statuses"));

  }

  @Test
  public void testEditTicketPost() {
    String expectedResponse = "success";
    Tenant tenant = tenantDAO.find(2L);

    Ticket ticket = supportService.create("TestTitle", "TestDescription", "WEB", user);
    String ticketNumber = ticket.getCaseNumber();
    ticket.setStatus(TicketStatus.NEW);
    ticket.setOwner(tenant.getOwner());

    ticket.setDescription("New Description");
    ticket.setStatus(TicketStatus.CLOSED);
    TicketForm ticketForm = new TicketForm();
    ticketForm.setTicket(ticket);

    String response = controller.editTicket(tenant, null, tenant.getParam(), null, null, ticket.getCaseNumber(),
        request, ticketForm, map);

    Ticket newTicket = supportService.get(ticketNumber);
    Assert.assertEquals(expectedResponse, response);
    Assert.assertEquals("New Description", newTicket.getDescription());
    Assert.assertEquals(TicketStatus.CLOSED, newTicket.getStatus());
  }

  @Test
  public void testcloseTicket() {
    try {
      Tenant tenant = tenantDAO.find(4L);

      Ticket ticket = supportService.create("TestTitle", "TestDescription", "WEB", user);
      String ticketNumber = ticket.getCaseNumber();
      TicketForm ticketForm = new TicketForm();
      ticketForm.setTicket(ticket);

      controller.closeTicket(tenant, ticketNumber, request, ticketForm, map);
      Ticket newTicket = supportService.get(ticketNumber);
      Assert.assertEquals(TicketStatus.CLOSED, newTicket.getStatus());
      Assert.assertTrue(DateUtils.isSameDay(new Date(), newTicket.getUpdatedAt()));
      Assert.assertEquals(getRootUser(), newTicket.getUpdatedBy());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testhomeTicketsCount() {
    try {
      Tenant tenant = tenantDAO.find(4L);

      Ticket ticket1 = supportService.create("TestTitle", "TestDescription", "WEB", user);
      ticket1.setStatus(TicketStatus.NEW);
      ticket1.setOwner(tenant.getOwner());

      Ticket ticket2 = supportService.create("TestTitle", "TestDescription", "WEB", user);
      ticket2.setStatus(TicketStatus.CLOSED);
      ticket2.setOwner(tenant.getOwner());

      Ticket ticket3 = supportService.create("TestTitle", "TestDescription", "WEB", user);
      ticket3.setStatus(TicketStatus.WORKING);
      ticket3.setOwner(tenant.getOwner());

      controller.homeTicketsCount(tenant, tenant.getParam(), map, httpSession, request);

      Assert.assertEquals(1, map.get("new_tickets_count"));
      Assert.assertEquals(1, map.get("working_tickets_count"));
      Assert.assertEquals(1, map.get("closed_tickets_count"));
      Assert.assertEquals(0, map.get("escalated_tickets_count"));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateFormToEmail() {
    try {
      Tenant tenant = tenantDAO.find(4L);

      Ticket ticket = supportService.create("TestTitle", "TestDescription", "WEB", user);
      TicketForm ticketForm = new TicketForm();
      ticketForm.setTicket(ticket);

      request.setParameter("lang", Locale.getDefault().toString());

      controller.createFormToEmail(tenant.getParam(), tenant, request, ticketForm, map);

      Assert.assertEquals("Email Sent", map.get("message"));
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertTrue((Boolean) map.get("status"));
      Assert.assertNotNull(map.get("ticketForm"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
}
