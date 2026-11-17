import {
  CategoriaLancamento,
  ClassificacaoCusto,
  Cultura,
  IndicadoresPeriodo,
  Lancamento,
  SerieHistoricaItem,
  StatusSemaforo,
  TipoLancamento,
} from '../types';

export const CATEGORIAS_META: Record<
  CategoriaLancamento,
  { rotulo: string; tipoPadrao: TipoLancamento; classificacaoPadrao: ClassificacaoCusto }
> = {
  VENDAS: { rotulo: 'Vendas', tipoPadrao: 'ENTRADA', classificacaoPadrao: 'NAO_APLICAVEL' },
  SEMENTES: { rotulo: 'Sementes', tipoPadrao: 'SAIDA', classificacaoPadrao: 'VARIAVEL' },
  ADUBO: { rotulo: 'Adubo', tipoPadrao: 'SAIDA', classificacaoPadrao: 'VARIAVEL' },
  FRETE: { rotulo: 'Frete', tipoPadrao: 'SAIDA', classificacaoPadrao: 'VARIAVEL' },
  AGUA: { rotulo: 'Água', tipoPadrao: 'SAIDA', classificacaoPadrao: 'FIXO' },
  ENERGIA: { rotulo: 'Energia', tipoPadrao: 'SAIDA', classificacaoPadrao: 'FIXO' },
  MAO_DE_OBRA: { rotulo: 'Mão de Obra', tipoPadrao: 'SAIDA', classificacaoPadrao: 'FIXO' },
  OUTROS: { rotulo: 'Outros', tipoPadrao: 'SAIDA', classificacaoPadrao: 'VARIAVEL' },
};

const round2 = (n: number): number => Math.round((n + Number.EPSILON) * 100) / 100;

export function calcularSemaforo(
  resultado: number,
  receitaTotal: number,
  pontoEquilibrio: number | null
): StatusSemaforo {
  if (resultado <= 0) {
    return 'VERMELHO';
  }
  if (pontoEquilibrio !== null && receitaTotal >= round2(pontoEquilibrio * 1.1)) {
    return 'VERDE';
  }
  return 'AMARELO';
}

