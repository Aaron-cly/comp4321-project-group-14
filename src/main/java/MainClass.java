import crawler.Crawler;
import crawler.ResultWriter;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.rocksdb.*;

import java.io.IOException;
import java.util.Arrays;

public class MainClass {
    static String URL = "http://www.cse.ust.hk";

    public static void main(String[] args) throws RocksDBException, IOException {

        Crawler crawler = new Crawler(URL);
        System.out.println("Running Crawler.....");
        crawler.crawlFromRoot(30);

        ResultWriter.write_spider_result();

        System.out.println("Indexed pages written to spider_result.txt");

    }



}
