import crawler.Crawler;
import indexer.Indexer;
import model.ResultWriter;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class MainClass {
    static String URL = "http://www.cse.ust.hk";

    public static void main(String[] args) throws RocksDBException, IOException {
//        runCrawler();
        testSearchEngine();
    }

    public static void runCrawler() throws IOException {
        boolean freshStart = true;
        boolean crawlRequired = true;

        Instant start;
        Instant finish;
        long timeElapsed;

        if (freshStart) {
            Repository.destroyAll();
        }
        Repository.openConnections();


        if (crawlRequired) {
            Crawler crawler = new Crawler(URL);
            System.out.println("Running Crawler...");
            start = Instant.now();
            crawler.crawlFromRoot(30);
//        crawler.crawlFromRoot();
            finish = Instant.now();
            timeElapsed = Duration.between(start, finish).toSeconds();
            System.out.println("Time elapsed crawling pages: " + timeElapsed + " seconds\n");

            System.out.println("Writing spider result...");
            ResultWriter.write_spider_result();
            System.out.println("Indexed pages written to spider_result.txt\n");

            System.out.println("Constructing Inverted Index...");
            start = Instant.now();
            Indexer.construct_invertedIndex();
            finish = Instant.now();
            timeElapsed = Duration.between(start, finish).toSeconds();
            System.out.println("Time elapsed constructing Inverted Index: " + timeElapsed + " seconds\n");

            System.out.println("Writing Inverted Index...");
            ResultWriter.write_inverted_file();

            System.out.println("Inverted index file written to inverted_file.txt");
        }


        Repository.closeAllConnections();
    }

    public static void testSearchEngine() {
        Repository.openConnections();

        String query = "admissions";
//        var map = SearchEngine.computeMap_pageId_wordFrequency(query);
//        System.out.println(map);

        Repository.closeAllConnections();
    }
}
