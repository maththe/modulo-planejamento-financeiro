import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle,
  HandTap,
  Plant,
  WifiSlash,
} from '@phosphor-icons/react';
import { ContextualHelpButton } from './ContextualHelpButton';

interface OnboardingModalProps {
  visivel: boolean;
  aoConcluir: () => void;
}

const PASSOS = [
  {
    titulo: '1. Anote em até 3 toques',
    descricao:
      'Escolha Entrada ou Saída, toque no desenho da categoria, digite o valor no teclado grande e confirme.',
    icone: <HandTap size={72} weight="duotone" color="#2C5745" />,
    ajudaTitulo: 'Ajuda: Registro Rápido',
    ajudaTexto:
      'O aplicativo sugere primeiro a categoria que você mais usa. Assim você registra suas vendas e gastos sem precisar digitar texto.',
    ajudaDicas: [
      'Toque 1: Escolha Entrada ou Saída e o ícone da categoria.',
      'Toque 2: Digite o valor no teclado numérico grande.',
      'Toque 3: Toque em Confirmar Registro.',
    ],
  },
  {
    titulo: '2. Funciona sem internet',
    descricao:
      'Mesmo na horta sem sinal 3G, seus lançamentos ficam guardados no celular e sobem sozinhos quando a rede voltar.',
    icone: <WifiSlash size={72} weight="duotone" color="#3C402B" />,
    ajudaTitulo: 'Ajuda: Modo Sem Rede',
    ajudaTexto:
      'Quando estiver sem internet, a barra do topo mostra o aviso "Pendente". Assim que o sinal voltar, tudo é sincronizado sem duplicar.',
    ajudaDicas: [
      'Não feche o navegador limpando o histórico antes de sincronizar.',
      'Você também pode tocar em Sincronizar na barra do topo.',
    ],
  },
  {
    titulo: '3. Custos por cultura e saúde da horta',
    descricao:
      'Divida cada gasto entre Alface, Couve, Tomate e Cenoura e acompanhe o Semáforo e o Caderno do mês.',
    icone: <Plant size={72} weight="duotone" color="#2C5745" />,
    ajudaTitulo: 'Ajuda: Custos e Saúde',
    ajudaTexto:
      'Na aba Custos você divide os gastos entre as culturas. Na aba Saúde você consulta o Semáforo, os indicadores e a tabela mensal.',
    ajudaDicas: [
      'A soma dos valores divididos entre culturas deve ser igual ao gasto total.',
      'Use o botão "?" no alto de qualquer tela sempre que tiver dúvida.',
    ],
  },
];

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ visivel, aoConcluir }) => {
  const [passo, setPasso] = useState(0);
  const atual = PASSOS[passo];

  const handleClose = () => {
    setPasso(0);
    aoConcluir();
  };

  return (
    <Dialog
      visible={visivel}
      onHide={handleClose}
      modal
      closable={false}
      header={`Boas-vindas (${passo + 1} de 3)`}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', paddingTop: '8px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: '6px' }}>
            {PASSOS.map((_, idx) => (
              <span
                key={idx}
                style={{
                  width: idx === passo ? '28px' : '12px',
                  height: '12px',
                  borderRadius: '999px',
                  backgroundColor: idx === passo ? '#2C5745' : '#D5CEBE',
                  transition: 'all 0.2s',
                }}
              />
            ))}
          </div>
          <ContextualHelpButton
            titulo={atual.ajudaTitulo}
            texto={atual.ajudaTexto}
            dicas={atual.ajudaDicas}
            ariaLabel={`Ajuda da tela ${passo + 1} de boas-vindas`}
          />
        </div>

        <div
          style={{
            backgroundColor: '#EFEAE3',
            borderRadius: '16px',
            padding: '24px 16px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            textAlign: 'center',
            gap: '12px',
            border: '2px solid #EBE3A7',
          }}
        >
          <div
            style={{
              width: '104px',
              height: '104px',
              borderRadius: '50%',
              backgroundColor: '#EBE3A7',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {atual.icone}
          </div>
          <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 800, color: '#3C402B' }}>
            {atual.titulo}
          </h3>
          <p style={{ margin: 0, fontSize: '0.98rem', lineHeight: 1.5, color: '#2E2910', fontWeight: 600 }}>
            {atual.descricao}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', justifyContent: 'space-between' }}>
          {passo > 0 ? (
            <Button
              type="button"
              label="Voltar"
              icon={<ArrowLeft size={20} weight="bold" style={{ marginRight: '6px' }} />}
              onClick={() => setPasso((p) => p - 1)}
              className="touch-target"
              style={{
                flex: 1,
                backgroundColor: '#EFEAE3',
                borderColor: '#3C402B',
                color: '#2E2910',
              }}
            />
          ) : (
            <Button
              type="button"
              label="Pular"
              onClick={handleClose}
              className="touch-target"
              style={{
                flex: 1,
                backgroundColor: '#EFEAE3',
                borderColor: '#D5CEBE',
                color: '#3C402B',
              }}
            />
          )}

          {passo < PASSOS.length - 1 ? (
            <Button
              type="button"
              label="Próximo"
              icon={<ArrowRight size={20} weight="bold" style={{ marginLeft: '6px' }} />}
              iconPos="right"
              onClick={() => setPasso((p) => p + 1)}
              className="touch-target"
              style={{
                flex: 1,
                backgroundColor: '#2C5745',
                borderColor: '#2C5745',
                color: '#FFFFFF',
              }}
            />
          ) : (
            <Button
              type="button"
              label="Começar"
              icon={<CheckCircle size={22} weight="bold" style={{ marginRight: '6px' }} />}
              onClick={handleClose}
              className="touch-target"
              style={{
                flex: 1,
                backgroundColor: '#C4E456',
                borderColor: '#3C402B',
                color: '#2E2910',
              }}
            />
          )}
        </div>
      </div>
    </Dialog>
  );
};
