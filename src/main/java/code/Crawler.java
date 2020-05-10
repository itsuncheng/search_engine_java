package code;

import java.util.Vector;
import java.net.URL;
import org.htmlparser.beans.StringBean;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import java.util.regex.Pattern;
import org.htmlparser.beans.LinkBean;


/**
 * A class used to crawl
 */
public class Crawler
{
    private String url;
    private Parser parser;


    /**
     * Constructor
     * @param _url the url to be crawl
     */
    public Crawler(String _url){
        url = _url;
        try{
            parser = new Parser(url);
        }catch (ParserException pe){
            pe.printStackTrace();
        }

    }

    /**
     * Get all the links in specific page
     * @return links in the page
     * @throws ParserException
     */
    public Vector<String> extractLinks() throws ParserException

    {
        // extract links in url and return them
        Vector<String> result = new Vector<String>();
        LinkBean bean = new LinkBean();
        bean.setURL(url);
        URL[] urls = bean.getLinks();
        for (URL s : urls) {
            result.add(s.toString());
        }
        return result;

    }

    /**
     * Get the page title (lower case)
     * @return the page title
     * @throws ParserException
     */
    public String getPageTitle() throws ParserException
    {
        String result = null;
        parser.setResource(url);
        NodeFilter filter = new NodeClassFilter(TitleTag.class);
        NodeList nodeList = parser.parse(filter);
        result = ((TitleTag) nodeList.elementAt(0)).getTitle();
        return result.toLowerCase();
    }

    /**
     * Get the page body content (lower case)
     * @return the page body content
     * @throws ParserException
     */
    public String getPageBody() throws ParserException
    {
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        String contents = bean.getStrings().substring(getPageTitle().length());
        contents = Pattern.compile("[\\W\\d]").matcher(contents).replaceAll(" ");// eliminate the special char and number
        contents = Pattern.compile("\\s+").matcher(contents).replaceAll(" ");// eliminate empty char
        return contents.toLowerCase();
    }

    /**
     * Get the last modified day
     * @return the last modified day
     * @throws ParserException
     */
    public String getLastModDay() throws ParserException
    {
    	String lastMod = parser.getConnection().getHeaderField("last-modified");
    	if (lastMod == null) {
    		lastMod = parser.getConnection().getHeaderField("date");
    	}
        return lastMod;
    }

    /**
     * Get the page size
     * @return the size of page
     * @throws ParserException
     */
    public int getSizeOfPage() throws ParserException
    {
    	int size = parser.getConnection().getContentLength();
    	if (size==-1) {
    		StringBean bean = new StringBean();
            bean.setURL(url);
            bean.setLinks(false);
            String contents = bean.getStrings();
            size=contents.length();
    	}
        return size;
    }


    public static void main(String[] args) {

        try {
            Crawler crawler = new Crawler("http://www.cse.ust.hk");
        	System.out.println(crawler.getPageTitle());
        	System.out.println(crawler.getPageBody());
        	System.out.println(crawler.getLastModDay());
        	System.out.println(crawler.getSizeOfPage());
//            for (String link : crawler.extractLinks()) {
//                crawler = new code.Crawler(link);
//                System.out.println(crawler.getPageBody());
//            }
//            System.out.println(crawler.getLastModDay());
//            System.out.println(crawler.getSizeOfPage());
        	
        }catch (ParserException pe){
            pe.printStackTrace();
        }

    }
}