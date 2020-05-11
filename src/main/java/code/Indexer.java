package code;

import org.htmlparser.util.ParserException;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * A class used to index
 */
public class Indexer {
    private Crawler crawler;
    private StopStem stopStem;
    private Vector<String> visited;
    private Vector<String> waitList;
    private Vector<PageInfo> pages;
    private Map<String, String> parentLinks;

    /**
     * Constructor and finish crawling and index
     * @param rootURL the root url used to crawl
     * @param numOfPage the number of page want to crawl
     */
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
        addParentLink();
    }

    /**
     * A BFS algorithm used to crawl
     * @param rootURL the root url used to crawl
     * @param numOfPage the number of page want to crawl
     */
    private void BFS(String rootURL, int numOfPage){
        try{
            Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
            Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
            // index first root url first
            String firstInWaitlist = rootURL;
            if(pageID_PageInfo.needUpdate(firstInWaitlist, crawler.getLastModDay(), page_ID_Bi)){
                String pageID = page_ID_Bi.IdBiConversion(firstInWaitlist);
                deleteIndex(pageID);
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
                if(pageID_PageInfo.needUpdate(firstInWaitlist, crawler.getLastModDay(), page_ID_Bi)){
                    String pageID = page_ID_Bi.IdBiConversion(firstInWaitlist);
                    deleteIndex(pageID);
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

    /**
     * Index the url
     * @param URL url to be crawl
     * @param childLink the children link of the url
     */
    public void indexing(String URL, Vector<String> childLink){
        try{
            Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
            Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
            Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
            Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();
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
            pageID_Links.addLinks(pageInfo, page_ID_Bi,0);
            forwardIndex.addKeywordFreq(pageInfo, word_ID_Bi);
            pages.add(pageInfo);

        }catch(ParserException pe){
            pe.printStackTrace();
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }

    /**
     * Index word and add keyword to pageInfo
     * @param pageID the page id of page to be index
     * @param words the words to be index
     * @param dbfile the database file used to save the data
     * @param pageInfo the page information of the page given
     * @throws RocksDBException
     */
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

    /**
     * Delete the page id (key) which want to update
     * @param pageID the page id to be delete
     */
    public void deleteIndex(String pageID) throws RocksDBException{
        DbTypeEnum.getDbtypeEnum("ForwardIndex").getDatabase().delEntry(pageID);
        Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
        Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();
        titleInvertedFile.deletePageIDForWord(pageID);
        bodyInvertedFile.deletePageIDForWord(pageID);
    }

    /**
     * Record parent link to pageInfo
     * @param parentURL the parent link to be record
     * @param childLink the child link need to pair with the parent link
     */
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


    /**
     * Add parent link to related page in database
     */
    private void addParentLink(){
        Database pageID_Links = DbTypeEnum.getDbtypeEnum("PageID_Links").getDatabase();
        Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
        try {
            for (PageInfo page : pages) {
                String[] link = parentLinks.get(page.getUrl()).split(",");
                for (String l : link) {
                    page.addParentLink(l);
                }
                pageID_Links.addLinks(page, page_ID_Bi,1);
            }
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try{
            Indexer in = new Indexer("http://www.cse.ust.hk", 30);
            Database.printAll();
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }
}
