/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: Apache Tomcat/10.0.20
 * Generated at: 2022-04-28 18:25:08 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import java.util.*;
import java.io.*;
import indexer.Indexer;
import engine.SearchEngine;
import repository.Repository;
import model.RetrievedDocument;

public final class index_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent,
                 org.apache.jasper.runtime.JspSourceImports {

  private static final jakarta.servlet.jsp.JspFactory _jspxFactory =
          jakarta.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private static final java.util.Set<java.lang.String> _jspx_imports_packages;

  private static final java.util.Set<java.lang.String> _jspx_imports_classes;

  static {
    _jspx_imports_packages = new java.util.HashSet<>();
    _jspx_imports_packages.add("java.util");
    _jspx_imports_packages.add("jakarta.servlet");
    _jspx_imports_packages.add("java.io");
    _jspx_imports_packages.add("jakarta.servlet.http");
    _jspx_imports_packages.add("jakarta.servlet.jsp");
    _jspx_imports_classes = new java.util.HashSet<>();
    _jspx_imports_classes.add("repository.Repository");
    _jspx_imports_classes.add("model.RetrievedDocument");
    _jspx_imports_classes.add("engine.SearchEngine");
    _jspx_imports_classes.add("indexer.Indexer");
  }

  private volatile jakarta.el.ExpressionFactory _el_expressionfactory;
  private volatile org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public java.util.Set<java.lang.String> getPackageImports() {
    return _jspx_imports_packages;
  }

  public java.util.Set<java.lang.String> getClassImports() {
    return _jspx_imports_classes;
  }

  public jakarta.el.ExpressionFactory _jsp_getExpressionFactory() {
    if (_el_expressionfactory == null) {
      synchronized (this) {
        if (_el_expressionfactory == null) {
          _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        }
      }
    }
    return _el_expressionfactory;
  }

  public org.apache.tomcat.InstanceManager _jsp_getInstanceManager() {
    if (_jsp_instancemanager == null) {
      synchronized (this) {
        if (_jsp_instancemanager == null) {
          _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
        }
      }
    }
    return _jsp_instancemanager;
  }

  public void _jspInit() {
  }

  public void _jspDestroy() {
  }

  public void _jspService(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.HttpServletResponse response)
      throws java.io.IOException, jakarta.servlet.ServletException {

    if (!jakarta.servlet.DispatcherType.ERROR.equals(request.getDispatcherType())) {
      final java.lang.String _jspx_method = request.getMethod();
      if ("OPTIONS".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        return;
      }
      if (!"GET".equals(_jspx_method) && !"POST".equals(_jspx_method) && !"HEAD".equals(_jspx_method)) {
        response.setHeader("Allow","GET, HEAD, POST, OPTIONS");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "JSPs only permit GET, POST or HEAD. Jasper also permits OPTIONS");
        return;
      }
    }

    final jakarta.servlet.jsp.PageContext pageContext;
    jakarta.servlet.http.HttpSession session = null;
    final jakarta.servlet.ServletContext application;
    final jakarta.servlet.ServletConfig config;
    jakarta.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    jakarta.servlet.jsp.JspWriter _jspx_out = null;
    jakarta.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<!DOCTYPE html>\n");
      out.write("<html>\n");
      out.write("<head>\n");
      out.write("  <meta charset=\"utf-8\">\n");
      out.write("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
      out.write("  <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n");
      out.write("  <title>COMP 4321 Project Group 14</title>\n");
      out.write("</head>\n");
      out.write("<body>\n");
      out.write("  <header>\n");
      out.write("    <nav>\n");
      out.write("      <ul>\n");
      out.write("        <li><a href=#>About</a></li>\n");
      out.write("        <li><a href=#>Store</a></li>\n");
      out.write("      </ul>\n");
      out.write("      <ul>\n");
      out.write("        <li><a class=\"small-text\" href=#>Gmail</a></li>\n");
      out.write("        <li><a class=\"small-text\" href=#>Images</a></li>\n");
      out.write("        <li class=\"menu-bg\"><img\n");
      out.write("          class=\"menu-button\"\n");
      out.write("          src=\"images/menu.png\"\n");
      out.write("          alt=\"menu button\">\n");
      out.write("        </li>\n");
      out.write("        <li><img\n");
      out.write("          class=\"profile-pic\"\n");
      out.write("          src=\"images/profile.png\"\n");
      out.write("          alt=\"profile pic\">\n");
      out.write("        </li>\n");
      out.write("      </ul>\n");
      out.write("    </nav>\n");
      out.write("  </header>\n");
      out.write("\n");
      out.write("  <section class=\"search-wrapper\">\n");
      out.write("    <img\n");
      out.write("      class=\"logo\"\n");
      out.write("      src=\"images/logo.png\"\n");
      out.write("      alt=\"logo\">\n");
      out.write("    <form method=\"get\" action=\"SearchPage.jsp\">\n");
      out.write("      <img\n");
      out.write("        class=\"search\"\n");
      out.write("        src=\"images/search.png\">\n");
      out.write("      <img\n");
      out.write("        class=\"mic\"\n");
      out.write("        src=\"images/mic.png\">\n");
      out.write("      <input type=\"text\" name=\"search_query\">\n");
      out.write("      <div class=\"buttons\">\n");
      out.write("        <button type=\"submit\">Google Search</button>\n");
      out.write("        <button type=\"button\">I'm Feeling Lucky</button>\n");
      out.write("      </div>\n");
      out.write("    </form>\n");
      out.write("\n");
      out.write("\n");
      out.write("  </section>\n");
      out.write("\n");
      out.write("  <footer>\n");
      out.write("    <div class=\"panel\">\n");
      out.write("      <p>United Kingdom</p>\n");
      out.write("    </div>\n");
      out.write("    <div class=\"panel\">\n");
      out.write("      <ul>\n");
      out.write("        <li><a href=#>Advertising</a></li>\n");
      out.write("        <li><a href=#>Business</a></li>\n");
      out.write("        <li><a href=#>How Search Works</a></li>\n");
      out.write("      </ul>\n");
      out.write("      <div class=\"carbon\">\n");
      out.write("        <img\n");
      out.write("          class=\"leaf\"\n");
      out.write("          src=\"images/leaf.png\"\n");
      out.write("          alt=\"leaf\">\n");
      out.write("        <a href=#>Carbon neutral since 2007</a>\n");
      out.write("      </div>\n");
      out.write("      <ul class=\"right\">\n");
      out.write("        <li><a href=#>Privacy</a></li>\n");
      out.write("        <li><a href=#>Terms</a></li>\n");
      out.write("        <li><a href=#>Settings</a></li>\n");
      out.write("      </ul>\n");
      out.write("    </div>\n");
      out.write("  </footer>\n");
      out.write("\n");
      out.write("</body>\n");
      out.write("</html>");
    } catch (java.lang.Throwable t) {
      if (!(t instanceof jakarta.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try {
            if (response.isCommitted()) {
              out.flush();
            } else {
              out.clearBuffer();
            }
          } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
