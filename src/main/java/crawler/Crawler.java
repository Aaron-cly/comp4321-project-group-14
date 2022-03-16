package crawler;

import indexer.Indexer;
import model.MetaData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import repository.Repository;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Crawler {
    static final int VALUE_CRAWL_ALL = Integer.MAX_VALUE;
    String rootURL = "http://www.cse.ust.hk";
    static int currentDocCount = 0;
    ArrayList<String> urlList = new ArrayList<>();
    static ArrayList<String> stopWords;
    Indexer indexer = new Indexer();

    static {
        // retrieve stopwords from text file
        try {
            var list = Files.readAllLines(Paths.get("./stopwords.txt"));
//            list.forEach(System.out::println);
            stopWords = new ArrayList<>(list);
        } catch (Exception e) {
            System.out.println("Something went wrong while reading the file");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, RocksDBException {
        String url = "http://www.cse.ust.hk";

        Crawler crawler = new Crawler(url);
        crawler.crawlFromRoot(30);

        System.out.println(crawler.getUrlList());

        System.out.println(Repository.Page.getPageUrl("71588285"));
        Repository.ForwardFrequency.print();
    }

    public Crawler(String rootURL) {
        this.rootURL = rootURL;
    }

    public ArrayList<String> getUrlList() {
        return this.urlList;
    }

    private Connection.Response getResponse(String url) throws IOException {
        Connection conn = Jsoup.connect(url);
        return conn.execute();
    }

    private HashSet<String> getPagesFromURL(String url) throws IOException {
        return getPagesFromURL(url, Integer.MAX_VALUE);
    }

    // obtain all pages embedded in html of a URL
    private HashSet<String> getPagesFromURL(String url, int numPages) throws IOException {
        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }

        if (res == null) return new HashSet<>();

        Elements links = doc.select("a[href]"); // get all anchors with href attr

        final String strippedUrl = url.charAt(url.length() - 1) == '/' ? url.substring(0, url.length() - 1) : url;
        var urlSet = links.stream()
                .map(link -> link.attr("href"))
                .filter(link -> link.startsWith("/") && !link.equals("/"))  // only needs relative urls since they are on the root link
                .map(link -> strippedUrl + link)  // map to complete url
                .collect(Collectors.toCollection(HashSet::new));

        return urlSet.stream().limit(numPages).collect(Collectors.toCollection(HashSet::new));
    }

    public void crawlFromRoot() throws IOException {
        crawlFromRoot(VALUE_CRAWL_ALL);
    }

    // crawl URLs from the root url
    public void crawlFromRoot(int numPages) throws IOException {
        Connection.Response res = null;
        try {
            res = getResponse(this.rootURL);
        } catch (IOException e) {
            throw new IOException("Invalid root url");
        }

        List<String> crawlList = List.of(this.rootURL);
        boolean isCrawlAll = numPages == VALUE_CRAWL_ALL;
        this.urlList.add(this.rootURL);

        // index the root and get page info from root
        crawlPage(rootURL);

        int currentIndex = 0;
        int crawledNum = 0;
        while (crawledNum < numPages) {
            String currentURL = this.urlList.get(currentIndex);
            var pagesOnURL = getPagesFromURL(currentURL, numPages - this.urlList.size());
            this.urlList.addAll(pagesOnURL);

            crawlPage(currentURL);
            currentIndex++;
            crawledNum++;
        }
    }

    private void crawlPage(String url) throws IOException {

        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }

        String lastModifiedDate = getLastModifiedDate(res, doc);
        var pagesOnURL = getPagesFromURL(url);

        // index the current page
        var rootFrequencies = consolidateFrequencies(extractWords(doc));

        // for page size
        var connection = new URL(url).openConnection();

        indexer.insert_page(url);
        indexer.update_ForwardFrequency(url, rootFrequencies);
    }

    // extract all words from a page
    public List<String> extractWords(Document d) {
        var list = new ArrayList<String>();
        var elements = d.body().select("*");
        for (var e : elements) {
            // skip the elements with empty content
            String[] words = e.ownText().split("\\W+");

            // filter out strings containing numbers and stopwords
            list.addAll(
                    Arrays.stream(words)
                            .filter(s -> !s.equals("") && !s.matches(".*\\d.*") && !stopWords.contains(s.toLowerCase()))
                            .collect(Collectors.toList())
            );
        }
        return list;
    }

    private HashMap<String, Integer> consolidateFrequencies(List<String> extracted) {
        var frequencies = new HashMap<String, Integer>();
        extracted.forEach(s -> {
            var currentFreqOfS = frequencies.getOrDefault(s, 0);
            frequencies.put(s, currentFreqOfS + 1);
        });
//        frequencies.forEach((s, i) -> System.out.println(s + ": " + i));
        return frequencies;
    }

    private void outputTxtFile() {

    }

    private String getLastModifiedDate(Connection.Response res, Document doc) {
        var lastModifiedSpans = doc.select("span:contains(Last updated)");
        if (lastModifiedSpans.size() == 0) {
            if (!res.hasHeader("last-modified")) return "N/A";
            return res.header("last-modified");
        }
        return lastModifiedSpans.get(0).ownText().substring(
                lastModifiedSpans.get(0).ownText().length() - 10
        );
    }

}

