import {
  CategoriaLancamento,
  Cultura,
  Lancamento,
  ProdutorHorta,
  RateioItem,
  UsuarioResumo,
} from '../types';

export const HORTA_ID = '11111111-1111-1111-1111-111111111101';
export const HORTA_NOME = 'Horta Comunitária Raízes do Bairro';

export const DEMO_USUARIOS: UsuarioResumo[] = [
  {
    id: '22222222-2222-2222-2222-222222222201',
    nome: 'Seu João da Silva',
    login: 'joao.silva',
    perfil: 'PRODUTOR',
    hortaId: HORTA_ID,
    hortaNome: HORTA_NOME,
    totpHabilitado: false,
  },
  {
    id: '22222222-2222-2222-2222-222222222202',
    nome: 'Dona Maria dos Santos',
    login: 'maria.santos',
    perfil: 'PRODUTOR',
    hortaId: HORTA_ID,
    hortaNome: HORTA_NOME,
    totpHabilitado: false,
  },
  {
    id: '22222222-2222-2222-2222-222222222203',
    nome: 'Carlos Eduardo',
    login: 'carlos.gestor',
    perfil: 'GESTOR',
    hortaId: HORTA_ID,
    hortaNome: HORTA_NOME,
    totpHabilitado: true,
  },
];

export const DEMO_PRODUTORES: ProdutorHorta[] = [
  { id: '22222222-2222-2222-2222-222222222201', nome: 'Seu João da Silva', login: 'joao.silva' },
  { id: '22222222-2222-2222-2222-222222222202', nome: 'Dona Maria dos Santos', login: 'maria.santos' },
];

export const DEMO_CULTURAS: Cultura[] = [
  { id: '33333333-3333-3333-3333-333333333301', nome: 'Alface', icone: 'Leaf', unidade: 'maço' },
  { id: '33333333-3333-3333-3333-333333333302', nome: 'Couve', icone: 'Plant', unidade: 'maço' },
  { id: '33333333-3333-3333-3333-333333333303', nome: 'Tomate', icone: 'Orange', unidade: 'kg' },
  { id: '33333333-3333-3333-3333-333333333304', nome: 'Cenoura', icone: 'Carrot', unidade: 'kg' },
];

export const DEMO_PRODUCOES: Record<string, Record<string, Record<string, number>>> = {
  '22222222-2222-2222-2222-222222222201': {
    '2026-10': {
      '33333333-3333-3333-3333-333333333301': 320,
      '33333333-3333-3333-3333-333333333302': 140,
      '33333333-3333-3333-3333-333333333303': 85,
      '33333333-3333-3333-3333-333333333304': 65,
    },
    '2026-11': {
      '33333333-3333-3333-3333-333333333301': 460,
      '33333333-3333-3333-3333-333333333302': 210,
      '33333333-3333-3333-3333-333333333303': 135,
      '33333333-3333-3333-3333-333333333304': 85,
    },
  },
  '22222222-2222-2222-2222-222222222202': {
    '2026-10': {
      '33333333-3333-3333-3333-333333333301': 225,
      '33333333-3333-3333-3333-333333333302': 110,
      '33333333-3333-3333-3333-333333333303': 55,
      '33333333-3333-3333-3333-333333333304': 38,
    },
    '2026-11': {
      '33333333-3333-3333-3333-333333333301': 360,
      '33333333-3333-3333-3333-333333333302': 160,
      '33333333-3333-3333-3333-333333333303': 95,
      '33333333-3333-3333-3333-333333333304': 70,
    },
  },
};

function makeRateios(total: number, vals: [number, number, number, number]): RateioItem[] {
  return DEMO_CULTURAS.map((c, idx) => ({
    culturaId: c.id,
    culturaNome: c.nome,
    valorRateado: vals[idx],
    percentual: Math.round((vals[idx] / total) * 10000) / 100,
  }));
}

