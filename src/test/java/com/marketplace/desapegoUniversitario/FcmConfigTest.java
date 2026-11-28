package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.config.FcmConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FcmConfigTest {

    @Test
    void clienteFcmDeveInterromperRequisicaoQueExcedeOTimeoutDeLeitura() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/devagar", troca -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            troca.sendResponseHeaders(200, -1);
            troca.close();
        });
        servidor.start();
        try {
            RestClient cliente = new FcmConfig().fcmRestClient(Duration.ofMillis(100));
            String uri = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/devagar";

            long inicio = System.nanoTime();
            assertThrows(RuntimeException.class,
                    () -> cliente.get().uri(uri).retrieve().toBodilessEntity());
            long decorrido = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

            assertTrue(decorrido < 800, "A requisicao demorou " + decorrido + "ms apesar do timeout de 100ms");
        } finally {
            servidor.stop(0);
        }
    }
}
