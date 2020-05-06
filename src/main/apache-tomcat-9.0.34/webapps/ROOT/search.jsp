<%@ page import="java.util.*" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="skeleton.css">
    <title>Document</title>
</head>
<body>
    <div style="position:absolute; top:10%; left:10%; ">
        <b>The words you entered are:</b>
        <br>
        <%
            String s = request.getParameter("txtname");
            if(s != null){
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
                for (String w : words_phrases) {
                    out.println(w + "<br>");
                }
            } 
        %>
    </div>
</body>
</html>