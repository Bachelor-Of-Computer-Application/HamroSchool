package hammroSchool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HelloApplicationTest {
    @Test
    void contextLoads() {
        HelloController controller = new HelloController();
        assertNotNull(controller);
    }
}
