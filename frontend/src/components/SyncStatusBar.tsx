import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import {
  ArrowsClockwise,
  CheckCircle,
  ClockCountdown,
  WarningCircle,
  WifiHigh,
  WifiSlash,
} from '@phosphor-icons/react';
import { ConflitoSync, GlobalSyncState } from '../types';

interface SyncStatusBarProps {
  estadoSync: GlobalSyncState;
  qtdPendentes: number;
  modoOfflineSimulado: boolean;
  conflitos: ConflitoSync[];
  somenteLeitura: boolean;
  onAlternarOffline: () => void;
  onSincronizarAgora: () => void;
}

export const SyncStatusBar: React.FC<SyncStatusBarProps> = ({
  estadoSync,
  qtdPendentes,
  modoOfflineSimulado,
  conflitos,
  somenteLeitura,
  onAlternarOffline,
  onSincronizarAgora,
}) => {
  const [verConflitos, setVerConflitos] = useState(false);

  const renderBadge = () => {
    if (estadoSync === 'SINCRONIZANDO') {
      return (
        <span className="sync-pill sync-pill--sincronizando">
          <ArrowsClockwise size={18} weight="bold" />
          Sincronizando...
        </span>
      );
    }
    if (estadoSync === 'CONFLITO') {
      return (
        <span className="sync-pill sync-pill--conflito">
          <WarningCircle size={18} weight="bold" />
          Conflito ({conflitos.length})
        </span>
      );
    }
    if (estadoSync === 'PENDENTE' || qtdPendentes > 0) {
      return (
        <span className="sync-pill sync-pill--pendente">
          <ClockCountdown size={18} weight="bold" />
          Pendente ({qtdPendentes})
        </span>
      );
    }
    return (
      <span className="sync-pill sync-pill--sincronizado">
        <CheckCircle size={18} weight="bold" />
        Sincronizado
      </span>
    );
  };

  return (
    <>
      <div className="sync-bar" role="status" aria-live="polite">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          {renderBadge()}
          {conflitos.length > 0 && (
            <button
              type="button"
              onClick={() => setVerConflitos(true)}
              style={{
                minHeight: '36px',
                padding: '4px 10px',
                borderRadius: '10px',
                border: '1px solid #EBE3A7',
                backgroundColor: '#2E2910',
                color: '#EBE3A7',
                fontWeight: 700,
                fontSize: '0.78rem',
                cursor: 'pointer',
              }}
            >
              Ver log ({conflitos.length})
            </button>
          )}
        </div>

        {!somenteLeitura && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button
              type="button"
              onClick={onAlternarOffline}
              style={{
                minHeight: '40px',
                padding: '6px 10px',
                borderRadius: '10px',
                border: '1px solid #EBE3A7',
                backgroundColor: modoOfflineSimulado ? '#EBE3A7' : '#2C5745',
                color: modoOfflineSimulado ? '#2E2910' : '#FFFFFF',
                fontWeight: 700,
                fontSize: '0.78rem',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                cursor: 'pointer',
              }}
            >
              {modoOfflineSimulado ? <WifiSlash size={16} weight="bold" /> : <WifiHigh size={16} weight="bold" />}
              {modoOfflineSimulado ? 'Sem rede (3G)' : 'Com rede'}
            </button>

            {qtdPendentes > 0 && !modoOfflineSimulado && (
              <button
                type="button"
                onClick={onSincronizarAgora}
                style={{
                  minHeight: '40px',
                  padding: '6px 12px',
                  borderRadius: '10px',
                  border: '2px solid #2E2910',
                  backgroundColor: '#C4E456',
                  color: '#2E2910',
                  fontWeight: 800,
                  fontSize: '0.8rem',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '5px',
                  cursor: 'pointer',
                }}
              >
                <ArrowsClockwise size={16} weight="bold" />
                Sincronizar
              </button>
            )}
          </div>
        )}
      </div>

      <Dialog
        header="Log de Conflitos de Sincronização"
        visible={verConflitos}
        onHide={() => setVerConflitos(false)}
        modal
        dismissableMask
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', paddingTop: '8px' }}>
          <p style={{ margin: 0, fontSize: '0.9rem', fontWeight: 600, color: '#2E2910' }}>
            Regra aplicada automaticamente: <strong>Última escrita vence</strong>.
          </p>
          {conflitos.map((c) => (
            <div
              key={c.id}
              style={{
                backgroundColor: '#EFEAE3',
                border: '1px solid #D5CEBE',
                borderRadius: '10px',
                padding: '10px',
                fontSize: '0.82rem',
              }}
            >
              <div>
                <strong>Chave:</strong> {c.chaveIdempotencia}
              </div>
              <div>
                <strong>Versão servidor:</strong> v{c.versaoServidor} | <strong>Cliente:</strong> v{c.versaoCliente}
              </div>
              <div>
                <strong>Regra:</strong> {c.regraAplicada}
              </div>
            </div>
          ))}
          <Button
            label="Fechar"
            onClick={() => setVerConflitos(false)}
            className="touch-target"
            style={{ backgroundColor: '#2C5745', borderColor: '#2C5745', color: '#FFFFFF' }}
          />
        </div>
      </Dialog>
    </>
  );
};
