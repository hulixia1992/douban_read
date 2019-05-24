package data.proxydata;

import com.google.gson.annotations.SerializedName;

public class ProxyDataItem {
    @SerializedName("ip")
    public String ip;
    @SerializedName("port")
    public String port;
}
