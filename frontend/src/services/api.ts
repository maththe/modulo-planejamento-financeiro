import {
  CadernoDigitalResponse,
  CategoriaLancamento,
  ConflitoSync,
  Cultura,
  IndicadoresPeriodo,
  Lancamento,
  OfflineQueueItem,
  ProdutorHorta,
  RateioItem,
  SerieHistoricaItem,
  SugestaoCategoriaResponse,
  SyncLoteResponse,
  TipoLancamento,
  UsuarioResumo,
} from '../types';
import {
  CATEGORIAS_META,
  calcularIndicadoresLocal,
  calcularSerieHistoricaLocal,
} from './calculadoraIndicadores';
import {
  criarLancamentosIniciaisDemo,
  DEMO_CULTURAS,
  DEMO_PRODUCOES,
  DEMO_PRODUTORES,
  DEMO_USUARIOS,
} from './demoData';
import {
  atualizarStatusNaFila,
  enfileirarOffline,
  listarFilaOffline,
  removerDaFila,
} from './offlineQueue';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';
export const IS_DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true';

const DEMO_STORAGE_KEY = 'agro_financeiro_demo_lancamentos_v1';
const DEMO_CONFLITOS_KEY = 'agro_financeiro_demo_conflitos_v1';
const AUTH_TOKEN_KEY = 'agro_financeiro_token';
const AUTH_USER_KEY = 'agro_financeiro_user';

function carregarDemoLancamentos(): Lancamento[] {
  const raw = localStorage.getItem(DEMO_STORAGE_KEY);
  if (raw) {
    try {
      return JSON.parse(raw) as Lancamento[];
    } catch {
      // reset se corrompido
    }
  }
  const iniciais = criarLancamentosIniciaisDemo();
  localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(iniciais));
  return iniciais;
}

function salvarDemoLancamentos(lista: Lancamento[]): void {
  localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(lista));
}

function carregarDemoConflitos(): ConflitoSync[] {
  const raw = localStorage.getItem(DEMO_CONFLITOS_KEY);
  if (!raw) return [];
  try {
    return JSON.parse(raw) as ConflitoSync[];
  } catch {
    return [];
  }
}

function salvarDemoConflitos(lista: ConflitoSync[]): void {
  localStorage.setItem(DEMO_CONFLITOS_KEY, JSON.stringify(lista));
}

export function obterSessaoSalva(): { token: string; usuario: UsuarioResumo } | null {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const rawUser = localStorage.getItem(AUTH_USER_KEY);
  if (!token || !rawUser) return null;
  try {
    const usuario = JSON.parse(rawUser) as UsuarioResumo;
    usuario.nome = usuario.nome.replace(/\s*\((Gestor|Produtor)\)\s*$/i, '');
    return { token, usuario };
  } catch {
    return null;
  }
}

export function salvarSessao(token: string, usuario: UsuarioResumo): void {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(usuario));
}

export function limparSessao(): void {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
}

async function fetchAuth<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = new Headers(options.headers || {});
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!res.ok) {
    let msg = `Erro ${res.status}`;
    try {
      const errBody = await res.json();
      msg = errBody.erro || errBody.message || msg;
    } catch {
      // fallback
    }
    throw new Error(msg);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export async function login(
  loginStr: string,
  senha: string,
  codigoTotp?: string
): Promise<{ token: string; usuario: UsuarioResumo }> {
  if (IS_DEMO_MODE) {
    const encontrado = DEMO_USUARIOS.find((u) => u.login === loginStr.trim());
    if (!encontrado || senha !== '123456') {
      throw new Error('Login ou senha inválidos (use 123456 na demonstração).');
    }
    const sessao = { token: `demo-token-${encontrado.login}`, usuario: encontrado };
    salvarSessao(sessao.token, sessao.usuario);
    return sessao;
  }

  const res = await fetchAuth<{ token: string; usuario: UsuarioResumo }>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ login: loginStr, senha, codigoTotp: codigoTotp || null }),
  });
  salvarSessao(res.token, res.usuario);
  return res;
}

