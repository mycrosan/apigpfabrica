package br.compneusgppremium.api.util;
//ajuste
public enum ResumoEntidade {
    COBERTURA("cobertura", "dt_create"),
    QUALIDADE("controle_qualidade", "dt_create"),
    PRODUCAO("producao", "dt_create"),
    CARCACA("carcaca", "dt_create");

    private final String tabela;
    private final String coluna;

    ResumoEntidade(String tabela, String coluna) {
        this.tabela = tabela;
        this.coluna = coluna;
    }

    public String getTabela() { return tabela; }
    public String getColuna() { return coluna; }
}

