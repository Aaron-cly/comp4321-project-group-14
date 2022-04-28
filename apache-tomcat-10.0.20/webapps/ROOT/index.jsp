<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="indexer.Indexer" %>
<%@ page import="engine.SearchEngine"%>
<%@ page import="repository.Repository"%>
<%@ page import="model.RetrievedDocument"%>


<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" type="text/css" href="style.css">
  <title>COMP 4321 Project Group 14</title>
</head>
<body>
  <header>
    <nav>
      <ul>
        <li><a href=#>About</a></li>
        <li><a href=#>Store</a></li>
      </ul>
      <ul>
        <li><a class="small-text" href=#>Gmail</a></li>
        <li><a class="small-text" href=#>Images</a></li>
        <li class="menu-bg"><img
          class="menu-button"
          src="images/menu.png"
          alt="menu button">
        </li>
        <li><img
          class="profile-pic"
          src="images/profile.png"
          alt="profile pic">
        </li>
      </ul>
    </nav>
  </header>

  <section class="search-wrapper">
    <img
      class="logo"
      src="images/logo.png"
      alt="logo">
    <form method="get" action="SearchPage.jsp">
      <img
        class="search"
        src="images/search.png">
      <img
        class="mic"
        src="images/mic.png">
      <input type="text" name="search_query">
      <div class="buttons">
        <button type="submit">Google Search</button>
        <button type="button">I'm Feeling Lucky</button>
      </div>
    </form>


  </section>

  <footer>
    <div class="panel">
      <p>United Kingdom</p>
    </div>
    <div class="panel">
      <ul>
        <li><a href=#>Advertising</a></li>
        <li><a href=#>Business</a></li>
        <li><a href=#>How Search Works</a></li>
      </ul>
      <div class="carbon">
        <img
          class="leaf"
          src="images/leaf.png"
          alt="leaf">
        <a href=#>Carbon neutral since 2007</a>
      </div>
      <ul class="right">
        <li><a href=#>Privacy</a></li>
        <li><a href=#>Terms</a></li>
        <li><a href=#>Settings</a></li>
      </ul>
    </div>
  </footer>

</body>
</html>