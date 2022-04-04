package engine;

import repository.Repo;
import repository.Repository;

public class Subject {
    Repo.Page pageRepo = new Repo.Page();

    public static String subjectMethod(String url) {
        return Repository.Page.getPageId(url);
    }
}
