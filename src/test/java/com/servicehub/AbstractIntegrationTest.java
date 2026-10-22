package com.servicehub;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base dos testes de integração: sobe a aplicação inteira numa porta aleatória,
 * com PostgreSQL real (Testcontainers) e banco limpo antes de cada teste.
 * Traz atalhos para cadastrar, autenticar e chamar a API com token.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "servicehub.jwt.secret=" + AbstractIntegrationTest.JWT_SECRET,
                "servicehub.admin.email=" + AbstractIntegrationTest.ADMIN_EMAIL,
                "servicehub.admin.password=" + AbstractIntegrationTest.PASSWORD
        })
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    public static final String JWT_SECRET = "segredo-de-teste-com-mais-de-32-bytes-0123456789";
    public static final String ADMIN_EMAIL = "admin@servicehub.com";
    public static final String PASSWORD = "Senha123";

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void limparBanco() throws Exception {
        jdbc.execute("TRUNCATE TABLE reviews, service_requests, services, users RESTART IDENTITY CASCADE");
        // recria o ADMIN inicial apagado pelo TRUNCATE
        context.getBean(org.springframework.boot.ApplicationRunner.class).run(null);
    }

    // ---------- atalhos ----------

    protected Map<String, Object> registrar(String nome, String email, String role) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/register",
                Map.of("fullName", nome, "email", email, "password", PASSWORD, "role", role), Map.class);
        assertThat(resp.getStatusCode()).as("cadastro de %s", email).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    protected long registrarId(String nome, String email, String role) {
        return ((Number) registrar(nome, email, role).get("id")).longValue();
    }

    protected String login(String email) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD), Map.class);
        assertThat(resp.getStatusCode()).as("login de %s", email).isEqualTo(HttpStatus.OK);
        return (String) resp.getBody().get("accessToken");
    }

    protected String adminToken() {
        return login(ADMIN_EMAIL);
    }

    protected <T> ResponseEntity<T> call(HttpMethod method, String url, String token, Object body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), type);
    }

    protected ResponseEntity<Map> get(String url, String token) {
        return call(HttpMethod.GET, url, token, null, Map.class);
    }

    protected ResponseEntity<Map> post(String url, String token, Object body) {
        return call(HttpMethod.POST, url, token, body, Map.class);
    }

    protected ResponseEntity<Map> put(String url, String token, Object body) {
        return call(HttpMethod.PUT, url, token, body, Map.class);
    }

    protected ResponseEntity<Map> patch(String url, String token, Object body) {
        return call(HttpMethod.PATCH, url, token, body, Map.class);
    }

    protected ResponseEntity<Map> delete(String url, String token) {
        return call(HttpMethod.DELETE, url, token, null, Map.class);
    }

    protected long criarServico(String providerToken, String title, String price, String category) {
        ResponseEntity<Map> resp = post("/api/v1/services", providerToken,
                Map.of("title", title, "price", price, "category", category));
        assertThat(resp.getStatusCode()).as("criação do serviço %s", title).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }
}
