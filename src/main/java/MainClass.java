import crawler.Crawler;
import model.ResultWriter;
import indexer.Indexer;
import org.rocksdb.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class MainClass {
    static String URL = "http://www.cse.ust.hk";

    public static void main(String[] args) throws RocksDBException, IOException {
        Instant start;
        Instant finish;
        long timeElapsed;

        Crawler crawler = new Crawler(URL);
        System.out.println("Running Crawler...");
        start = Instant.now();
        // crawler.crawlFromRoot(2000);
        crawler.crawlFromRoot();
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toSeconds();
        System.out.println("Time elapsed crawling pages: " + timeElapsed + " seconds\n");
        System.out.println("Total number of pages found under root: " + crawler.getUrlList().size());


        System.out.println("Writing page children info to file...");
        ResultWriter.write_page_children(crawler.getMap_page_children());



        // System.out.println("Writing spider result...");
        // ResultWriter.write_spider_result();
        // System.out.println("Indexed pages written to spider_result.txt\n");

        // System.out.println("Constructing Inverted Index...");
        // start = Instant.now();
        // Indexer.construct_invertedIndex();
        // finish = Instant.now();
        // timeElapsed = Duration.between(start, finish).toSeconds();
        // System.out.println("Time elapsed constructing Inverted Index: " + timeElapsed + " seconds\n");

        // System.out.println("Writing Inverted Index...");
        // ResultWriter.write_inverted_file();

        // System.out.println("Inverted index file written to inverted_file.txt");

    }
}
