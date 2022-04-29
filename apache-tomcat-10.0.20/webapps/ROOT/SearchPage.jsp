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

                        out.println("<input type='text' name='search_query' id='searchQuery' value=' " + yes + "'>");
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

if (query != null){
		Instant before = Instant.now();
    Repository.openConnections();
    List<RetrievedDocument> results = SearchEngine.processQuery(query);
		Instant after = Instant.now();
		long delta = Duration.between(before, after).toMillis(); 
		String output = "About "+ results.size() +" results ( "+ ((float)delta / 1000.0) + " seconds)";
	
			out.println("<div id='searchInfo'><span>" + output + "</span></div>");
	
    for(int i = 0; i < results.size(); i++){
        out.println(results.get(i).htmlString());
    }

    Repository.closeAllConnections();
}%>


<!-- 		 
		 <div id="searchResults">
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Is there a working sample of the google custome search rest... </a>
				</div>
				<div class="url">
					stackoverflow.com/.../is-there-a-working-sample-of-the-google-custome-...
				</div>
				<div class="content">
				<span class="date">
					Mar 6, 2014 -
				</span>
					 The place to start with rest api is here: https://developers.google.com/custom-search/json-api/v1/introduction. Example: &lt;div id="content"&gt;&lt;/div&gt; ...
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Send a Sample Request - Google Developers </a>
				</div>
				<div class="url">
					https://developers.google.com/doubleclick-search/v2/requests
				</div>
				<div class="content">
				<span class="date">
					Sep 16, 2014 -
				</span>
					After you've set everything up, make sure you can successfully send a sample request to the DoubleClick Search API. The following code ...
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Code Samples - Google Maps JavaScript API v3 - Google ... </a>
				</div>
				<div class="url">
					https://developers.google.com/maps/documentation/javascript/examples/

				</div>
				<div class="content">
				<span class="date">
				</span>
					 All of the examples contained in the Google Maps JavaScript API ... details . Place search pagination . Search for up to 200 places with Radar Search . Place ... 
					 Simple Map - Geolocation - Simple markers - Localizing the Map
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">YouTube Data API: Python Code Samples - Google </a>
				</div>
				<div class="url">
					https://developers.google.com/youtube/v3/code_samples/python

				</div>
				<div class="content">
				<span class="date">
				</span>
					 The following code samples, which use the Google APIs Client Library ... Search by keyword ( search.list ); Search by topic ( search.list ); Add a ...

				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Sample Extensions - Google Chrome </a>
				</div>
				<div class="url">
					https://developer.chrome.com/extensions/samples

				</div>
				<div class="content">
				<span class="date">
					Jun 5, 2014 -
				</span>
					 My Bookmarks. A browser action with a popup dump of all bookmarks, including search, add, edit and delete. Calls: ... Context Menus Sample (with Event Page).
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">About Samples for Google App Engine - Google Code </a>
				</div>
				<div class="url">
					code.google.com/p/google-app-engine-samples/
				</div>
				<div class="content">
				<span class="date">
				</span>
					 About Samples for Google App Engine. This project contains code for sample applications that run on Google App Engine. All samples ... search, browse source.
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Google Custom Search Sample Code / Forums / Community /... </a>
				</div>
				<div class="url">
					https://ellislab.com/forums/viewthread/242368/

				</div>
				<div class="content">
				<span class="date">
					Jan 27, 2014 - 3 posts - 3 authors<br />
				</span>
					 Does anyone have sample code for integrating google custom search into expression engine? essentially. 1. you have a search field 2. enter ...
				</div>
			</div>
			<div class="resultWrapper">
				<div class="heading">
					<a href="#">Google Maps Sample Project | Getting started - SoapUI </a>
				</div>
				<div class="url">
					www.soapui.org &gt; Documentation &gt; Getting started &gt; API Sample Projects

				</div>
				<div class="content">
				<span class="date">
				</span>
					 The sample project for the Google Maps defines a number of requests for each of ... Nearby Search Sample: Performs a nearby search with given coordinates ...
				</div>
			</div>
			<div class="resultWrapper" id="last">
				<div class="heading">
					<a href="#">Quickstart - CasperJS 1.1.0-DEV documentation </a>
				</div>
				<div class="url">
					casperjs.readthedocs.org/en/latest/quickstart.html
				</div>
				<div class="content">
				<span class="date">
				</span>
					 In the following example, we'll query google for two terms consecutively, ... casper.start('http://google.fr/', function() { // search for 'casperjs' from google form ...
				</div>
			</div>
		 </div>
		  -->
		 <!-- <div id="relatedSearches">
			<hr />
			<div>
				<span id="relatedSearchesHead">
					Searches related to sample google search<br />
				</span>
				
				<a href="#">sample google <b>forms education</b></a>
				<br />
				<a href="#">sample google <b>docs spreadsheet</b></a>
				<br/>
				<a href="#">google <b>docs questionnaire template</b></a>
			</div>
			<hr />
		 </div>
		 
		 <div id="pageNo">
			<div id="pageNoInside">
				 <div id="Go"></div>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div class="o"></div></a>
				 <a href="#"><div id="gle"></div></a>
				 <a href="#"><a href="#"><div id="rightArrow"></div></a>
			 </div>
			 <div id="pageDigits">
				<span><b>1</b></span> <a href="#"><span>2</span></a> <a href="#"><span>3</span></a> <a href="#"><span>4</span></a> <a href="#"><span>5</span></a> <a href="#"><span>6</span></a> <a href="#"><span>7</span></a> <a href="#"><span>8</span></a> <a href="#"><span>9</span></a> <a href="#"><span>10</span></a> <a href="#"><span id="next">Next</span></a>
			 </div>
		</div> -->
		 
		<div id="footer">
			<a href="#"><span id="help">Help</span></a> <a href="#"><span>Send feedback</span></a> <a href="#"><span>Privacy</span></a> <a href="#"><span>Terms</span></a>
		</div>
	</body>
</html>