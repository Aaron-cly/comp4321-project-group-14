import crawler.Crawler;
import engine.SearchEngine;
import indexer.Indexer;
import model.ResultWriter;
import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class MainClass {
    static String URL = "http://www.cse.ust.hk";
    static int targetNumPages = 30;
    static boolean freshStart = false;

    public static void main(String[] args) throws RocksDBException, IOException {
        if ((args.length != 1 && args.length != 2) || (!args[0].equals("FRESH_CRAWL") && !args[0].equals("CONTINUE_CRAWL"))) {
            System.out.println("Invalid Arguments." +
                    " Please input run --args=\"[FRESH_CRAWL|CONTINUE_CRAWL] [num_pages_to_crawl]\" , omit num_pages_to_crawl to crawl all pages");
            return;
        }

        freshStart = args[0].equals("FRESH_CRAWL");

        if (args.length == 1) {
            targetNumPages = 0;
        } else {
            targetNumPages = Integer.parseInt(args[1]);
        }

        runCrawler();
//        runQuery("FAQ \"Postgraduate Students\"");
//        runQuery("Postgraduate");
    }

    public static void runCrawler() throws IOException {
        Instant start;
        Instant finish;
        long timeElapsed;

        if (freshStart) {
            Repository.destroyAll();
        }
        Repository.openConnections();
        Crawler crawler = new Crawler(URL);
        System.out.println("Running Crawler...");
        start = Instant.now();
        if (targetNumPages == 0)
            crawler.crawlFromRoot();
        else
            crawler.crawlFromRoot(targetNumPages);
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toSeconds();
        System.out.println("Time elapsed crawling pages: " + timeElapsed + " seconds\n");

        System.out.println("Writing spider result...");
        ResultWriter.write_spider_result();
        System.out.println("Indexed pages written to spider_result.txt\n");

        System.out.println("Constructing Inverted Index for Content&Title...");
        start = Instant.now();
        Indexer.construct_invertedIndex();
        Indexer.construct_invertedIndex_Title();
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toSeconds();
        System.out.println("Time elapsed constructing Inverted Index: " + timeElapsed + " seconds\n");

//            System.out.println("Writing Inverted Index...");
//            ResultWriter.write_inverted_file();
//            ResultWriter.write_invertedTitle_file();
//            ResultWriter.write_forwardTitle_File();
//            System.out.println("Inverted index file written to inverted_file.txt");

        Repository.closeAllConnections();
    }

    public static void runQuery(String query) throws RocksDBException {
        Repository.openConnections();

        var resultList = SearchEngine.processQuery(query);
        ResultWriter.write_queryResult(resultList);

        Repository.closeAllConnections();
    }
}
