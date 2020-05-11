import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.util.*;

public class SearchEngine {

    static final int TOTAL_NUM_PAGES = 30;      // The total number of pages
    static final Double TITLE_BONUS_WEIGHT = 1.0;   // The title bonus weight
    static StopStem stopStem = new StopStem("src/main/stopwords.txt");

    public SearchEngine() {

    }

    /**
     * Simple Search (not phrase)
     * @param inputQuery the query string (one word)
     * @return weightMap the map from pageID to weight
     * @throws RocksDBException
     */
    public static Map<String, Double> simple_search(String inputQuery) throws RocksDBException{
        Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
        Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
        Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();


//        // Pre-process the query, store in query
//        for(String w : inputQuery){
//            // remove stop word
//            if(!stopStem.isStopWord(w)){
//                query.add(stopStem.stem(w));
//            }
//        }

        // Pre-process the input query
        String query = stopStem.stem(inputQuery);

        // Get the wordID of the query word
        byte[] wordID = word_ID_Bi.getDb().get(query.getBytes());

        // query word not found
        if(wordID == null) return null;

        // A map from pageID to query word weight
        Map<String, Double> weightMap = new HashMap<String, Double>();

        /*
         * TITLE SEARCH
         */
        // Add bonus weight if query word found in title

        // Check whether wordID is in titleInvertedFile database

        if(titleInvertedFile.getDb().get(wordID) != null){
            String content = new String(titleInvertedFile.getDb().get(wordID));
            for(String s : content.split(" ")){
                String pageID = s.split(",")[0]; //pageID is the first item seperated by commas

                Double weight = weightMap.get(pageID);
                if(weight == null) weight = 0.0;
                weight += TITLE_BONUS_WEIGHT;
                weightMap.put(pageID, weight);
            }
        }

        /*
         * BODY SEARCH
         */

        if(bodyInvertedFile.getDb().get(wordID) != null){
            String content = new String(bodyInvertedFile.getDb().get(wordID));

            // calculate document frequency
            Double df = (double) content.split(" ").length;

            // calculate inverse document frequency
            Double idf = Math.log(TOTAL_NUM_PAGES / df) / Math.log(2);

            // loop each page contains in the query term's posting list
            for(String s : content.split(" ")){
                byte[] pageID = s.split(",")[0].getBytes();

                // calculate term frequency
                Double tf = (double) s.split(",").length - 1.0;

                // calculate maximum term frequency
                Double max_tf = calculateMaxTf(pageID);

                if(max_tf <= 0.0) continue;;

                Double weight = (tf / max_tf) * idf;

                // update weightMap using the calculated weight
                Double sumWeight = weightMap.get(new String(pageID));
                if(sumWeight == null) sumWeight = 0.0;
                sumWeight += weight;
                weightMap.put(new String(pageID), sumWeight);
            }
        }

        return weightMap;
    }


