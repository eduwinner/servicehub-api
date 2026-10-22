package com.servicehub.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusResponse {
    private String status;
    private String versao;
    private String mensagem;
}
