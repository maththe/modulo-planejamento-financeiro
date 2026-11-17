package br.com.agricultura.planejamentofinanceiro.domain.entity;

import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lancamentos_recorrentes")
public class LancamentoRecorrente {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produtor_id", nullable = false)
    private Usuario produtor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultura_id")
    private Cultura cultura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_custo", nullable = false, length = 20)
    private ClassificacaoCusto classificacaoCusto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaLancamento categoria;

    @Column(length = 255)
    private String descricao;

    @Column(name = "valor_padrao", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorPadrao;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public LancamentoRecorrente() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getProdutor() {
        return produtor;
    }

    public void setProdutor(Usuario produtor) {
        this.produtor = produtor;
    }

    public Cultura getCultura() {
        return cultura;
    }

    public void setCultura(Cultura cultura) {
        this.cultura = cultura;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public void setTipo(TipoLancamento tipo) {
        this.tipo = tipo;
    }

    public ClassificacaoCusto getClassificacaoCusto() {
        return classificacaoCusto;
    }

    public void setClassificacaoCusto(ClassificacaoCusto classificacaoCusto) {
        this.classificacaoCusto = classificacaoCusto;
    }

    public CategoriaLancamento getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaLancamento categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorPadrao() {
        return valorPadrao;
    }

    public void setValorPadrao(BigDecimal valorPadrao) {
        this.valorPadrao = valorPadrao;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
