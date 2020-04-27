import java.util.*;

public class PageInfo {
    private String title;
    private String url;
    private String lastModDay;
    private int pageSize;
    private Map<String, Integer> keywordFreq;
    private Vector<String> childLink;
    private Vector<String> parentLink;

    public PageInfo(String title, String url, String lastModDay, int pageSize) {
        this.title = title;
        this.url = url;
        this.lastModDay = lastModDay;
        this.pageSize = pageSize;
        this.keywordFreq = new HashMap<String, Integer>();
        this.childLink = new Vector<String>();
        this.parentLink = new Vector<String>();
    }

    public void addKeyword(String keyword){
        if(keywordFreq.containsKey(keyword)){
            keywordFreq.put(keyword,keywordFreq.get(keyword)+1);
        }else{
            keywordFreq.put(keyword,1);
        }
    }

    public void addChildLink(String url){
        if(url != this.url && !childLink.contains(url)){
            childLink.add(url);
        }
    }

    public void addParentLink(String url){
        if(url != this.url && !parentLink.contains(url)){
            parentLink.add(url);
        }
    }


    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getLastModDay() {
        return lastModDay;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getBasicInfo(){
        return getTitle()+","+getLastModDay()+","+getPageSize()+" ";
    }

    public Map<String, Integer> getKeywordFreq() {
        return keywordFreq;
    }

    public String getKeywordFreqInfo(){
        String result = "";
        Object[] arr = keywordFreq.keySet().toArray();
        Arrays.sort(arr);
        for(Object key : arr){
            result += key + "," + keywordFreq.get(key) +" ";
        }
        return result;
    }

    public Vector<String> getChildLink(){return childLink;}

    public String getChildLinkInfo() {
        String result = "";
        for (String s : childLink) {
            result += s+",";
        }
        return result;
    }

    public Vector<String> getParentLink(){return parentLink;}

    public String getParentLinkInfo() {
        String result = "";
        for (String s : parentLink) {
            result += s+",";
        }
        return result;
    }
}