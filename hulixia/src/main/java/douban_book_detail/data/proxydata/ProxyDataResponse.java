package douban_book_detail.data.proxydata;

import com.google.gson.annotations.SerializedName;

public class ProxyDataResponse {
    @SerializedName("code")
    public String code;
    @SerializedName("msg")
    public String msg;
    @SerializedName("douban_book_detail/data")
    public ProxyData data;
}
