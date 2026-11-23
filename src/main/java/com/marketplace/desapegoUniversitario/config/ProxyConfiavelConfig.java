package com.marketplace.desapegoUniversitario.config;

import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

@Configuration
public class ProxyConfiavelConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final Pattern IPV4 = Pattern.compile(
            "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
                    + "(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");

    private final String trustedProxies;

    public ProxyConfiavelConfig(@Value("${app.rate-limit.trusted-proxies:}") String trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        if (!StringUtils.hasText(trustedProxies)) {
            return;
        }
        String[] redes = Arrays.stream(trustedProxies.split("\\|", -1))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(rede -> IPV4.matcher(rede).matches())
                .toArray(String[]::new);
        if (redes.length == 0) {
            return;
        }
        RemoteIpValve valve = new RemoteIpValve();
        valve.setInternalProxies(String.join(",", redes));
        valve.setRemoteIpHeader("X-Forwarded-For");
        valve.setProtocolHeader("X-Forwarded-Proto");
        valve.setHostHeader("X-Forwarded-Host");
        factory.addEngineValves(valve);
    }
}