    /**
     * Phrase Search (like "computer Science")
     * @param inputQuery a String of words seperated by space
     * @return weightMap the map from pageID to weight
     * @throws RocksDBException
     */
    public static Map<String, Double> phrase_search(String inputQuery) throws RocksDBException{
        Database word_ID_Bi = DbTypeEnum.getDbtypeEnum("Word_ID_Bi").getDatabase();
        Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
        Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();
        Database forwardIndex = DbTypeEnum.getDbtypeEnum("ForwardIndex").getDatabase();

        Vector<String> query = new Vector<String>();
        // Pre-process the input query
        for(String w : inputQuery.split(" ")){
            // remove stop word
            if(!stopStem.isStopWord(w)){
                query.add(stopStem.stem(w));
            }
        }
        int querySize = query.size();

        // a map from pageID to query word weight
        Map<String, Double> weightMap = new HashMap<String, Double>();

        // consider the first term
        byte[] wordID = word_ID_Bi.getDb().get(query.get(0).getBytes());
        // word not found
        if(wordID == null) return null;


        /*
         * TITLE SEARCH
         */
        // Add bonus weight if query word found in title

        // Check whether wordID is in titleInvertedFile database

        if(titleInvertedFile.getDb().get(wordID) != null){
            // intersect map of or words in the phrase
            Map<String, String> intersectMap = convertToMap(wordID, 0);

            for(String w : query){
                if(w == query.get(0)) continue; // skip the first term

                wordID = word_ID_Bi.getDb().get(w.getBytes());

                // query term not found
                if(wordID == null || titleInvertedFile.getDb().get(wordID) == null){
                    intersectMap.clear();
                    break;
                }

                Map<String, String> tmpMap = convertToMap(wordID, 0);
                // intersect the maps
                intersectMap.keySet().retainAll(tmpMap.keySet());
            }

            // intersection exists
            if(!intersectMap.isEmpty()){
                // check whether exactly phrase exists by checking position consecutivity

                // pageID of pages containing this phrase
                Vector<String> pagesContainPhrase = new Vector<String>();

                // check each page in intersetion map
                for(Map.Entry<String, String> entry : intersectMap.entrySet()){
                    String pageID = entry.getKey();
                    String wordPosition = entry.getValue();

                    for(String s : wordPosition.split(",")){
                        int delta = 1; // the increment of word position
                        boolean found = true;

                        for(String w : query){  // check each query item
                            if (w == query.get(0)) continue; //skip the first term
                            wordID = word_ID_Bi.getDb().get(w.getBytes());
                            boolean contain = containWordPos(pageID, wordID, Integer.parseInt(s) + delta, 0);
                            if(!contain){
                                found = false;
                                break;
                            }
                            delta++;
                        }
                        if(found){
                            Double weight = weightMap.get(pageID);
                            if(weight == null) weight = 0.0;
                            weight += TITLE_BONUS_WEIGHT;
                            weightMap.put(pageID, weight);
                        }
                    }
                }
            }
        }


        /*
         * BODY SEARCH
         */

        // Check whether wordID is in bodyInvertedFile database

        if(bodyInvertedFile.getDb().get(wordID) != null){
            // intersect map of or words in the phrase
            Map<String, String> intersectMap = convertToMap(wordID, 1);

            for(String w : query){
                if(w.equals(query.get(0))) continue; // skip the first term

                wordID = word_ID_Bi.getDb().get(w.getBytes());

                // query term not found
                if(wordID == null || bodyInvertedFile.getDb().get(wordID) == null){
                    intersectMap.clear();
                    break;
                }

                Map<String, String> tmpMap = convertToMap(wordID, 1);
                // intersect the maps
                intersectMap.keySet().retainAll(tmpMap.keySet());
            }

            // intersection exists
            if(!intersectMap.isEmpty()){
                // check whether exactly phrase exists by checking position consecutivity

                // pageID of pages containing this phrase
                Vector<String> pagesContainPhrase = new Vector<String>();
                // map from pageId to normalized tf
                Map<String, Double> normTf = new HashMap<String, Double>();

                // check each page in intersection map
                for(Map.Entry<String, String> entry : intersectMap.entrySet()){
                    String pageID = entry.getKey();
                    String wordPosition = entry.getValue();
                    Double tf = 0.0;

                    for(String s : wordPosition.split(",")){
                        int delta = 1; // the increment of word position
                        boolean found = true;

                        for(String w : query){  // check each query item
                            if (w == query.get(0)) continue; //skip the first term
                            wordID = word_ID_Bi.getDb().get(w.getBytes());
                            boolean contain = containWordPos(pageID, wordID, Integer.parseInt(s) + delta, 1);
                            if(!contain){
                                found = false;
                                break;
                            }
                            delta++;
                        }
                        if(found){
                            // phrase found, term frequency increase
                            if(!pagesContainPhrase.contains(pageID))
                                pagesContainPhrase.add(pageID);
                            tf++;
                        }
                    }

                    // calculate weight
                    if(tf > 0){
                        // calculate maximum term frequency
                        Double max_tf = calculateMaxTf(pageID.getBytes());
                        if(max_tf <= 0.0) continue;
                        // store in map
                        normTf.put(pageID, tf / max_tf);
                    }
                }

                // calculate idf
                Double df = (double) pagesContainPhrase.size();
                Double idf = Math.log(TOTAL_NUM_PAGES / df) / Math.log(2);

                //calculate weight for each page
                for(Map.Entry<String, Double> entry : normTf.entrySet()){
                    String pageID = entry.getKey();

                    Double weight = weightMap.get(pageID);
                    if(weight == null) weight = 0.0;
                    // weight = (tf / max_tf) * idf
                    weight += entry.getValue() * idf;

                    //update weight map
                    weightMap.put(pageID, weight);
                }
            }
        }
        return weightMap;
    }

    /**
     * Calculate the maximum term frequency of a page
     * @param pageID corresponding pageID
     * @return max_tf the maximum term frequency
     * @throws RocksDBException
     */
    private static Double calculateMaxTf(byte[] pageID) throws RocksDBException{
        Database forwardIndex = DbTypeEnum.getDbtypeEnum("ForwardIndex").getDatabase();
        Double max_tf = 0.0;
        String termFrequency = new String(forwardIndex.getDb().get(pageID));
        for (String p : termFrequency.split(" ")){ // loop all the keyword to find the maximum term frequency
            Double _tf = Double.parseDouble(p.split(",")[1]);
            if(_tf > max_tf){
                max_tf = _tf;
            }
        }
        return max_tf;
    }

    /**
     * Convert byte[] content in inverted files to map (from pageID to word positions)
     * @param wordID
     * @param type 0 means in title inverted file, 1 means in body inverted file
     * @return map a map from pageID to word positions
     * @throws RocksDBException
     */
    private static Map<String, String> convertToMap(byte[] wordID, int type) throws RocksDBException{
        Map<String, String> map = new HashMap<String, String>();
        String content = null;
        if(type == 0){
            Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
            if(titleInvertedFile.getDb().get(wordID) != null){
                content = new String(titleInvertedFile.getDb().get(wordID));
            }
        }
        else{
            Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();
            if(bodyInvertedFile.getDb().get(wordID) != null){
                content = new String(bodyInvertedFile.getDb().get(wordID));
            }
        }

        if(content != null) {
            for (String s : content.split(" ")) {
                String pageID = s.split(",")[0];
                String wordPos = s.substring(pageID.length() + 1);
                map.put(pageID, wordPos);
            }
        }

        return map;
    }

