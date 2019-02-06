package org.exoplatform.addon.ethereum.wallet.service.utils;

import java.io.*;
import java.security.Principal;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.collections.MapUtils;

@SuppressWarnings("all")
class FakeHTTPServletResponse implements HttpServletRequest {
  @Override
  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse) throws IllegalStateException {
    return null;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    return null;
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

  }

  @Override
  public void setAttribute(String name, Object o) {
  }

  @Override
  public void removeAttribute(String name) {
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public int getServerPort() {
    return 80;
  }

  @Override
  public String getServerName() {
    return "FAKE_SERVER_NAME";
  }

  @Override
  public String getScheme() {
    return "https";
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return null;
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public String getRemoteHost() {
    return null;
  }

  @Override
  public String getRemoteAddr() {
    return null;
  }

  @Override
  public String getRealPath(String path) {
    return null;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return null;
  }

  @Override
  public String getProtocol() {
    return null;
  }

  @Override
  public String[] getParameterValues(String name) {
    return null;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, String[]> getParameterMap() {
    return MapUtils.EMPTY_MAP;
  }

  @Override
  public String getParameter(String name) {
    return null;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return null;
  }

  @Override
  public Locale getLocale() {
    return Locale.getDefault();
  }

  @Override
  public int getLocalPort() {
    return 0;
  }

  @Override
  public String getLocalName() {
    return null;
  }

  @Override
  public String getLocalAddr() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  @Override
  public DispatcherType getDispatcherType() {
    return null;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public int getContentLength() {
    return 0;
  }

  @Override
  public String getCharacterEncoding() {
    return null;
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return null;
  }

  @Override
  public Object getAttribute(String name) {
    return null;
  }

  @Override
  public AsyncContext getAsyncContext() {
    return null;
  }

  @Override
  public void logout() throws ServletException {

  }

  @Override
  public void login(String username,
                    String password) throws ServletException {

  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public HttpSession getSession(boolean create) {
    return null;
  }

  @Override
  public HttpSession getSession() {
    return new FakeHTTPSession();
  }

  @Override
  public String getServletPath() {
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    return null;
  }

  @Override
  public String getRequestURI() {
    return "/portal";
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public String getQueryString() {
    return null;
  }

  @Override
  public String getPathTranslated() {
    return null;
  }

  @Override
  public String getPathInfo() {
    return null;
  }

  @Override
  public Collection<Part> getParts() throws IOException,
                                     ServletException {
    return null;
  }

  @Override
  public Part getPart(String name) throws IOException,
                                   ServletException {
    return null;
  }

  @Override
  public String getMethod() {
    return null;
  }

  @Override
  public int getIntHeader(String name) {
    return 0;
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return null;
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return null;
  }

  @Override
  public String getHeader(String name) {
    return null;
  }

  @Override
  public long getDateHeader(String name) {
    return 0;
  }

  @Override
  public Cookie[] getCookies() {
    return null;
  }

  @Override
  public String getContextPath() {
    return "/portal";
  }

  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException,
                                                            ServletException {
    return false;
  }
}
