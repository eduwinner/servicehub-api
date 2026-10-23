package com.servicehub.service;

import org.springframework.web.util.HtmlUtils;

/**
 * Sanitização de texto livre (Aula 8): remove espaços das pontas e escapa só os caracteres que formam
 * marcação HTML (&lt; &gt; &amp; " '), para que um front-end que renderize o campo não execute HTML enviado por
 * outro usuário (XSS). Acentos ficam intactos (escapa em UTF-8). O texto é "desescapado" antes, então reenviar
 * num PUT o valor recebido num GET não acumula escapes (operação idempotente).
 */
final class TextSanitizer {

    private TextSanitizer() {
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : HtmlUtils.htmlEscape(HtmlUtils.htmlUnescape(trimmed), "UTF-8");
    }
}