    /**
     * Check whether a page contains a word in a specific position
     * @param pageID the pageID of the page
     * @param wordID the wordID of the word
     * @param wordPos the specific word position
     * @param type 0 means in title, 1 means in body
     * @return true or false
     * @throws RocksDBException
     */
    private static boolean containWordPos(String pageID, byte[] wordID, int wordPos, int type) throws RocksDBException{
        Database titleInvertedFile = DbTypeEnum.getDbtypeEnum("TitleInvertedFile").getDatabase();
        Database bodyInvertedFile = DbTypeEnum.getDbtypeEnum("BodyInvertedFile").getDatabase();

        String content = null;
        if(type == 0){
            if(titleInvertedFile.getDb().get(wordID) != null){
                content = new String(titleInvertedFile.getDb().get(wordID));
            }
        }
        else{
            if(bodyInvertedFile.getDb().get(wordID) != null){
                content = new String(bodyInvertedFile.getDb().get(wordID));
            }
        }

        if(content != null) {
            for(String s : content.split(" ")) {
                if (!s.split(",")[0].equals(pageID)) continue;
                for (String pos : s.split(",")) {
                    if (pos.equals(pageID)) continue;
                    if (Integer.parseInt(pos) == wordPos) return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Search union of phrases and single words, return the best pages
     *
     * @param inputQuery the input query vector contains single words string and phrase string, e.g. "Computer Science", "UST"
     * @param numResults number of results needed
     * @return results a vector of pageID
     * @throws RocksDBException
     */
    public static Vector<String> search(Vector<String> inputQuery, int numResults) throws RocksDBException{
        // weight map
        Map<String, Double> sumWeightMap = new HashMap<String, Double>();
        // score map
        Map<String, Double> scoreMap = new HashMap<String, Double>();

        Vector<String> query = new Vector<String>();

        for(String t : inputQuery){
            if(stopStem.isStopWord(t)) continue;
            query.add(t);
            // weight map of a single word
            Map<String, Double> weightMap;
            if(t.split(" ").length == 1){
                weightMap = simple_search(t);
            }
            else weightMap = phrase_search(t);

            if(weightMap != null) {
                for (Map.Entry<String, Double> entry : weightMap.entrySet()) {
                    String pageID = entry.getKey();
                    Double weight = sumWeightMap.get(pageID);
                    if (weight == null) weight = 0.0;
                    weight += entry.getValue();
                    //update sum up weight map
                    sumWeightMap.put(pageID, weight);
                }
            }
        }

        Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
        //calculate the score of each pages
        for(Map.Entry<String, Double> entry : sumWeightMap.entrySet()){
            String pageID = entry.getKey();
            Double sumWeight = entry.getValue();

            String content = new String(pageID_PageInfo.getDb().get(pageID.getBytes()));
            // get sum of words which is the page size
            int sumWord = Integer.parseInt(content.split(",")[content.split(",").length-1]);
            Double documentLength = Math.sqrt(sumWord);
            Double queryLength = Math.sqrt(query.size());
            // calculate score
            Double score = sumWeight / (documentLength * queryLength);
            //update score map
            scoreMap.put(pageID, score);
        }

        // sort the value of the score map
        List<Map.Entry<String, Double> > list = new ArrayList(scoreMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue() > o1.getValue() ? 1 : -1;
            }
        });

        // record the result
        Vector<String> result = new Vector<String>();
        for(Map.Entry<String, Double> entry : list){
            result.add(entry.getKey());
            if(result.size() == numResults) break;
        }

        return result;
    }
    
    /**
     * Fetch pageInfo of given pageId
     * @param pageId
     * @return pageInfo - a string contains page title, URL, last modification date and page size
     * @throws RocksDBException
     */
    public static String pageId_to_pageInfo(String pageId) throws RocksDBException{
        // Database needed
        Database pageID_PageInfo = DbTypeEnum.getDbtypeEnum("PageID_PageInfo").getDatabase();
        Database page_ID_Bi = DbTypeEnum.getDbtypeEnum("Page_ID_Bi").getDatabase();

        String content = new String(pageID_PageInfo.getDb().get(pageId.getBytes()));
        String[] info = content.split(",");
        String output = "Page title: " + info[0] + "\nURL: " + new String(page_ID_Bi.getDb().get(pageId.getBytes())) + "\nLast modification date: " + info[1]
                + " " + info[2] + " \nsize of page: " + info[3] + "\n";
        return output;
    }

//    public static void main(String[] args){
//        Vector<String> queries = new Vector<String>();
//        queries.add("HKUST");
//        queries.add("Computer Science");
//        try{
//            Vector<String> search_result = SearchEngine.search(queries, 10);
//        } catch (RocksDBException e){
//            e.printStackTrace();
//        }
//    }
}
