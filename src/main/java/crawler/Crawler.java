package crawler;

import indexer.Indexer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Crawler {
    static final int VALUE_CRAWL_ALL = Integer.MAX_VALUE;
    String rootURL = "http://www.cse.ust.hk";
    HashSet<String> urlSet = new HashSet<>();   // avoid crawling the same page twice
    ArrayList<String> urlList = new ArrayList<>();
    Indexer indexer = new Indexer();

    public Crawler(String rootURL) {
        this.rootURL = rootURL;
    }

    public ArrayList<String> getUrlList() {
        return this.urlList;
    }

    private Connection.Response getResponse(String url) throws IOException {
        Connection conn = Jsoup.connect(url).timeout(1000);
        return conn.execute();
    }

    private HashSet<String> getPagesFromURL(String url) throws IOException {
        return getPagesFromURL(url, Integer.MAX_VALUE);
    }

    private static final List<String> extensionList = List.of(".pdf", "png", ".jpeg", ".jpg", ".mp4", ".mp3", ".doc", ".zip", ".rar", ".ppt", ".pptx", ".docx", ".bib", ".Z", ".ps", ".tgz", ".wmv");

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
            System.out.println(currentIndex + ": " + currentURL);
            if (currentIndex % 500 == 0) {
                System.out.printf("Crawled and indexed %d pages\n", currentIndex);
            }
        }
        indexer.construct_parents_from_child_links();
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

        // open connection for page size
        var connection = new URL(url).openConnection();
        connection.setConnectTimeout(5 * 1000);
        var pgSize = connection.getContentLength();
        String lastModifiedDate = getLastModifiedDate(res, doc);

        // insert new page if url should not be ignore
        if(!indexer.shouldIgnoreUrl(url, lastModifiedDate)) {
            indexer.insert_new_page(doc, url, lastModifiedDate, pgSize, pagesOnURL);
        }
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

