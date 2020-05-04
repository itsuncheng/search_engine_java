import org.htmlparser.util.ParserException;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Indexer {
    private Crawler crawler;
    private StopStem stopStem;
    private Vector<String> visited;
    private Vector<String> waitList;
    private Vector<PageInfo> pages;
    private Map<String, String> parentLinks;

    public Indexer(String rootURL, int numOfPage) {
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
    }

    private void BFS(String rootURL, int numOfPage){
        try{
            Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
            // index first root url first
            String firstInWaitlist = rootURL;
            if(pageID_PageInfo.needUpdate(firstInWaitlist, crawler.getLastModDay())){
                pageID_PageInfo.delEntry(firstInWaitlist);
            }
            visited.add(firstInWaitlist);
            Vector<String> childLink = crawler.extractLinks();
            waitList.addAll(childLink);
            int count = 1;
            recordParentLinks(firstInWaitlist, childLink);
            indexing(rootURL,childLink);
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
                if(pageID_PageInfo.needUpdate(firstInWaitlist, crawler.getLastModDay())){
                    pageID_PageInfo.delEntry(firstInWaitlist);
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
            Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
            Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();
            Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
            Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
            Database pageID_Links = DbTypeEnum.getDbtypeEnum("PageID_Links").getDatabase();
            Database forwardIndex = DbTypeEnum.getDbtypeEnum("ForwardIndex").getDatabase();

            // initialize page information data structure
            String title = crawler.getPageTitle();
            String pageID = page_ID_Bi.IdBiConversion(URL);
            PageInfo pageInfo = new PageInfo(pageID, title, URL, crawler.getLastModDay(), crawler.getSizeOfPage());

            for (String cl : childLink) {
                page_ID_Bi.IdBiConversion(cl);
                pageInfo.addChildLink(cl);
            }
            // add title words to database
            indexWord(pageID, title, titleInvertedFile, pageInfo);
            // add body words to database
            indexWord(pageID, crawler.getPageBody(), bodyInvertedFile, pageInfo);
            pageID_PageInfo.addBasicPageInfo(pageInfo);
            pageID_Links.addLinks(pageInfo);
            forwardIndex.addKeywordFreq(pageInfo);
            pages.add(pageInfo);

        }catch(ParserException pe){
            pe.printStackTrace();
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }
    // index word and add keyword to pageInfo
    public void indexWord(String pageID, String words, Database dbfile, PageInfo pageInfo) throws RocksDBException {
        Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
        String[] wordList = words.trim().split(" ");
        String result;
        for (int i = 0; i < wordList.length; i++) {
            if(!stopStem.isStopWord(wordList[i])){
                result = stopStem.stem(wordList[i]);
                if(result.length() > 0) {
                    String wordID = word_ID_Bi.IdBiConversion(wordList[i]);
                    dbfile.addWord(wordID, pageID, i+1);
                    pageInfo.addKeyword(wordList[i]);//
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

    public static void main(String[] args) {
        try{
            Indexer in = new Indexer("http://www.cse.ust.hk", 30);
            Database pageID_PageInfo = new Database("src/main/DB/PageID_PageInfo");
            Database pageID_Links = new Database("src/main/DB/PageID_Links");
            Database forwardIndex = new Database("src/main/DB/ForwardIndex");

            Database.printAll(pageID_PageInfo.getDb(), pageID_Links.getDb(), forwardIndex.getDb());
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }
}
