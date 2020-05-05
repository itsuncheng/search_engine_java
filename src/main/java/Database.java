import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


/**
 * A class used to store, update, delete or search the index files
 */
public class Database
{
    private RocksDB db;
    private Options options;

    /**
     * Initialize the constructor
     * @param dbPath the database path used to stored
     */
    public Database(String dbPath)
    {
        try {
            this.options = new Options();
            this.options.setCreateIfMissing(true);
            this.db = RocksDB.open(options, dbPath);
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }

    /**
     * Convert the keyword and url into id and save to related database
     * the id act as an integer which is started from 1 but use string to represent
     * # is record the max id reached
     * @param content the keyword or url want to store in the database
     * @return the id of the keyword or url
     * @throws RocksDBException
     */
    public String IdBiConversion(String content) throws RocksDBException{
        byte[] value = db.get("#".getBytes());
        byte[] id = db.get(content.getBytes());
        if (value == null){
            //use # to represent the max word id/number of word so save the # to 1 in first word
            //add word and word id bidirectional conversion in database
            db.put("#".getBytes(),"1".getBytes());
            db.put(content.getBytes(), "1".getBytes());
            db.put("1".getBytes(), content.getBytes());
            return "1";
        }else if(id == null){//no saved before
            // increase # by 1 and add word and word id conversion
            int max = Integer.parseInt(new String(value));
            byte[] newID = String.valueOf(max+1).getBytes();
            db.put("#".getBytes(), newID);
            db.put(content.getBytes(), newID);
            db.put(newID, content.getBytes());
            return String.valueOf(max+1);
        }else return new String(id);
    }

    /**
     * Add basic page information (title, url, last modified day and page size)
     * @param pageInfo the class with contain the basic page information
     * @throws RocksDBException
     */
    public void addBasicPageInfo(PageInfo pageInfo) throws RocksDBException
    {
        // information separated by ,
        String content = pageInfo.getBasicInfo();
        db.put(pageInfo.getPageID().getBytes(), content.getBytes());
    }

    /**
     * Add links (including child link and parent link)
     * @param pageInfo the class with contain the basic page information
     * @param page_ID_Bi the database saved page and page id
     * @throws RocksDBException
     */
    public void addLinks(PageInfo pageInfo, Database page_ID_Bi) throws RocksDBException
    {
        // change link to pageID
        // same type of link separated by , and child links and parent links are separated by a space
        String content = "";
        for (String s:pageInfo.getChildLink()){
            content += page_ID_Bi.IdBiConversion(s)+",";
        }
        content+=" ";

        for (String s : pageInfo.getParentLink()) {
            content += page_ID_Bi.IdBiConversion(s)+",";
        }
        db.put(pageInfo.getPageID().getBytes(), content.getBytes());

    }

    /**
     * Add keyword with its frequency
     * @param pageInfo the class with contain the basic page information
     * @param word_ID_Bi the database saved word and word id
     * @throws RocksDBException
     */
    public void addKeywordFreq(PageInfo pageInfo, Database word_ID_Bi) throws RocksDBException
    {
        // change the keyword to wordID
        // keyword and its frequency are separated by ,
        // different set of keyword are separated by a space
        String content = "";
        Map<String, Integer> keywordFreq = pageInfo.getKeywordFreq();
        for (String key : keywordFreq.keySet()) {
            content += word_ID_Bi.IdBiConversion(key) +","+ keywordFreq.get(key) +" ";
        }
        db.put(pageInfo.getPageID().getBytes(), content.getBytes());
    }

    /**
     * record pageID and position y in order to perform phase search
     * @param wordID the word id started from 1
     * @param pageID the word id started from 1
     * @param y the position of a word in a page after stemming
     * @throws RocksDBException
     */
    public void addWord(String wordID, String pageID, int y) throws RocksDBException
    {
        byte[] content = db.get(wordID.getBytes());
        if (content == null) {
            content = (pageID + "," + y).getBytes();
        } else {
            content = (new String(content) + " " + pageID + "," + y).getBytes();
        }
        db.put(wordID.getBytes(), content);
    }

    /**
     * Delete the data with a specific key
     * @param key the key want to delete
     * @throws RocksDBException
     */
    public void delEntry(String key) throws RocksDBException {
        db.remove(key.getBytes());
    }

    /**
     * Check if the page need update
     * @param URL the url used to check
     * @param lastModDay the last modified day of url
     * @param page_ID_Bi the database contain page and page id
     * @return true if need update otherwise false
     * @throws RocksDBException
     */
    public boolean needUpdate(String URL, String lastModDay, Database page_ID_Bi) throws RocksDBException{
        String pageID = page_ID_Bi.IdBiConversion(URL);
        byte[] content = db.get(pageID.getBytes());
        if (content == null) return false;
        else {
            if (new String(content).contains(lastModDay))return true;
            return false;
        }
    }

    /**
     * Used for test
     * @throws RocksDBException
     */
    public static void printAll() throws RocksDBException
    {
        Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
        Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
        Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
        Database pageID_Links = DbTypeEnum.getDbtypeEnum("PageID_Links").getDatabase();
        Database forwardIndex = DbTypeEnum.getDbtypeEnum("ForwardIndex").getDatabase();

        try {
            File file = new File("src/main/spider_result.txt");
            PrintWriter pw = new PrintWriter(file);

            RocksIterator iter = pageID_PageInfo.getDb().newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next()) {
                String[] info = new String(iter.value()).split(",");
                String output = "Page title: " + info[0] + "\nURL: " + new String(page_ID_Bi.getDb().get(iter.key())) + "\nLast modification date: " + info[1]
                        + ", size of page: " + info[2] + "\n";
                // words
                String content = new String(forwardIndex.getDb().get(iter.key()));
                for (String s : content.split(" ")) {
                    String[] key_value = s.split(",");
                    output+= new String(word_ID_Bi.getDb().get(key_value[0].getBytes()))+","+ key_value[1]+" ";
                }
                output += "\n";
                // children links
                String links = new String(pageID_Links.getDb().get(iter.key()));
                for (String s : links.split(" ")[0].split(",")) {
                    output += new String(page_ID_Bi.getDb().get(s.getBytes())) + "\n";
                }
                pw.write(output);
                pw.write("-------------------------------------------------------------------------------------------" +
                        "--------------------------------------------\n");
            }
            pw.close();
        }catch (IOException ie){
            ie.printStackTrace();
        }

    }

    /**
     * @return the rocksdb object
     */
    public RocksDB getDb(){
        return db;
    }

}