function makeLanc(
  id: string,
  produtorId: string,
  culturaIdx: number | null,
  tipo: 'ENTRADA' | 'SAIDA',
  classificacao: 'NAO_APLICAVEL' | 'FIXO' | 'VARIAVEL',
  categoria: CategoriaLancamento,
  rotulo: string,
  descricao: string,
  valor: number,
  quantidade: number | null,
  data: string,
  rateiosVals?: [number, number, number, number]
): Lancamento {
  const cultura = culturaIdx !== null ? DEMO_CULTURAS[culturaIdx] : null;
  return {
    id,
    chaveIdempotencia: `demo-${id}`,
    produtorId,
    culturaId: cultura ? cultura.id : null,
    culturaNome: cultura ? cultura.nome : 'Geral da Horta',
    tipo,
    classificacaoCusto: classificacao,
    categoria,
    categoriaRotulo: rotulo,
    descricao,
    valor,
    quantidade,
    dataLancamento: data,
    anoMes: data.slice(0, 7),
    versao: 1,
    saldoAcumulado: null,
    rateios: rateiosVals ? makeRateios(valor, rateiosVals) : [],
    atualizadoEm: `${data}T12:00:00`,
    syncStatus: 'SINCRONIZADO',
  };
}

const JOAO_ID = '22222222-2222-2222-2222-222222222201';
const MARIA_ID = '22222222-2222-2222-2222-222222222202';

