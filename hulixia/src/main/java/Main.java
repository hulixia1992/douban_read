import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import data.DoubanData;
import data.SaveInfoData;
import data.WhereBuyData;
import data.proxydata.ProxyDataItem;
import data.proxydata.ProxyDataResponse;
import okhttp3.*;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UnzippingInterceptor implements Interceptor {

    @Override

    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        return unzip(response);
    }

    // copied from okhttp3.internal.http.HttpEngine (because is private)
    private Response unzip(final Response response) throws IOException {
        if (response.body() == null) {
            return response;
        }

        //check if we have gzip response
        String contentEncoding = response.headers().get("Content-Encoding");

        //this is used to decompress gzipped responses
        if (contentEncoding != null && contentEncoding.equals("gzip")) {
            Long contentLength = response.body().contentLength();
            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders)
                    .body(new RealResponseBody(response.body().contentType().toString(), contentLength, Okio.buffer(responseBody)))
                    .build();
        } else {
            return response;
        }
    }
}

public class Main {
    private static int excelNum = 1;
    private static int rowNum = 0;
    private static int itemNum = 0;
    private static Proxy proxy = null;
    private static ArrayBlockingQueue<ProxyDataItem> proxies = new ArrayBlockingQueue<>(5);
    private static Stopwatch sw = Stopwatch.createStarted();

    private static final OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(new UnzippingInterceptor())
            .followRedirects(false)
            .followSslRedirects(false);
    private static OkHttpClient client = null;

