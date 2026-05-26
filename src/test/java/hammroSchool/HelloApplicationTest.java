package hammroSchool;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.hammroschool.controller.AuthController;

public class HelloApplicationTest {
    @Test
    void contextLoads() {
        AuthController controller = new AuthController();
        assertNotNull(controller);
    }
}
