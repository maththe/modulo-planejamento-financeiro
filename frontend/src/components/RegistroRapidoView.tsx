import React, { useEffect, useState } from 'react';
import {
  ArrowCircleDown,
  ArrowCircleUp,
  Backspace,
  CheckCircle,
  Coins,
  Drop,
  Lightning,
  Lock,
  Package,
  Plant,
  PlusCircle,
  ShoppingBag,
  Sparkle,
  Truck,
  UsersThree,
} from '@phosphor-icons/react';
import {
  CategoriaLancamento,
  Cultura,
  TipoLancamento,
} from '../types';
import { CATEGORIAS_META, formatarMoeda } from '../services/calculadoraIndicadores';
import { registrarLancamento, sugerirCategoria } from '../services/api';
import { ContextualHelpButton } from './ContextualHelpButton';

interface RegistroRapidoViewProps {
  produtorId: string;
  culturas: Cultura[];
  somenteLeitura: boolean;
  modoOffline: boolean;
  onLancamentoCriado: () => void;
}

const CATEGORIAS_ENTRADA: CategoriaLancamento[] = ['VENDAS'];
const CATEGORIAS_SAIDA: CategoriaLancamento[] = [
  'SEMENTES',
  'ADUBO',
  'AGUA',
  'ENERGIA',
  'FRETE',
  'MAO_DE_OBRA',
  'OUTROS',
];

function renderCategoriaIcone(categoria: CategoriaLancamento) {
  switch (categoria) {
    case 'VENDAS':
      return <ShoppingBag size={28} weight="duotone" />;
    case 'SEMENTES':
      return <Plant size={28} weight="duotone" />;
    case 'ADUBO':
      return <Package size={28} weight="duotone" />;
    case 'AGUA':
      return <Drop size={28} weight="duotone" />;
    case 'ENERGIA':
      return <Lightning size={28} weight="duotone" />;
    case 'FRETE':
      return <Truck size={28} weight="duotone" />;
    case 'MAO_DE_OBRA':
      return <UsersThree size={28} weight="duotone" />;
    default:
      return <Coins size={28} weight="duotone" />;
  }
}

