import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;


public class InvertedIndex
{
    private RocksDB db;
    private Options options;

    InvertedIndex(String dbPath) throws RocksDBException
    {
        this.options = new Options();
        this.options.setCreateIfMissing(true);
        this.db = RocksDB.open(options, dbPath);
    }

    public void addPageInfo(PageInfo pageInfo) throws RocksDBException
    {
        // different type information separated by "'"
        // every links information separated by ","
        String content = pageInfo.getTitle()+"'"+pageInfo.getLastModDay()+"'"+pageInfo.getPageSize()+"'";

        Vector<String> childLink = pageInfo.getChildLink();
        for (int i = 0; i < childLink.size(); i++) {
            content += childLink.elementAt(i)+",";
        }
        content += "'";

        Vector<String> parentLink = pageInfo.getParentLink();
        for (int i = 0; i < parentLink.size(); i++) {
            content += parentLink.elementAt(i)+",";
        }
        content += "'";

        for (Map.Entry<String, Integer> entry : pageInfo.getKeywordFreq().entrySet()) {
            content += entry.getKey()+" "+entry.getValue()+";";
        }
        content += "'";

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

    public void printAll() throws RocksDBException
    {

        try {
            File file = new File("src/main/spider_result.txt");
            PrintWriter pw = new PrintWriter(file);

            RocksIterator iter = db.newIterator();
            for(iter.seekToFirst(); iter.isValid(); iter.next()) {
                String[] info = new String(iter.value()).split("'");
                String output = "Page title: " + info[0] + "\nURL: " + new String(iter.key()) + "\nLast modification date: " + info[1]
                        + ", size of page: " + info[2] + "\n";
                // words
                output += info[5] + "\n";
                // children links
                for (String s : info[3].split(",")) {
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


}