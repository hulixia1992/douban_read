package douban_book_detail.data;

import com.geccocrawler.gecco.annotation.HtmlField;
import com.geccocrawler.gecco.spider.HtmlBean;

public class RatingData implements HtmlBean {
    @HtmlField(cssPath = "#interest_sectl > div > div.rating_self.clearfix > strong")
    public String ratingNum;
    @HtmlField(cssPath = "")
    public String peopleNum;

    @HtmlField(cssPath = "#interest_sectl > div > span:nth-child(5)")
    public String fiveRating;

    @HtmlField(cssPath = "#interest_sectl > div > span:nth-child(9)")
    public String fourRating;

    @HtmlField(cssPath = "#interest_sectl > div > span:nth-child(13)")
    public String threeRating;

    @HtmlField(cssPath = "#interest_sectl > div > span:nth-child(17))")
    public String twoRating;

    @HtmlField(cssPath = "#interest_sectl > div > span:nth-child(21)")
    public String oneRating;

}
