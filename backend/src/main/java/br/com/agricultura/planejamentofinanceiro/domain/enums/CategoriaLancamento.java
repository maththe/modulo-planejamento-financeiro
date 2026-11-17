package br.com.agricultura.planejamentofinanceiro.domain.enums;

public enum CategoriaLancamento {
    VENDAS("Vendas da Horta", TipoLancamento.ENTRADA, ClassificacaoCusto.NAO_APLICAVEL),
    SEMENTES("Sementes e Mudas", TipoLancamento.SAIDA, ClassificacaoCusto.VARIAVEL),
    ADUBO("Adubo e Composto", TipoLancamento.SAIDA, ClassificacaoCusto.VARIAVEL),
    FRETE("Frete e Transporte", TipoLancamento.SAIDA, ClassificacaoCusto.VARIAVEL),
    AGUA("Água e Irrigação", TipoLancamento.SAIDA, ClassificacaoCusto.FIXO),
    ENERGIA("Energia Elétrica", TipoLancamento.SAIDA, ClassificacaoCusto.FIXO),
    MAO_DE_OBRA("Mão de Obra", TipoLancamento.SAIDA, ClassificacaoCusto.FIXO),
    OUTROS("Outros", TipoLancamento.SAIDA, ClassificacaoCusto.VARIAVEL);

    private final String rotulo;
    private final TipoLancamento tipoPadrao;
    private final ClassificacaoCusto classificacaoPadrao;

    CategoriaLancamento(String rotulo, TipoLancamento tipoPadrao, ClassificacaoCusto classificacaoPadrao) {
        this.rotulo = rotulo;
        this.tipoPadrao = tipoPadrao;
        this.classificacaoPadrao = classificacaoPadrao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public TipoLancamento getTipoPadrao() {
        return tipoPadrao;
    }

    public ClassificacaoCusto getClassificacaoPadrao() {
        return classificacaoPadrao;
    }
}
