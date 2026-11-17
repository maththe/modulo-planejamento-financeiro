export type PerfilUsuario = 'PRODUTOR' | 'GESTOR';
export type TipoLancamento = 'ENTRADA' | 'SAIDA';
export type ClassificacaoCusto = 'NAO_APLICAVEL' | 'FIXO' | 'VARIAVEL';
export type CategoriaLancamento =
  | 'VENDAS'
  | 'SEMENTES'
  | 'ADUBO'
  | 'FRETE'
  | 'AGUA'
  | 'ENERGIA'
  | 'MAO_DE_OBRA'
  | 'OUTROS';
export type StatusSemaforo = 'VERDE' | 'AMARELO' | 'VERMELHO';
export type SyncItemStatus = 'SINCRONIZADO' | 'PENDENTE' | 'SINCRONIZANDO' | 'CONFLITO';
export type GlobalSyncState = 'SINCRONIZADO' | 'PENDENTE' | 'SINCRONIZANDO' | 'CONFLITO';

export interface UsuarioResumo {
  id: string;
  nome: string;
  login: string;
  perfil: PerfilUsuario;
  hortaId: string;
  hortaNome: string;
  totpHabilitado: boolean;
}

export interface ProdutorHorta {
  id: string;
  nome: string;
  login: string;
}

export interface Cultura {
  id: string;
  nome: string;
  icone: string;
  unidade: string;
}

export interface RateioItem {
  id?: string;
  culturaId: string;
  culturaNome: string;
  valorRateado: number;
  percentual: number;
}

export interface Lancamento {
  id: string;
  chaveIdempotencia: string;
  produtorId: string;
  culturaId: string | null;
  culturaNome: string;
  tipo: TipoLancamento;
  classificacaoCusto: ClassificacaoCusto;
  categoria: CategoriaLancamento;
  categoriaRotulo: string;
  descricao: string;
  valor: number;
  quantidade: number | null;
  dataLancamento: string;
  anoMes: string;
  versao: number;
  saldoAcumulado: number | null;
  rateios: RateioItem[];
  atualizadoEm: string;
  syncStatus?: SyncItemStatus;
}

export interface CadernoDigitalResponse {
  anoMes: string;
  periodoFechado: boolean;
  totalEntradas: number;
  totalSaidas: number;
  saldoPeriodo: number;
  lancamentos: Lancamento[];
}

export interface SugestaoCategoriaResponse {
  tipo: TipoLancamento;
  categoriaSugerida: CategoriaLancamento;
  rotuloSugerido: string;
  rankingHistorico: Array<{
    categoria: CategoriaLancamento;
    rotulo: string;
    quantidadeUso: number;
  }>;
}

export interface IndicadorCultura {
  culturaId: string;
  culturaNome: string;
  unidade: string;
  quantidadeProduzida: number;
  receita: number;
  custosVariaveis: number;
  custosFixosRateados: number;
  custoTotal: number;
  custoUnitario: number | null;
  margemContribuicao: number;
  resultado: number;
}

export interface IndicadoresPeriodo {
  anoMes: string;
  periodoFechado: boolean;
  receitaTotal: number;
  custosFixos: number;
  custosVariaveis: number;
  custosTotais: number;
  margemContribuicao: number;
  pontoEquilibrio: number | null;
  resultado: number;
  margemNulaOuNegativa: boolean;
  avisoMargem: string | null;
  statusSemaforo: StatusSemaforo;
  indicadoresPorCultura: IndicadorCultura[];
}

export interface SerieHistoricaItem {
  anoMes: string;
  periodoFechado: boolean;
  receitaTotal: number;
  custosFixos: number;
  custosVariaveis: number;
  custosTotais: number;
  margemContribuicao: number;
  pontoEquilibrio: number | null;
  resultado: number;
  margemNulaOuNegativa: boolean;
  statusSemaforo: StatusSemaforo;
}

export interface OfflineQueueItem {
  chaveIdempotencia: string;
  produtorId: string;
  culturaId: string | null;
  culturaNome: string;
  tipo: TipoLancamento;
  classificacaoCusto: ClassificacaoCusto;
  categoria: CategoriaLancamento;
  categoriaRotulo: string;
  descricao: string;
  valor: number;
  quantidade: number | null;
  dataLancamento: string;
  anoMes: string;
  versaoCliente: number;
  clienteAtualizadoEm: string;
  status: SyncItemStatus;
  rateios: Array<{ culturaId: string; valorRateado: number }>;
}

export interface ConflitoSync {
  id: string;
  chaveIdempotencia: string;
  lancamentoId: string;
  versaoServidor: number;
  versaoCliente: number;
  regraAplicada: string;
  payloadAnterior: string;
  payloadNovo: string;
  resolvidoEm: string;
}

export interface SyncLoteResponse {
  totalRecebidos: number;
  sincronizados: number;
  idempotentes: number;
  conflitos: number;
  erros: number;
  resultados: Array<{
    chaveIdempotencia: string;
    status: string;
    lancamentoId: string | null;
    versaoAtual: number | null;
    mensagem: string;
  }>;
}
