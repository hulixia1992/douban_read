import data.DoubanUrlData;
import data.SaveInfoData;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Utils {
    private static String INFO_FAIL_PATH = "/Users/xuqiaolun/PySpider/read/douban_index_info.txt";
    private static String ERROR_URL_FILE = "/Users/xuqiaolun/PySpider/read/error_url.txt";
    //   private static String SAVE_EXCEL_FILE = "D:/other/error_url.txt";

    private static String PAGE_NUM = "page_num";
    private static String ITEM_NUM = "item_num";

    static String getUrlFilePath(int pageNum) {
        return "/Users/xuqiaolun/PySpider/douban/" + pageNum + ".txt";
    }

    static String getExcelFile(int pageNum) {
        return "D:/other/douban_read_info/" + pageNum + ".xls";
    }

    static String getCSVFile(int pageNum) {
        return "/Users/xuqiaolun/PySpider/read/data/" + pageNum + ".csv";
    }

    static void saveIndexInfo(int pageNum, int itemNum) throws IOException {
        //创建properties集合
        Properties prop = new Properties();

        //存储
        prop.setProperty(PAGE_NUM, pageNum + "");
        prop.setProperty(ITEM_NUM, itemNum + "");


        //调试list显示在控制台上
        prop.list(System.out);

        //调用store存储到文件里  使用ISO-8859-1 字符编码
        FileOutputStream fos = new FileOutputStream(INFO_FAIL_PATH);
        prop.store(fos, "name+age");


    }

    static SaveInfoData getSaveInfo() throws IOException {
        //调用load将文件里的数据读取
        Properties prop = new Properties();
        FileInputStream fis = new FileInputStream(INFO_FAIL_PATH);
        prop.load(fis);
        SaveInfoData data = new SaveInfoData();
        data.pageNum = Integer.parseInt(prop.getProperty(PAGE_NUM, "1"));
        data.itemNum = Integer.parseInt(prop.getProperty(ITEM_NUM, "0"));
        return data;
    }

    static List<DoubanUrlData> getDoubanUrls(int pageNum) throws IOException {
        List<DoubanUrlData> urldatas = new ArrayList<>();
        File urlFile = new File(getUrlFilePath(pageNum));
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(urlFile));//构造一个BufferedReader类来读取文件
        String s = "";
        while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
            result.append(System.lineSeparator() + s);
        }
        br.close();
        String[] urlIndex = result.toString().split("\n");
        for (String info : urlIndex) {

            String[] itemInfo = info.split("=");
            if (itemInfo.length == 2) {
                DoubanUrlData data = new DoubanUrlData();
                data.ISBN = itemInfo[0];
                data.url = itemInfo[1];
                String[] urlDetail = itemInfo[1].split("/");
                if (urlDetail.length > 2) {
                    data.ID = urlDetail[urlDetail.length - 2];
                }
                urldatas.add(data);
            }
        }
        return urldatas;
    }

    static void saveErrorUrl(String url) {

        BufferedWriter out = null;
        try {
            File errorFile = new File(ERROR_URL_FILE);
            if (!errorFile.exists()) {
                errorFile.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(errorFile, true)));
            out.write(url + System.lineSeparator());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("保存错误文件出错:" + e.getMessage());
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                System.out.println("关闭错误文件流出错:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

