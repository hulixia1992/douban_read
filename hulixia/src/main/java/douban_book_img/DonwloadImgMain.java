package douban_book_img;

import com.google.common.base.Stopwatch;
import douban_book_detail.data.SaveInfoData;
import utils.Utils;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class DonwloadImgMain {
    private static int excelNum = 1;
    private static int itemNum = 0;
    private static String proxyUrl;
    private static Stopwatch sw = Stopwatch.createStarted();

    public static void main(String[] rags) {
        SaveInfoData data = null;
        try {
            data = Utils.getSaveInfo();
            excelNum = data.pageNum;
            itemNum = data.itemNum;
            proxyUrl = data.proxyUrl;
            while (true) {
                String filepath = Utils.getUrlFilePath(excelNum);
                File file = new File(filepath);
                if (!file.exists()) {
                    break;
                }
                startToGetData(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void startToGetData(File urlFile) throws Exception {
        Scanner scanner = new Scanner(urlFile);
        scanner.useDelimiter("\n");

        int index = 0;
        while (scanner.hasNext()) {
            String line = scanner.next();
            if (index < itemNum) {
                index++;
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            System.out.println("当前从" + index + "行开始打印");
            String[] segs = line.split("=", 2);
            if (segs.length != 2) {
                continue;
            }
            String url = segs[1];
            if (null == url || url.trim().length() == 0) {
                continue;
            }

            System.out.println("Start fetch url: " + url);

            sw.reset();
            sw.start();
            DoubanImgData data = PicUtils.parseData("https://book.douban.com/subject/27194720/");
            System.out.println("抓取数据花费: " + sw.elapsed(TimeUnit.MILLISECONDS));
            PicUtils.downloadPicture(data.imgUrl, data.ISBN);

            System.out.println("图书数据: " + data.toString());
            //  index++;
            System.out.println("结束抓取: " + excelNum + ": " + itemNum);
            itemNum++;
        }
    }}

