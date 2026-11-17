import React, { useEffect, useState } from 'react';
import {
  ArrowCircleDown,
  ArrowCircleUp,
  BookOpen,
  CalendarBlank,
  CheckCircle,
  ClockCountdown,
  Lock,
  WarningCircle,
} from '@phosphor-icons/react';
import { CadernoDigitalResponse } from '../types';
import { carregarCaderno } from '../services/api';
import { formatarAnoMesLabel, formatarMoeda } from '../services/calculadoraIndicadores';
import { ContextualHelpButton } from './ContextualHelpButton';

interface CadernoDigitalViewProps {
  produtorId: string;
  anoMes: string;
  mesesDisponiveis: string[];
  onMudarMes: (mes: string) => void;
  modoOffline: boolean;
  refreshTick: number;
}

export const CadernoDigitalView: React.FC<CadernoDigitalViewProps> = ({
  produtorId,
  anoMes,
  mesesDisponiveis,
  onMudarMes,
  modoOffline,
  refreshTick,
}) => {
  const [caderno, setCaderno] = useState<CadernoDigitalResponse | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    carregarCaderno(produtorId, anoMes, modoOffline)
      .then((res) => {
        if (ativo) setCaderno(res);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [produtorId, anoMes, modoOffline, refreshTick]);

  const formatarData = (iso: string) => {
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div className="screen-header-card">
        <div>
          <h2 className="screen-title">
            <BookOpen size={24} weight="bold" color="#2C5745" />
            Caderno Digital
          </h2>
          <p className="screen-subtitle">Extrato do mês com saldo acumulado</p>
        </div>
        <ContextualHelpButton
          titulo="Ajuda: Caderno Digital"
          texto="Aqui ficam todas as entradas e saídas do mês selecionado, em ordem de data, mostrando quanto ficou no caixa após cada lançamento."
          dicas={[
            'Use os botões de mês para consultar períodos anteriores.',
            'Lançamentos feitos sem rede aparecem com a marcação "Pendente".',
          ]}
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

        {caderno?.periodoFechado && (
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: '#EBE3A7',
              color: '#2E2910',
              padding: '6px 10px',
              borderRadius: '8px',
              fontSize: '0.82rem',
              fontWeight: 800,
            }}
          >
            <Lock size={16} weight="bold" />
            Período fechado (resultado congelado)
          </div>
        )}
      </div>

      {/* Resumo do periodo */}
      {caderno && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
          <div className="card-agro" style={{ padding: '10px' }}>
            <div style={{ fontSize: '0.72rem', fontWeight: 800, color: '#1E5631' }}>ENTRADAS</div>
            <div style={{ fontSize: '0.96rem', fontWeight: 800, color: '#1E5631', marginTop: '4px' }}>
              {formatarMoeda(caderno.totalEntradas)}
            </div>
          </div>
          <div className="card-agro" style={{ padding: '10px' }}>
            <div style={{ fontSize: '0.72rem', fontWeight: 800, color: '#9E2A2B' }}>SAÍDAS</div>
            <div style={{ fontSize: '0.96rem', fontWeight: 800, color: '#9E2A2B', marginTop: '4px' }}>
              {formatarMoeda(caderno.totalSaidas)}
            </div>
          </div>
          <div className="card-agro" style={{ padding: '10px', backgroundColor: '#EBE3A7' }}>
            <div style={{ fontSize: '0.72rem', fontWeight: 800, color: '#2E2910' }}>SALDO MÊS</div>
            <div style={{ fontSize: '0.96rem', fontWeight: 800, color: '#2E2910', marginTop: '4px' }}>
              {formatarMoeda(caderno.saldoPeriodo)}
            </div>
          </div>
        </div>
      )}

      {/* Lista de lancamentos */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {carregando ? (
          <div className="card-agro" style={{ textAlign: 'center', fontWeight: 700 }}>
            Carregando caderno...
          </div>
        ) : !caderno || caderno.lancamentos.length === 0 ? (
          <div className="card-agro" style={{ textAlign: 'center', fontWeight: 700 }}>
            Nenhum lançamento registrado neste mês.
          </div>
        ) : (
          caderno.lancamentos.map((item) => {
            const isEntrada = item.tipo === 'ENTRADA';
            const status = item.syncStatus || 'SINCRONIZADO';
            return (
              <div
                key={item.chaveIdempotencia}
                className="card-agro"
                style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {isEntrada ? (
                      <ArrowCircleUp size={28} weight="fill" color="#1E5631" />
                    ) : (
                      <ArrowCircleDown size={28} weight="fill" color="#9E2A2B" />
                    )}
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '0.98rem', color: '#2E2910' }}>
                        {item.categoriaRotulo}
                      </div>
                      <div style={{ fontSize: '0.82rem', color: '#3C402B', fontWeight: 600 }}>
                        {item.descricao} • {item.culturaNome}
                      </div>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <div
                      style={{
                        fontWeight: 800,
                        fontSize: '1.02rem',
                        color: isEntrada ? '#1E5631' : '#9E2A2B',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {isEntrada ? '+' : '-'} {formatarMoeda(item.valor)}
                    </div>
                    <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#3C402B' }}>
                      {formatarData(item.dataLancamento)}
                    </div>
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    paddingTop: '6px',
                    borderTop: '1px solid #EFEAE3',
                    fontSize: '0.82rem',
                  }}
                >
                  <span className={`badge-sync badge-sync--${status}`}>
                    {status === 'PENDENTE' && <ClockCountdown size={13} weight="bold" />}
                    {status === 'SINCRONIZADO' && <CheckCircle size={13} weight="bold" />}
                    {status === 'CONFLITO' && <WarningCircle size={13} weight="bold" />}
                    {status === 'SINCRONIZADO'
                      ? 'Sincronizado'
                      : status === 'PENDENTE'
                      ? 'Pendente'
                      : status === 'SINCRONIZANDO'
                      ? 'Sincronizando'
                      : 'Conflito'}
                  </span>
                  <span style={{ fontWeight: 800, color: '#3C402B' }}>
                    Saldo acumulado: {formatarMoeda(item.saldoAcumulado)}
                  </span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
