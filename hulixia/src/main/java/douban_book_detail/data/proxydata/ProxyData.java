package douban_book_detail.data.proxydata;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProxyData {
    @SerializedName("count")
    public int count;
    @SerializedName("proxy_list")
    public List<ProxyDataItem> proxy_list;
}
