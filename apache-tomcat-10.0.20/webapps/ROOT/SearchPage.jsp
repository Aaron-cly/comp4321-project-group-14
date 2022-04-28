<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="indexer.Indexer" %>
<%@ page import="engine.SearchEngine"%>
<%@ page import="repository.Repository"%>
<%@ page import="model.RetrievedDocument"%>

<html>

<head>
    <title>COMP 4321 Project Group 14</title>
</head>

<body>

    <form method="post" action="SearchPage.jsp">
        <input type="text" name="search_query">
        <input type="submit" value="Submit">
    </form>

<%
String query = request.getParameter("search_query");

if (query != null){
    Repository.openConnections();
    List<RetrievedDocument> results = SearchEngine.processQuery(query);
    for(int i = 0; i < results.size(); i++){
        out.println(results.get(i).htmlString());
    }

    Repository.closeAllConnections();
}%>



</body>

</html>