export const RegistroRapidoView: React.FC<RegistroRapidoViewProps> = ({
  produtorId,
  culturas,
  somenteLeitura,
  modoOffline,
  onLancamentoCriado,
}) => {
  const [tipo, setTipo] = useState<TipoLancamento>('ENTRADA');
  const [categoria, setCategoria] = useState<CategoriaLancamento>('VENDAS');
  const [categoriaSugerida, setCategoriaSugerida] = useState<CategoriaLancamento>('VENDAS');
  const [culturaId, setCulturaId] = useState<string | null>(null);
  const [valorTexto, setValorTexto] = useState<string>('0');
  const [quantidadeTexto, setQuantidadeTexto] = useState<string>('');
  const [salvando, setSalvando] = useState(false);
  const [aviso, setAviso] = useState<{ tipo: 'sucesso' | 'offline' | 'erro'; texto: string } | null>(null);

  useEffect(() => {
    let ativo = true;
    sugerirCategoria(produtorId, tipo, modoOffline).then((resp) => {
      if (!ativo) return;
      setCategoriaSugerida(resp.categoriaSugerida);
      setCategoria(resp.categoriaSugerida);
    });
    return () => {
      ativo = false;
    };
  }, [produtorId, tipo, modoOffline]);

  const categoriasDisponiveis = tipo === 'ENTRADA' ? CATEGORIAS_ENTRADA : CATEGORIAS_SAIDA;

  const valorNumerico = parseFloat(valorTexto.replace(',', '.')) || 0;

  const pressionarDigito = (digito: string) => {
    setAviso(null);
    setValorTexto((atual) => {
      if (digito === ',') {
        if (atual.includes(',')) return atual;
        return `${atual},`;
      }
      if (digito === '00') {
        if (atual === '0') return '0';
        if (atual.includes(',')) {
          const [, dec] = atual.split(',');
          if (dec.length >= 2) return atual;
          if (dec.length === 1) return `${atual}0`;
          return `${atual}00`;
        }
        return atual.length >= 6 ? atual : `${atual}00`;
      }
      if (atual === '0') {
        return digito;
      }
      if (atual.includes(',')) {
        const [, dec] = atual.split(',');
        if (dec.length >= 2) return atual;
        return `${atual}${digito}`;
      }
      return atual.length >= 6 ? atual : `${atual}${digito}`;
    });
  };

  const apagarDigito = () => {
    setValorTexto((atual) => {
      if (atual.length <= 1) return '0';
      const proximo = atual.slice(0, -1);
      return proximo === '' ? '0' : proximo;
    });
  };

  const confirmar = async () => {
    if (valorNumerico <= 0) {
      setAviso({ tipo: 'erro', texto: 'Digite um valor maior que zero.' });
      return;
    }

    setSalvando(true);
    setAviso(null);
    try {
      const hoje = new Date();
      const ano = hoje.getFullYear();
      const mes = String(hoje.getMonth() + 1).padStart(2, '0');
      const dia = String(hoje.getDate()).padStart(2, '0');
      const dataLancamento = `${ano}-${mes}-${dia}`;

      const qtdNum =
        tipo === 'ENTRADA' && quantidadeTexto.trim() !== ''
          ? parseFloat(quantidadeTexto.replace(',', '.')) || null
          : null;

      const culturaObj = culturas.find((c) => c.id === culturaId);
      const desc = culturaObj
        ? `${CATEGORIAS_META[categoria].rotulo} - ${culturaObj.nome}`
        : CATEGORIAS_META[categoria].rotulo;

      const res = await registrarLancamento(
        produtorId,
        culturas,
        {
          tipo,
          categoria,
          valor: Math.round(valorNumerico * 100) / 100,
          culturaId,
          quantidade: qtdNum,
          descricao: desc,
          dataLancamento,
        },
        modoOffline
      );

      setValorTexto('0');
      setQuantidadeTexto('');
      setAviso({
        tipo: res.offline ? 'offline' : 'sucesso',
        texto: res.offline
          ? 'Salvo no celular (Pendente). Será enviado quando o sinal voltar.'
          : 'Lançamento confirmado e salvo com sucesso!',
      });
      onLancamentoCriado();
    } catch (err) {
      setAviso({
        tipo: 'erro',
        texto: err instanceof Error ? err.message : 'Erro ao salvar lançamento.',
      });
    } finally {
      setSalvando(false);
    }
  };

  if (somenteLeitura) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div className="screen-header-card">
          <div>
            <h2 className="screen-title">
              <PlusCircle size={24} weight="bold" color="#2C5745" />
              Registro Rápido
            </h2>
            <p className="screen-subtitle">Acesso somente leitura para o Gestor</p>
          </div>
          <ContextualHelpButton
            titulo="Ajuda: Registro Rápido"
            texto="O perfil Gestor visualiza os registros dos produtores da horta em modo leitura."
          />
        </div>
        <div className="card-agro" style={{ textAlign: 'center', padding: '24px 16px' }}>
          <Lock size={48} weight="duotone" color="#3C402B" />
          <p style={{ fontWeight: 800, color: '#3C402B', marginBottom: '4px' }}>
            Modo Leitura (Gestor)
          </p>
          <p style={{ margin: 0, fontSize: '0.92rem', color: '#2E2910' }}>
            Use as abas <strong>Caderno</strong>, <strong>Custos</strong> e <strong>Saúde</strong> para acompanhar o produtor selecionado.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div className="screen-header-card">
        <div>
          <h2 className="screen-title">
            <PlusCircle size={24} weight="bold" color="#2C5745" />
            Registrar em 3 toques
          </h2>
          <p className="screen-subtitle">1. Categoria • 2. Valor • 3. Confirmar</p>
        </div>
        <ContextualHelpButton
          titulo="Ajuda: Registrar em 3 toques"
          texto="Escolha se entrou dinheiro (Entrada) ou se você pagou algo (Saída), toque no ícone, digite o valor no teclado grande e confirme."
          dicas={[
            'A categoria com o selo "Sugerida" é a que você mais usa.',
            'Se estiver sem sinal 3G, o registro fica salvo no aparelho até reconectar.',
          ]}
        />
      </div>

      {/* Passo 1: Entrada / Saida + Categoria */}
      <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div className="tipo-toggle-grid">
          <button
            type="button"
            className={`tipo-btn ${tipo === 'ENTRADA' ? 'active-entrada' : ''}`}
            onClick={() => {
              setTipo('ENTRADA');
              setAviso(null);
            }}
          >
            <ArrowCircleUp size={24} weight="bold" />
            Entrada
          </button>
          <button
            type="button"
            className={`tipo-btn ${tipo === 'SAIDA' ? 'active-saida' : ''}`}
            onClick={() => {
              setTipo('SAIDA');
              setAviso(null);
            }}
          >
            <ArrowCircleDown size={24} weight="bold" />
            Saída
          </button>
        </div>

        <div className="categoria-grid">
          {categoriasDisponiveis.map((cat) => {
            const selecionada = categoria === cat;
            const sugerida = categoriaSugerida === cat;
            return (
              <button
                key={cat}
                type="button"
                className={`categoria-btn ${selecionada ? 'selected' : ''}`}
                onClick={() => {
                  setCategoria(cat);
                  setAviso(null);
                }}
              >
                {sugerida && (
                  <span className="sugestao-badge">
                    <Sparkle size={11} weight="fill" style={{ marginRight: '3px', verticalAlign: 'middle' }} />
                    Sugerida
                  </span>
                )}
                {renderCategoriaIcone(cat)}
                <span>{CATEGORIAS_META[cat].rotulo}</span>
              </button>
            );
          })}
        </div>

        <div>
          <div style={{ fontSize: '0.82rem', fontWeight: 800, color: '#3C402B', marginBottom: '6px' }}>
            Cultura (opcional):
          </div>
          <div className="cultura-chips">
            <button
              type="button"
              className={`cultura-chip ${culturaId === null ? 'selected' : ''}`}
              onClick={() => setCulturaId(null)}
            >
              Geral da Horta
            </button>
            {culturas.map((c) => (
              <button
                key={c.id}
                type="button"
                className={`cultura-chip ${culturaId === c.id ? 'selected' : ''}`}
                onClick={() => setCulturaId(c.id)}
              >
                {c.nome}
              </button>
            ))}
          </div>
        </div>

        {tipo === 'ENTRADA' && culturaId !== null && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <label htmlFor="qtdInput" style={{ fontSize: '0.85rem', fontWeight: 800, whiteSpace: 'nowrap' }}>
              Quantidade ({culturas.find((c) => c.id === culturaId)?.unidade}):
            </label>
            <input
              id="qtdInput"
              type="text"
              inputMode="decimal"
              placeholder="Ex: 20"
              value={quantidadeTexto}
              onChange={(e) => setQuantidadeTexto(e.target.value)}
              style={{
                minHeight: '48px',
                flex: 1,
                borderRadius: '10px',
                border: '2px solid #D5CEBE',
                padding: '6px 12px',
                fontSize: '1rem',
                fontWeight: 700,
              }}
            />
          </div>
        )}
      </div>

      {/* Passo 2: Teclado numerico ampliado */}
      <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div className="valor-visor" aria-label="Valor digitado">
          {valorTexto.includes(',')
            ? `R$ ${valorTexto}`
            : formatarMoeda(valorNumerico)}
        </div>

        <div className="keypad-grid">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', ',', '0', '00'].map((tecla) => (
            <button
              key={tecla}
              type="button"
              className="keypad-btn"
              onClick={() => pressionarDigito(tecla)}
            >
              {tecla}
            </button>
          ))}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
          <button
            type="button"
            className="keypad-btn keypad-btn--acao"
            onClick={apagarDigito}
          >
            <Backspace size={22} weight="bold" style={{ marginRight: '6px' }} />
            Apagar
          </button>
          <button
            type="button"
            className="keypad-btn keypad-btn--acao"
            onClick={() => setValorTexto('0')}
          >
            Limpar
          </button>
        </div>

        {aviso && (
          <div
            role="status"
            style={{
              padding: '10px 12px',
              borderRadius: '12px',
              fontWeight: 800,
              fontSize: '0.9rem',
              backgroundColor:
                aviso.tipo === 'sucesso'
                  ? '#E8F5E9'
                  : aviso.tipo === 'offline'
                  ? '#FFF7DA'
                  : '#FBEAEA',
              color:
                aviso.tipo === 'sucesso'
                  ? '#1E5631'
                  : aviso.tipo === 'offline'
                  ? '#8A6500'
                  : '#9E2A2B',
              border: '2px solid currentColor',
            }}
          >
            {aviso.texto}
          </div>
        )}

        {/* Passo 3: Confirmar */}
        <button
          type="button"
          className="btn-confirmar-primario"
          disabled={salvando || valorNumerico <= 0}
          onClick={confirmar}
        >
          <CheckCircle size={26} weight="bold" />
          {salvando ? 'Salvando...' : 'Confirmar Registro'}
        </button>
      </div>
    </div>
  );
};
