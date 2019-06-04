package douban_book_img;

import com.google.gson.Gson;
import douban_book_detail.data.proxydata.ProxyDataItem;
import douban_book_detail.data.proxydata.ProxyDataResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.UnzippingInterceptor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static utils.Utils.isTextEmpty;
import static utils.Utils.saveErrorUrl;

/**
 * 下载图片工具类
 */
public class PicUtils {
    private static Proxy proxy = null;
    private static String PROXY_URL;
    private static ArrayBlockingQueue<ProxyDataItem> proxies = new ArrayBlockingQueue<>(5);
    private static final OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(new UnzippingInterceptor())
            .followRedirects(false)
            .followSslRedirects(false);
    private static OkHttpClient client = null;
    /**
     * 图片保存的路径
     */
    public static String SAVE_IMG_PATH = "D:/other/douban_img/";

    //链接url下载图片
    public static void downloadPicture(String imgUrl, String ISBN) throws IOException {
        URL url = null;

        url = new URL(imgUrl);
        DataInputStream dataInputStream = new DataInputStream(url.openStream());

        FileOutputStream fileOutputStream = new FileOutputStream(new File(SAVE_IMG_PATH + ISBN + ".jpg"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = dataInputStream.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }
        fileOutputStream.write(output.toByteArray());
        dataInputStream.close();
        fileOutputStream.close();
    }

    private static ProxyDataItem takeProxy() throws IOException, InterruptedException {
        while (proxies.size() == 0) {
            getProxy();
        }

        return proxies.take();
    }

    private static void getProxy() throws IOException, InterruptedException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(PROXY_URL).build();
        ProxyDataResponse pd = null;
        while (true) {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.out.println("代理API请求失败：" + response.code());
                Thread.sleep(10 * 1000);
                continue;
            }

            Gson gson = new Gson();
            pd = gson.fromJson(Objects.requireNonNull(response.body()).string(), ProxyDataResponse.class);
            if (!pd.code.equals("10001")) {
                System.out.println("代理API返回码错误：" + pd.code);
                Thread.sleep(10 * 1000);
                continue;
            }

            break;
        }

        for (ProxyDataItem p :
                pd.data.proxy_list) {
            proxies.put(p);
        }
    }

    public static DoubanImgData parseData(String url) throws IOException, InterruptedException {
        Request request = new Request.Builder().url(url).build();

        while (true) {
//            System.out.println("进入获取html");
//            if (proxy == null) {
//                ProxyDataItem data = takeProxy();
//                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.ip, Integer.parseInt(data.port)));
//            }
            if (client == null) {
                client=builder.build();
                //client = builder.proxy(proxy).build();
            }


            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    System.out.println("Response status code: " + response.code());
                    if (response.code() == 404) {
                        DoubanImgData data = new DoubanImgData();
                        saveErrorUrl(url + ":" + "404");
                        return data;
                    }
                    proxy = null;
                    client = null;

                    continue;
                }
                ResponseBody responseBody = Objects.requireNonNull(response.body());

                String html = responseBody.string();
                if (isTextEmpty(html)) {
                    System.out.println("获取html为空，重试");
                    proxy = null;
                    client = null;

                    continue;
                }

                DoubanImgData data = getData(html);
                if (isTextEmpty(data.ISBN)) {
                    if (html.contains("你想访问的条目豆瓣不收录")) {
                        System.out.println("你想访问的条目豆瓣不收录: " + url);
                    }
                    System.out.println("解析HTML失败: " + html);
                    proxy = null;
                    client = null;
                    saveErrorUrl(url + ":解析HTML失败");
                    continue;
                }
                return data;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                proxy = null;
                client = null;
            }
        }
    }

    private static DoubanImgData getData(String html) throws Exception {
        DoubanImgData data = new DoubanImgData();

        Document document = Jsoup.parse(html);


        if (document.select("#info").size() > 0) {
            Elements infoEle = document.select("#info").get(0).children();
            // if()
         //   data.imgUrl = document.select("#mainpic > a").attr("href");
            data.imgUrl = document.select("#mainpic > a > img").attr("src");
            for (Element element : infoEle) {
                if (element.text().contains("ISBN")) {
                    data.ISBN = element.nextSibling().outerHtml();
                }
            }
        }
        return data;
    }
}
