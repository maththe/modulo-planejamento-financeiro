package br.com.agricultura.planejamentofinanceiro.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conflitos_sincronizacao")
public class ConflitoSincronizacao {

    @Id
    private UUID id;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Column(name = "lancamento_id", nullable = false)
    private UUID lancamentoId;

    @Column(name = "produtor_id", nullable = false)
    private UUID produtorId;

    @Column(name = "versao_servidor", nullable = false)
    private Long versaoServidor;

    @Column(name = "versao_cliente", nullable = false)
    private Long versaoCliente;

    @Column(name = "regra_aplicada", nullable = false, length = 50)
    private String regraAplicada;

    @Column(name = "payload_anterior", columnDefinition = "TEXT")
    private String payloadAnterior;

    @Column(name = "payload_novo", columnDefinition = "TEXT")
    private String payloadNovo;

    @Column(name = "resolvido_em", nullable = false)
    private LocalDateTime resolvidoEm;

    public ConflitoSincronizacao() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public void setChaveIdempotencia(String chaveIdempotencia) {
        this.chaveIdempotencia = chaveIdempotencia;
    }

    public UUID getLancamentoId() {
        return lancamentoId;
    }

    public void setLancamentoId(UUID lancamentoId) {
        this.lancamentoId = lancamentoId;
    }

    public UUID getProdutorId() {
        return produtorId;
    }

    public void setProdutorId(UUID produtorId) {
        this.produtorId = produtorId;
    }

    public Long getVersaoServidor() {
        return versaoServidor;
    }

    public void setVersaoServidor(Long versaoServidor) {
        this.versaoServidor = versaoServidor;
    }

    public Long getVersaoCliente() {
        return versaoCliente;
    }

    public void setVersaoCliente(Long versaoCliente) {
        this.versaoCliente = versaoCliente;
    }

    public String getRegraAplicada() {
        return regraAplicada;
    }

    public void setRegraAplicada(String regraAplicada) {
        this.regraAplicada = regraAplicada;
    }

    public String getPayloadAnterior() {
        return payloadAnterior;
    }

    public void setPayloadAnterior(String payloadAnterior) {
        this.payloadAnterior = payloadAnterior;
    }

    public String getPayloadNovo() {
        return payloadNovo;
    }

    public void setPayloadNovo(String payloadNovo) {
        this.payloadNovo = payloadNovo;
    }

    public LocalDateTime getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(LocalDateTime resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }
}
