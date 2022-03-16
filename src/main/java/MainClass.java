import crawler.Crawler;
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
        System.out.println("Message from MainClass");
        Crawler crawler = new Crawler(URL);
        crawler.main(null);

    }



}
