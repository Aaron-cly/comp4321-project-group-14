package crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrawlerTest {
    Crawler crawler = null;
    String rootURL = "http://www.cse.ust.hk";

    @BeforeEach
    void setUp() {
        crawler = new Crawler(rootURL);
    }

    @Test
    void test_crawlFromRoot() {
        int numPages = 30;
        try {
            crawler.crawlFromRoot(numPages);
        } catch (IOException e) {
        }
        assertEquals(numPages, crawler.getUrlList().size());
    }

    @Test
    void test_crawlFromRoot_All() {
        try {
            crawler.crawlFromRoot();
        } catch (IOException e) {
        }
        assertTrue(crawler.getUrlList().size() > 100);
        crawler.getUrlList().forEach(url -> {
            assertTrue(url.startsWith(rootURL));
        });
    }
}
