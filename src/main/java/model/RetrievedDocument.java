package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class RetrievedDocument extends PageInfo {
    public double score;
    public HashMap<String, Integer> keyword_freq;

    private RetrievedDocument(String pageTitle, String url, String lastModifiedData, HashSet<String> childLinks, String pageSize, int max_termFreq) {
        super(pageTitle, url, lastModifiedData, childLinks, pageSize, max_termFreq);
    }

    public RetrievedDocument(PageInfo pageInfo, double score, HashMap<String, Integer> keyword_freq) {
        this(pageInfo.pageTitle, pageInfo.url, pageInfo.lastModifiedDate, pageInfo.childLinks, pageInfo.pageSize, pageInfo.max_termFreq);
        this.parentLinks = pageInfo.parentLinks;
        this.score = score;
        this.keyword_freq = keyword_freq;
    }

    @Override
    public String toString() {
        return String.format("%.2f", score) + "\t" + pageTitle + '\n' +
                "\t\t" + url + '\n' +
                "\t\t" + lastModifiedDate + ", " + pageSize + '\n' +
                "\t\t" + keyword_freq + '\n' +
                "\t\tParent Links" + '\n' +
                "\t\t" + parentLinks.toString() + '\n' +
                "\t\tChild Links" + '\n' +
                "\t\t" + childLinks.toString() + '\n' +
                "\t=======================================================\n" ;
    }

}
