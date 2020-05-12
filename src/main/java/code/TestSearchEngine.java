package code;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.rocksdb.RocksDBException;

public class TestSearchEngine {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
    	Vector<String> queries = new Vector<String>();
		queries.add("hkust");
//		queries.add("Computer Science");
//		queries.add("News");
		try {
			List<Map.Entry<String, Double>> search_results = SearchEngine.search(queries, 10);
	        
	        Vector<String> results = new Vector<String>();
	        Vector<Double> scores = new Vector<Double>();
	        for(Map.Entry<String, Double> entry : search_results){
	            results.add(entry.getKey());
	            scores.add(entry.getValue());
	        }
	        
			for (int i=0; i<results.size(); i++) {
				String[] output = SearchEngine.pageId_to_pageInfo(results.get(i), scores.get(i));
				
				String score = output[0];
				String title = output[1];
				String URL = output[2];
				String lastModDay = output[3];
				String sizeOfPage = output[4];
				String topWords = output[5];
				String parentLinks = output[6];
				String childLinks = output[7];
				System.out.println("Score: " + score);
				System.out.println("Title: " + title); 
				System.out.println("URL: " + URL);
				System.out.println("Last Mod Day: " + lastModDay + ", Size of Page: " + sizeOfPage);
				System.out.println("Top frequent words and their frequency: " + topWords);
				System.out.println("Parent Links:\n" + parentLinks);
				System.out.println("Child Links:\n" + childLinks);
			}
		} catch (RocksDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//	    File directory = new File("src/main/DB");
//        if (directory.isDirectory()) {
//        	String[] files = directory.list();
//        	for (String file: files) {
//        		System.out.println(file);
//        	}
//        	if (directory.length() > 0) {
//        		System.out.println("The directory " + directory.getPath() + " is not empty");
//        	} else {
//        		System.out.println("The directory " + directory.getPath() + " is empty");
//        	}
//        }
	}

}
