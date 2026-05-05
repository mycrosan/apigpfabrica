package br.compneusgppremium.api.controller.model;

import lombok.Data;

@Data
public class ResumoCarcacasModel {
    private long mesRetrasado;
    private long mesPassado;
    private long mesAtual;
    private long anteontem;
    private long ontem;
    private long hoje;
}
