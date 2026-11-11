package com.servicehub.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Porta interna para o Prometheus coletar /actuator/prometheus sem token (ADR-0008).
 * Só existe quando servicehub.metrics.internal-port > 0; o Compose não publica essa porta,
 * então ela só é alcançada de dentro da rede dos containers. A liberação do caminho, e só dele,
 * está no SecurityConfig.
 */
@Configuration
@ConditionalOnExpression("${servicehub.metrics.internal-port:0} > 0")
public class MetricsPortConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> metricsConnector(
            @Value("${servicehub.metrics.internal-port}") int port) {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(port);
            factory.addAdditionalConnectors(connector);
        };
    }
}
