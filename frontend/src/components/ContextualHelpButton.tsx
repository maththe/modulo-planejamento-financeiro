import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { Question } from '@phosphor-icons/react';

interface ContextualHelpButtonProps {
  titulo: string;
  texto: string;
  dicas?: string[];
  ariaLabel?: string;
}

export const ContextualHelpButton: React.FC<ContextualHelpButtonProps> = ({
  titulo,
  texto,
  dicas = [],
  ariaLabel = 'Ajuda sobre esta tela',
}) => {
  const [aberto, setAberto] = useState(false);

  return (
    <>
      <button
        type="button"
        className="help-btn-contextual touch-target"
        aria-label={ariaLabel}
        title="Ajuda"
        onClick={() => setAberto(true)}
      >
        <Question size={26} weight="bold" />
      </button>

      <Dialog
        header={titulo}
        visible={aberto}
        onHide={() => setAberto(false)}
        modal
        dismissableMask
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', paddingTop: '8px' }}>
          <p style={{ margin: 0, fontSize: '1rem', lineHeight: 1.5, color: '#2E2910', fontWeight: 600 }}>
            {texto}
          </p>
          {dicas.length > 0 && (
            <ul style={{ margin: 0, paddingLeft: '18px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {dicas.map((dica, i) => (
                <li key={i} style={{ fontSize: '0.92rem', color: '#3C402B', fontWeight: 600 }}>
                  {dica}
                </li>
              ))}
            </ul>
          )}
          <Button
            label="Entendi"
            onClick={() => setAberto(false)}
            className="touch-target"
            style={{
              backgroundColor: '#2C5745',
              borderColor: '#2C5745',
              color: '#FFFFFF',
              marginTop: '6px',
            }}
          />
        </div>
      </Dialog>
    </>
  );
};
