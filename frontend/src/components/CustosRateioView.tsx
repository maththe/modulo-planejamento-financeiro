import React, { useEffect, useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { CalendarBlank, Calculator, CheckCircle, Sliders } from '@phosphor-icons/react';
import { CadernoDigitalResponse, Cultura, Lancamento } from '../types';
import { atualizarRateiosLancamento, carregarCaderno } from '../services/api';
import { formatarAnoMesLabel, formatarMoeda } from '../services/calculadoraIndicadores';
import { ContextualHelpButton } from './ContextualHelpButton';

interface CustosRateioViewProps {
  produtorId: string;
  culturas: Cultura[];
  anoMes: string;
  mesesDisponiveis: string[];
  onMudarMes: (mes: string) => void;
  somenteLeitura: boolean;
  modoOffline: boolean;
  refreshTick: number;
  onRateioAtualizado: () => void;
}

export const CustosRateioView: React.FC<CustosRateioViewProps> = ({
  produtorId,
  culturas,
  anoMes,
  mesesDisponiveis,
  onMudarMes,
  somenteLeitura,
  modoOffline,
  refreshTick,
  onRateioAtualizado,
}) => {
  const [caderno, setCaderno] = useState<CadernoDigitalResponse | null>(null);
  const [custoEmEdicao, setCustoEmEdicao] = useState<Lancamento | null>(null);
  const [valoresRateio, setValoresRateio] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);
  const [erroModal, setErroModal] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    carregarCaderno(produtorId, anoMes, modoOffline).then((res) => {
      if (ativo) setCaderno(res);
    });
    return () => {
      ativo = false;
    };
  }, [produtorId, anoMes, modoOffline, refreshTick]);

  const custos = (caderno?.lancamentos || []).filter((l) => l.tipo === 'SAIDA');
  const custosFixos = custos.filter((l) => l.classificacaoCusto === 'FIXO');
  const custosVariaveis = custos.filter((l) => l.classificacaoCusto !== 'FIXO');

  const totalFixos = custosFixos.reduce((acc, c) => acc + c.valor, 0);
  const totalVariaveis = custosVariaveis.reduce((acc, c) => acc + c.valor, 0);

  const abrirModalRateio = (custo: Lancamento) => {
    const mapa: Record<string, string> = {};
    for (const c of culturas) {
      const encontrado = custo.rateios.find((r) => r.culturaId === c.id);
      mapa[c.id] = encontrado ? encontrado.valorRateado.toFixed(2) : '0.00';
    }
    setValoresRateio(mapa);
    setErroModal(null);
    setCustoEmEdicao(custo);
  };

  const dividirIgual = () => {
    if (!custoEmEdicao || culturas.length === 0) return;
    const total = custoEmEdicao.valor;
    const base = Math.floor((total / culturas.length) * 100) / 100;
    const novo: Record<string, string> = {};
    let acum = 0;
    culturas.forEach((c, idx) => {
      const val = idx === culturas.length - 1 ? Math.round((total - acum) * 100) / 100 : base;
      acum += val;
      novo[c.id] = val.toFixed(2);
    });
    setValoresRateio(novo);
    setErroModal(null);
  };

  const somaRateioAtual = culturas.reduce((acc, c) => {
    const v = parseFloat((valoresRateio[c.id] || '0').replace(',', '.')) || 0;
    return acc + v;
  }, 0);

  const somaArredondada = Math.round(somaRateioAtual * 100) / 100;
  const diferenca = custoEmEdicao ? Math.round((custoEmEdicao.valor - somaArredondada) * 100) / 100 : 0;

  const salvarRateio = async () => {
    if (!custoEmEdicao) return;
    if (Math.abs(diferenca) > 0.001) {
      setErroModal('A soma dos rateios deve ser exatamente igual ao custo total.');
      return;
    }

    setSalvando(true);
    setErroModal(null);
    try {
      const payload = culturas.map((c) => ({
        culturaId: c.id,
        valorRateado: Math.round((parseFloat((valoresRateio[c.id] || '0').replace(',', '.')) || 0) * 100) / 100,
      }));
      await atualizarRateiosLancamento(produtorId, custoEmEdicao.id, culturas, payload);
      setCustoEmEdicao(null);
      onRateioAtualizado();
    } catch (err) {
      setErroModal(err instanceof Error ? err.message : 'Não foi possível salvar o rateio.');
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div className="screen-header-card">
        <div>
          <h2 className="screen-title">
            <Calculator size={24} weight="bold" color="#2C5745" />
            Custos e Rateio
          </h2>
          <p className="screen-subtitle">Custos fixos, variáveis e divisão por cultura</p>
        </div>
        <ContextualHelpButton
          titulo="Ajuda: Custos e Rateio"
          texto="Aqui os gastos aparecem separados em Custos Fixos e Custos Variáveis. Você pode dividir cada custo entre Alface, Couve, Tomate e Cenoura."
          dicas={[
            'A soma dos valores divididos entre as culturas deve bater exatamente com o custo total.',
            'Use "Dividir por igual" para repartir rapidamente em partes iguais.',
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
      </div>

      {/* Totais Fixos vs Variaveis */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
        <div className="card-agro">
          <div style={{ fontSize: '0.75rem', fontWeight: 800, color: '#3C402B' }}>CUSTOS FIXOS</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#2E2910', marginTop: '4px' }}>
            {formatarMoeda(totalFixos)}
          </div>
          <div style={{ fontSize: '0.76rem', color: '#3C402B', marginTop: '2px' }}>
            Água, Energia, Mão de Obra
          </div>
        </div>
        <div className="card-agro">
          <div style={{ fontSize: '0.75rem', fontWeight: 800, color: '#3C402B' }}>CUSTOS VARIÁVEIS</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#2E2910', marginTop: '4px' }}>
            {formatarMoeda(totalVariaveis)}
          </div>
          <div style={{ fontSize: '0.76rem', color: '#3C402B', marginTop: '2px' }}>
            Sementes, Adubo, Frete
          </div>
        </div>
      </div>

      {/* Lista de custos com rateios */}
      {custos.length === 0 ? (
        <div className="card-agro" style={{ textAlign: 'center', fontWeight: 700 }}>
          Nenhum custo registrado neste mês.
        </div>
      ) : (
        custos.map((custo) => (
          <div key={custo.chaveIdempotencia} className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
              <div>
                <span
                  style={{
                    display: 'inline-block',
                    padding: '2px 8px',
                    borderRadius: '999px',
                    fontSize: '0.72rem',
                    fontWeight: 800,
                    backgroundColor: custo.classificacaoCusto === 'FIXO' ? '#EBE3A7' : '#EFEAE3',
                    color: '#2E2910',
                    marginBottom: '4px',
                  }}
                >
                  {custo.classificacaoCusto === 'FIXO' ? 'Custo Fixo' : 'Custo Variável'}
                </span>
                <div style={{ fontWeight: 800, fontSize: '1rem' }}>{custo.categoriaRotulo}</div>
                <div style={{ fontSize: '0.82rem', color: '#3C402B' }}>{custo.descricao}</div>
              </div>
              <div style={{ fontWeight: 800, fontSize: '1.08rem', color: '#9E2A2B' }}>
                {formatarMoeda(custo.valor)}
              </div>
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {custo.rateios.map((r) => (
                <span
                  key={r.culturaId}
                  style={{
                    backgroundColor: '#EFEAE3',
                    border: '1px solid #D5CEBE',
                    borderRadius: '8px',
                    padding: '4px 8px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                  }}
                >
                  {r.culturaNome}: {formatarMoeda(r.valorRateado)} ({r.percentual}%)
                </span>
              ))}
            </div>

            {!somenteLeitura && !caderno?.periodoFechado && custo.syncStatus !== 'PENDENTE' && (
              <button
                type="button"
                className="touch-target"
                onClick={() => abrirModalRateio(custo)}
                style={{
                  borderRadius: '12px',
                  border: '2px solid #2C5745',
                  backgroundColor: '#FFFFFF',
                  color: '#2C5745',
                  fontWeight: 800,
                  fontSize: '0.9rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: 'pointer',
                }}
              >
                <Sliders size={18} weight="bold" />
                Ajustar Rateio entre Culturas
              </button>
            )}
          </div>
        ))
      )}

      {/* Modal de Rateio */}
      <Dialog
        header={custoEmEdicao ? `Rateio: ${custoEmEdicao.categoriaRotulo}` : 'Rateio de Custo'}
        visible={custoEmEdicao !== null}
        onHide={() => setCustoEmEdicao(null)}
        modal
      >
        {custoEmEdicao && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', paddingTop: '8px' }}>
            <div
              style={{
                backgroundColor: '#EBE3A7',
                padding: '10px 12px',
                borderRadius: '10px',
                display: 'flex',
                justifyContent: 'space-between',
                fontWeight: 800,
              }}
            >
              <span>Custo Total:</span>
              <span>{formatarMoeda(custoEmEdicao.valor)}</span>
            </div>

            <Button
              type="button"
              label="Dividir por igual"
              onClick={dividirIgual}
              className="touch-target"
              style={{ backgroundColor: '#EFEAE3', borderColor: '#3C402B', color: '#2E2910' }}
            />

            {culturas.map((c) => (
              <div key={c.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
                <label htmlFor={`rateio-${c.id}`} style={{ fontWeight: 800, flex: 1 }}>
                  {c.nome}
                </label>
                <input
                  id={`rateio-${c.id}`}
                  type="number"
                  step="0.01"
                  min="0"
                  value={valoresRateio[c.id] || ''}
                  onChange={(e) => setValoresRateio((prev) => ({ ...prev, [c.id]: e.target.value }))}
                  style={{
                    minHeight: '48px',
                    width: '130px',
                    borderRadius: '10px',
                    border: '2px solid #D5CEBE',
                    padding: '6px 10px',
                    fontSize: '1rem',
                    fontWeight: 800,
                    textAlign: 'right',
                  }}
                />
              </div>
            ))}

            <div
              style={{
                padding: '10px',
                borderRadius: '10px',
                backgroundColor: Math.abs(diferenca) <= 0.001 ? '#E8F5E9' : '#FBEAEA',
                color: Math.abs(diferenca) <= 0.001 ? '#1E5631' : '#9E2A2B',
                fontWeight: 800,
                fontSize: '0.88rem',
              }}
            >
              Soma informada: {formatarMoeda(somaArredondada)} | Diferença: {formatarMoeda(diferenca)}
            </div>

            {erroModal && (
              <div style={{ color: '#9E2A2B', fontWeight: 800, fontSize: '0.88rem' }}>{erroModal}</div>
            )}

            <Button
              type="button"
              disabled={salvando || Math.abs(diferenca) > 0.001}
              label={salvando ? 'Salvando...' : 'Salvar Rateio'}
              icon={<CheckCircle size={20} weight="bold" style={{ marginRight: '6px' }} />}
              onClick={salvarRateio}
              className="touch-target"
              style={{
                backgroundColor: '#C4E456',
                borderColor: '#3C402B',
                color: '#2E2910',
                fontWeight: 800,
              }}
            />
          </div>
        )}
      </Dialog>
    </div>
  );
};
