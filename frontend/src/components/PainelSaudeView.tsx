import React, { useEffect, useState } from 'react';
import { CalendarBlank, Heartbeat, WarningCircle } from '@phosphor-icons/react';
import { Cultura, IndicadoresPeriodo, SerieHistoricaItem } from '../types';
import { carregarIndicadores, carregarSerieHistorica } from '../services/api';
import { formatarAnoMesLabel, formatarMoeda } from '../services/calculadoraIndicadores';
import { ContextualHelpButton } from './ContextualHelpButton';

interface PainelSaudeViewProps {
  produtorId: string;
  culturas: Cultura[];
  anoMes: string;
  mesesDisponiveis: string[];
  onMudarMes: (mes: string) => void;
  modoOffline: boolean;
  refreshTick: number;
}

export const PainelSaudeView: React.FC<PainelSaudeViewProps> = ({
  produtorId,
  culturas,
  anoMes,
  mesesDisponiveis,
  onMudarMes,
  modoOffline,
  refreshTick,
}) => {
  const [indicadores, setIndicadores] = useState<IndicadoresPeriodo | null>(null);
  const [serie, setSerie] = useState<SerieHistoricaItem[]>([]);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    Promise.all([
      carregarIndicadores(produtorId, anoMes, culturas, modoOffline),
      carregarSerieHistorica(produtorId, culturas, modoOffline),
    ])
      .then(([ind, ser]) => {
        if (!ativo) return;
        setIndicadores(ind);
        setSerie(ser);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, [produtorId, anoMes, culturas, modoOffline, refreshTick]);

  const criterioSemaforo = (ind: IndicadoresPeriodo) => {
    if (ind.statusSemaforo === 'VERDE') {
      return 'Resultado > 0 e Receita ≥ +10% do Ponto de Equilíbrio';
    }
    if (ind.statusSemaforo === 'AMARELO') {
      return 'Resultado > 0 e Receita < +10% do Ponto de Equilíbrio';
    }
    return 'Resultado ≤ 0';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div className="screen-header-card">
        <div>
          <h2 className="screen-title">
            <Heartbeat size={24} weight="bold" color="#2C5745" />
            Saúde Financeira
          </h2>
          <p className="screen-subtitle">Semáforo, indicadores e tabela histórica</p>
        </div>
        <ContextualHelpButton
          titulo="Ajuda: Saúde Financeira"
          texto="Consulte nesta tela o Semáforo do período, os valores calculados pelo motor de indicadores, o custo unitário por cultura e a tabela histórica mensal."
        />
      </div>

      {/* Seletor de mes */}
      <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 800, fontSize: '0.88rem' }}>
          <CalendarBlank size={20} weight="bold" color="#2C5745" />
          Período:
        </div>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {mesesDisponiveis.map((mes) => (
            <button
              key={mes}
              type="button"
              onClick={() => onMudarMes(mes)}
              className="touch-target"
              style={{
                flex: 1,
                minWidth: '130px',
                padding: '10px 12px',
                borderRadius: '12px',
                border: mes === anoMes ? '2px solid #2C5745' : '2px solid #D5CEBE',
                backgroundColor: mes === anoMes ? '#2C5745' : '#EFEAE3',
                color: mes === anoMes ? '#FFFFFF' : '#2E2910',
                fontWeight: 800,
                cursor: 'pointer',
              }}
            >
              {formatarAnoMesLabel(mes)}
            </button>
          ))}
        </div>
      </div>

      {carregando || !indicadores ? (
        <div className="card-agro" style={{ textAlign: 'center', fontWeight: 700 }}>
          Calculando indicadores...
        </div>
      ) : (
        <>
          {/* Semaforo */}
          <div className={`semaforo-box semaforo-box--${indicadores.statusSemaforo}`}>
            <div className="semaforo-luzes" aria-hidden="true">
              <span
                className={`semaforo-luz ${indicadores.statusSemaforo === 'VERMELHO' ? 'ativa' : ''}`}
                style={{ backgroundColor: '#E53935' }}
              />
              <span
                className={`semaforo-luz ${indicadores.statusSemaforo === 'AMARELO' ? 'ativa' : ''}`}
                style={{ backgroundColor: '#FBC02D' }}
              />
              <span
                className={`semaforo-luz ${indicadores.statusSemaforo === 'VERDE' ? 'ativa' : ''}`}
                style={{ backgroundColor: '#43A047' }}
              />
            </div>
            <div>
              <div style={{ fontSize: '0.78rem', fontWeight: 800, textTransform: 'uppercase' }}>
                Semáforo Financeiro
              </div>
              <div style={{ fontSize: '1.3rem', fontWeight: 800 }}>
                {indicadores.statusSemaforo}
              </div>
              <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>
                {criterioSemaforo(indicadores)}
              </div>
            </div>
          </div>

          {/* Aviso de margem nula ou negativa */}
          {indicadores.margemNulaOuNegativa && indicadores.avisoMargem && (
            <div
              role="alert"
              style={{
                backgroundColor: '#FFF7DA',
                border: '2px solid #8A6500',
                color: '#8A6500',
                borderRadius: '12px',
                padding: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontWeight: 800,
                fontSize: '0.88rem',
              }}
            >
              <WarningCircle size={22} weight="bold" />
              <span>{indicadores.avisoMargem}</span>
            </div>
          )}

          {/* Indicadores tecnicos (apenas nomes tecnicos) */}
          <div className="indicadores-grid">
            <div className="indicador-card">
              <div className="indicador-label">Receita Total</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.receitaTotal)}</div>
            </div>
            <div className="indicador-card">
              <div className="indicador-label">Custos Totais</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.custosTotais)}</div>
            </div>
            <div className="indicador-card">
              <div className="indicador-label">Custos Fixos</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.custosFixos)}</div>
            </div>
            <div className="indicador-card">
              <div className="indicador-label">Custos Variáveis</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.custosVariaveis)}</div>
            </div>
            <div className="indicador-card">
              <div className="indicador-label">Margem de Contribuição</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.margemContribuicao)}</div>
            </div>
            <div className="indicador-card">
              <div className="indicador-label">Ponto de Equilíbrio (R$)</div>
              <div className="indicador-valor">
                {indicadores.margemNulaOuNegativa
                  ? 'N/A'
                  : formatarMoeda(indicadores.pontoEquilibrio)}
              </div>
            </div>
            <div className="indicador-card" style={{ gridColumn: 'span 2', backgroundColor: '#EBE3A7' }}>
              <div className="indicador-label">Resultado</div>
              <div className="indicador-valor">{formatarMoeda(indicadores.resultado)}</div>
            </div>
          </div>

          {/* Indicadores por Cultura */}
          <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#3C402B' }}>
              Indicadores por Cultura
            </h3>
            <div className="tabela-scroll">
              <table className="tabela-tecnica">
                <thead>
                  <tr>
                    <th>Cultura</th>
                    <th>Qtd. Produzida</th>
                    <th>Custo Total</th>
                    <th>Custo Unitário</th>
                    <th>Receita</th>
                    <th>Margem de Contribuição</th>
                    <th>Ponto de Equilíbrio (R$)</th>
                    <th>Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  {indicadores.indicadoresPorCultura.map((c) => (
                    <tr key={c.culturaId}>
                      <td>{c.culturaNome}</td>
                      <td>
                        {c.quantidadeProduzida} {c.unidade}
                      </td>
                      <td>{formatarMoeda(c.custoTotal)}</td>
                      <td>
                        {c.custoUnitario !== null
                          ? `${formatarMoeda(c.custoUnitario)}/${c.unidade}`
                          : '—'}
                      </td>
                      <td>{formatarMoeda(c.receita)}</td>
                      <td>{formatarMoeda(c.margemContribuicao)}</td>
                      <td>
                        {c.margemNulaOuNegativa
                          ? 'N/A (Margem ≤ 0)'
                          : formatarMoeda(c.pontoEquilibrio)}
                      </td>
                      <td>{formatarMoeda(c.resultado)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Serie Historica Mensal (Tabela, sem graficos) */}
          <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#3C402B' }}>
              Série Histórica Mensal
            </h3>
            <div className="tabela-scroll">
              <table className="tabela-tecnica">
                <thead>
                  <tr>
                    <th>Período</th>
                    <th>Receita</th>
                    <th>Custos Fixos</th>
                    <th>Custos Variáveis</th>
                    <th>Margem de Contribuição</th>
                    <th>Ponto de Equilíbrio (R$)</th>
                    <th>Resultado</th>
                    <th>Semáforo</th>
                  </tr>
                </thead>
                <tbody>
                  {serie.map((item) => (
                    <tr key={item.anoMes}>
                      <td>{formatarAnoMesLabel(item.anoMes)}</td>
                      <td>{formatarMoeda(item.receitaTotal)}</td>
                      <td>{formatarMoeda(item.custosFixos)}</td>
                      <td>{formatarMoeda(item.custosVariaveis)}</td>
                      <td>{formatarMoeda(item.margemContribuicao)}</td>
                      <td>
                        {item.margemNulaOuNegativa ? 'N/A' : formatarMoeda(item.pontoEquilibrio)}
                      </td>
                      <td>{formatarMoeda(item.resultado)}</td>
                      <td>{item.statusSemaforo}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
