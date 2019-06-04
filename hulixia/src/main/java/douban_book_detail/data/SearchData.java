package douban_book_detail.data;

import com.geccocrawler.gecco.annotation.*;
import com.geccocrawler.gecco.request.HttpRequest;
import com.geccocrawler.gecco.spider.HtmlBean;


@Gecco(matchUrl ="https://book.douban.com/subject_search?search_text=9787506365437",pipelines = {"searchPipeLine"})
public class SearchData implements HtmlBean {
    public HttpRequest getRequest() {
        return request;
    }


    public void setRequest(HttpRequest request) {
        this.request = request;
    }
    @RequestParameter
    private int isbn;
    @Request
    private HttpRequest request;

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
    //#wrapper#wrapper
//#root > div > div._5scqbiaga > div._s8oergugd > div:nth-child(1) > div > div > div > p > span:nth-child(1) > a
    //Text代表解析text文档（html代表解析html文档，attr代表解析标签属性等）
//#root > div > div._5scqbiaga > div._s8oergugd > div:nth-child(1) > div > div > a
    //#root > div > div._1rnh7rwcl > div._efuqfu6co > div:nth-child(1) > div > div > a
    @Href(click=true)
    @HtmlField(cssPath = "div#root > div.detail > div.title > a")
    private String detailUrl;


    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    @Href(click=true)
    @HtmlField(cssPath = "#db-global-nav > div > div.top-nav-info > a")
    private String loginUrl;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @HtmlField(cssPath = "#wrapper")
    private String content;
}
