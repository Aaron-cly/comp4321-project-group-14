package engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.Repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SubjectTest {

    @Test
    void test() {

        try (MockedStatic<Repository.Page> utilities = Mockito.mockStatic(Repository.Page.class)) {
            utilities.when(() -> Repository.Page.getPageId("page1")).thenReturn(String.valueOf("page1".hashCode()));
//            assertThat(StaticUtils.name()).isEqualTo("Eugen");
        }

        assertEquals(String.valueOf("page1".hashCode()), Subject.subjectMethod("page1"));

    }

}
