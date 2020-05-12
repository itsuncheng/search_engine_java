<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
trimDirectiveWhitespaces="true" session="true" %>
<%@ page import="java.net.*, java.io.*, java.util.*, java.text.*, javax.servlet.*, javax.servlet.http.*,
org.json.*, org.rocksdb.*, org.rocksdb.util.*, code.*" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="skeleton.css">
    <title>Form example</title>
</head>
<body>
    <div style="position:absolute; top:35%; right:0; left:0; text-align:center; ">
        <h3>Search Engine</h3>
        <form action="DBController" method="POST">
            Please enter a list of words, separated by space (use double quotes for phrases):
            <br><br>
            <input style="width: 50%;" type="text" name="txtname" placeholder="&quot;Computer Science&quot; HKUST" required>
            <br>
            <input class="button-primary" type="submit" value="Submit">
        </form>
    </div>
    </div>
    <% 	
	    Vector<String> queries = new Vector<String>();
		queries.add("HKUST");
		queries.add("Computer Science");
		Vector<String> search_results = SearchEngine.search(queries, 10);
    %>
</body>
</html>