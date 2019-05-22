import data.DoubanData;
import data.WhereBuyData;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;

public class Main {
    public static int excelNum = 1;
    public static int rowNum = 0;
    public static int itemNum = 0;


    private static void getData(String url, String id) {
        DoubanData data = new DoubanData();
        Connection conn = Jsoup.connect(url);
        Document document = null;
        try {
            document = conn.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
//初始化基本数据
        data.bookname = document.select("div#wrapper > h1 > span").text();
        System.out.println(data.bookname);

        data.author = document.select("#info > span:nth-child(1) > a").text();
        Elements infoEle = document.select("#info").get(0).children();
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
                    System.out.println(data.translator);
                }
            }

        }
        //#link-report > span.short > div
        data.contentIntro = String.join(System.lineSeparator(), document.select("#link-report > div > div.intro").select("p").eachText());
        if (document.select("#link-report > span.all.hidden > div > div") != null) {
            data.contentIntro += String.join(System.lineSeparator(), document.select("#link-report > span.all.hidden > div > div.intro").select("p").eachText());
        }

        data.directory = String.join(System.lineSeparator(), document.select("#dir_" + id + "_short").eachText());
        if (document.select("#dir_" + id + "_full") != null) {
            data.directory += String.join(System.lineSeparator(), document.select("#dir_" + id + "_full").eachText());
        }
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
            System.out.println(data.ratingData.fiveRating);
            data.ratingData.ratingNum = document.select("#interest_sectl > div > div.rating_self > strong").text();
            data.ratingData.peopleNum = document.select("#interest_sectl > div > div.rating_self.clearfix > div > div.rating_sum > span > a > span").text();

            data.ratingData.fourRating = document.select("#interest_sectl > div > span:nth-child(9)").text();
            data.ratingData.threeRating = document.select("#interest_sectl > div > span:nth-child(13)").text();
            data.ratingData.twoRating = document.select("#interest_sectl > div > span:nth-child(17)").text();
            data.ratingData.oneRating = document.select("#interest_sectl > div > span:nth-child(21)").text();
            System.out.println(data.ratingData.oneRating);
        }
        //初始化购买地方
        if (document.select("#buyinfo-printed > ul").size() > 0) {
            Elements whereBugEles = document.select("#buyinfo-printed > ul").get(0).children();
            for (Element whereBuyEle : whereBugEles) {
                if (isTextEmpty(whereBuyEle.attr("class")) && whereBuyEle.children().size() > 0) {
                    WhereBuyData whereBuyData = new WhereBuyData();
                    whereBuyData.provider = whereBuyEle.children().get(0).children().get(0).text();
                    whereBuyData.link = whereBuyEle.children().get(0).attr("href");
                    whereBuyData.price = whereBuyEle.children().get(1).children().get(0).text();
                    System.out.println(whereBuyData.price + whereBuyData.link + whereBuyData.provider);
                    data.whereBuyData.add(whereBuyData);
                }
            }
        }
        Elements elements = document.select("#content > div > div.article > div.related_info").select("h2");
        for (Element element :
                elements) {
            if (element.text().contains("作者简介")) {
                data.authorInfo = String.join(System.lineSeparator(), element.nextElementSibling().select("div.intro > p").eachText());
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
            String promotion = "";
            for (Element element : promotionEles) {
                if (element.children().size() == 2) {
                    promotion += element.children().get(1).text() + ",";
                }
            }
            if (promotion.length() > 1) {
                data.promotion = promotion.substring(0, promotion.length() - 1);
            } else {
                data.promotion = promotion;
            }
        }
        try {
            wirteIntoExcel(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(data.readingNum + ":" + data.readedNum + ":" + data.wantReadNum);
    }

    private static boolean isTextEmpty(String content) {
        return content == null || content.equals("");
    }

    public static void wirteIntoExcel(DoubanData data) throws Exception {
        File excelFile = new File("D:/other/douban_read_info/douban_read_" + excelNum + ".xls");
        if (!excelFile.exists()) {

            HSSFWorkbook workbook = new HSSFWorkbook();//创建Excel文件(Workbook)
            HSSFSheet sheet = workbook.createSheet();//创建工作表(Sheet)
            sheet = workbook.createSheet("Test");//创建工作表(Sheet)
            FileOutputStream out = new FileOutputStream(excelFile);
            workbook.write(out);//保存Excel文件
        }
        rowNum = getExcelLineNum(excelFile);
        if (getExcelLineNum(excelFile) == 0) {//新输入数据
            //
            rowNum = 0;
            itemNum = 0;
            initExcel(excelFile);
        } else if (itemNum == 500) {
            excelNum++;
            rowNum = 0;
            itemNum = 0;
            excelFile = new File("D:/other/douban_read_info/douban_read_" + excelNum + ".xlsx");
            initExcel(excelFile);
        }
        //  rowNum++;
        itemNum++;
        insertData(excelFile, data, returnWorkBookGivenFileHandle(excelFile));


    }

    private static void insertData(File excelFile, DoubanData data, HSSFWorkbook wb) throws IOException {
        if (wb == null) {
            throw new IOException("hssfworkbook是空的");
        }
        HSSFSheet sheet = wb.getSheet("book_info");
        HSSFRow row = sheet.createRow(rowNum);//
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
        if(buySize<0){
            buySize=0;
        }
        HSSFCell cell = row.createCell(0);// 创建行的单元格,也是从0开始
        cell.setCellValue(data.bookname);
        CellRangeAddress region;
        if(buySize>0){
         region = new CellRangeAddress(rowNum, rowNum + buySize, 0, 0);
        sheet.addMergedRegion(region);}

        cell = row.createCell(1);
        cell.setCellValue(data.oriAuthor);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 1, 1);
        sheet.addMergedRegion(region);}

        cell = row.createCell(2);
        cell.setCellValue(data.subTitle);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 2, 2);
        sheet.addMergedRegion(region);}

        cell = row.createCell(3);
        cell.setCellValue(data.author);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 3, 3);
        sheet.addMergedRegion(region);}

        cell = row.createCell(4);
        cell.setCellValue(data.publish);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 4, 4);
        sheet.addMergedRegion(region);}

        cell = row.createCell(5);
        cell.setCellValue(data.publishTime);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 5, 5);
        sheet.addMergedRegion(region);}

        cell = row.createCell(6);
        cell.setCellValue(data.pageNumber);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 6, 6);
        sheet.addMergedRegion(region);}

        cell = row.createCell(7);
        cell.setCellValue(data.price);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 7, 7);
        sheet.addMergedRegion(region);}

        cell = row.createCell(8);
        cell.setCellValue(data.binging);//装帧
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 8, 8);
        sheet.addMergedRegion(region);}

        cell = row.createCell(9);
        cell.setCellValue(data.seriesOfBook);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 9, 9);
        sheet.addMergedRegion(region);}

        cell = row.createCell(10);
        cell.setCellValue(data.authorInfo);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 10, 10);
        sheet.addMergedRegion(region);}

        cell = row.createCell(11);
        cell.setCellValue(data.ISBN);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 11, 11);
        sheet.addMergedRegion(region);}

        cell = row.createCell(12);
        cell.setCellValue(data.translator);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 12, 12);
        sheet.addMergedRegion(region);}

        cell = row.createCell(13);
        cell.setCellValue(data.contentIntro);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 13, 13);
        sheet.addMergedRegion(region);}

        cell = row.createCell(14);
        cell.setCellValue(data.directory);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 14, 14);
        sheet.addMergedRegion(region);}

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
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 15, 15);
        sheet.addMergedRegion(region);}

        cell = row.createCell(16);
        cell.setCellValue(data.producer);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 16, 16);
        sheet.addMergedRegion(region);}

        cell = row.createCell(17);
        cell.setCellValue(data.ratingData.ratingNum);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 17, 17);
        sheet.addMergedRegion(region);}

        cell = row.createCell(18);
        cell.setCellValue(data.ratingData.peopleNum);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 18, 18);
        sheet.addMergedRegion(region);}

        cell = row.createCell(19);
        cell.setCellValue(data.ratingData.fiveRating);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 19, 19);
        sheet.addMergedRegion(region);}

        cell = row.createCell(20);
        cell.setCellValue(data.ratingData.fourRating);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 20, 20);
        sheet.addMergedRegion(region);}

        cell = row.createCell(21);
        cell.setCellValue(data.ratingData.threeRating);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 21, 21);
        sheet.addMergedRegion(region);}

        cell = row.createCell(22);
        cell.setCellValue(data.ratingData.twoRating);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 22, 22);
        sheet.addMergedRegion(region);}

        cell = row.createCell(23);
        cell.setCellValue(data.ratingData.oneRating);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 23, 23);
        sheet.addMergedRegion(region);}


        cell = row.createCell(27);
        cell.setCellValue(data.promotion);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 27, 27);
        sheet.addMergedRegion(region);}

        cell = row.createCell(28);
        cell.setCellValue(data.readingNum);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 28, 28);
        sheet.addMergedRegion(region);}

        cell = row.createCell(29);
        cell.setCellValue(data.readedNum);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 29, 29);
        sheet.addMergedRegion(region);}

        cell = row.createCell(30);
        cell.setCellValue(data.wantReadNum);
        if(buySize>0){
        region = new CellRangeAddress(rowNum, rowNum + buySize, 30, 30);
        sheet.addMergedRegion(region);}

        rowNum += buySize;
        OutputStream outputStream = new FileOutputStream(excelFile);
        wb.write(outputStream);
        outputStream.close();

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
        return sheet.getPhysicalNumberOfRows();
    }

    public static void main(String[] rags) {
        // try {
        //  wirteIntoExcel(new DoubanData());
        getData("https://book.douban.com/subject/1562932/", "1562932");
//        } catch (Exception ex) {
//            System.out.println(ex.getMessage());
//        }

    }
}
