-- Schema inicial do MVP Modulo de Planejamento Financeiro

CREATE TABLE hortas (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    horta_id UUID NOT NULL REFERENCES hortas(id),
    nome VARCHAR(150) NOT NULL,
    login VARCHAR(80) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    totp_secret VARCHAR(64),
    totp_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE culturas (
    id UUID PRIMARY KEY,
    horta_id UUID NOT NULL REFERENCES hortas(id),
    nome VARCHAR(80) NOT NULL,
    icone VARCHAR(40) NOT NULL,
    unidade VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producoes_mensais (
    id UUID PRIMARY KEY,
    produtor_id UUID NOT NULL REFERENCES usuarios(id),
    cultura_id UUID NOT NULL REFERENCES culturas(id),
    ano_mes VARCHAR(7) NOT NULL,
    quantidade_produzida NUMERIC(14,2) NOT NULL,
    CONSTRAINT uq_producao_mensal UNIQUE (produtor_id, cultura_id, ano_mes)
);

CREATE TABLE lancamentos (
    id UUID PRIMARY KEY,
    chave_idempotencia VARCHAR(100) NOT NULL UNIQUE,
    produtor_id UUID NOT NULL REFERENCES usuarios(id),
    cultura_id UUID REFERENCES culturas(id),
    tipo VARCHAR(20) NOT NULL,
    classificacao_custo VARCHAR(20) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    valor NUMERIC(14,2) NOT NULL,
    quantidade NUMERIC(14,2),
    data_lancamento DATE NOT NULL,
    ano_mes VARCHAR(7) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 1,
    excluido BOOLEAN NOT NULL DEFAULT FALSE,
    excluido_em TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lancamentos_produtor_mes ON lancamentos (produtor_id, ano_mes, excluido);
CREATE INDEX idx_lancamentos_categoria ON lancamentos (produtor_id, tipo, categoria);

CREATE TABLE rateios_custo (
    id UUID PRIMARY KEY,
    lancamento_id UUID NOT NULL REFERENCES lancamentos(id) ON DELETE CASCADE,
    cultura_id UUID NOT NULL REFERENCES culturas(id),
    valor_rateado NUMERIC(14,2) NOT NULL,
    percentual NUMERIC(6,2) NOT NULL
);

CREATE INDEX idx_rateios_lancamento ON rateios_custo (lancamento_id);

CREATE TABLE lancamentos_recorrentes (
    id UUID PRIMARY KEY,
    produtor_id UUID NOT NULL REFERENCES usuarios(id),
    cultura_id UUID REFERENCES culturas(id),
    tipo VARCHAR(20) NOT NULL,
    classificacao_custo VARCHAR(20) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    valor_padrao NUMERIC(14,2) NOT NULL,
    dia_vencimento INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fechamentos_periodo (
    id UUID PRIMARY KEY,
    produtor_id UUID NOT NULL REFERENCES usuarios(id),
    ano_mes VARCHAR(7) NOT NULL,
    receita_total NUMERIC(14,2) NOT NULL,
    custos_fixos NUMERIC(14,2) NOT NULL,
    custos_variaveis NUMERIC(14,2) NOT NULL,
    custo_total NUMERIC(14,2) NOT NULL,
    resultado NUMERIC(14,2) NOT NULL,
    margem_contribuicao NUMERIC(14,2) NOT NULL,
    ponto_equilibrio NUMERIC(14,2),
    status_semaforo VARCHAR(20) NOT NULL,
    resumo_json TEXT,
    fechado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_fechamento_periodo UNIQUE (produtor_id, ano_mes)
);

CREATE TABLE conflitos_sincronizacao (
    id UUID PRIMARY KEY,
    chave_idempotencia VARCHAR(100) NOT NULL,
    lancamento_id UUID NOT NULL,
    produtor_id UUID NOT NULL,
    versao_servidor BIGINT NOT NULL,
    versao_cliente BIGINT NOT NULL,
    regra_aplicada VARCHAR(50) NOT NULL,
    payload_anterior TEXT,
    payload_novo TEXT,
    resolvido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
