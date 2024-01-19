package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {

    private Javalin app;

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName).toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeEach
    public final void setApp() throws IOException, SQLException {
        app = App.getApp();
    }

    @Test
    public void testShowMainPage() {
        JavalinTest.test(app, ((server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string())
                    .contains("<p class=\"lead\">Бесплатно проверяйте сайты на SEO пригодность</p>");
        }));
    }

    @Test
    public void testUrlPage() {
        JavalinTest.test(app, ((server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Сайты");
        }));
    }

    @Test
    public void testUrlPageNumber() {
        var mockServer = new MockWebServer();
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + mockServer.url("/test").toString());
            assertThat(client.get("/urls?page=1").code()).isEqualTo(200);
            assertThat(client.get("/urls?page=1").body().string()).contains("/urls?page=1");
        });
    }

    @Test
    public void testCreateUrl() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls", "url=https://google.com/12345");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string())
                    .contains("<a href=\"/urls/1\">https://google.com</a>");

            var response2 = client.post("/urls", "url=https://google.com/12345");
            assertThat(response2.code()).isEqualTo(200);
            assertThat(response2.body().string())
                    .contains("<a class=\"navbar-brand\" href=\"/\">Анализатор страниц</a>");
            assertThat(UrlRepository.checkUrlExist("https://google.com")).isTrue();
        });
    }

    @Test
    public void testListUrls() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    public void testShow() {
        JavalinTest.test(app, (server, client) -> {
            var url = new Url("https://google.com");
            UrlRepository.save(url);
            var newUrl = UrlRepository.findByName("https://google.com");
            var id = newUrl.get().getId();
            var response = client.get("/urls/" + id);
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    public void testUrlNotFound() {
        JavalinTest.test(app, (server, client) -> {
            UrlRepository.destroy(25);
            var response = client.get("/urls/25");
            assertThat(response.code()).isEqualTo(404);
        });
    }
}
