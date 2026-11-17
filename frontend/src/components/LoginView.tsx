import React, { useState } from 'react';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { Key, Plant, ShieldCheck, SignIn, User } from '@phosphor-icons/react';
import { IS_DEMO_MODE, login as autenticarApi } from '../services/api';
import { UsuarioResumo } from '../types';

interface LoginViewProps {
  aoAutenticar: (usuario: UsuarioResumo) => void;
}

const PERFIS_RAPIDOS = [
  { login: 'joao.silva', titulo: 'Seu João da Silva', subtitulo: 'Produtor da Horta', precisaTotp: false },
  { login: 'maria.santos', titulo: 'Dona Maria dos Santos', subtitulo: 'Produtora da Horta', precisaTotp: false },
  { login: 'carlos.gestor', titulo: 'Carlos Eduardo', subtitulo: 'Gestor (Somente Leitura)', precisaTotp: true },
];

export const LoginView: React.FC<LoginViewProps> = ({ aoAutenticar }) => {
  const [loginStr, setLoginStr] = useState('joao.silva');
  const [senha, setSenha] = useState('123456');
  const [codigoTotp, setCodigoTotp] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  const isGestorSelecionado = loginStr.trim() === 'carlos.gestor';
  const exigirTotp = isGestorSelecionado && !IS_DEMO_MODE;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      const resp = await autenticarApi(loginStr, senha, exigirTotp ? codigoTotp : undefined);
      aoAutenticar(resp.usuario);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível entrar.');
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="app-shell" style={{ paddingBottom: '24px' }}>
      <header className="app-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Plant size={32} weight="duotone" color="#C4E456" />
          <div>
            <h1 className="app-header-title">Planejamento Financeiro</h1>
            <p className="app-header-sub">Agricultura Urbana e Periurbana</p>
          </div>
        </div>
      </header>

      <main className="main-content">
        {IS_DEMO_MODE && (
          <div
            style={{
              backgroundColor: '#EBE3A7',
              border: '2px solid #3C402B',
              borderRadius: '14px',
              padding: '12px',
              fontWeight: 700,
              fontSize: '0.88rem',
              color: '#2E2910',
            }}
          >
            Modo Demonstração Online: dados fictícios salvos no navegador, sem backend e sem exigir TOTP para o gestor.
          </div>
        )}

        <div className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800, color: '#3C402B' }}>
            Escolha um perfil de acesso
          </h2>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {PERFIS_RAPIDOS.map((p) => {
              const ativo = loginStr === p.login;
              return (
                <button
                  key={p.login}
                  type="button"
                  onClick={() => {
                    setLoginStr(p.login);
                    setSenha('123456');
                    setErro(null);
                  }}
                  className="touch-target"
                  style={{
                    width: '100%',
                    padding: '12px 14px',
                    borderRadius: '12px',
                    border: ativo ? '2px solid #2C5745' : '2px solid #D5CEBE',
                    backgroundColor: ativo ? '#2C5745' : '#EFEAE3',
                    color: ativo ? '#FFFFFF' : '#2E2910',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {p.precisaTotp ? <ShieldCheck size={24} weight="bold" /> : <User size={24} weight="bold" />}
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '0.96rem' }}>{p.titulo}</div>
                      <div style={{ fontSize: '0.8rem', opacity: 0.9 }}>{p.subtitulo}</div>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 800 }}>{p.login}</span>
                </button>
              );
            })}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="card-agro" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label htmlFor="loginInput" style={{ fontWeight: 800, fontSize: '0.9rem' }}>
              Usuário
            </label>
            <InputText
              id="loginInput"
              value={loginStr}
              onChange={(e) => setLoginStr(e.target.value)}
              className="touch-target"
              style={{ width: '100%', fontSize: '1rem' }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label htmlFor="senhaInput" style={{ fontWeight: 800, fontSize: '0.9rem' }}>
              Senha
            </label>
            <InputText
              id="senhaInput"
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              className="touch-target"
              style={{ width: '100%', fontSize: '1rem' }}
            />
          </div>

          {exigirTotp && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label htmlFor="totpInput" style={{ fontWeight: 800, fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Key size={18} weight="bold" />
                Código TOTP (Google Authenticator)
              </label>
              <InputText
                id="totpInput"
                inputMode="numeric"
                maxLength={6}
                placeholder="000000"
                value={codigoTotp}
                onChange={(e) => setCodigoTotp(e.target.value.replace(/\D/g, ''))}
                className="touch-target"
                style={{ width: '100%', fontSize: '1.2rem', letterSpacing: '4px', textAlign: 'center', fontWeight: 800 }}
              />
            </div>
          )}

          {erro && (
            <div
              role="alert"
              style={{
                backgroundColor: '#FBEAEA',
                border: '2px solid #9E2A2B',
                color: '#9E2A2B',
                borderRadius: '10px',
                padding: '10px',
                fontWeight: 700,
                fontSize: '0.88rem',
              }}
            >
              {erro}
            </div>
          )}

          <Button
            type="submit"
            disabled={carregando}
            label={carregando ? 'Entrando...' : 'Entrar no Sistema'}
            icon={<SignIn size={22} weight="bold" style={{ marginRight: '8px' }} />}
            className="touch-target"
            style={{
              width: '100%',
              backgroundColor: '#C4E456',
              borderColor: '#3C402B',
              color: '#2E2910',
              fontSize: '1.05rem',
              fontWeight: 800,
            }}
          />
        </form>
      </main>
    </div>
  );
};
