package engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter;
//import org.mockito.junit.jupiter.MockitoExtension;
import repository.Repository;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

//@ExtendWith(MockitoExtension.class)
public class SubjectTest {

    @Test
    void test() {
//        try (MockedStatic<Repository.Page> mb = Mockito.mockStatic(Repository.Page.class)) {

        try (MockedStatic<Repository.Page> utilities = Mockito.mockStatic(Repository.Page.class)) {
            utilities.when(() -> Repository.Page.getPageId("page1")).thenReturn(String.valueOf("page1".hashCode()));
            assertEquals(String.valueOf("page1".hashCode()), Subject.subjectMethod("page1"));
        }


    }

}