export async function listarCulturas(): Promise<Cultura[]> {
  if (IS_DEMO_MODE) {
    return DEMO_CULTURAS;
  }
  return fetchAuth<Cultura[]>('/horta/culturas');
}

export async function listarProdutoresHorta(): Promise<ProdutorHorta[]> {
  if (IS_DEMO_MODE) {
    return DEMO_PRODUTORES;
  }
  return fetchAuth<ProdutorHorta[]>('/horta/produtores');
}

function combinarComFilaOffline(
  produtorId: string,
  anoMes: string,
  base: Lancamento[],
  fila: OfflineQueueItem[],
  periodoFechadoApi?: boolean
): CadernoDigitalResponse {
  const pendentesDoMes = fila.filter((f) => f.anoMes === anoMes && f.status !== 'SINCRONIZADO');
  const mapChaves = new Map<string, Lancamento>();

  for (const item of base) {
    mapChaves.set(item.chaveIdempotencia, {
      ...item,
      syncStatus: item.syncStatus || 'SINCRONIZADO',
    });
  }

  for (const p of pendentesDoMes) {
    const rateios: RateioItem[] = p.rateios.map((r) => ({
      culturaId: r.culturaId,
      culturaNome: DEMO_CULTURAS.find((c) => c.id === r.culturaId)?.nome || 'Cultura',
      valorRateado: r.valorRateado,
      percentual: Math.round((r.valorRateado / p.valor) * 10000) / 100,
    }));

    mapChaves.set(p.chaveIdempotencia, {
      id: p.chaveIdempotencia,
      chaveIdempotencia: p.chaveIdempotencia,
      produtorId,
      culturaId: p.culturaId,
      culturaNome: p.culturaNome,
      tipo: p.tipo,
      classificacaoCusto: p.classificacaoCusto,
      categoria: p.categoria,
      categoriaRotulo: p.categoriaRotulo,
      descricao: p.descricao,
      valor: p.valor,
      quantidade: p.quantidade,
      dataLancamento: p.dataLancamento,
      anoMes: p.anoMes,
      versao: p.versaoCliente,
      saldoAcumulado: null,
      rateios,
      atualizadoEm: p.clienteAtualizadoEm,
      syncStatus: p.status,
    });
  }

  const ordenados = Array.from(mapChaves.values()).sort((a, b) =>
    a.dataLancamento.localeCompare(b.dataLancamento) || a.atualizadoEm.localeCompare(b.atualizadoEm)
  );

  let totalEntradas = 0;
  let totalSaidas = 0;
  let saldo = 0;
  const comSaldo = ordenados.map((l) => {
    if (l.tipo === 'ENTRADA') {
      totalEntradas += l.valor;
      saldo += l.valor;
    } else {
      totalSaidas += l.valor;
      saldo -= l.valor;
    }
    return {
      ...l,
      saldoAcumulado: Math.round((saldo + Number.EPSILON) * 100) / 100,
    };
  });

  return {
    anoMes,
    periodoFechado:
      periodoFechadoApi !== undefined
        ? periodoFechadoApi
        : anoMes === '2026-10' && produtorId === DEMO_PRODUTORES[0].id,
    totalEntradas: Math.round(totalEntradas * 100) / 100,
    totalSaidas: Math.round(totalSaidas * 100) / 100,
    saldoPeriodo: Math.round(saldo * 100) / 100,
    lancamentos: comSaldo,
  };
}

