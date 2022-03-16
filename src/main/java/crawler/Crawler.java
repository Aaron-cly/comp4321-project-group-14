package crawler;

import indexer.Indexer;
import model.MetaData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    public static void main(String[] args) throws IOException {
        String url = "http://www.cse.ust.hk";

        Crawler crawler = new Crawler(url);
//        try {
//            var res = Jsoup.connect(url).execute();
//            var doc = res.parse();
//            List<String> words = crawler.extractWords(doc);
//            crawler.consolidateFrequencies(words);
//
//        } catch (IOException e) {
//        }

        crawler.crawlFromRoot(30);
         crawler.indexer.printInvertedIndex();
        crawler.indexer.printMetaInfo();
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

//    private List<String> getPagesFromURL(String url) {
//        return getPagesFromURL(url, Integer.MAX_VALUE);
//    }

    // obtain all pages embedded in html of a URL
    private List<String> getPagesFromURL(String url, int numPages) throws IOException {
        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }

        if (res == null) return new ArrayList<>();

        Elements links = doc.select("a[href]"); // get all anchors with href attr

        final String strippedUrl = url.charAt(url.length() - 1) == '/' ? url.substring(0, url.length() - 1) : url;

        var urlList = links.stream()
                .map(link -> link.attr("href"))
                .filter(link -> link.startsWith("/") && !link.equals("/"))  // only needs relative urls since they are on the root link
                .map(link -> strippedUrl + link)  // map to complete url
                .collect(Collectors.toList());
        var uniqueChildLinks = new HashSet<String>(urlList);

        urlList = urlList.stream().limit(numPages).collect(Collectors.toList());

        // index the current page
        var rootFrequencies = consolidateFrequencies(extractWords(doc));
        indexer.updateIndex(currentDocCount, rootFrequencies);
        var connection = new URL(strippedUrl).openConnection();
        var metaData = new MetaData(
                doc.title(),
                strippedUrl,
                rootFrequencies,
                new Date(connection.getLastModified()),
                connection.getContentLength(),
                uniqueChildLinks
        );
        indexer.addMetaInformation(currentDocCount++, metaData);

        return urlList;
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

        // index the root
        var rootFrequencies = consolidateFrequencies(extractWords(res.parse()));
        indexer.updateIndex(currentDocCount++, rootFrequencies);

        while (!crawlList.isEmpty()) {
            for (String currentURL : crawlList) {
                if (isCrawlAll || this.urlList.size() < numPages) {
                    List<String> pagesOnURL = getPagesFromURL(currentURL, numPages - this.urlList.size());
                    this.urlList.addAll(pagesOnURL);
                    crawlList = new ArrayList<>(pagesOnURL);
                } else return;
            }
        }
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

}

