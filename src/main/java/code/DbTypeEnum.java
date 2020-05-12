package code;

/**
 * A enum class to manage databases
 */
public enum DbTypeEnum
{
//    TitleInvertedFile("TitleInvertedFile", new Database("src/main/DB/TitleInvertedFile")),
//    BodyInvertedFile("BodyInvertedFile", new Database("src/main/DB/BodyInvertedFile")),
//    Word_ID_Bi("Word_ID_Bi", new Database("src/main/DB/Word_ID_Bi")),
//    Page_ID_Bi("Page_ID_Bi", new Database("src/main/DB/Page_ID_Bi")),
//    PageID_Links("PageID_Links", new Database("src/main/DB/PageID_Links")),
//    PageID_PageInfo("PageID_PageInfo", new Database("src/main/DB/PageID_PageInfo")),
//    ForwardIndex("ForwardIndex", new Database("src/main/DB/ForwardIndex")),
//    ;
	
//	TitleInvertedFile("TitleInvertedFile", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/TitleInvertedFile")),
//	BodyInvertedFile("BodyInvertedFile", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/BodyInvertedFile")),
//	Word_ID_Bi("Word_ID_Bi", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/Word_ID_Bi")),
//	Page_ID_Bi("Page_ID_Bi", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/Page_ID_Bi")),
//	PageID_Links("PageID_Links", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/PageID_Links")),
//	PageID_PageInfo("PageID_PageInfo", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/PageID_PageInfo")),
//	ForwardIndex("ForwardIndex", new Database("/Users/raymondcheng/Documents/HKUST/Year 2/2020 Spring/COMP 4321/Project/COMP4321_project/apache-tomcat-9.0.34/webapps/ROOT/DB/ForwardIndex")),
//	;
	
	TitleInvertedFile("TitleInvertedFile", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/TitleInvertedFile")),
	BodyInvertedFile("BodyInvertedFile", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/BodyInvertedFile")),
	Word_ID_Bi("Word_ID_Bi", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/Word_ID_Bi")),
	Page_ID_Bi("Page_ID_Bi", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/Page_ID_Bi")),
	PageID_Links("PageID_Links", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/PageID_Links")),
	PageID_PageInfo("PageID_PageInfo", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/PageID_PageInfo")),
	ForwardIndex("ForwardIndex", new Database("/root/apache-tomcat-9.0.34/webapps/ROOT/DB/ForwardIndex")),
	;

    private String name;
    private Database database;

    DbTypeEnum(String name, Database database) {
        this.name = name;
        this.database = database;
    }

    public static DbTypeEnum getDbtypeEnum(String name){
        for (DbTypeEnum dbTypeEnum : DbTypeEnum.values()) {
            if (dbTypeEnum.getName().equals(name)){
                return dbTypeEnum;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public Database getDatabase() {
        return database;
    }

}