export async function carregarCaderno(
  produtorId: string,
  anoMes: string,
  modoOffline: boolean
): Promise<CadernoDigitalResponse> {
  const fila = await listarFilaOffline(produtorId);

  if (IS_DEMO_MODE || modoOffline) {
    const todos = carregarDemoLancamentos().filter((l) => l.produtorId === produtorId && l.anoMes === anoMes);
    return combinarComFilaOffline(produtorId, anoMes, todos, fila);
  }

  try {
    const resp = await fetchAuth<CadernoDigitalResponse>(
      `/lancamentos?anoMes=${encodeURIComponent(anoMes)}&produtorId=${encodeURIComponent(produtorId)}`
    );
    return combinarComFilaOffline(produtorId, anoMes, resp.lancamentos, fila, resp.periodoFechado);
  } catch {
    const todos = carregarDemoLancamentos().filter((l) => l.produtorId === produtorId && l.anoMes === anoMes);
    return combinarComFilaOffline(produtorId, anoMes, todos, fila);
  }
}

export async function sugerirCategoria(
  produtorId: string,
  tipo: TipoLancamento,
  modoOffline: boolean
): Promise<SugestaoCategoriaResponse> {
  if (!IS_DEMO_MODE && !modoOffline) {
    try {
      return await fetchAuth<SugestaoCategoriaResponse>(
        `/lancamentos/sugestao-categoria?tipo=${tipo}&produtorId=${encodeURIComponent(produtorId)}`
      );
    } catch {
      // fallback local
    }
  }

  const todos = carregarDemoLancamentos().filter((l) => l.produtorId === produtorId && l.tipo === tipo);
  const counts = new Map<CategoriaLancamento, number>();
  for (const l of todos) {
    counts.set(l.categoria, (counts.get(l.categoria) || 0) + 1);
  }
  const ranking = Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([categoria, quantidadeUso]) => ({
      categoria,
      rotulo: CATEGORIAS_META[categoria].rotulo,
      quantidadeUso,
    }));

  const sugerida: CategoriaLancamento =
    ranking.length > 0 ? ranking[0].categoria : tipo === 'ENTRADA' ? 'VENDAS' : 'SEMENTES';

  return {
    tipo,
    categoriaSugerida: sugerida,
    rotuloSugerido: CATEGORIAS_META[sugerida].rotulo,
    rankingHistorico: ranking,
  };
}

