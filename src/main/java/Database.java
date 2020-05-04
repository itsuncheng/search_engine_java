import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class Database
{
    private RocksDB db;
    private Options options;

    Database(String dbPath)
    {
        try {
            this.options = new Options();
            this.options.setCreateIfMissing(true);
            this.db = RocksDB.open(options, dbPath);
        }catch (RocksDBException re){
            re.printStackTrace();
        }
    }

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

    public void addBasicPageInfo(PageInfo pageInfo) throws RocksDBException
    {
        // information separated by ,
        String content = pageInfo.getBasicInfo();
        db.put(pageInfo.getPageID().getBytes(), content.getBytes());
    }

    public void addLinks(PageInfo pageInfo) throws RocksDBException
    {
        Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
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

    public void addKeywordFreq(PageInfo pageInfo) throws RocksDBException
    {
        // change the keyword to wordID
        // keyword and its frequency are separated by ,
        // different set of keyword are separated by a space
        Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
        String content = "";
        Map<String, Integer> keywordFreq = pageInfo.getKeywordFreq();
        for (String key : keywordFreq.keySet()) {
            content += word_ID_Bi.IdBiConversion(key) +","+ keywordFreq.get(key) +" ";
        }
        db.put(pageInfo.getPageID().getBytes(), content.getBytes());
    }

    // record pageID and position y in order to perform phase search
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

    public void delEntry(String key) throws RocksDBException {
        db.remove(key.getBytes());
    }

    public static void printAll(RocksDB pageFile, RocksDB wordFile, RocksDB linkFile) throws RocksDBException
    {

        try {
            Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
            Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
            File file = new File("src/main/spider_result.txt");
            PrintWriter pw = new PrintWriter(file);

            RocksIterator iter = pageFile.newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next()) {
                String[] info = new String(iter.value()).split(",");
                String output = "Page title: " + info[0] + "\nURL: " + new String(iter.key()) + "\nLast modification date: " + info[1]
                        + ", size of page: " + info[2] + "\n";
                // words
                String[] content = new String(wordFile.get(iter.key())).split(" ");
                for (String s : content) {
                    String[] key_value = s.split(",");
                    output+= word_ID_Bi.IdBiConversion(key_value[0])+","+ key_value[1]+" ";
                }
                output += "\n";
                // children links
                String[] links = new String(linkFile.get(iter.key())).split(" ");
                for (String s : links[0].split(",")) {
                    output += page_ID_Bi.IdBiConversion(s) + "\n";
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

    public boolean needUpdate(String URL, String lastModDay) throws RocksDBException{
        Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();
        String pageID = page_ID_Bi.IdBiConversion(URL);
        String content = new String(db.get(pageID.getBytes()));
        if (content == null) return false;
        else {
            if (content.contains(lastModDay))return true;
            return false;
        }
    }

    public RocksDB getDb(){
        return db;
    }

}