package crawler;

import indexer.Indexer;
import model.PageInfo;
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
    HashSet<String> urlSet = new HashSet<>();   // avoid crawling the same page twice
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
        boolean isCrawlAll = numPages == VALUE_CRAWL_ALL;
        this.urlList.add(this.rootURL);

        int currentIndex = 0;
        while (currentIndex < urlList.size() && (isCrawlAll || currentIndex < numPages)) {
            String currentURL = this.urlList.get(currentIndex);
            var pagesOnURL = getPagesFromURL(currentURL);
            for (String page : pagesOnURL) {
                if (!urlSet.contains(page)) {
                    this.urlSet.add(page);
                    this.urlList.add(page);
                }
            }
            this.urlList.addAll(pagesOnURL);

            crawlPage(currentURL);
            currentIndex++;
            if (currentIndex % 500 == 0) {
                System.out.printf("Crawled and indexed %d pages\n", currentIndex);
            }
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
        if (doc == null) return;

        String lastModifiedDate = getLastModifiedDate(res, doc);
        var pagesOnURL = getPagesFromURL(url);

        // index the current page
        var map_word_posList = consolidatePositions(extractWords(doc));

        // for page size
        var connection = new URL(url).openConnection();
        connection.getContentLength();

        PageInfo pageInfo = new PageInfo(doc.title(), lastModifiedDate,
                pagesOnURL,
                String.valueOf(connection.getContentLength())
        );

        indexer.insert_page(url);
        indexer.update_ForwardFrequency(url, map_word_posList);
        indexer.add_pageInfo(url, pageInfo);
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
                            .map(String::toLowerCase)
                            .filter(s -> !s.equals("") && !s.matches(".*\\d.*") && !stopWords.contains(s))
                            .collect(Collectors.toList())
            );
        }
        return list;
    }

    // construct word -> List(position)
    private HashMap<String, List<Integer>> consolidatePositions(List<String> extracted) {
        var wordPositions = new HashMap<String, List<Integer>>();
        for (int i = 0; i < extracted.size(); i++) {
            String word = extracted.get(i);

            List<Integer> posList = wordPositions.getOrDefault(word, null);
            if (posList == null) {
                posList = new ArrayList<>();
                posList.add(i);
                wordPositions.put(word, posList);
            } else {
                posList.add(i);
            }
        }
        return wordPositions;
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