export async function registrarLancamento(
  produtorId: string,
  culturas: Cultura[],
  dados: {
    tipo: TipoLancamento;
    categoria: CategoriaLancamento;
    valor: number;
    culturaId: string | null;
    quantidade: number | null;
    descricao: string;
    dataLancamento: string;
  },
  modoOffline: boolean
): Promise<{ offline: boolean; chaveIdempotencia: string }> {
  const meta = CATEGORIAS_META[dados.categoria];
  const classificacao = dados.tipo === 'ENTRADA' ? 'NAO_APLICAVEL' : meta.classificacaoPadrao;
  const anoMes = dados.dataLancamento.slice(0, 7);
  const chaveIdempotencia = `lan-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

  const rateiosInput: Array<{ culturaId: string; valorRateado: number }> = [];
  if (dados.tipo === 'SAIDA') {
    if (dados.culturaId) {
      rateiosInput.push({ culturaId: dados.culturaId, valorRateado: dados.valor });
    } else if (culturas.length > 0) {
      const base = Math.floor((dados.valor / culturas.length) * 100) / 100;
      let acum = 0;
      culturas.forEach((c, idx) => {
        const val = idx === culturas.length - 1 ? Math.round((dados.valor - acum) * 100) / 100 : base;
        acum += val;
        rateiosInput.push({ culturaId: c.id, valorRateado: val });
      });
    }
  }

  const salvarLocalOffline = async () => {
    const culturaNome =
      dados.culturaId ? culturas.find((c) => c.id === dados.culturaId)?.nome || 'Cultura' : 'Geral da Horta';
    const item: OfflineQueueItem = {
      chaveIdempotencia,
      produtorId,
      culturaId: dados.culturaId,
      culturaNome,
      tipo: dados.tipo,
      classificacaoCusto: classificacao,
      categoria: dados.categoria,
      categoriaRotulo: meta.rotulo,
      descricao: dados.descricao || meta.rotulo,
      valor: dados.valor,
      quantidade: dados.quantidade,
      dataLancamento: dados.dataLancamento,
      anoMes,
      versaoCliente: 1,
      clienteAtualizadoEm: new Date().toISOString(),
      status: 'PENDENTE',
      rateios: rateiosInput,
    };
    await enfileirarOffline(item);
    return { offline: true, chaveIdempotencia };
  };

  if (modoOffline || !navigator.onLine) {
    return salvarLocalOffline();
  }

  if (IS_DEMO_MODE) {
    const todos = carregarDemoLancamentos();
    const culturaNome =
      dados.culturaId ? culturas.find((c) => c.id === dados.culturaId)?.nome || 'Cultura' : 'Geral da Horta';
    const novo: Lancamento = {
      id: chaveIdempotencia,
      chaveIdempotencia,
      produtorId,
      culturaId: dados.culturaId,
      culturaNome,
      tipo: dados.tipo,
      classificacaoCusto: classificacao,
      categoria: dados.categoria,
      categoriaRotulo: meta.rotulo,
      descricao: dados.descricao || meta.rotulo,
      valor: dados.valor,
      quantidade: dados.quantidade,
      dataLancamento: dados.dataLancamento,
      anoMes,
      versao: 1,
      saldoAcumulado: null,
      rateios: rateiosInput.map((r) => ({
        culturaId: r.culturaId,
        culturaNome: culturas.find((c) => c.id === r.culturaId)?.nome || 'Cultura',
        valorRateado: r.valorRateado,
        percentual: Math.round((r.valorRateado / dados.valor) * 10000) / 100,
      })),
      atualizadoEm: new Date().toISOString(),
      syncStatus: 'SINCRONIZADO',
    };
    todos.push(novo);
    salvarDemoLancamentos(todos);
    return { offline: false, chaveIdempotencia };
  }

  try {
    await fetchAuth('/lancamentos', {
      method: 'POST',
      body: JSON.stringify({
        chaveIdempotencia,
        culturaId: dados.culturaId,
        tipo: dados.tipo,
        classificacaoCusto: classificacao,
        categoria: dados.categoria,
        descricao: dados.descricao || meta.rotulo,
        valor: dados.valor,
        quantidade: dados.quantidade,
        dataLancamento: dados.dataLancamento,
        rateios: rateiosInput.length > 0 ? rateiosInput : undefined,
      }),
    });
    return { offline: false, chaveIdempotencia };
  } catch {
    return salvarLocalOffline();
  }
}

export async function atualizarRateiosLancamento(
  produtorId: string,
  lancamentoId: string,
  culturas: Cultura[],
  rateios: Array<{ culturaId: string; valorRateado: number }>
): Promise<void> {
  if (IS_DEMO_MODE) {
    const todos = carregarDemoLancamentos();
    const alvo = todos.find((l) => l.id === lancamentoId && l.produtorId === produtorId);
    if (!alvo) throw new Error('Custo não encontrado.');
    const soma = Math.round(rateios.reduce((acc, r) => acc + r.valorRateado, 0) * 100) / 100;
    if (Math.abs(soma - alvo.valor) > 0.001) {
      throw new Error('A soma dos rateios deve ser igual ao custo total.');
    }
    alvo.rateios = rateios.map((r) => ({
      culturaId: r.culturaId,
      culturaNome: culturas.find((c) => c.id === r.culturaId)?.nome || 'Cultura',
      valorRateado: r.valorRateado,
      percentual: Math.round((r.valorRateado / alvo.valor) * 10000) / 100,
    }));
    alvo.versao += 1;
    salvarDemoLancamentos(todos);
    return;
  }

  await fetchAuth(`/lancamentos/${lancamentoId}/rateios`, {
    method: 'POST',
    body: JSON.stringify({ rateios }),
  });
}

export async function carregarIndicadores(
  produtorId: string,
  anoMes: string,
  culturas: Cultura[],
  modoOffline: boolean
): Promise<IndicadoresPeriodo> {
  const fila = await listarFilaOffline(produtorId);

  if (!IS_DEMO_MODE && !modoOffline && fila.length === 0) {
    try {
      return await fetchAuth<IndicadoresPeriodo>(
        `/indicadores?anoMes=${encodeURIComponent(anoMes)}&produtorId=${encodeURIComponent(produtorId)}`
      );
    } catch {
      // fallback local
    }
  }

  const caderno = await carregarCaderno(produtorId, anoMes, true);
  const producoes = (DEMO_PRODUCOES[produtorId] && DEMO_PRODUCOES[produtorId][anoMes]) || {};
  return calcularIndicadoresLocal(anoMes, caderno.periodoFechado, culturas, caderno.lancamentos, producoes);
}

export async function carregarSerieHistorica(
  produtorId: string,
  culturas: Cultura[],
  modoOffline: boolean
): Promise<SerieHistoricaItem[]> {
  if (!IS_DEMO_MODE && !modoOffline) {
    try {
      return await fetchAuth<SerieHistoricaItem[]>(
        `/indicadores/serie-historica?produtorId=${encodeURIComponent(produtorId)}`
      );
    } catch {
      // fallback local
    }
  }

  const todos = carregarDemoLancamentos().filter((l) => l.produtorId === produtorId);
  const mesesFechados = new Set<string>(
    produtorId === DEMO_PRODUTORES[0].id ? ['2026-10'] : []
  );
  return calcularSerieHistoricaLocal(
    mesesFechados,
    culturas,
    todos,
    DEMO_PRODUCOES[produtorId] || {}
  );
}

export async function sincronizarFila(
  produtorId: string,
  culturas: Cultura[],
  forcarConflitoDemo = false
): Promise<{
  sincronizados: number;
  conflitos: number;
  erros: number;
}> {
  const fila = await listarFilaOffline(produtorId);
  const pendentes = fila.filter((f) => f.status === 'PENDENTE' || f.status === 'CONFLITO');
  if (pendentes.length === 0 && !forcarConflitoDemo) {
    return { sincronizados: 0, conflitos: 0, erros: 0 };
  }

  for (const item of pendentes) {
    await atualizarStatusNaFila(item.chaveIdempotencia, 'SINCRONIZANDO');
  }

  if (IS_DEMO_MODE) {
    const todos = carregarDemoLancamentos();
    const conflitos = carregarDemoConflitos();
    let syncCount = 0;
    let conflitoCount = 0;

    if (forcarConflitoDemo && pendentes.length === 0) {
      const alvo = todos.find((l) => l.produtorId === produtorId && l.anoMes === '2026-11');
      if (alvo) {
        const valorAnterior = alvo.valor;
        const novoValor = Math.round((valorAnterior + 15) * 100) / 100;
        alvo.versao += 1;
        alvo.valor = novoValor;
        alvo.syncStatus = 'CONFLITO';
        salvarDemoLancamentos(todos);
        conflitos.unshift({
          id: `conf-${Date.now()}`,
          chaveIdempotencia: alvo.chaveIdempotencia,
          lancamentoId: alvo.id,
          versaoServidor: alvo.versao - 1,
          versaoCliente: 1,
          regraAplicada: 'ULTIMA_ESCRITA_VENCE',
          payloadAnterior: JSON.stringify({ valor: valorAnterior, versao: alvo.versao - 1 }),
          payloadNovo: JSON.stringify({ valor: novoValor, versao: alvo.versao }),
          resolvidoEm: new Date().toISOString(),
        });
        salvarDemoConflitos(conflitos);
        return { sincronizados: 0, conflitos: 1, erros: 0 };
      }
    }

    for (const item of pendentes) {
      const existenteIdx = todos.findIndex((l) => l.chaveIdempotencia === item.chaveIdempotencia);
      const novoObj: Lancamento = {
        id: item.chaveIdempotencia,
        chaveIdempotencia: item.chaveIdempotencia,
        produtorId,
        culturaId: item.culturaId,
        culturaNome: item.culturaNome,
        tipo: item.tipo,
        classificacaoCusto: item.classificacaoCusto,
        categoria: item.categoria,
        categoriaRotulo: item.categoriaRotulo,
        descricao: item.descricao,
        valor: item.valor,
        quantidade: item.quantidade,
        dataLancamento: item.dataLancamento,
        anoMes: item.anoMes,
        versao: item.versaoCliente,
        saldoAcumulado: null,
        rateios: item.rateios.map((r) => ({
          culturaId: r.culturaId,
          culturaNome: culturas.find((c) => c.id === r.culturaId)?.nome || 'Cultura',
          valorRateado: r.valorRateado,
          percentual: Math.round((r.valorRateado / item.valor) * 10000) / 100,
        })),
        atualizadoEm: new Date().toISOString(),
        syncStatus: 'SINCRONIZADO',
      };

      if (existenteIdx >= 0) {
        const ant = todos[existenteIdx];
        novoObj.versao = ant.versao + 1;
        novoObj.syncStatus = 'CONFLITO';
        todos[existenteIdx] = novoObj;
        conflitos.unshift({
          id: `conf-${Date.now()}`,
          chaveIdempotencia: item.chaveIdempotencia,
          lancamentoId: ant.id,
          versaoServidor: ant.versao,
          versaoCliente: item.versaoCliente,
          regraAplicada: 'ULTIMA_ESCRITA_VENCE',
          payloadAnterior: JSON.stringify({ valor: ant.valor, versao: ant.versao }),
          payloadNovo: JSON.stringify({ valor: item.valor, versao: novoObj.versao }),
          resolvidoEm: new Date().toISOString(),
        });
        conflitoCount++;
      } else {
        todos.push(novoObj);
        syncCount++;
      }
      await removerDaFila(item.chaveIdempotencia);
    }

    salvarDemoLancamentos(todos);
    salvarDemoConflitos(conflitos);
    return { sincronizados: syncCount, conflitos: conflitoCount, erros: 0 };
  }

  const lotePayload = {
    itens: pendentes.map((p) => ({
      chaveIdempotencia: p.chaveIdempotencia,
      culturaId: p.culturaId,
      tipo: p.tipo,
      classificacaoCusto: p.classificacaoCusto,
      categoria: p.categoria,
      descricao: p.descricao,
      valor: p.valor,
      quantidade: p.quantidade,
      dataLancamento: p.dataLancamento,
      versaoCliente: p.versaoCliente,
      clienteAtualizadoEm: p.clienteAtualizadoEm,
      rateios: p.rateios.length > 0 ? p.rateios : undefined,
    })),
  };

  const resp = await fetchAuth<SyncLoteResponse>('/sync/lotes', {
    method: 'POST',
    body: JSON.stringify(lotePayload),
  });

  for (const res of resp.resultados) {
    if (res.status === 'SINCRONIZADO' || res.status === 'IDEMPOTENTE' || res.status === 'CONFLITO') {
      await removerDaFila(res.chaveIdempotencia);
    } else {
      await atualizarStatusNaFila(res.chaveIdempotencia, 'PENDENTE');
    }
  }

  return {
    sincronizados: resp.sincronizados + resp.idempotentes,
    conflitos: resp.conflitos,
    erros: resp.erros,
  };
}

export async function listarConflitos(produtorId: string): Promise<ConflitoSync[]> {
  if (IS_DEMO_MODE) {
    return carregarDemoConflitos();
  }
  try {
    return await fetchAuth<ConflitoSync[]>(`/sync/conflitos?produtorId=${encodeURIComponent(produtorId)}`);
  } catch {
    return carregarDemoConflitos();
  }
}
