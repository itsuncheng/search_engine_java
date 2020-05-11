package code;

import java.util.*;

public class PageInfo {
    private String pageID;
    private String title;
    private String url;
    private String lastModDay;
    private int pageSize;
    private Map<String, Integer> keywordFreq;
    private Vector<String> childLink;
    private Vector<String> parentLink;

    public PageInfo(String pageID, String title, String url, String lastModDay, int pageSize) {
        this.pageID = pageID;
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

    public String getPageID() {
        return pageID;
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

    public Vector<String> getChildLink(){return childLink;}

    public Vector<String> getParentLink(){return parentLink;}

}
