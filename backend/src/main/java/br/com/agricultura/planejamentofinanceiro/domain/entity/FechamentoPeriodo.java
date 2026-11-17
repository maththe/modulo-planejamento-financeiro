package br.com.agricultura.planejamentofinanceiro.domain.entity;

import br.com.agricultura.planejamentofinanceiro.domain.enums.StatusSemaforo;
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
@Table(name = "fechamentos_periodo")
public class FechamentoPeriodo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produtor_id", nullable = false)
    private Usuario produtor;

    @Column(name = "ano_mes", nullable = false, length = 7)
    private String anoMes;

    @Column(name = "receita_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal receitaTotal;

    @Column(name = "custos_fixos", nullable = false, precision = 14, scale = 2)
    private BigDecimal custosFixos;

    @Column(name = "custos_variaveis", nullable = false, precision = 14, scale = 2)
    private BigDecimal custosVariaveis;

    @Column(name = "custo_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoTotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal resultado;

    @Column(name = "margem_contribuicao", nullable = false, precision = 14, scale = 2)
    private BigDecimal margemContribuicao;

    @Column(name = "ponto_equilibrio", precision = 14, scale = 2)
    private BigDecimal pontoEquilibrio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_semaforo", nullable = false, length = 20)
    private StatusSemaforo statusSemaforo;

    @Column(name = "resumo_json", columnDefinition = "TEXT")
    private String resumoJson;

    @Column(name = "fechado_em", nullable = false)
    private LocalDateTime fechadoEm;

    public FechamentoPeriodo() {
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

    public String getAnoMes() {
        return anoMes;
    }

    public void setAnoMes(String anoMes) {
        this.anoMes = anoMes;
    }

    public BigDecimal getReceitaTotal() {
        return receitaTotal;
    }

    public void setReceitaTotal(BigDecimal receitaTotal) {
        this.receitaTotal = receitaTotal;
    }

    public BigDecimal getCustosFixos() {
        return custosFixos;
    }

    public void setCustosFixos(BigDecimal custosFixos) {
        this.custosFixos = custosFixos;
    }

    public BigDecimal getCustosVariaveis() {
        return custosVariaveis;
    }

    public void setCustosVariaveis(BigDecimal custosVariaveis) {
        this.custosVariaveis = custosVariaveis;
    }

    public BigDecimal getCustoTotal() {
        return custoTotal;
    }

    public void setCustoTotal(BigDecimal custoTotal) {
        this.custoTotal = custoTotal;
    }

    public BigDecimal getResultado() {
        return resultado;
    }

    public void setResultado(BigDecimal resultado) {
        this.resultado = resultado;
    }

    public BigDecimal getMargemContribuicao() {
        return margemContribuicao;
    }

    public void setMargemContribuicao(BigDecimal margemContribuicao) {
        this.margemContribuicao = margemContribuicao;
    }

    public BigDecimal getPontoEquilibrio() {
        return pontoEquilibrio;
    }

    public void setPontoEquilibrio(BigDecimal pontoEquilibrio) {
        this.pontoEquilibrio = pontoEquilibrio;
    }

    public StatusSemaforo getStatusSemaforo() {
        return statusSemaforo;
    }

    public void setStatusSemaforo(StatusSemaforo statusSemaforo) {
        this.statusSemaforo = statusSemaforo;
    }

    public String getResumoJson() {
        return resumoJson;
    }

    public void setResumoJson(String resumoJson) {
        this.resumoJson = resumoJson;
    }

    public LocalDateTime getFechadoEm() {
        return fechadoEm;
    }

    public void setFechadoEm(LocalDateTime fechadoEm) {
        this.fechadoEm = fechadoEm;
    }
}
