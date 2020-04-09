import java.util.Vector;
import java.net.URL;

import org.htmlparser.beans.StringBean;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import java.util.StringTokenizer;
import org.htmlparser.beans.LinkBean;



public class Crawler
{
    private String url;
    private Parser parser;

    //    private InvertedIndex index;
    Crawler(String _url){
        url = _url;
        try{
            parser = new Parser(url);
        }catch (ParserException pe){
            pe.printStackTrace();
        }


//        try
//        {
//            // a static method that loads the RocksDB C++ library.
//            RocksDB.loadLibrary();
//            // modify the path to your database
//            String path = "./spider_result.txt";
//            index = new InvertedIndex(path);
//        }
//        catch(RocksDBException e)
//        {
//            System.err.println(e.toString());
//        }

    }
    public Vector<String> extractWords() throws ParserException

    {
        // extract words in url and return them
        // use StringTokenizer to tokenize the result from StringBean
        Vector<String> result = new Vector<String>();
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        String contents = bean.getStrings();
        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        return result;

    }
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

    public String getPageTitle() throws ParserException
    {
        String result = null;
        parser.setResource(url);
        NodeFilter filter = new NodeClassFilter(TitleTag.class);
        NodeList nodeList = parser.parse(filter);
        result = ((TitleTag) nodeList.elementAt(0)).getTitle();
        return result;
    }

    public String getPageBody() throws ParserException
    {
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        String contents = bean.getStrings().substring(getPageTitle().length()).replaceAll("\\s+", " ");
        return contents;
    }

    public String getLastModDay() throws ParserException
    {
    	String lastMod = parser.getConnection().getHeaderField("last-modified");
    	if (lastMod == null) {
    		lastMod = parser.getConnection().getHeaderField("date");
    	}
        return lastMod;
    }

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
//                crawler = new Crawler(link);
//                System.out.println(crawler.getPageBody());
//            }
//            System.out.println(crawler.getLastModDay());
//            System.out.println(crawler.getSizeOfPage());
        	
        }catch (ParserException pe){
            pe.printStackTrace();
        }

    }
}