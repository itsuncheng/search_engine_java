<%@ page import="java.net.*, java.io.*, java.util.*, java.text.*, javax.servlet.*, javax.servlet.http.*,
org.json.*, org.rocksdb.*, org.rocksdb.util.*" %>
<%@ page import="code.*" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="skeleton.css">
    <title>Document</title>
</head>
<body>
   <!--  <div style="position:absolute; top:10%; left:10%; "> -->
   <div style="margin: auto; width: 80%; padding: 10px;">
        <h5>Search Results:</h5>
        <br>
        <%
            String s = request.getParameter("txtname");
        	int numResults = Integer.parseInt(request.getParameter("numresults"));
            String[] words = s.split(" ");
            ArrayList<String> words_phrases = new ArrayList<String>();
            int index=0;
            for (int i=0; i<words.length; i++) {
                if (words[i].charAt(0)=='"'){
                    String phrase = "";
                    if (words[i].charAt(words[i].length()-1)=='"'){
                        phrase = words[i].substring(1, words[i].length()-1);
                        words_phrases.add(phrase);
                        continue;
                    }
                    phrase = words[i].substring(1);
                    for (int j=i+1; j<words.length; j++){
                        int length = words[j].length();
                        if (words[j].charAt(length-1)=='"'){
                            phrase+=" " + words[j].substring(0, length-1);
                            i = j;
                            break;
                        }else{
                            phrase+=" " + words[j];
                        }
                    }
                    words_phrases.add(phrase);
                }else{
                    words_phrases.add(words[i]);
                }
            }
/*             for (String w : words_phrases) {
                out.println(w + "<br>");
            } */
            
            
            /* String dbPath = "/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB";
    	    File directory = new File(dbPath);
    	    if (directory.isDirectory()) {
    	    	String[] files = directory.list();
    	    	if (directory.length() > 0) {
    	    		System.out.println("The directory " + directory.getPath() + " is not empty");
    	    	} else {
    	    		System.out.println("The directory " + directory.getPath() + " is empty");
    	    	    Indexer in = new Indexer("http://www.cse.ust.hk", 30);
    	    	    Database.printAll();
    	    	}
    	    } */
    	    
    		Vector<String> queries = new Vector<String>();
    		for (String w : words_phrases) 
    			queries.add(w);
    		try {
    			List<Map.Entry<String, Double>> search_results = SearchEngine.search(queries, numResults);
    	        Vector<String> results = new Vector<String>();
    	        Vector<Double> scores = new Vector<Double>();
    	        for(Map.Entry<String, Double> entry : search_results){
    	            results.add(entry.getKey());
    	            /* out.println("score: " + entry.getValue()); */
    	            scores.add(entry.getValue());
    	        }
    	        for (int i=0; i<results.size(); i++) {
    				String[] output = SearchEngine.pageId_to_pageInfo(results.get(i), scores.get(i));
    				String score = output[0];
    				String title = output[1];
    				String URL = output[2];
    				String lastModDay = output[3];
    				String sizeOfPage = output[4];
    				String topWords = output[5];
    				String parentLinks = output[6].replaceAll("\n","<br>");
    				String childLinks = output[7].replaceAll("\n","<br>");
    				
    				out.println("<div class=\"row\">");
    				out.println("<div class=\"three columns\">");
    				out.println("<b>Score: </b>" + score);
    				out.println("</div>");
    				out.println("<div class=\"nine columns\">");
    				out.println("<b>Title: </b><a href=\"" + URL + "\">"+ title + "</a><br>"); 
    				out.println("<b>URL: </b><a href=\"" + URL + "\">"+ URL + "</a><br>");
    				out.println("<b>Last modification date: </b>" + lastModDay + ", <b>size of page: </b>" + sizeOfPage + "<br>");
    				out.println("<b>Top frequent words and their frequency: </b>" + topWords + "<br>");
    				out.println("<b>Parent Links: </b><br>" + parentLinks);
    				out.println("<b>Child Links: </b><br>" + childLinks + "<br>");
    				out.println("</div>");
    				out.println("</div>");
    			}
    		} catch (RocksDBException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            
        %>
<!--         <div class="row">
		    <div class="three columns">One</div>
		    <div class="nine columns">Eleven Eleven  Eleven Eleven Eleven Eleven  Eleven Eleven</div>
		</div> -->
		<a href="www.google.com"></a>
    </div>
</body>
</html>