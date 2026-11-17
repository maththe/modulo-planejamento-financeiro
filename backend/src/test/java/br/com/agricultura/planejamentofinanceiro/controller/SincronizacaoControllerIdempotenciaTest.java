package br.com.agricultura.planejamentofinanceiro.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.agricultura.planejamentofinanceiro.domain.entity.ConflitoSincronizacao;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.repository.ConflitoSincronizacaoRepository;
import br.com.agricultura.planejamentofinanceiro.repository.LancamentoRepository;
import br.com.agricultura.planejamentofinanceiro.security.TotpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SincronizacaoControllerIdempotenciaTest {

    private static final UUID PRODUTOR_JOAO_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ConflitoSincronizacaoRepository conflitoRepository;

    @Autowired
    private TotpService totpService;

    @Test
    void deveGarantirIdempotenciaLoteParcialEUltimaEscritaVence() throws Exception {
        String tokenProdutor = autenticar("joao.silva", "123456", null);
        long contagemInicial = lancamentoRepository.countByProdutorIdAndExcluidoFalse(PRODUTOR_JOAO_ID);

        String loteJson = """
                {
                  "itens": [
                    {
                      "chaveIdempotencia": "offline-test-001",
                      "culturaId": "33333333-3333-3333-3333-333333333301",
                      "tipo": "ENTRADA",
                      "categoria": "VENDAS",
                      "descricao": "Venda alface feira domingo",
                      "valor": 145.50,
                      "quantidade": 70,
                      "dataLancamento": "2026-11-17",
                      "versaoCliente": 1
                    },
                    {
                      "chaveIdempotencia": "offline-test-002",
                      "tipo": "SAIDA",
                      "classificacaoCusto": "VARIAVEL",
                      "categoria": "ADUBO",
                      "descricao": "Saco composto orgânico",
                      "valor": 80.00,
                      "dataLancamento": "2026-11-17",
                      "versaoCliente": 1
                    }
                  ]
                }
                """;

        // 1. Primeiro envio cria os 2 lancamentos
        mockMvc.perform(post("/api/sync/lotes")
                        .header("Authorization", "Bearer " + tokenProdutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecebidos").value(2))
                .andExpect(jsonPath("$.sincronizados").value(2))
                .andExpect(jsonPath("$.idempotentes").value(0))
                .andExpect(jsonPath("$.erros").value(0));

        assertThat(lancamentoRepository.countByProdutorIdAndExcluidoFalse(PRODUTOR_JOAO_ID))
                .isEqualTo(contagemInicial + 2);

        // 2. Reenvio identico do mesmo lote nao duplica nada (idempotencia)
        mockMvc.perform(post("/api/sync/lotes")
                        .header("Authorization", "Bearer " + tokenProdutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecebidos").value(2))
                .andExpect(jsonPath("$.sincronizados").value(0))
                .andExpect(jsonPath("$.idempotentes").value(2))
                .andExpect(jsonPath("$.conflitos").value(0))
                .andExpect(jsonPath("$.erros").value(0));

        assertThat(lancamentoRepository.countByProdutorIdAndExcluidoFalse(PRODUTOR_JOAO_ID))
                .isEqualTo(contagemInicial + 2);

        // 3. Envio com alteracao na mesma chave gera conflito resolvido por "ultima escrita vence"
        String loteConflitoJson = """
                {
                  "itens": [
                    {
                      "chaveIdempotencia": "offline-test-001",
                      "culturaId": "33333333-3333-3333-3333-333333333301",
                      "tipo": "ENTRADA",
                      "categoria": "VENDAS",
                      "descricao": "Venda alface feira domingo corrigida",
                      "valor": 160.00,
                      "quantidade": 75,
                      "dataLancamento": "2026-11-17",
                      "versaoCliente": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/sync/lotes")
                        .header("Authorization", "Bearer " + tokenProdutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loteConflitoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflitos").value(1))
                .andExpect(jsonPath("$.resultados[0].status").value("CONFLITO"))
                .andExpect(jsonPath("$.resultados[0].versaoAtual").value(2));

        Lancamento atualizado = lancamentoRepository.findByChaveIdempotencia("offline-test-001").orElseThrow();
        assertThat(atualizado.getValor()).isEqualByComparingTo("160.00");
        assertThat(atualizado.getVersao()).isEqualTo(2L);

        List<ConflitoSincronizacao> conflitos = conflitoRepository.findByProdutorIdOrderByResolvidoEmDesc(PRODUTOR_JOAO_ID);
        assertThat(conflitos).isNotEmpty();
        assertThat(conflitos.get(0).getRegraAplicada()).isEqualTo("ULTIMA_ESCRITA_VENCE");

        // 4. Lote parcial: 1 item valido e 1 item invalido (valor negativo)
        String loteParcialJson = """
                {
                  "itens": [
                    {
                      "chaveIdempotencia": "offline-test-003",
                      "tipo": "SAIDA",
                      "classificacaoCusto": "VARIAVEL",
                      "categoria": "FRETE",
                      "descricao": "Frete feira",
                      "valor": 45.00,
                      "dataLancamento": "2026-11-17",
                      "versaoCliente": 1
                    },
                    {
                      "chaveIdempotencia": "offline-test-invalido",
                      "tipo": "SAIDA",
                      "categoria": "FRETE",
                      "descricao": "Valor negativo",
                      "valor": -10.00,
                      "dataLancamento": "2026-11-17",
                      "versaoCliente": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/sync/lotes")
                        .header("Authorization", "Bearer " + tokenProdutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loteParcialJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecebidos").value(2))
                .andExpect(jsonPath("$.sincronizados").value(1))
                .andExpect(jsonPath("$.erros").value(1));

        assertThat(lancamentoRepository.findByChaveIdempotencia("offline-test-003")).isPresent();
        assertThat(lancamentoRepository.findByChaveIdempotencia("offline-test-invalido")).isEmpty();
    }

    @Test
    void deveExigirTotpParaGestorEManterModoSomenteLeitura() throws Exception {
        // Gestor sem TOTP deve receber 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"carlos.gestor\",\"senha\":\"123456\"}"))
                .andExpect(status().isUnauthorized());

        // Gestor com TOTP valido autentica, le dados de produtor da horta e recebe 403 ao tentar gravar
        String codigoTotp = totpService.gerarCodigoAtual("JBSWY3DPEHPK3PXP");
        String tokenGestor = autenticar("carlos.gestor", "123456", codigoTotp);

        mockMvc.perform(get("/api/indicadores")
                        .param("anoMes", "2026-11")
                        .param("produtorId", PRODUTOR_JOAO_ID.toString())
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusSemaforo").value("VERDE"));

        mockMvc.perform(post("/api/lancamentos")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "ENTRADA",
                                  "categoria": "VENDAS",
                                  "valor": 50.00,
                                  "dataLancamento": "2026-11-17"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String autenticar(String login, String senha, String totp) throws Exception {
        String body = totp == null
                ? String.format("{\"login\":\"%s\",\"senha\":\"%s\"}", login, senha)
                : String.format("{\"login\":\"%s\",\"senha\":\"%s\",\"codigoTotp\":\"%s\"}", login, senha, totp);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
