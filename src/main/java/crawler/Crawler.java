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

/** This class is responsible for crawling the pages from the domain cse.ust.hk */
public class Crawler {
    static final int VALUE_CRAWL_ALL = Integer.MAX_VALUE;
    String rootURL = "https://cse.hkust.edu.hk";
    HashSet<String> urlSet = new HashSet<>();   // avoid crawling the same page twice
    ArrayList<String> urlList = new ArrayList<>();
    Indexer indexer = new Indexer();

    /** Sole Constructor.
     *
     * @param rootURL  The url from which crawling starts
     */
    public Crawler(String rootURL) {
        this.rootURL = rootURL;
    }

    /** @return {@code this.urlList}*/
    public ArrayList<String> getUrlList() {
        return this.urlList;
    }

    /** Helper Function for getting the response from Jsoup
     *
     * @param url Target Url to get response from
     * @return A Connection.Response object
     * @throws IOException
     */
    private Connection.Response getResponse(String url) throws IOException {
        Connection conn = Jsoup.connect(url).timeout(1000).followRedirects(true);
        return conn.execute();
    }

    private static List<String> banList = List.of("ftp", "@", "Password_Only", "?", "#", "www", "http", "index.html");

    /** Helper Method to check if link is valid
     *
     * @param link  the url/link to be checked
     * @return  a boolean value indicating whether the link is valid
     */
    private boolean validLink(String link) {
        boolean valid = banList.stream().noneMatch(link::contains)
                            && !link.startsWith("javascript")
                            && !link.equals("/");
        if (!valid) return false;

        // get the file of the link and check extension
        int index = link.lastIndexOf('/');
        if (index == link.length()-1) {
            return true;
        } else {
            String file = link.substring(index + 1);
            if (file.contains(".")) {
                return file.endsWith(".html") || file.endsWith(".htm") || file.endsWith(".txt");
            } else {
                return true;
            }
        }
    }

    /** Gets all the child links from a page. Child links are filtered through {@link #validLink(String)}, and checked
     *  for html files. For some links, they are appended to the root cse.ust.hk domain
     *
     * @param url  Target Url to get child links from
     * @return  A set containing all the child links from the url
     */
    private HashSet<String> getPagesFromURL(String url) {
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
                    if (link.equals(".")) {
                        return rootURL;
                    }
                    if (link.startsWith("/")) {
                        return rootURL + link;
                    } else if (link.startsWith("./")) {
                        int mountPoint = currentUrl.lastIndexOf('/');
                        String baseUrl = currentUrl.substring(0, mountPoint + 1);

                        return baseUrl + link.substring(2);
                    } else if (link.startsWith("../")) {
                        // System.out.print(currentUrl + ":" + link + "    :");
                        String baseUrl = null;
                        int mountPoint = currentUrl.lastIndexOf('/');
                        baseUrl = currentUrl.substring(0, mountPoint);
                        while (link.startsWith("../")) {
                            mountPoint = baseUrl.lastIndexOf('/');
                            baseUrl = baseUrl.substring(0, mountPoint);
                            if (link.length() > 3)
                                link = link.substring(3);
                            else {
                                link = "";
                                break;
                            }
                        }
                        char lastChar = baseUrl.charAt(baseUrl.length() - 1);
                        String child = baseUrl + (lastChar == '/' ? "" : '/') + link;
                        // System.out.println(child);
                        return child;
                    } else {
                        if (currentUrl.endsWith(".html") || currentUrl.endsWith(".htm")) {
                            int mountPoint = currentUrl.lastIndexOf('/');
                            String baseUrl = currentUrl.substring(0, mountPoint + 1);
                            return baseUrl + link;
                        } else if (currentUrl.endsWith(".html/") || currentUrl.endsWith(".htm/")) {
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

        return urlSet;
    }

    /** Calls {@link #crawlFromRoot(int)} with VALUE_CRAWL_ALL, to indicate that the cralwer should
     * crawl all the pages
     * @throws IOException
     */
    public void crawlFromRoot() throws IOException {
        crawlFromRoot(VALUE_CRAWL_ALL);
    }

    /**
     * Crawls and indexes pages from the root of the rootUrl specified in the constructor. Follows a BFS
     * approach, whereby it starts with root node/links, gathers all its child nodes/links and goes through
     * the child nodes subsequently.
     * @param numPages  The number of pages to be crawled.
     * @throws IOException
     */
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
        System.out.println(currentIndex);
        indexer.construct_parents_from_child_links();
    }

    /** Crawls and indexed a page. Will invoke {@link indexer.Indexer#shouldIgnoreUrl(java.lang.String, java.lang.String)}
     *  to check if urls should be ignored. If they should not be ignored, then it calls {@link Indexer#insert_new_page(Document, String, String, int, HashSet)}
     *  to index the page.
     * @param url  the page url to be indexed
     * @param pagesOnURL  The child links of the page
     * @throws IOException
     */
    private void crawlPage(String url, HashSet<String> pagesOnURL) throws IOException {
        Document doc = null;
        Connection.Response res = null;
        try {
            res = getResponse(url);
            doc = res.parse();
        } catch (IOException e) {
        }
        if (doc == null) return;

        res.url().toString();

        // open connection for page size
        var connection = new URL(url).openConnection();
        connection.setConnectTimeout(5 * 1000);
        var pgSize = connection.getContentLength();
        String lastModifiedDate = getLastModifiedDate(res, doc);

        // insert new page if url should not be ignored
        if (!indexer.shouldIgnoreUrl(url, lastModifiedDate)) {
            indexer.insert_new_page(doc, url, lastModifiedDate, pgSize, pagesOnURL);
        }
    }

    /** Gets the lastModifiedDate of a Page. It checks whether the page has a last modified date explicitly
     * written somewhere in the content of the page, and then further checks if there is any information
     * regarding the last modified date in the header response. If the last modified date is still not found,
     * then it returns "N/A"
     * @param res  The connection response of connecting to the page
     * @param doc  The document of the page
     * @return  The last modified date if found, and "N/A" if not found.
     */
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