export function calcularIndicadoresLocal(
  anoMes: string,
  periodoFechado: boolean,
  culturas: Cultura[],
  lancamentos: Lancamento[],
  producoesMap: Record<string, number>
): IndicadoresPeriodo {
  let receitaTotal = 0;
  let custosFixos = 0;
  let custosVariaveis = 0;

  const porCulturaMap: Record<
    string,
    {
      cultura: Cultura;
      receita: number;
      cv: number;
      cf: number;
      qtdVendida: number;
    }
  > = {};

  for (const c of culturas) {
    porCulturaMap[c.id] = { cultura: c, receita: 0, cv: 0, cf: 0, qtdVendida: 0 };
  }

  for (const l of lancamentos) {
    if (l.anoMes !== anoMes) continue;
    if (l.tipo === 'ENTRADA') {
      receitaTotal += l.valor;
      if (l.culturaId && porCulturaMap[l.culturaId]) {
        porCulturaMap[l.culturaId].receita += l.valor;
        if (l.quantidade) {
          porCulturaMap[l.culturaId].qtdVendida += l.quantidade;
        }
      }
    } else {
      const isFixo = l.classificacaoCusto === 'FIXO';
      if (isFixo) {
        custosFixos += l.valor;
      } else {
        custosVariaveis += l.valor;
      }

      if (l.rateios && l.rateios.length > 0) {
        for (const r of l.rateios) {
          const acc = porCulturaMap[r.culturaId];
          if (acc) {
            if (isFixo) acc.cf += r.valorRateado;
            else acc.cv += r.valorRateado;
          }
        }
      } else if (l.culturaId && porCulturaMap[l.culturaId]) {
        if (isFixo) porCulturaMap[l.culturaId].cf += l.valor;
        else porCulturaMap[l.culturaId].cv += l.valor;
      }
    }
  }

  receitaTotal = round2(receitaTotal);
  custosFixos = round2(custosFixos);
  custosVariaveis = round2(custosVariaveis);
  const custosTotais = round2(custosFixos + custosVariaveis);
  const resultado = round2(receitaTotal - custosTotais);
  const margemContribuicao = round2(receitaTotal - custosVariaveis);

  const margemNulaOuNegativa = receitaTotal <= 0 || margemContribuicao <= 0;
  const avisoMargem = margemNulaOuNegativa
    ? 'Margem de contribuição nula ou negativa: Ponto de Equilíbrio não aplicável.'
    : null;

  let pontoEquilibrio: number | null = null;
  if (!margemNulaOuNegativa) {
    if (custosFixos === 0) {
      pontoEquilibrio = 0;
    } else {
      const indiceMargem = margemContribuicao / receitaTotal;
      pontoEquilibrio = round2(custosFixos / indiceMargem);
    }
  }

  const statusSemaforo = calcularSemaforo(resultado, receitaTotal, pontoEquilibrio);

  const indicadoresPorCultura = culturas.map((c) => {
    const acc = porCulturaMap[c.id];
    const prod = producoesMap[c.id] || acc.qtdVendida || 0;
    const cv = round2(acc.cv);
    const cf = round2(acc.cf);
    const custoTotal = round2(cv + cf);
    const receita = round2(acc.receita);
    return {
      culturaId: c.id,
      culturaNome: c.nome,
      unidade: c.unidade,
      quantidadeProduzida: round2(prod),
      receita,
      custosVariaveis: cv,
      custosFixosRateados: cf,
      custoTotal,
      custoUnitario: prod > 0 ? round2(custoTotal / prod) : null,
      margemContribuicao: round2(receita - cv),
      resultado: round2(receita - custoTotal),
    };
  });

  return {
    anoMes,
    periodoFechado,
    receitaTotal,
    custosFixos,
    custosVariaveis,
    custosTotais,
    margemContribuicao,
    pontoEquilibrio,
    resultado,
    margemNulaOuNegativa,
    avisoMargem,
    statusSemaforo,
    indicadoresPorCultura,
  };
}

export function calcularSerieHistoricaLocal(
  mesesFechados: Set<string>,
  culturas: Cultura[],
  lancamentos: Lancamento[],
  producoesPorMes: Record<string, Record<string, number>>
): SerieHistoricaItem[] {
  const meses = Array.from(new Set(lancamentos.map((l) => l.anoMes))).sort();
  return meses.map((mes) => {
    const ind = calcularIndicadoresLocal(
      mes,
      mesesFechados.has(mes),
      culturas,
      lancamentos,
      producoesPorMes[mes] || {}
    );
    return {
      anoMes: ind.anoMes,
      periodoFechado: ind.periodoFechado,
      receitaTotal: ind.receitaTotal,
      custosFixos: ind.custosFixos,
      custosVariaveis: ind.custosVariaveis,
      custosTotais: ind.custosTotais,
      margemContribuicao: ind.margemContribuicao,
      pontoEquilibrio: ind.pontoEquilibrio,
      resultado: ind.resultado,
      margemNulaOuNegativa: ind.margemNulaOuNegativa,
      statusSemaforo: ind.statusSemaforo,
    };
  });
}

export function formatarMoeda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—';
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function formatarAnoMesLabel(anoMes: string): string {
  const [ano, mes] = anoMes.split('-');
  const nomes: Record<string, string> = {
    '01': 'Janeiro',
    '02': 'Fevereiro',
    '03': 'Março',
    '04': 'Abril',
    '05': 'Maio',
    '06': 'Junho',
    '07': 'Julho',
    '08': 'Agosto',
    '09': 'Setembro',
    '10': 'Outubro',
    '11': 'Novembro',
    '12': 'Dezembro',
  };
  return `${nomes[mes] || mes}/${ano}`;
}
