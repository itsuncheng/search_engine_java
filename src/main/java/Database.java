import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;


public class Database
{
    public RocksDB db;
    private Options options;

    Database(String dbPath) throws RocksDBException
    {
        this.options = new Options();
        this.options.setCreateIfMissing(true);
        this.db = RocksDB.open(options, dbPath);
    }

    public void addBasicPageInfo(PageInfo pageInfo) throws RocksDBException
    {
        // information separated by ,
        String content = pageInfo.getBasicInfo();
        db.put(pageInfo.getUrl().getBytes(), content.getBytes());
    }

    public void addLinks(PageInfo pageInfo) throws RocksDBException
    {
        // same type of link separated by , and child links and parent links are separated by space
        String content = pageInfo.getChildLinkInfo() + " "+ pageInfo.getParentLinkInfo();
        db.put(pageInfo.getUrl().getBytes(), content.getBytes());

    }

    public void addKeywordFreq(PageInfo pageInfo) throws RocksDBException
    {
        // keyword and its frequency are separated by ,
        // different set of keyword are separated by space
        String content = pageInfo.getKeywordFreqInfo();
        db.put(pageInfo.getUrl().getBytes(), content.getBytes());
    }

    // record pageID and position y in order to perform phase search
    public void addWord(String word, String pageID, int y) throws RocksDBException
    {
        byte[] content = db.get(word.getBytes());
        if (content == null) {
            content = (pageID + "," + y).getBytes();
        } else {
            content = (new String(content) + " " + pageID + "," + y).getBytes();
        }
        db.put(word.getBytes(), content);
    }

    public void delEntry(String key) throws RocksDBException {
        db.remove(key.getBytes());
    }

    public void printAll(RocksDB wordFile, RocksDB linkFile) throws RocksDBException
    {

        try {
            File file = new File("src/main/spider_result.txt");
            PrintWriter pw = new PrintWriter(file);

            RocksIterator iter = db.newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next()) {
                String[] info = new String(iter.value()).split(",");
                String output = "Page title: " + info[0] + "\nURL: " + new String(iter.key()) + "\nLast modification date: " + info[1]
                        + ", size of page: " + info[2] + "\n";
                // words
                output += new String(wordFile.get(iter.key())) + "\n";
                // children links
                String[] links = new String(linkFile.get(iter.key())).split(" ");
                for (String s : links[0].split(",")) {
                    output += s + "\n";
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

    public boolean needUpdate(String url, String lastModDay){
        RocksIterator iter = db.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            if( url == (new String(iter.key())) ){
                if((new String(iter.value())).contains(lastModDay))
                    return true;
            }
        }
        return false;
    }

    public RocksDB getDb(){
        return db;
    }

}