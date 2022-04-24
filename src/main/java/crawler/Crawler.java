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

    private static final List<String> extensionList = List.of(".pdf", "png", ".jpeg", ".jpg", ".mp4", ".mp3", ".doc", ".zip", ".rar", ".ppt", ".pptx", ".docx", ".bib", ".Z", ".ps", ".tgz");

    private boolean validLink(String link) {

        return !link.equals("/") && !link.startsWith("../") && !link.contains("ftp") && !link.contains("@") && !link.equals("index.html")
                && !link.equals(".") && !link.contains("?") && !link.contains("#") && !link.contains("http")
                && !link.startsWith("javascript")
                && extensionList.stream().noneMatch(ext -> link.toLowerCase().endsWith(ext.toLowerCase()));
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

        if (doc == null) return new HashSet<>();

        Elements links = doc.select("a[href]"); // get all anchors with href attr

        final String currentUrl = url;
        var urlSet = links.stream()
                .map(link -> link.attr("href"))
                .filter(this::validLink)
                .map(link -> {
                    if (link.startsWith("/")) {
                        return rootURL + link;
                    } else if (link.startsWith("./")) {
                        int mountPoint = currentUrl.lastIndexOf('/');
                        String baseUrl = currentUrl.substring(0, mountPoint + 1);

                        return baseUrl + link.substring(2);
                    } else {
                        if (currentUrl.endsWith(".html")) {
                            int mountPoint = currentUrl.lastIndexOf('/');
                            String baseUrl = currentUrl.substring(0, mountPoint + 1);
                            return baseUrl + link;
                        } else if (currentUrl.endsWith(".html/")) {
                            int mountPoint = currentUrl.substring(0, currentUrl.length() - 1).lastIndexOf('/');
                            String baseUrl = currentUrl.substring(0, mountPoint + 1);
                            return baseUrl + link;
                        } else {
                            char lastChar = currentUrl.charAt(currentUrl.length() - 1);
                            return currentUrl + (lastChar == '/' ? "" : '/') + link;
                        }
                    }
                })
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
        this.urlSet.add(this.rootURL);

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

            crawlPage(currentURL, pagesOnURL);
            currentIndex++;
            if (currentIndex % 500 == 0) {
                System.out.printf("Crawled and indexed %d pages\n", currentIndex);
            }
        }
    }

    private void crawlPage(String url, HashSet<String> pagesOnURL) throws IOException {

        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }
        if (doc == null) return;

        String lastModifiedDate = getLastModifiedDate(res, doc);

        // index the current page
        var map_word_posList = consolidatePositions(extractWords(doc));
        var max_termFreq = getMax_termFreq(map_word_posList);

        // for page size
        var connection = new URL(url).openConnection();
        connection.getContentLength();

        PageInfo pageInfo = new PageInfo(doc.title(), url, lastModifiedDate,
                pagesOnURL,
                String.valueOf(connection.getContentLength()),
                max_termFreq
        );

        indexer.insert_page(url);
        indexer.update_ForwardIndex(url, map_word_posList);
        indexer.add_pageInfo(url, pageInfo);

        // index the title of the page too
        int lastIndex = doc.title().lastIndexOf(" |");
        String title = lastIndex == -1 ? doc.title() : doc.title().substring(0, lastIndex);
        var wordList_title =
                Arrays.stream(title.split(" "))
                        .filter(w -> !w.isBlank())
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

        map_word_posList = consolidatePositions(wordList_title);
        indexer.update_ForwardIndex_Title(url, map_word_posList);
    }

    public int getMax_termFreq(HashMap<String, List<Integer>> word_posList) {
        int max = 0;
        for (var posList : word_posList.values()) {
            max = Math.max(max, posList.size());
        }
        return max;
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

