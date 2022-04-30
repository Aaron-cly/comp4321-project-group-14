<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.time.*"%>
<%@ page import="indexer.Indexer" %>
<%@ page import="engine.SearchEngine"%>
<%@ page import="repository.Repository"%>
<%@ page import="model.RetrievedDocument"%>


<!doctype html>
<html>
	<head>
		<title> &nbsp;&nbsp; Google </title>
		
		<link rel="stylesheet" href="stylesheets/reset.css">
		<link rel="stylesheet" href="stylesheets/styles.css">
	</head>
	
	<body>
		<div id="header">
			<div id="logoWrapper">
				<div id="logo" onclick="window.location='/'">
				</div>
			</div>
		
			<div id="searchQueryWrapper">
				<div id="searchForm">
                        <!-- PrintWriter out = res.getWriter();  -->
					<form action="SearchPage.jsp" method="get">
                        <%
                        String yes = request.getParameter("search_query");

                        out.println("<input type='text' name='search_query' id='searchQuery' value='" + yes + "'>");
                        %>
                        <button type='submit' id='submit'></button>
					</form>
					<div id="searchIcon"></div>
				</div>
			</div>
			
			<div id="headerRight">
				<div id=headerRightContent>
					<!-- <a href="#"><span>+user</span></a>&nbsp;&nbsp;&nbsp;  -->
					<div id="apps">&nbsp;</div>&nbsp;&nbsp;&nbsp;
					<div id="notifications">&nbsp;</div>&nbsp;&nbsp;&nbsp;
					<div id="googleShare">&nbsp;</div>&nbsp;&nbsp;&nbsp;
					<div id="dp">&nbsp;</div>	
				</div>
			</div>
		</div>
	 
		 <div id="searchOptions">
			<div id="bottomLine">
			</div>
			<div id="placeholder">
			</div>
			<div id="soWeb">
				Web
			</div>
		 </div>


		 

         <%
String query = request.getParameter("search_query");
// do stuff

Repository.closeAllConnections();

if (query != null){
		Instant before = Instant.now();
    Repository.openConnections();
    List<RetrievedDocument> results = SearchEngine.processQuery(query);
		Instant after = Instant.now();
		long delta = Duration.between(before, after).toMillis(); 
		String output = "About "+ results.size() +" results ( "+ ((float)delta / 1000.0) + " seconds)";
	
			out.println("<div id='searchInfo'><span>" + output + "</span></div>");
	
    for(int i = 0; i < results.size() && i < 50; i++){
        out.println(results.get(i).htmlString());
    }


}%>
		 
		<div id="footer">
			<a href="#"><span id="help">Help</span></a> <a href="#"><span>Send feedback</span></a> <a href="#"><span>Privacy</span></a> <a href="#"><span>Terms</span></a>
		</div>
	</body>
</html>