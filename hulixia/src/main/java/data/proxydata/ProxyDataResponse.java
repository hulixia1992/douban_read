package data.proxydata;

import com.google.gson.annotations.SerializedName;

public class ProxyDataResponse {
    @SerializedName("code")
    public String code;
    @SerializedName("msg")
    public String msg;
    @SerializedName("data")
    public ProxyData data;
}