    private static DoubanData parseData(String url, String id) throws IOException, InterruptedException {
        Request request = new Request.Builder().url(url).build();

        while (true) {
            System.out.println("进入获取html");
            if (proxy == null) {
                ProxyDataItem data = takeProxy();
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.ip, Integer.parseInt(data.port)));
            }
            if (client == null) {
                client = builder.proxy(proxy).build();
            }


            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    System.out.println("Response status code: "+ response.code());
                    if (response.code() == 404) {
                        DoubanData data = new DoubanData();
                        data.bookname = "404 NOT FOUND";
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

                DoubanData data = getData(html, id);
                if (isTextEmpty(data.bookname) && isTextEmpty(data.ISBN)) {
                    if (html.contains("你想访问的条目豆瓣不收录")) {
                        System.out.println("你想访问的条目豆瓣不收录: " + url);
                        DoubanData d = new DoubanData();
                        d.bookname = "404 NOT FOUND";
                        return d;
                    }
                    System.out.println("解析HTML失败: " + html);
                    proxy = null;
                    client = null;
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

    private static ProxyDataItem takeProxy() throws IOException, InterruptedException {
        while (proxies.size() == 0) {
            getProxy();
        }

        return proxies.take();
    }


    private static void getProxy() throws IOException, InterruptedException {
        String url = "http://www.zdopen.com/ShortProxy/GetIP/?api=201905242211089237&akey=190022e61c4b2fc4&order=2&type=3";
        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder().url(url).build();
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


    private static DoubanData getData(String html, String id) throws Exception {
        DoubanData data = new DoubanData();

        Document document = Jsoup.parse(html);
        data.bookname = document.select("div#wrapper > h1 > span").text();
        data.author = document.select("#info > span:nth-child(1) > a").text();
        if (isTextEmpty(data.author)) {
            data.author = document.select("#info > a:nth-child(2)").text();
        }

        if (document.select("#info").size() > 0) {
            Elements infoEle = document.select("#info").get(0).children();
            // if()

            for (Element element : infoEle) {
                if (element.text().contains("出版社")) {
                    data.publish = element.nextSibling().outerHtml();

                } else if (element.text().contains("副标题")) {
                    data.subTitle = element.nextSibling().outerHtml();

                } else if (element.text().contains("原作名")) {
                    data.oriAuthor = element.nextSibling().outerHtml();

                } else if (element.text().contains("出品方")) {
                    data.producer = element.nextSibling().outerHtml();
                    if (data.producer.equals("&nbsp;")) {
                        data.producer = element.nextElementSibling().text();
                    }

                } else if (element.text().contains("出版年")) {
                    data.publishTime = element.nextSibling().outerHtml();

                } else if (element.text().contains("页数")) {
                    data.pageNumber = element.nextSibling().outerHtml();

                } else if (element.text().contains("定价")) {
                    data.price = element.nextSibling().outerHtml();

                } else if (element.text().contains("装帧")) {
                    data.binging = element.nextSibling().outerHtml();

                } else if (element.text().contains("丛书")) {
                    data.seriesOfBook = element.nextSibling().outerHtml();
                    if (data.seriesOfBook.equals("&nbsp;")) {
                        data.seriesOfBook = element.nextElementSibling().text();
                    }

                } else if (element.text().contains("ISBN")) {
                    data.ISBN = element.nextSibling().outerHtml();

                } else if (element.text().contains("原作名")) {
                    data.oriBookname = element.nextSibling().outerHtml();

                } else if (element.children().size() > 0) {
                    if (element.children().get(0).text().contains("译者")) {
                        data.translator = element.children().get(1).text();
//                        System.out.println(data.translator);
                    }
                }

            }
        }

        //#link-report > span.short > div
//        data.contentIntro = String.join(System.lineSeparator(), document.select("#link-report > div > div.intro").select("p").eachText());
        if (document.select("#link-report > span.all.hidden > div > div.intro") != null) {
            data.contentIntro = String.join(System.lineSeparator(), document.select("#link-report > span.all.hidden > div > div.intro").select("p").eachText());
        } else {
            data.contentIntro = String.join(System.lineSeparator(), document.select("#link-report > div > div.intro").select("p").eachText());
        }

//        data.directory = String.join(System.lineSeparator(), document.select("#dir_" + id + "_full").eachText());
        Elements brs = document.select("#dir_" + id + "_full").select("br");
        List<String> dirs = new ArrayList<>();
        for (Element br :
                brs) {
            dirs.add(br.previousSibling().outerHtml().trim());
        }
        data.directory = String.join(System.lineSeparator(), dirs);

        //初始化标签
        if (document.select("#db-tags-section > div").size() > 0) {
            Elements tagEles = document.select("#db-tags-section > div").get(0).children();
            for (Element tagEle : tagEles) {
                data.tags.add(tagEle.child(0).text());
            }
        }
        //初始化评分数据
        //   String fiveRate=document.select("#interest_sectl > div > span:nth-child(5)").get(0).text();
        if (document.select("#interest_sectl > div > span:nth-child(5)").size() > 0) {
            data.ratingData.fiveRating = document.select("#interest_sectl > div > span:nth-child(5)").get(0).text();
//            System.out.println(data.ratingData.fiveRating);
            data.ratingData.ratingNum = document.select("#interest_sectl > div > div.rating_self > strong").text();
            data.ratingData.peopleNum = document.select("#interest_sectl > div > div.rating_self.clearfix > div > div.rating_sum > span > a > span").text();

            data.ratingData.fourRating = document.select("#interest_sectl > div > span:nth-child(9)").text();
            data.ratingData.threeRating = document.select("#interest_sectl > div > span:nth-child(13)").text();
            data.ratingData.twoRating = document.select("#interest_sectl > div > span:nth-child(17)").text();
            data.ratingData.oneRating = document.select("#interest_sectl > div > span:nth-child(21)").text();
//            System.out.println(data.ratingData.oneRating);
        }
        //初始化购买地方
        if (document.select("#buyinfo-printed > ul").size() > 0) {
            Elements whereBugEles = document.select("#buyinfo-printed > ul").get(0).children();
            for (Element whereBuyEle : whereBugEles) {
                if (isTextEmpty(whereBuyEle.attr("class")) && whereBuyEle.children().size() > 0) {
                    try {
                        WhereBuyData whereBuyData = new WhereBuyData();
                        whereBuyData.provider = whereBuyEle.children().get(0).children().get(0).text();
                        whereBuyData.link = whereBuyEle.children().get(0).attr("href");
                        whereBuyData.price = whereBuyEle.children().get(1).children().get(0).text();
//                        System.out.println(whereBuyData.price + whereBuyData.link + whereBuyData.provider);
                        data.whereBuyData.add(whereBuyData);
                    } catch (Exception ignored) {

                    }

                }
            }
        }
        Elements elements = document.select("#content > div > div.article > div.related_info").select("h2");
        for (Element element :
                elements) {
            if (element.text().contains("作者简介")) {
                if (element.nextElementSibling().select("span.all.hidden > div.intro") != null) {
                    data.authorInfo = String.join(System.lineSeparator(), element.nextElementSibling().select("span.all.hidden > div.intro").select("p").eachText());
                } else {
                    data.authorInfo = String.join(System.lineSeparator(), element.nextElementSibling().select("div.intro > p").eachText());
                }

            }
        }
        if (document.select("#collector").size() > 0) {
            Elements readInfoEles = document.select("#collector").get(0).children();
            for (Element readInfoEle : readInfoEles) {
                if (readInfoEle.attr("class").equals("pl")) {
                    String readInfo = readInfoEle.children().get(0).text();
                    if (readInfo.contains("在读")) {
                        data.readingNum = readInfo.substring(0, readInfo.indexOf("人"));
                    } else if (readInfo.contains("读过")) {
                        data.readedNum = readInfo.substring(0, readInfo.indexOf("人"));
                    } else if (readInfo.contains("想读")) {
                        data.wantReadNum = readInfo.substring(0, readInfo.indexOf("人"));
                    }
                }
            }
        }
        //初始化推介
        if (document.select("#db-rec-section > div").size() > 0) {
            Elements promotionEles = document.select("#db-rec-section > div").get(0).children();
            StringBuilder promotion = new StringBuilder();
            for (Element element : promotionEles) {
                if (element.children().size() == 2) {
                    promotion.append(element.children().get(1).text()).append(",");
                }
            }
            if (promotion.length() > 1) {
                data.promotion = promotion.substring(0, promotion.length() - 1);
            } else {
                data.promotion = promotion.toString();
            }
        }

//        wirteIntoExcel(data);

//        System.out.println(data.readingNum + ":" + data.readedNum + ":" + data.wantReadNum);

        return data;
    }

    private static boolean isTextEmpty(String content) {
        return content == null || content.trim().equals("");
    }

    private static void wirteIntoExcel(File file, DoubanData data) throws Exception {
//        File excelFile = new File(Utils.getExcelFile(excelNum));
//        if (!excelFile.exists()) {
//
//            HSSFWorkbook workbook = new HSSFWorkbook();//创建Excel文件(Workbook)
//            HSSFSheet sheet = workbook.createSheet();//创建工作表(Sheet)
//            sheet = workbook.createSheet("Test");//创建工作表(Sheet)
//            FileOutputStream out = new FileOutputStream(excelFile);
//            workbook.write(out);//保存Excel文件
//        }
//        rowNum = getExcelLineNum(excelFile);
        System.out.println("获取行数:" + rowNum);
//        if (rowNum == 0) {//新输入数据
//            //
//            rowNum = 0;
//            itemNum = 0;
//            initExcel(excelFile);
//        }
//        else if (itemNum == 500) {
//            excelNum++;
//            rowNum = 0;
//            itemNum = 0;
//            excelFile = new File("D:/other/douban_read_info/douban_read_" + excelNum + ".xlsx");
//            initExcel(excelFile);
//        }
        //  rowNum++;
//        itemNum++;
        Utils.saveIndexInfo(excelNum, itemNum);
        HSSFWorkbook wb = returnWorkBookGivenFileHandle(file);
        insertData(data, wb);

        rowNum = getExcelLineNum(file);
        System.out.println("当前行数:" + rowNum);
        OutputStream outputStream = new FileOutputStream(file);
        wb.write(outputStream);
        outputStream.close();
    }

    private static void insertData(DoubanData data, HSSFWorkbook wb) throws Exception {
        if (wb == null) {
            throw new IOException("hssfworkbook是空的");
        }
        HSSFSheet sheet = wb.getSheet("book_info");
        rowNum += 1;
        HSSFRow row = sheet.createRow(rowNum);//
        System.out.println("新建行数::" + rowNum);
        int buySize = data.whereBuyData.size();
        for (int i = 0; i < buySize; i++) {
            HSSFRow newRows = sheet.createRow(rowNum + i);
            HSSFCell cell = newRows.createCell(24);// 创建行的单元格,也是从0开始
            cell.setCellValue(data.whereBuyData.get(i).provider);
            cell = newRows.createCell(25);
            cell.setCellValue(data.whereBuyData.get(i).link);
            cell = newRows.createCell(26);
            cell.setCellValue(data.whereBuyData.get(i).price);
        }
        buySize--;
        if (buySize < 0) {
            buySize = 0;
        }
        HSSFCell cell = row.createCell(0);// 创建行的单元格,也是从0开始
        cell.setCellValue(data.bookname);
        CellRangeAddress region;
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 0, 0);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(1);
        cell.setCellValue(data.oriAuthor);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 1, 1);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(2);
        cell.setCellValue(data.subTitle);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 2, 2);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(3);
        cell.setCellValue(data.author);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 3, 3);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(4);
        cell.setCellValue(data.publish);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 4, 4);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(5);
        cell.setCellValue(data.publishTime);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 5, 5);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(6);
        cell.setCellValue(data.pageNumber);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 6, 6);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(7);
        cell.setCellValue(data.price);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 7, 7);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(8);
        cell.setCellValue(data.binging);//装帧
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 8, 8);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(9);
        cell.setCellValue(data.seriesOfBook);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 9, 9);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(10);
        cell.setCellValue(data.authorInfo);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 10, 10);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(11);
        cell.setCellValue(data.ISBN);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 11, 11);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(12);
        cell.setCellValue(data.translator);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 12, 12);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(13);
        cell.setCellValue(data.contentIntro);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 13, 13);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(14);
        cell.setCellValue(data.directory);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 14, 14);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(15);
        StringBuilder tags = new StringBuilder();
        for (String tag : data.tags) {
            tags.append(tag).append(",");
        }
        if (tags.toString().length() > 2) {
            cell.setCellValue(tags.toString().substring(0, tags.toString().length() - 1));
        } else {
            cell.setCellValue(tags.toString());
        }
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 15, 15);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(16);
        cell.setCellValue(data.producer);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 16, 16);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(17);
        cell.setCellValue(data.ratingData.ratingNum);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 17, 17);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(18);
        cell.setCellValue(data.ratingData.peopleNum);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 18, 18);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(19);
        cell.setCellValue(data.ratingData.fiveRating);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 19, 19);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(20);
        cell.setCellValue(data.ratingData.fourRating);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 20, 20);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(21);
        cell.setCellValue(data.ratingData.threeRating);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 21, 21);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(22);
        cell.setCellValue(data.ratingData.twoRating);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 22, 22);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(23);
        cell.setCellValue(data.ratingData.oneRating);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 23, 23);
            sheet.addMergedRegion(region);
        }


        cell = row.createCell(27);
        cell.setCellValue(data.promotion);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 27, 27);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(28);
        cell.setCellValue(data.readingNum);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 28, 28);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(29);
        cell.setCellValue(data.readedNum);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 29, 29);
            sheet.addMergedRegion(region);
        }

        cell = row.createCell(30);
        cell.setCellValue(data.wantReadNum);
        if (buySize > 0) {
            region = new CellRangeAddress(rowNum, rowNum + buySize, 30, 30);
            sheet.addMergedRegion(region);
        }

        //  rowNum += buySize;

    }

    /**
     * 得到一个已有的工作薄的POI对象
     *
     * @return
     */
    private static HSSFWorkbook returnWorkBookGivenFileHandle(File f) {
        HSSFWorkbook wb = null;
        FileInputStream fis = null;

        try {
            if (f != null) {
                fis = new FileInputStream(f);
                wb = new HSSFWorkbook(fis);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return wb;
    }

    private static void initExcel(File excelFile) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();//创建Excel文件(Workbook)
        HSSFSheet sheet = workbook.createSheet("book_info");//创建工作表(Sheet)
        HSSFRow row = sheet.createRow(rowNum);// 创建行,从0开始
        HSSFCell cell = row.createCell(0);// 创建行的单元格,也是从0开始
        cell.setCellValue("书名");
        cell = row.createCell(1);//
        cell.setCellValue("原作者");
        cell = row.createCell(2);//
        cell.setCellValue("副标题");
        cell = row.createCell(3);//
        cell.setCellValue("作者");
        cell = row.createCell(4);//
        cell.setCellValue("出版社");
        cell = row.createCell(5);//
        cell.setCellValue("出版日期");
        cell = row.createCell(6);//
        cell.setCellValue("页数");
        cell = row.createCell(7);//
        cell.setCellValue("价格");
        cell = row.createCell(8);//
        cell.setCellValue("装帧");
        cell = row.createCell(9);//
        cell.setCellValue("丛书");

        cell = row.createCell(10);//
        cell.setCellValue("作者简介");
        cell = row.createCell(11);//
        cell.setCellValue("ISBN");
        cell = row.createCell(12);//
        cell.setCellValue("译者");
        cell = row.createCell(13);//
        cell.setCellValue("内容简介");
        cell = row.createCell(14);//
        cell.setCellValue("目录");

        cell = row.createCell(15);
        cell.setCellValue("标签");
        cell = row.createCell(16);
        cell.setCellValue("出品方");
        cell = row.createCell(17);
        cell.setCellValue("评分");
        row.createCell(18);
        row.createCell(19);
        row.createCell(20);
        row.createCell(21);
        row.createCell(22);
        row.createCell(23);
        CellRangeAddress region = new CellRangeAddress(0, 0, 17, 23);
        sheet.addMergedRegion(region);

        cell = row.createCell(24);
        cell.setCellValue("在哪买");
        row.createCell(25);
        row.createCell(26);

        region = new CellRangeAddress(0, 0, 24, 26);
        sheet.addMergedRegion(region);

        cell = row.createCell(27);
        cell.setCellValue("相关推荐");
        cell = row.createCell(28);
        cell.setCellValue("在读");
        cell = row.createCell(29);
        cell.setCellValue("读过");
        cell = row.createCell(30);
        cell.setCellValue("想读");

        row = sheet.createRow(++rowNum);
        row.createCell(0);

        row.createCell(1);
        row.createCell(2);
        row.createCell(3);
        row.createCell(4);
        row.createCell(5);
        row.createCell(6);
        row.createCell(7);
        row.createCell(8);
        row.createCell(9);
        row.createCell(10);
        row.createCell(11);
        row.createCell(12);
        row.createCell(13);
        row.createCell(14);
        row.createCell(15);
        row.createCell(16);
        cell = row.createCell(17);
        cell.setCellValue("平均");
        cell = row.createCell(18);
        cell.setCellValue("人数");
        cell = row.createCell(19);
        cell.setCellValue("5星");
        cell = row.createCell(20);
        cell.setCellValue("4星");
        cell = row.createCell(21);
        cell.setCellValue("3星");
        cell = row.createCell(22);
        cell.setCellValue("2星");
        cell = row.createCell(23);
        cell.setCellValue("1星");

        cell = row.createCell(24);
        cell.setCellValue("供应商");
        cell = row.createCell(25);
        cell.setCellValue("链接");
        cell = row.createCell(26);
        cell.setCellValue("价格");
        row.createCell(27);
        row.createCell(28);
        row.createCell(29);
        row.createCell(30);
        OutputStream outputStream = new FileOutputStream(excelFile);
        workbook.write(outputStream);
        outputStream.close();

    }

    private static int getExcelLineNum(File excelFile) throws Exception {
        InputStream is = new FileInputStream(excelFile.getAbsolutePath());
        // jxl提供的Workbook类
        Workbook workbook = new HSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        // 得到所有的行数
        return sheet.getLastRowNum();
    }

    private static void startToGetData(File urlFile, CSVPrinter printer) throws Exception {
        Scanner scanner = new Scanner(urlFile);
        scanner.useDelimiter("\n");

        int index = 0;
        Gson gson = new Gson();

        while (scanner.hasNext()) {
            String line = scanner.next();
            if (index < itemNum) {
                index++;
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            System.out.println("当前从"+index+"行开始打印");
            String[] segs = line.split("=", 2);
            if (segs.length != 2) {
                continue;
            }
            String url = segs[1];
            if (null == url || url.trim().length() == 0) {
                continue;
            }

            System.out.println("Start fetch url: " + url);

            Pattern pattern = Pattern.compile("[^0-9]");
            Matcher matcher = pattern.matcher(url);
            String id = matcher.replaceAll("");

            sw.reset();
            sw.start();
            DoubanData data = parseData(url, id);
            System.out.println("抓取数据花费: " + sw.elapsed(TimeUnit.MILLISECONDS));

//            wirteIntoExcel(excelFile, data);

            System.out.println("图书数据: " + data.toString());

            if (!data.bookname.contains("404 NOT FOUND")) {
                sw.reset();
                sw.start();
                printer.printRecord(data.bookname, data.oriAuthor, data.subTitle, data.author, data.publish, data.publishTime,
                        data.pageNumber, data.price, data.binging, data.seriesOfBook, data.authorInfo, data.ISBN, data.translator,
                        data.contentIntro, data.directory, gson.toJson(data.tags), data.producer, gson.toJson(data.ratingData),
                        gson.toJson(data.whereBuyData), data.promotion, data.readingNum, data.readedNum, data.wantReadNum);
                printer.flush();
                System.out.println("写入数据花费: " + sw.elapsed(TimeUnit.MILLISECONDS));
            }

            index++;
            System.out.println("结束抓取: " + excelNum + ": " + itemNum);
            itemNum++;
        }
    }

    public static void main(String[] rags) throws IOException {
        SaveInfoData data = Utils.getSaveInfo();
        excelNum = data.pageNum;
        itemNum = data.itemNum;

        while (true) {
            OpenOption[] options = new OpenOption[] {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(Utils.getCSVFile(excelNum)), Charset.forName("UTF-8"), options);

            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                    "书名", "原作者", "副标题", "作者", "出版社", "出版日期", "页数", "价格", "装帧", "丛书",
                    "作者简介", "ISBN", "译者", "内容简介", "目录", "标签", "出品方", "评分", "在哪买",
                    "相关推荐", "在读", "读过", "想读"
            ));

            try {
                String filepath = Utils.getUrlFilePath(excelNum);
                File file = new File(filepath);
                if (!file.exists()) {
                    break;
                }

                startToGetData(file, printer);

                System.out.println("开始新的一页:" + excelNum);
                excelNum++;
                itemNum = 0;

            } catch (Exception e) {
                e.printStackTrace();
                Utils.saveIndexInfo(excelNum, itemNum);
            } finally {
                printer.close(true);
                writer.close();
            }
        }
    }
}
