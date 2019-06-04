package douban_book_detail.data;

import com.geccocrawler.gecco.annotation.PipelineName;
import com.geccocrawler.gecco.pipeline.Pipeline;
import com.geccocrawler.gecco.request.HttpRequest;
import com.geccocrawler.gecco.scheduler.SchedulerContext;

@PipelineName("searchPipeLine")
public class SearchPipeLine implements Pipeline<SearchData> {
    public void process(SearchData searchData) {
        String detailUrl = searchData.getDetailUrl();
        HttpRequest currRequest=searchData.getRequest();
        System.out.println("爬虫结果： "+ searchData.getContent());
        SchedulerContext.into(currRequest.subRequest(detailUrl));
    }
}
