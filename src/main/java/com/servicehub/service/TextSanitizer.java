package com.servicehub.service;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Sanitização de texto livre (Aula 8): remove espaços das pontas e escapa HTML antes de gravar,
 * para que um front-end que renderize o campo não execute marcação enviada por outro usuário (XSS).
 */
final class TextSanitizer {

    private TextSanitizer() {
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : StringEscapeUtils.escapeHtml4(trimmed);
    }
}
