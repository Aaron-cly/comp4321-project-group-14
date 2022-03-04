package crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Crawler {
    static final int VALUE_CRAWL_ALL = Integer.MAX_VALUE;
    String rootURL = "http://www.cse.ust.hk";
    ArrayList<String> urlList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        String url = "http://www.cse.ust.hk";

        Crawler crawler = new Crawler(url);
        crawler.crawlFromRoot();
        System.out.println(crawler.getUrlList().size());
        System.out.println(crawler.getUrlList());
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

    private List<String> getPagesFromURL(String url) {
        return getPagesFromURL(url, Integer.MAX_VALUE);
    }

    // obtain all pages embedded in html of a URL
    private List<String> getPagesFromURL(String url, int numPages) {
        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }

        if (res == null) return new ArrayList<>();

        Elements links = doc.select("a[href]"); // get all anchors with href attr

        List<String> urlList = links.stream()
                .map(link -> link.attr("href")) // get the attribute of the element
                .filter(link -> link.startsWith("/") && !link.equals("/"))  // only needs relative urls since they are on the root link
                .map(link -> url + link)  // map to complete url
                .limit(numPages)
                .collect(Collectors.toList());

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
}

