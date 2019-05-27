package data;
import com.geccocrawler.gecco.annotation.*;
import com.geccocrawler.gecco.spider.HtmlBean;

import java.util.ArrayList;
import java.util.List;



public class
DoubanData implements HtmlBean{
    @Text
    @HtmlField(cssPath = "#wrapper > h1 > span")
    public String bookname;
    public String oriBookname;
    public String oriAuthor;
    public String producer;



    @Text
    @HtmlField(cssPath = "#info > span:nth-child(8)")
    public String subTitle;
    @HtmlField(cssPath = "#info > span:nth-child(1) > a")
    public String author;

    @HtmlField(cssPath = "#info > br:nth-child(4)")
    public String publish;
    public String publishTime;
    public String pageNumber;
    public String price;
    public String binging;
    public String ISBN;
    public String seriesOfBook;
    public String translator;
    public String contentIntro;
    public String directory;
    public String promotion;//推介
    public List<String> tags=new ArrayList<String>();
    public RatingData ratingData=new RatingData();
    public List<WhereBuyData> whereBuyData=new ArrayList<WhereBuyData>();
    public String authorInfo;
    @HtmlField(cssPath = "#collector > p:nth-child(5) > a")
    public String readingNum;
    @HtmlField(cssPath = "#collector > p:nth-child(6) > a")
    public String readedNum;
    @HtmlField(cssPath = "#collector > p:nth-child(7) > a")
    public String wantReadNum;

    @Override
    public String toString() {
        return String.format("name: %s, price: %s, isbn: %s, author: %s", this.bookname, this.price, this.ISBN, this.author);
    }
}
