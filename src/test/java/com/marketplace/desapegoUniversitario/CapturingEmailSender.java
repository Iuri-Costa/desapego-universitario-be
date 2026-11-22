package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.service.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Primary
@Profile("test")
public class CapturingEmailSender implements EmailSender {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9-]+)");
    private static final List<Mensagem> MENSAGENS = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        MENSAGENS.add(new Mensagem(destinatario, assunto, corpo));
    }

    public static String ultimoTokenPara(String destinatario) {
        return MENSAGENS.stream()
                .filter(mensagem -> mensagem.destinatario().equals(destinatario))
                .map(mensagem -> {
                    Matcher matcher = TOKEN.matcher(mensagem.corpo());
                    return matcher.find() ? matcher.group(1) : null;
                })
                .filter(token -> token != null)
                .reduce((primeiro, ultimo) -> ultimo)
                .orElseThrow(() -> new IllegalStateException("Nenhum token capturado para " + destinatario));
    }

    public static long contarPara(String destinatario) {
        return MENSAGENS.stream().filter(mensagem -> mensagem.destinatario().equals(destinatario)).count();
    }

    public static void limpar() {
        MENSAGENS.clear();
    }

    private record Mensagem(String destinatario, String assunto, String corpo) {
    }
}
