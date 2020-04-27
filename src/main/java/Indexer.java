import org.htmlparser.util.ParserException;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Indexer {
    private InvertedIndex titleFile;
    private InvertedIndex bodyFile;
    private InvertedIndex pageInfoFile;
    private InvertedIndex linkFile;
    private InvertedIndex wordFile;
    private Crawler crawler;
    private StopStem stopStem;
    private Vector<String> visited;
    private Vector<String> waitList;
    private Vector<PageInfo> pages;
    private Map<String, String> parentLinks;

    public Indexer(String rootURL, int numOfPage) {
        try{
            this.titleFile = new InvertedIndex("src/main/DB/titleDB");
            this.bodyFile = new InvertedIndex("src/main/DB/bodyDB");
            this.pageInfoFile = new InvertedIndex("src/main/DB/pageDB");
            this.linkFile = new InvertedIndex("src/main/DB/linkDB");
            this.wordFile = new InvertedIndex("src/main/DB/wordDB");
            this.crawler = new Crawler(rootURL);
            this.stopStem = new StopStem("src/main/stopwords.txt");
            this.visited = new Vector<String>();
            this.waitList = new Vector<String>();
            this.pages = new Vector<PageInfo>();
            this.parentLinks = new HashMap<String, String>();
            //crawling
            BFS(rootURL, numOfPage);
            //add parent link
            for (PageInfo page : pages) {
                String[] link = parentLinks.get(page.getUrl()).split(",");
                for (String l : link) {
                    page.addParentLink(l);
                }
            }

        }catch (RocksDBException ex){
            ex.printStackTrace();
        }
    }

    private void BFS(String rootURL, int numOfPage){
        try{
            // index first root url first
            String firstInWaitlist = rootURL;
            if(pageInfoFile.needUpdate(firstInWaitlist, crawler.getLastModDay())){
                pageInfoFile.delEntry(firstInWaitlist);
            }
            visited.add(firstInWaitlist);
            Vector<String> childLink = crawler.extractLinks();
            waitList.addAll(childLink);
            int count = 1;
            recordParentLinks(firstInWaitlist, childLink);
            indexing(firstInWaitlist,childLink);
            // index remaining url up to max page set by user
            while(!waitList.isEmpty() && count <= numOfPage){
                // no more link
                if(waitList.isEmpty()) break;
                //next link
                firstInWaitlist = waitList.firstElement();
                //check duplicate: if yes jump to next
                if (visited.contains(firstInWaitlist)) {
                    waitList.remove(firstInWaitlist);
                    continue;
                }
                //update db
                crawler = new Crawler(firstInWaitlist);
                if(pageInfoFile.needUpdate(firstInWaitlist, crawler.getLastModDay())){
                    pageInfoFile.delEntry(firstInWaitlist);
                }
                //successful extract information from a link
                childLink = crawler.extractLinks();
                waitList.addAll(childLink);
                waitList.remove(firstInWaitlist);
                visited.add(firstInWaitlist);
                count++;
                recordParentLinks(firstInWaitlist, childLink);
                indexing(firstInWaitlist, childLink);
            }
        }catch (ParserException e)
        {
            e.printStackTrace ();
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }

    public void indexing(String URL, Vector<String> childLink){
        try{
            // initialize page information data structure
            String title = crawler.getPageTitle();
            PageInfo pageInfo = new PageInfo(title, URL, crawler.getLastModDay(), crawler.getSizeOfPage());
            for (String cl : childLink) {
                pageInfo.addChildLink(cl);
            }
            // add title words to database
            indexWord(URL, title, titleFile, pageInfo);
            // add body words to database
            indexWord(URL, crawler.getPageBody(), bodyFile, pageInfo);
            pageInfoFile.addBasicPageInfo(pageInfo);
            linkFile.addLinks(pageInfo);
            wordFile.addKeywordFreq(pageInfo);
            pages.add(pageInfo);

        }catch(ParserException pe){
            pe.printStackTrace();
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }
    // index word and add keyword to pageInfo
    public void indexWord(String URL, String words, InvertedIndex dbfile, PageInfo pageInfo) throws RocksDBException {
    	String[] wordList = words.trim().split(" ");
        String result;
        for (int i = 0; i < wordList.length; i++) {
            if(!stopStem.isStopWord(wordList[i])){
                result = stopStem.stem(wordList[i]);
                if(result.length() > 0) {
                    dbfile.addWord(wordList[i], URL, i+1);
                    pageInfo.addKeyword(wordList[i]);
                }
            }
        }
    }
    // record parent link to pageInfo
    public void recordParentLinks(String parentURL, Vector<String> childLink){
        //pair up children and parent
        for (String cl : childLink) {
            if (cl != parentURL){// impossible for its parent is itself
                String parentLink = parentLinks.get(cl);
                if(parentLink == null){
                    parentLink = parentURL+",";
                }else{
                    parentLink += parentURL+",";
                }
                parentLinks.put(cl,parentLink);
            }
        }
    }

    public InvertedIndex getTitleFile() {
        return titleFile;
    }

    public InvertedIndex getBodyFile() {
        return bodyFile;
    }

    public InvertedIndex getPageInfoFile() {
        return pageInfoFile;
    }

    public InvertedIndex getWordFile() {return wordFile;}

    public InvertedIndex getLinkFile() {return linkFile;}

    public static void main(String[] args) {
        try{
            Indexer in = new Indexer("http://www.cse.ust.hk", 30);
            in.getPageInfoFile().printAll(in.getWordFile().getDb(), in.getLinkFile().getDb());
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }
}