export function criarLancamentosIniciaisDemo(): Lancamento[] {
  return [
    // Joao 2026-10 (AMARELO)
    makeLanc('j10-1', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'SEMENTES', 'Sementes e Mudas', 'Sementes e mudas da semana', 220, null, '2026-10-02', [55, 40, 75, 50]),
    makeLanc('j10-2', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'ADUBO', 'Adubo e Composto', 'Composto orgânico e húmus', 270, null, '2026-10-04', [65, 50, 95, 60]),
    makeLanc('j10-3', JOAO_ID, 0, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de alface na feira livre', 650, 320, '2026-10-08'),
    makeLanc('j10-4', JOAO_ID, null, 'SAIDA', 'FIXO', 'AGUA', 'Água e Irrigação', 'Conta de água da horta', 210, null, '2026-10-10', [55, 40, 70, 45]),
    makeLanc('j10-5', JOAO_ID, 1, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de couve para quitanda', 420, 140, '2026-10-14'),
    makeLanc('j10-6', JOAO_ID, null, 'SAIDA', 'FIXO', 'ENERGIA', 'Energia Elétrica', 'Energia da bomba de irrigação', 190, null, '2026-10-15', [45, 35, 65, 45]),
    makeLanc('j10-7', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'FRETE', 'Frete e Transporte', 'Frete entrega feira e sacolão', 160, null, '2026-10-18', [40, 30, 55, 35]),
    makeLanc('j10-8', JOAO_ID, 2, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de tomate orgânico', 680, 85, '2026-10-22'),
    makeLanc('j10-9', JOAO_ID, 3, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de cenoura em maços', 400, 65, '2026-10-26'),
    makeLanc('j10-10', JOAO_ID, null, 'SAIDA', 'FIXO', 'MAO_DE_OBRA', 'Mão de Obra', 'Diárias de capina e colheita', 1000, null, '2026-10-28', [250, 190, 350, 210]),

    // Joao 2026-11 (VERDE)
    makeLanc('j11-1', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'SEMENTES', 'Sementes e Mudas', 'Sementes e mudas selecionadas', 290, null, '2026-11-02', [70, 55, 105, 60]),
    makeLanc('j11-2', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'ADUBO', 'Adubo e Composto', 'Adubo orgânico e biofertilizante', 380, null, '2026-11-03', [95, 70, 130, 85]),
    makeLanc('j11-3', JOAO_ID, 0, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de alface crespa na feira', 920, 460, '2026-11-06'),
    makeLanc('j11-4', JOAO_ID, null, 'SAIDA', 'FIXO', 'AGUA', 'Água e Irrigação', 'Rateio conta de água', 200, null, '2026-11-08', [50, 40, 65, 45]),
    makeLanc('j11-5', JOAO_ID, 1, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de couve para escola municipal', 630, 210, '2026-11-10'),
    makeLanc('j11-6', JOAO_ID, null, 'SAIDA', 'FIXO', 'ENERGIA', 'Energia Elétrica', 'Rateio conta de energia', 170, null, '2026-11-11', [40, 35, 55, 40]),
    makeLanc('j11-7', JOAO_ID, null, 'SAIDA', 'VARIAVEL', 'FRETE', 'Frete e Transporte', 'Frete entrega cestas agroecológicas', 250, null, '2026-11-12', [60, 45, 90, 55]),
    makeLanc('j11-8', JOAO_ID, 2, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de tomate italiano', 1080, 135, '2026-11-14'),
    makeLanc('j11-9', JOAO_ID, 3, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de cenoura fresca', 520, 85, '2026-11-15'),
    makeLanc('j11-10', JOAO_ID, null, 'SAIDA', 'FIXO', 'MAO_DE_OBRA', 'Mão de Obra', 'Ajuda de custo mutirão e colheita', 850, null, '2026-11-16', [210, 170, 290, 180]),

    // Maria 2026-10 (VERMELHO)
    makeLanc('m10-1', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'SEMENTES', 'Sementes e Mudas', 'Mudas de hortaliças', 200, null, '2026-10-03', [50, 40, 70, 40]),
    makeLanc('m10-2', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'ADUBO', 'Adubo e Composto', 'Esterco curtido e calcário', 240, null, '2026-10-05', [60, 45, 85, 50]),
    makeLanc('m10-3', MARIA_ID, 0, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de alface no bairro', 450, 225, '2026-10-09'),
    makeLanc('m10-4', MARIA_ID, null, 'SAIDA', 'FIXO', 'AGUA', 'Água e Irrigação', 'Rateio conta de água', 170, null, '2026-10-10', [45, 35, 55, 35]),
    makeLanc('m10-5', MARIA_ID, 1, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de couve na feira', 330, 110, '2026-10-15'),
    makeLanc('m10-6', MARIA_ID, null, 'SAIDA', 'FIXO', 'ENERGIA', 'Energia Elétrica', 'Rateio energia elétrica', 150, null, '2026-10-16', [35, 30, 50, 35]),
    makeLanc('m10-7', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'FRETE', 'Frete e Transporte', 'Transporte das caixas', 140, null, '2026-10-19', [35, 25, 50, 30]),
    makeLanc('m10-8', MARIA_ID, 2, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de tomate cereja', 440, 55, '2026-10-23'),
    makeLanc('m10-9', MARIA_ID, 3, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de cenoura orgânica', 230, 38, '2026-10-27'),
    makeLanc('m10-10', MARIA_ID, null, 'SAIDA', 'FIXO', 'MAO_DE_OBRA', 'Mão de Obra', 'Diárias auxiliares', 800, null, '2026-10-29', [200, 160, 270, 170]),

    // Maria 2026-11 (VERDE)
    makeLanc('m11-1', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'SEMENTES', 'Sementes e Mudas', 'Sementes orgânicas', 230, null, '2026-11-02', [60, 45, 80, 45]),
    makeLanc('m11-2', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'ADUBO', 'Adubo e Composto', 'Compostagem e adubo foliar', 300, null, '2026-11-04', [75, 60, 105, 60]),
    makeLanc('m11-3', MARIA_ID, 0, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de alface americana', 720, 360, '2026-11-07'),
    makeLanc('m11-4', MARIA_ID, null, 'SAIDA', 'FIXO', 'AGUA', 'Água e Irrigação', 'Rateio conta de água', 180, null, '2026-11-09', [45, 35, 60, 40]),
    makeLanc('m11-5', MARIA_ID, 1, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de couve manteiga', 480, 160, '2026-11-11'),
    makeLanc('m11-6', MARIA_ID, null, 'SAIDA', 'FIXO', 'ENERGIA', 'Energia Elétrica', 'Rateio energia elétrica', 160, null, '2026-11-12', [40, 30, 55, 35]),
    makeLanc('m11-7', MARIA_ID, null, 'SAIDA', 'VARIAVEL', 'FRETE', 'Frete e Transporte', 'Frete feira comunitária', 180, null, '2026-11-13', [45, 35, 65, 35]),
    makeLanc('m11-8', MARIA_ID, 2, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de tomate orgânico', 760, 95, '2026-11-14'),
    makeLanc('m11-9', MARIA_ID, 3, 'ENTRADA', 'NAO_APLICAVEL', 'VENDAS', 'Vendas da Horta', 'Venda de cenoura sem agrotóxico', 420, 70, '2026-11-15'),
    makeLanc('m11-10', MARIA_ID, null, 'SAIDA', 'FIXO', 'MAO_DE_OBRA', 'Mão de Obra', 'Diárias de apoio na colheita', 810, null, '2026-11-16', [200, 160, 280, 170]),
  ];
}
