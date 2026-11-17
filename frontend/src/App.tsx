import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  BookOpen,
  Calculator,
  Heartbeat,
  PlusCircle,
  Question,
  ShieldCheck,
  SignOut,
} from '@phosphor-icons/react';
import {
  ConflitoSync,
  Cultura,
  GlobalSyncState,
  ProdutorHorta,
  UsuarioResumo,
} from './types';
import {
  limparSessao,
  listarConflitos,
  listarCulturas,
  listarProdutoresHorta,
  obterSessaoSalva,
  sincronizarFila,
} from './services/api';
import { listarFilaOffline } from './services/offlineQueue';
import { LoginView } from './components/LoginView';
import { OnboardingModal } from './components/OnboardingModal';
import { SyncStatusBar } from './components/SyncStatusBar';
import { RegistroRapidoView } from './components/RegistroRapidoView';
import { CadernoDigitalView } from './components/CadernoDigitalView';
import { CustosRateioView } from './components/CustosRateioView';
import { PainelSaudeView } from './components/PainelSaudeView';

type Aba = 'registrar' | 'caderno' | 'custos' | 'saude';

const ONBOARDING_KEY = 'agro_onboarding_visto_v1';

export const App: React.FC = () => {
  const [usuario, setUsuario] = useState<UsuarioResumo | null>(() => obterSessaoSalva()?.usuario || null);
  const [abaAtiva, setAbaAtiva] = useState<Aba>('registrar');
  const [culturas, setCulturas] = useState<Cultura[]>([]);
  const [produtores, setProdutores] = useState<ProdutorHorta[]>([]);
  const [produtorSelecionadoId, setProdutorSelecionadoId] = useState<string>('');
  const [anoMes, setAnoMes] = useState<string>('2026-11');
  const [onboardingVisivel, setOnboardingVisivel] = useState<boolean>(false);

  const [modoOfflineSimulado, setModoOfflineSimulado] = useState<boolean>(!navigator.onLine);
  const [estadoSync, setEstadoSync] = useState<GlobalSyncState>('SINCRONIZADO');
  const [qtdPendentes, setQtdPendentes] = useState<number>(0);
  const [conflitos, setConflitos] = useState<ConflitoSync[]>([]);
  const [refreshTick, setRefreshTick] = useState<number>(0);

  const mesesDisponiveis = useMemo(() => {
    const hoje = new Date();
    const mesAtual = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`;
    const base = new Set<string>(['2026-10', '2026-11', mesAtual]);
    return Array.from(base).sort();
  }, []);

  const somenteLeitura = usuario?.perfil === 'GESTOR';
  const produtorAtivoId = somenteLeitura
    ? produtorSelecionadoId || produtores[0]?.id || ''
    : usuario?.id || '';

  const atualizarEstadoFila = useCallback(async () => {
    if (!produtorAtivoId) return;
    const [fila, listaConflitos] = await Promise.all([
      listarFilaOffline(produtorAtivoId),
      listarConflitos(produtorAtivoId),
    ]);
    const pendentes = fila.filter((f) => f.status === 'PENDENTE' || f.status === 'SINCRONIZANDO');
    setQtdPendentes(pendentes.length);
    setConflitos(listaConflitos);

    if (pendentes.length > 0) {
      setEstadoSync('PENDENTE');
    } else if (listaConflitos.length > 0) {
      setEstadoSync('CONFLITO');
    } else {
      setEstadoSync('SINCRONIZADO');
    }
  }, [produtorAtivoId]);

  const executarSincronizacao = useCallback(async () => {
    if (!produtorAtivoId || somenteLeitura || modoOfflineSimulado || !navigator.onLine) return;
    const fila = await listarFilaOffline(produtorAtivoId);
    if (fila.length === 0) return;

    setEstadoSync('SINCRONIZANDO');
    try {
      const res = await sincronizarFila(produtorAtivoId, culturas);
      const listaConflitos = await listarConflitos(produtorAtivoId);
      setConflitos(listaConflitos);
      const restante = await listarFilaOffline(produtorAtivoId);
      setQtdPendentes(restante.length);

      if (res.conflitos > 0) {
        setEstadoSync('CONFLITO');
      } else if (restante.length > 0) {
        setEstadoSync('PENDENTE');
      } else {
        setEstadoSync('SINCRONIZADO');
      }
      setRefreshTick((t) => t + 1);
    } catch {
      await atualizarEstadoFila();
    }
  }, [produtorAtivoId, somenteLeitura, modoOfflineSimulado, culturas, atualizarEstadoFila]);

  useEffect(() => {
    if (!usuario) return;

    if (!localStorage.getItem(ONBOARDING_KEY)) {
      setOnboardingVisivel(true);
    }
    if (usuario.perfil === 'GESTOR') {
      setAbaAtiva('saude');
    } else {
      setAbaAtiva('registrar');
      setProdutorSelecionadoId(usuario.id);
    }

    listarCulturas().then(setCulturas).catch(() => setCulturas([]));
    listarProdutoresHorta()
      .then((lista) => {
        setProdutores(lista);
        if (usuario.perfil === 'GESTOR' && lista.length > 0) {
          setProdutorSelecionadoId(lista[0].id);
        }
      })
      .catch(() => setProdutores([]));
  }, [usuario]);

  useEffect(() => {
    atualizarEstadoFila();
  }, [atualizarEstadoFila, refreshTick]);

  useEffect(() => {
    const aoVoltarOnline = () => {
      setModoOfflineSimulado(false);
      executarSincronizacao();
    };
    const aoFicarOffline = () => {
      setModoOfflineSimulado(true);
    };

    window.addEventListener('online', aoVoltarOnline);
    window.addEventListener('offline', aoFicarOffline);
    return () => {
      window.removeEventListener('online', aoVoltarOnline);
      window.removeEventListener('offline', aoFicarOffline);
    };
  }, [executarSincronizacao]);

  const alternarModoOffline = () => {
    const proximo = !modoOfflineSimulado;
    setModoOfflineSimulado(proximo);
    if (!proximo && navigator.onLine) {
      setTimeout(() => {
        executarSincronizacao();
      }, 150);
    }
  };

  const sair = () => {
    limparSessao();
    setUsuario(null);
  };

  if (!usuario) {
    return <LoginView aoAutenticar={(u) => setUsuario(u)} />;
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div style={{ minWidth: 0 }}>
          <h1 className="app-header-title">{usuario.hortaNome}</h1>
          <p className="app-header-sub">
            {usuario.nome} ({usuario.perfil === 'GESTOR' ? 'Gestor' : 'Produtor'})
          </p>
        </div>
        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            type="button"
            className="icon-btn-header"
            title="Ver tutorial inicial"
            aria-label="Abrir boas-vindas"
            onClick={() => setOnboardingVisivel(true)}
          >
            <Question size={20} weight="bold" />
            <span>Guia</span>
          </button>
          <button
            type="button"
            className="icon-btn-header"
            title="Sair"
            aria-label="Sair da conta"
            onClick={sair}
          >
            <SignOut size={20} weight="bold" />
          </button>
        </div>
      </header>

      <SyncStatusBar
        estadoSync={estadoSync}
        qtdPendentes={qtdPendentes}
        modoOfflineSimulado={modoOfflineSimulado}
        conflitos={conflitos}
        somenteLeitura={somenteLeitura}
        onAlternarOffline={alternarModoOffline}
        onSincronizarAgora={executarSincronizacao}
      />

      <main className="main-content">
        {somenteLeitura && produtores.length > 0 && (
          <div className="banner-gestor-leitura">
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 800, fontSize: '0.88rem' }}>
              <ShieldCheck size={20} weight="bold" color="#2C5745" />
              Modo Leitura (Gestor) — Selecione o produtor:
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {produtores.map((p) => {
                const ativo = p.id === produtorAtivoId;
                return (
                  <button
                    key={p.id}
                    type="button"
                    className="touch-target"
                    onClick={() => setProdutorSelecionadoId(p.id)}
                    style={{
                      flex: 1,
                      minWidth: '140px',
                      padding: '8px 12px',
                      borderRadius: '10px',
                      border: '2px solid #3C402B',
                      backgroundColor: ativo ? '#2C5745' : '#FFFFFF',
                      color: ativo ? '#FFFFFF' : '#2E2910',
                      fontWeight: 800,
                      fontSize: '0.85rem',
                      cursor: 'pointer',
                    }}
                  >
                    {p.nome}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {abaAtiva === 'registrar' && (
          <RegistroRapidoView
            produtorId={produtorAtivoId}
            culturas={culturas}
            somenteLeitura={somenteLeitura}
            modoOffline={modoOfflineSimulado}
            onLancamentoCriado={() => setRefreshTick((t) => t + 1)}
          />
        )}

        {abaAtiva === 'caderno' && (
          <CadernoDigitalView
            produtorId={produtorAtivoId}
            anoMes={anoMes}
            mesesDisponiveis={mesesDisponiveis}
            onMudarMes={setAnoMes}
            modoOffline={modoOfflineSimulado}
            refreshTick={refreshTick}
          />
        )}

        {abaAtiva === 'custos' && (
          <CustosRateioView
            produtorId={produtorAtivoId}
            culturas={culturas}
            anoMes={anoMes}
            mesesDisponiveis={mesesDisponiveis}
            onMudarMes={setAnoMes}
            somenteLeitura={somenteLeitura}
            modoOffline={modoOfflineSimulado}
            refreshTick={refreshTick}
            onRateioAtualizado={() => setRefreshTick((t) => t + 1)}
          />
        )}

        {abaAtiva === 'saude' && (
          <PainelSaudeView
            produtorId={produtorAtivoId}
            culturas={culturas}
            anoMes={anoMes}
            mesesDisponiveis={mesesDisponiveis}
            onMudarMes={setAnoMes}
            modoOffline={modoOfflineSimulado}
            refreshTick={refreshTick}
          />
        )}
      </main>

      <nav className="bottom-nav" aria-label="Menu principal">
        <button
          type="button"
          className={`bottom-nav-item ${abaAtiva === 'registrar' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('registrar')}
        >
          <PlusCircle size={24} weight="bold" />
          <span>Registrar</span>
        </button>
        <button
          type="button"
          className={`bottom-nav-item ${abaAtiva === 'caderno' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('caderno')}
        >
          <BookOpen size={24} weight="bold" />
          <span>Caderno</span>
        </button>
        <button
          type="button"
          className={`bottom-nav-item ${abaAtiva === 'custos' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('custos')}
        >
          <Calculator size={24} weight="bold" />
          <span>Custos</span>
        </button>
        <button
          type="button"
          className={`bottom-nav-item ${abaAtiva === 'saude' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('saude')}
        >
          <Heartbeat size={24} weight="bold" />
          <span>Saúde</span>
        </button>
      </nav>

      <OnboardingModal
        visivel={onboardingVisivel}
        aoConcluir={() => {
          localStorage.setItem(ONBOARDING_KEY, '1');
          setOnboardingVisivel(false);
        }}
      />
    </div>
  );
};
