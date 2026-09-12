import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';

const API = window.API_URL || 'http://localhost:8080/api';

function money(value) {
    return Number(value || 0).toFixed(2).replace('.', ',');
}

function formatDateTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('pt-BR');
}

function textoPossuiLetra(valor) {
    return /[A-Za-zÀ-ÖØ-öø-ÿ]/.test(String(valor || ''));
}

function exigirTextoComLetra(valor, nomeCampo, obrigatorio = false) {
    const texto = String(valor || '').trim();
    if (obrigatorio && !texto) {
        throw new Error(`${nomeCampo} é obrigatório.`);
    }
    if (texto && !textoPossuiLetra(texto)) {
        throw new Error(`${nomeCampo} precisa conter pelo menos uma letra; não pode ser apenas número.`);
    }
    return texto;
}

function exigirCampoPreenchido(valor, nomeCampo) {
    const texto = String(valor ?? '').trim();
    if (!texto) {
        throw new Error(`${nomeCampo} é obrigatório.`);
    }
    return texto;
}

function limparTextoLivre(valor) {
    return String(valor || '').trim();
}

function numeroNaoNegativo(valor, nomeCampo) {
    const texto = String(valor ?? '').trim();
    if (!texto) return 0;
    const numero = Number(texto.replace(',', '.'));
    if (!Number.isFinite(numero)) {
        throw new Error(`${nomeCampo} deve conter um número válido.`);
    }
    if (numero < 0) {
        throw new Error(`${nomeCampo} não pode ser negativo.`);
    }
    return numero;
}

function inteiroNaoNegativo(valor, nomeCampo) {
    const numero = numeroNaoNegativo(valor, nomeCampo);
    if (!Number.isInteger(numero)) {
        throw new Error(`${nomeCampo} deve ser um número inteiro.`);
    }
    return numero;
}

function numeroObrigatorioNaoNegativo(valor, nomeCampo) {
    exigirCampoPreenchido(valor, nomeCampo);
    return numeroNaoNegativo(valor, nomeCampo);
}

function inteiroObrigatorioNaoNegativo(valor, nomeCampo) {
    const numero = numeroObrigatorioNaoNegativo(valor, nomeCampo);
    if (!Number.isInteger(numero)) {
        throw new Error(`${nomeCampo} deve ser um número inteiro.`);
    }
    return numero;
}

function inteiroMaiorQueZero(valor, nomeCampo) {
    const numero = inteiroNaoNegativo(valor, nomeCampo);
    if (numero <= 0) {
        throw new Error(`${nomeCampo} deve ser maior que zero.`);
    }
    return numero;
}

function numeroMaiorQueZero(valor, nomeCampo) {
    const numero = numeroObrigatorioNaoNegativo(valor, nomeCampo);
    if (numero <= 0) {
        throw new Error(`${nomeCampo} deve ser maior que zero.`);
    }
    return numero;
}

function limparNumeroDigitado(valor) {
    const texto = String(valor ?? '');
    if (texto.includes('-') || texto.includes('+') || /e/i.test(texto)) return null;
    const normalizado = texto.replace(',', '.');
    if (normalizado !== '' && Number(normalizado) < 0) return null;
    return texto;
}

function bloquearTeclaNumeroInvalida(event) {
    if (['-', '+', 'e', 'E'].includes(event.key)) {
        event.preventDefault();
    }
}

function bloquearColagemNegativa(event) {
    const texto = event.clipboardData?.getData('text') || '';
    if (texto.includes('-') || texto.includes('+') || /e/i.test(texto)) {
        event.preventDefault();
    }
}

async function request(path, options = {}) {
    try {
        const headers = options.body instanceof FormData ? {} : {'Content-Type': 'application/json'};
        const response = await fetch(API + path, {
            headers,
            credentials: 'include',
            ...options
        });

        if (!response.ok) {
            let message = 'Erro na requisição.';
            try {
                const json = await response.json();
                message = json.erro || json.message || message;
            } catch {
            }
            const requestError = new Error(message);
            requestError.status = response.status;
            throw requestError;
        }

        if (response.status === 204) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        if (error.message === 'Failed to fetch') {
            throw new Error('Não foi possível conectar ao backend. Confirme se o Spring Boot está rodando na porta 8080.');
        }
        throw error;
    }
}

function EyeIcon({closed = false}) {
    if (closed) {
        return <svg viewBox="0 0 24 24">
            <path d="M3 3l18 18"/>
            <path d="M10.6 10.6A3 3 0 0 0 13.4 13.4"/>
            <path d="M9.9 4.3A10.8 10.8 0 0 1 12 4c6.5 0 10.5 8 10.5 8a18.7 18.7 0 0 1-3.1 4.2"/>
            <path d="M6.1 6.1C3.2 8 1.5 12 1.5 12s4 7 10.5 7a10.7 10.7 0 0 0 5.1-1.3"/>
        </svg>;
    }
    return <svg viewBox="0 0 24 24">
        <path d="M1.5 12s4-7 10.5-7 10.5 7 10.5 7-4 7-10.5 7S1.5 12 1.5 12Z"/>
        <circle cx="12" cy="12" r="3"/>
    </svg>;
}

function UserIcon() {
    return <svg className="sidebar-user-icon" viewBox="0 0 24 24">
        <path d="M20 21a8 8 0 0 0-16 0"/>
        <circle cx="12" cy="7" r="4"/>
    </svg>;
}

function Avatar({usuario, className = 'user-avatar'}) {
    const [falhou, setFalhou] = useState(false);
    useEffect(() => setFalhou(false), [usuario?.id, usuario?.caminhoFoto]);
    const iniciais = String(usuario?.nome || 'U').trim().split(/\s+/).slice(0, 2)
        .map(parte => parte.charAt(0).toUpperCase()).join('') || 'U';
    if (usuario?.caminhoFoto && !falhou) {
        return <img className={className} src={`${API}/usuarios/${usuario.id}/foto?v=${encodeURIComponent(usuario.caminhoFoto)}`}
                    alt={`Foto de ${usuario.nome}`} onError={() => setFalhou(true)}/>;
    }
    return <span className={`${className} avatar-fallback`} aria-label={`Avatar de ${usuario?.nome || 'usuário'}`}>{iniciais}</span>;
}

function PasswordInput({name, value, onChange, placeholder = 'Digite sua senha', required = false}) {
    const [visible, setVisible] = useState(false);
    return (
        <div className="password-wrapper">
            <input name={name} type={visible ? 'text' : 'password'} value={value} onChange={onChange}
                   placeholder={placeholder} required={required}/>
            <button type="button" className="toggle-password" title={visible ? 'Ocultar senha' : 'Mostrar senha'}
                    onClick={() => setVisible(!visible)}>
                <EyeIcon closed={visible}/>
            </button>
        </div>
    );
}

function Badge({children, type = 'neutral'}) {
    return <span className={`badge ${type}`}>{children}</span>;
}

function StatusBadge({status}) {
    const type = status === 'CANCELADA' ? 'cancel' : status === 'FINALIZADA' ? 'ok' : status === 'EM_ANDAMENTO' ? 'admin' : status === 'PAUSADA' ? 'low' : 'neutral';
    return <Badge type={type}>{status || '-'}</Badge>;
}

function statusObraDisponiveis(statusAtual) {
    const transicoes = {
        CADASTRADA: ['EM_ANDAMENTO', 'CANCELADA'],
        EM_ANDAMENTO: ['PAUSADA', 'FINALIZADA', 'CANCELADA'],
        PAUSADA: ['EM_ANDAMENTO', 'CANCELADA'],
        FINALIZADA: [],
        CANCELADA: []
    };
    return [statusAtual || 'CADASTRADA', ...(transicoes[statusAtual] || [])];
}

function Modal({children}) {
    return <div className="modal-bg">
        <div className="modal">
            <div className="modal-content">{children}</div>
        </div>
    </div>;
}

function Drawer({title, subtitle, children, onClose}) {
    return (
        <div className="drawer-bg">
            <aside className="drawer">
                <div className="drawer-head">
                    <div>
                        <h2>{title}</h2>
                        {subtitle && <p>{subtitle}</p>}
                    </div>
                    <button type="button" className="icon-button" onClick={onClose}>×</button>
                </div>
                <div className="drawer-body">{children}</div>
            </aside>
        </div>
    );
}

function ConfirmModal({title, text, onConfirm, onCancel}) {
    return (
        <Modal>
            <div className="confirm-card">
                <h2>{title}</h2>
                <p>{text}</p>
                <div className="modal-actions">
                    <button className="danger" onClick={onConfirm}>Confirmar</button>
                    <button className="secondary" onClick={onCancel}>Cancelar</button>
                </div>
            </div>
        </Modal>
    );
}

function DataTable({headers, children}) {
    const hasRows = React.Children.count(children) > 0;
    return (
        <div className="table">
            <table>
                <thead>
                <tr>{headers.map(header => <th key={header}>{header}</th>)}</tr>
                </thead>
                <tbody>
                {hasRows ? children : <tr>
                    <td colSpan={headers.length}>
                        <div className="empty">Nenhum registro encontrado.</div>
                    </td>
                </tr>}
                </tbody>
            </table>
        </div>
    );
}

function Login({onLogin}) {
    const [form, setForm] = useState({login: '', senha: ''});
    const [error, setError] = useState('');

    function updateField(event) {
        setForm({...form, [event.target.name]: event.target.value});
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError('');
        try {
            const payload = {
                login: exigirCampoPreenchido(form.login, 'Usuário'),
                senha: exigirCampoPreenchido(form.senha, 'Senha')
            };
            const usuario = await request('/auth/login', {method: 'POST', body: JSON.stringify(payload)});
            setForm({login: '', senha: ''});
            onLogin(usuario);
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <div className="login">
            <form className="login-card" onSubmit={handleSubmit} noValidate>
                <section className="login-brand"><img src="/logo-uniao.png" alt="UniControl"/></section>
                <section className="login-form">
                    <h1>UniControl</h1>
                    <p>Sistema de Gestão de Estoque e Obras</p>
                    <div><label>Usuário</label><input name="login" autoComplete="off" placeholder="Digite seu usuário"
                                                      value={form.login} onChange={updateField} required/></div>
                    <div><label>Senha</label><PasswordInput name="senha" value={form.senha} onChange={updateField} required/>
                    </div>
                    <button>Entrar</button>
                    {error && <div className="error">{error}</div>}
                </section>
            </form>
        </div>
    );
}

function Sidebar({user, page, setPage, collapsed, setCollapsed, onLogout}) {
    const isAdmin = user?.tipoUsuario === 'ADMINISTRADOR';
    const items = [
        {id: 'dashboard', label: 'Dashboard', icon: '▦', show: true},
        {id: 'usuarios', label: 'Usuários', icon: <UserIcon/>, show: isAdmin},
        {id: 'materiais', label: 'Materiais', icon: '▣', show: true},
        {id: 'movimentacoes', label: 'Entrada / Retirada', icon: '↕', show: true},
        {id: 'obras', label: 'Obras', icon: '⌂', show: true},
        {id: 'orcamentos', label: 'Orçamentos', icon: '$', show: true}
    ];

    return (
        <aside className="sidebar">
            <div className="brand">
                <div className="profile">
                    <Avatar usuario={user} className="profile-avatar"/>
                    <div>
                        <b>{user.nome}</b>
                        <small>{isAdmin ? 'Administrador' : 'Usuário'}</small>
                    </div>
                </div>
                <button className="toggle" title="Recolher menu" onClick={() => setCollapsed(!collapsed)}>☰</button>
            </div>
            <div className="nav">
                {items.filter(item => item.show).map(item => (
                    <button key={item.id} className={page === item.id ? 'active' : ''} onClick={() => setPage(item.id)}>
                        <span className="ico">{item.icon}</span><span className="label">{item.label}</span>
                    </button>
                ))}
            </div>
            <button className="logout" onClick={onLogout}><span className="ico">⎋</span><span
                className="label">Sair</span></button>
        </aside>
    );
}

function Dashboard({data, isAdmin}) {
    const retiradas = {};
    data.movs.filter(m => m.tipo === 'SAIDA').forEach(m => {
        const nome = m.materialNome || m.material?.nome || 'Material';
        retiradas[nome] = (retiradas[nome] || 0) + (m.quantidade || 0);
    });
    const topMateriais = Object.entries(retiradas).sort((a, b) => b[1] - a[1]).slice(0, 3);
    const ultimasObras = [...data.obras].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 3);

    return (
        <>
            <div className="top">
                <div><h1>UniControl</h1><p>Controle de materiais, obras e estoque.</p></div>
                <Badge type={isAdmin ? 'admin' : 'user'}>{isAdmin ? 'Administrador' : 'Usuário comum'}</Badge></div>
            <div className="cards">
                <div className="card"><h3>Materiais</h3>
                    <div className="metric">{data.materiais.length}</div>
                </div>
                <div className="card"><h3>Estoque baixo</h3>
                    <div
                        className="metric">{data.materiais.filter(m => (m.quantidade || 0) <= (m.estoqueMinimo || 0)).length}</div>
                </div>
                <div className="card"><h3>Obras</h3>
                    <div className="metric">{data.obras.length}</div>
                </div>
            </div>
            <div className="dash-grid">
                <div className="panel"><h2>Principais materiais usados</h2>
                    <div className="list">{topMateriais.length ? topMateriais.map(([nome, qtd]) => <div
                            className="list-item" key={nome}>
                            <div><b>{nome}</b></div>
                            <Badge type="ok">{qtd} un.</Badge></div>) :
                        <div className="empty">Sem retiradas registradas.</div>}</div>
                </div>
                <div className="panel"><h2>Últimas obras</h2>
                    <div className="list">{ultimasObras.length ? ultimasObras.map(obra => <div className="list-item"
                                                                                               key={obra.id}>
                            <div><b>{obra.nomeObra}</b><small>{obra.nomeCliente || ''}</small></div>
                            <StatusBadge status={obra.status}/></div>) :
                        <div className="empty">Nenhuma obra cadastrada.</div>}</div>
                </div>
            </div>
        </>
    );
}

const emptyUser = {id: '', nome: '', login: '', senha: '', tipoUsuario: 'USUARIO', status: 'ATIVO', caminhoFoto: null, foto: null, removerFoto: false};
const emptyMaterial = {
    id: '',
    nome: '',
    marca: '',
    cor: '',
    quantidade: 0,
    estoqueMinimo: '',
    valorCusto: '',
    valorVenda: '',
    larguraPeca: '',
    comprimentoPeca: '',
    unidadeMedida: 'CM',
    areaPecaM2: null,
    descricao: ''
};
const emptyObra = {
    id: '',
    orcamentoId: '',
    nomeObra: '',
    nomeCliente: '',
    endereco: '',
    metragem: '',
    status: 'CADASTRADA',
    descricao: ''
};

function Usuarios({usuarios, currentUser, setCurrentUser, reload, showMessage, openConfirm}) {
    const [modalUser, setModalUser] = useState(null);
    const [modalError, setModalError] = useState('');
    const [deletedOpen, setDeletedOpen] = useState(false);
    const [deletedUsers, setDeletedUsers] = useState([]);
    const updateField = e => {
        const {name, value} = e.target;
        setModalError('');
        setModalUser({...modalUser, [name]: value});
    };

    function selecionarFoto(event) {
        const arquivo = event.target.files?.[0] || null;
        if (arquivo && (!['image/jpeg', 'image/png', 'image/webp'].includes(arquivo.type) || arquivo.size > 5 * 1024 * 1024)) {
            setModalError('Selecione uma imagem JPG, PNG ou WEBP de até 5 MB.');
            event.target.value = '';
            return;
        }
        setModalError('');
        setModalUser({...modalUser, foto: arquivo, removerFoto: false});
    }

    async function loadDeletedUsers() {
        try {
            const result = await request('/usuarios/excluidos');
            setDeletedUsers(result || []);
            setDeletedOpen(true);
        } catch (err) {
            showMessage(err.message, 'error');
        }
    }

    async function restoreUser(usuario) {
        try {
            await request(`/usuarios/${usuario.id}/restaurar`, {method: 'PATCH'});
            showMessage('Usuário reativado.');
            await reload();
            await loadDeletedUsers();
        } catch (err) {
            showMessage(err.message, 'error');
        }
    }

    async function saveUser(event) {
        event.preventDefault();
        setModalError('');
        try {
            const payload = {
                nome: exigirTextoComLetra(modalUser.nome, 'Nome do usuário', true),
                login: exigirTextoComLetra(modalUser.login, 'Login do usuário', true),
                senha: modalUser.id ? limparTextoLivre(modalUser.senha) : exigirCampoPreenchido(modalUser.senha, 'Senha'),
                tipoUsuario: modalUser.tipoUsuario
            };
            const method = modalUser.id ? 'PUT' : 'POST';
            const path = modalUser.id ? `/usuarios/${modalUser.id}` : '/usuarios';
            let salvo = await request(path, {method, body: JSON.stringify(payload)});
            if (modalUser.removerFoto && salvo.caminhoFoto) {
                salvo = await request(`/usuarios/${salvo.id}/foto`, {method: 'DELETE'});
            }
            if (modalUser.foto) {
                const dadosFoto = new FormData();
                dadosFoto.append('foto', modalUser.foto);
                salvo = await request(`/usuarios/${salvo.id}/foto`, {method: 'PUT', body: dadosFoto});
            }
            if (salvo.id === currentUser.id) setCurrentUser(salvo);
            setModalUser(null);
            setModalError('');
            showMessage('Usuário salvo.');
            await reload();
        } catch (err) {
            setModalError(err.message);
        }
    }

    function inativarUser(usuario) {
        openConfirm('Inativar usuário', 'Deseja realmente inativar este usuário? Ele sairá da lista principal e poderá ser reativado depois.', async () => {
            try {
                await request(`/usuarios/${usuario.id}/status?status=INATIVO`, {method: 'PATCH'});
                showMessage('Usuário inativado.');
                await reload();
            } catch (err) {
                showMessage(err.message, 'error');
            }
        });
    }

    return (
        <>
            <div className="top">
                <div><h1>Usuários</h1><p>Cadastro e controle de permissões do sistema.</p></div>
                <div className="toolbar-actions">
                    <button className="ghost" onClick={loadDeletedUsers}>Inativos</button>
                    <button onClick={() => {
                        setModalError('');
                        setModalUser(emptyUser);
                    }}>Novo usuário
                    </button>
                </div>
            </div>
            <DataTable headers={['Usuário', 'Login', 'Perfil', 'Último acesso', 'Status', 'Ações']}>
                {usuarios.map(usuario => <tr key={usuario.id}>
                    <td><div className="user-cell"><Avatar usuario={usuario}/><b>{usuario.nome}</b></div></td>
                    <td>{usuario.login}</td>
                    <td><Badge
                        type={usuario.tipoUsuario === 'ADMINISTRADOR' ? 'admin' : 'user'}>{usuario.tipoUsuario === 'ADMINISTRADOR' ? 'Administrador' : 'Usuário'}</Badge>
                    </td>
                    <td>{usuario.ultimoAcesso ? formatDateTime(usuario.ultimoAcesso) : 'Nunca acessou'}</td>
                    <td><Badge type={usuario.status === 'INATIVO' ? 'cancel' : 'ok'}>{usuario.status === 'INATIVO' ? 'Inativo' : 'Ativo'}</Badge></td>
                    <td className="actions-cell">
                        <div className="table-actions">
                            <button onClick={() => setModalUser({...usuario, senha: '', foto: null, removerFoto: false})}>Editar</button>
                            <button className="secondary" disabled={usuario.id === currentUser.id}
                                    title={usuario.id === currentUser.id ? 'Você não pode inativar a própria conta.' : ''}
                                    onClick={() => inativarUser(usuario)}>Inativar</button>
                        </div>
                    </td>
                </tr>)}
            </DataTable>
            {modalUser && <Modal><h2>{modalUser.id ? 'Editar usuário' : 'Novo usuário'}</h2>{modalError &&
                <div className="modal-error">{modalError}</div>}
                <form onSubmit={saveUser} noValidate>
                    <div className="modal-grid">
                        <div><label>Nome</label><input name="nome" value={modalUser.nome} onChange={updateField} required/></div>
                        <div><label>Login</label><input type="search" name="unicontrolNovoLogin" autoComplete="one-time-code"
                                                       data-lpignore="true" data-1p-ignore="true" value={modalUser.login}
                                                       onChange={event => {
                                                           setModalError('');
                                                           setModalUser({...modalUser, login: event.target.value});
                                                       }} required/>
                        </div>
                        <div><label>Senha</label><PasswordInput name="senha" value={modalUser.senha || ''}
                                                                onChange={updateField}
                                                                placeholder={modalUser.id ? 'Deixe em branco para manter' : 'Digite a senha'}
                                                                required={!modalUser.id}/>
                        </div>
                        <div><label>Perfil</label><select name="tipoUsuario" value={modalUser.tipoUsuario}
                                                          onChange={updateField} required>
                            <option value="USUARIO">Usuário</option>
                            <option value="ADMINISTRADOR">Administrador</option>
                        </select></div>
                        <div className="span2"><label>Foto de perfil (opcional)</label>
                            <div className="photo-editor">
                                <Avatar usuario={{...modalUser, caminhoFoto: modalUser.removerFoto ? null : modalUser.caminhoFoto}}/>
                                <div><input type="file" accept="image/jpeg,image/png,image/webp" onChange={selecionarFoto}/>
                                    <small>JPG, PNG ou WEBP, até 5 MB.</small></div>
                                {modalUser.caminhoFoto && !modalUser.removerFoto &&
                                    <button type="button" className="secondary" onClick={() => setModalUser({...modalUser, foto: null, removerFoto: true})}>Remover foto</button>}
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions">
                        <button>Salvar</button>
                        <button type="button" className="secondary" onClick={() => {
                            setModalError('');
                            setModalUser(null);
                        }}>Cancelar
                        </button>
                    </div>
                </form>
            </Modal>}
            {deletedOpen && <Drawer title="Usuários inativos" onClose={() => setDeletedOpen(false)}><DataTable
                headers={['Nome', 'Login', 'Perfil', 'Ações']}>{deletedUsers.map(usuario => <tr key={usuario.id}>
                <td>{usuario.nome}</td>
                <td>{usuario.login}</td>
                <td><Badge
                    type={usuario.tipoUsuario === 'ADMINISTRADOR' ? 'admin' : 'user'}>{usuario.tipoUsuario === 'ADMINISTRADOR' ? 'Administrador' : 'Usuário'}</Badge></td>
                <td className="actions-cell">
                    <button onClick={() => restoreUser(usuario)}>Reativar</button>
                </td>
            </tr>)}</DataTable></Drawer>}
        </>
    );
}

function Materiais({
                       materiais,
                       currentUser,
                       isAdmin,
                       reload,
                       showMessage,
                       openConfirm,
                       setPage,
                       setMaterialSelecionadoSaida
                   }) {
    const [modalMaterial, setModalMaterial] = useState(null);
    const [modalError, setModalError] = useState('');
    const [quantityNotice, setQuantityNotice] = useState(false);
    const [deletedOpen, setDeletedOpen] = useState(false);
    const [deletedMateriais, setDeletedMateriais] = useState([]);
    const [visualizarMaterial, setVisualizarMaterial] = useState(null);
    const updateField = e => {
        const {name, value} = e.target;
        setModalError('');

        if (['quantidade', 'estoqueMinimo', 'valorCusto', 'valorVenda', 'larguraPeca', 'comprimentoPeca'].includes(name)) {
            const valorLimpo = limparNumeroDigitado(value);
            if (valorLimpo === null) {
                setModalError('O valor informado não pode ser negativo.');
                return;
            }
            setModalMaterial({...modalMaterial, [name]: valorLimpo});
            return;
        }

        setModalMaterial({...modalMaterial, [name]: value});
    };

    async function loadDeletedMateriais() {
        try {
            const result = await request('/materiais/excluidos');
            setDeletedMateriais(result || []);
            setDeletedOpen(true);
        } catch (err) {
            showMessage(err.message, 'error');
        }
    }

    async function restoreMaterial(material) {
        try {
            await request(`/materiais/${material.id}/restaurar`, {method: 'PATCH'});
            showMessage('Material restaurado.');
            await reload();
            await loadDeletedMateriais();
        } catch (err) {
            showMessage(err.message, 'error');
        }
    }

    async function saveMaterial(event) {
        event.preventDefault();
        setModalError('');

        try {
            const payload = {
                ...modalMaterial,
                nome: exigirTextoComLetra(modalMaterial.nome, 'Nome do material', true),
                marca: exigirTextoComLetra(modalMaterial.marca, 'Marca do material', true),
                cor: exigirTextoComLetra(modalMaterial.cor, 'Cor do material', true),
                descricao: exigirTextoComLetra(modalMaterial.descricao, 'Descrição do material', true),
                quantidade: inteiroObrigatorioNaoNegativo(modalMaterial.quantidade, 'Quantidade'),
                estoqueMinimo: inteiroObrigatorioNaoNegativo(modalMaterial.estoqueMinimo, 'Estoque mínimo'),
                valorCusto: numeroObrigatorioNaoNegativo(modalMaterial.valorCusto, 'Valor de custo'),
                valorVenda: numeroObrigatorioNaoNegativo(modalMaterial.valorVenda, 'Valor de venda'),
                larguraPeca: numeroMaiorQueZero(modalMaterial.larguraPeca, 'Largura da peça'),
                comprimentoPeca: numeroMaiorQueZero(modalMaterial.comprimentoPeca, 'Comprimento da peça'),
                unidadeMedida: exigirCampoPreenchido(modalMaterial.unidadeMedida, 'Unidade de medida'),
                areaPecaM2: null
            };
            const acao = modalMaterial.id ? 'Salvar alterações do material' : 'Confirmar cadastro do material';
            const detalhes = `${payload.nome} — ${payload.marca} — ${payload.cor}\n` +
                `Peça: ${payload.larguraPeca} × ${payload.comprimentoPeca} ${payload.unidadeMedida}\n` +
                `Estoque mínimo: ${payload.estoqueMinimo} | Venda: R$ ${money(payload.valorVenda)}\n` +
                'Confira os dados antes de confirmar.';
            openConfirm(acao, detalhes, async () => {
                try {
                    const method = modalMaterial.id ? 'PUT' : 'POST';
                    const path = modalMaterial.id ? `/materiais/${modalMaterial.id}` : '/materiais';
                    await request(path, {method, body: JSON.stringify(payload)});
                    setModalMaterial(null);
                    setModalError('');
                    setQuantityNotice(false);
                    showMessage('Material salvo.');
                    await reload();
                } catch (err) {
                    setModalError(err.message);
                }
            });
        } catch (err) {
            setModalError(err.message);
        }
    }

    function deleteMaterial(material) {
        openConfirm('Inativar material', 'Deseja realmente inativar este material? Ele continuará no histórico e poderá ser restaurado.', async () => {
            try {
                await request(`/materiais/${material.id}`, {method: 'DELETE'});
                showMessage('Material inativado.');
                await reload();
            } catch (err) {
                showMessage(err.message, 'error');
            }
        });
    }

    function movimentar(material) {
        setMaterialSelecionadoSaida(String(material.id));
        setPage('movimentacoes');
    }

    return (
        <>
            <div className="top">
                <div><h1>Materiais</h1><p>Cadastro, consulta e controle das quantidades em estoque.</p></div>
                {isAdmin && <div className="toolbar-actions">
                    <button className="ghost" onClick={loadDeletedMateriais}>Inativos</button>
                    <button onClick={() => {
                        setModalError('');
                        setQuantityNotice(false);
                        setModalMaterial(emptyMaterial);
                    }}>Novo material
                    </button>
                </div>}</div>
            <DataTable headers={['Nome', 'Marca', 'Cor', 'Dimensão', 'Qtd', 'Status', 'Ações']}>
                {materiais.map(material => <tr key={material.id}>
                    <td>{material.nome}</td>
                    <td>{material.marca || ''}</td>
                    <td>{material.cor || ''}</td>
                    <td>{material.larguraPeca && material.comprimentoPeca ? `${material.larguraPeca} × ${material.comprimentoPeca} ${material.unidadeMedida}` : '-'}</td>
                    <td>{material.quantidade}</td>
                    <td>{material.quantidade <= 0 ?
                        <Badge type="cancel">Sem material</Badge> : material.quantidade <= material.estoqueMinimo ?
                            <Badge type="low">Estoque baixo</Badge> : <Badge type="ok">Disponível</Badge>}</td>
                    <td className="actions-cell">
                        <div className="table-actions">
                            <button className="blue" onClick={() => setVisualizarMaterial(material)}>Visualizar</button>
                            {isAdmin ? <>
                            <button onClick={() => {
                                setModalError('');
                                setQuantityNotice(false);
                                setModalMaterial({...material});
                            }}>Editar
                            </button>
                            <button className="blue" onClick={() => movimentar(material)}>Movimentar</button>
                            <button className="danger" onClick={() => deleteMaterial(material)}>Inativar</button>
                        </> : <button className="blue" onClick={() => movimentar(material)}>Registrar
                            retirada</button>}</div>
                    </td>
                </tr>)}
            </DataTable>
            {modalMaterial && <Modal>
                <h2>{modalMaterial.id ? 'Editar material' : 'Novo material'}</h2>{modalError &&
                <div className="modal-error">{modalError}</div>}
                <form onSubmit={saveMaterial} noValidate>
                    <div className="modal-grid">{['nome', 'marca', 'cor'].map(field => <div key={field}>
                        <label>{field === 'nome' ? 'Nome' : field === 'marca' ? 'Marca' : 'Cor'}</label><input
                        name={field} value={modalMaterial[field] || ''} onChange={updateField} required/></div>)}
                        <div><label>Quantidade</label><input name="quantidade" type="number" min="0" step="1"
                                                             value={modalMaterial.quantidade}
                                                             readOnly
                                                             title="Altere a quantidade de material através da aba de entradas e retiradas."
                                                             onFocus={() => setQuantityNotice(true)}
                                                             onClick={() => setQuantityNotice(true)}
                                                             onKeyDown={e => {
                                                                 setQuantityNotice(true);
                                                                 e.preventDefault();
                                                             }}
                                                             onPaste={e => {
                                                                 setQuantityNotice(true);
                                                                 e.preventDefault();
                                                             }} onChange={updateField} required/>
                            {quantityNotice && <p className="muted-note">Altere a quantidade de material através da aba de entradas e retiradas.</p>}
                        </div>
                        <div><label>Estoque mínimo</label><input name="estoqueMinimo" type="number" min="0" step="1"
                                                                 value={modalMaterial.estoqueMinimo}
                                                                 onKeyDown={bloquearTeclaNumeroInvalida}
                                                                 onPaste={bloquearColagemNegativa}
                                                                 onChange={updateField} required/></div>
                        <div><label>Valor custo</label><input name="valorCusto" type="number" min="0" step="0.01"
                                                              value={modalMaterial.valorCusto}
                                                              onKeyDown={bloquearTeclaNumeroInvalida}
                                                              onPaste={bloquearColagemNegativa} onChange={updateField} required/>
                        </div>
                        <div><label>Valor venda</label><input name="valorVenda" type="number" min="0" step="0.01"
                                                              value={modalMaterial.valorVenda}
                                                              onKeyDown={bloquearTeclaNumeroInvalida}
                                                              onPaste={bloquearColagemNegativa} onChange={updateField} required/>
                        </div>
                        <div><label>Largura da peça</label><input name="larguraPeca" type="number" min="0.0001" step="0.01"
                                                                        value={modalMaterial.larguraPeca ?? ''}
                                                                        onKeyDown={bloquearTeclaNumeroInvalida}
                                                                        onPaste={bloquearColagemNegativa}
                                                                        onChange={updateField} required/></div>
                        <div><label>Comprimento da peça</label><input name="comprimentoPeca" type="number" min="0.0001" step="0.01"
                                                                            value={modalMaterial.comprimentoPeca ?? ''}
                                                                            onKeyDown={bloquearTeclaNumeroInvalida}
                                                                            onPaste={bloquearColagemNegativa}
                                                                            onChange={updateField} required/></div>
                        <div><label>Unidade de medida</label><select name="unidadeMedida" value={modalMaterial.unidadeMedida || 'CM'} onChange={updateField} required>
                            <option value="MM">Milímetro (mm)</option><option value="CM">Centímetro (cm)</option><option value="M">Metro (m)</option>
                        </select></div>
                        <div className="span2"><label>Área de uma peça (m²)</label><input readOnly value={
                            Number(modalMaterial.larguraPeca) > 0 && Number(modalMaterial.comprimentoPeca) > 0
                                ? ((Number(modalMaterial.larguraPeca) / (modalMaterial.unidadeMedida === 'MM' ? 1000 : modalMaterial.unidadeMedida === 'CM' ? 100 : 1)) *
                                   (Number(modalMaterial.comprimentoPeca) / (modalMaterial.unidadeMedida === 'MM' ? 1000 : modalMaterial.unidadeMedida === 'CM' ? 100 : 1))).toFixed(6)
                                : ''
                        } placeholder="Calculada automaticamente pelo sistema"/></div>
                        <div className="span2"><label>Descrição</label><textarea name="descricao"
                                                                                 value={modalMaterial.descricao || ''}
                                                                                 onChange={updateField} required/></div>
                    </div>
                    <div className="modal-actions">
                        <button>Salvar</button>
                        <button type="button" className="secondary" onClick={() => {
                            setModalError('');
                            setQuantityNotice(false);
                            setModalMaterial(null);
                        }}>Cancelar
                        </button>
                    </div>
                </form>
            </Modal>}
            {visualizarMaterial && <Modal>
                <h2>Visualizar material</h2>
                <div className="list">
                    <div className="list-item"><b>Nome</b><span>{visualizarMaterial.nome || '-'}</span></div>
                    <div className="list-item"><b>Descrição</b><span>{visualizarMaterial.descricao || '-'}</span></div>
                    <div className="list-item"><b>Marca</b><span>{visualizarMaterial.marca || '-'}</span></div>
                    <div className="list-item"><b>Cor</b><span>{visualizarMaterial.cor || '-'}</span></div>
                    <div className="list-item"><b>Status</b><Badge type={visualizarMaterial.ativo === false ? 'cancel' : 'ok'}>
                        {visualizarMaterial.ativo === false ? 'INATIVO' : 'ATIVO'}
                    </Badge></div>
                    <div className="list-item"><b>Largura da peça</b><span>{visualizarMaterial.larguraPeca ?? '-'} {visualizarMaterial.unidadeMedida || ''}</span></div>
                    <div className="list-item"><b>Comprimento da peça</b><span>{visualizarMaterial.comprimentoPeca ?? '-'} {visualizarMaterial.unidadeMedida || ''}</span></div>
                    <div className="list-item"><b>Unidade de medida</b><span>{visualizarMaterial.unidadeMedida || '-'}</span></div>
                    <div className="list-item"><b>Área da peça</b><span>{visualizarMaterial.areaPecaM2 != null
                        ? `${Number(visualizarMaterial.areaPecaM2).toLocaleString('pt-BR', {maximumFractionDigits: 6})} m²` : '-'}</span></div>
                    <div className="list-item"><b>Quantidade atual</b><span>{visualizarMaterial.quantidade ?? 0}</span></div>
                    <div className="list-item"><b>Estoque mínimo</b><span>{visualizarMaterial.estoqueMinimo ?? 0}</span></div>
                    {isAdmin && <div className="list-item"><b>Valor de custo</b><span>R$ {money(visualizarMaterial.valorCusto)}</span></div>}
                    <div className="list-item"><b>Valor de venda</b><span>R$ {money(visualizarMaterial.valorVenda)}</span></div>
                    <div className="list-item"><b>Data de cadastro</b><span>{formatDateTime(visualizarMaterial.dataCadastro)}</span></div>
                </div>
                <div className="modal-actions">
                    <button type="button" className="secondary" onClick={() => setVisualizarMaterial(null)}>Fechar</button>
                </div>
            </Modal>}
            {deletedOpen && <Drawer title="Materiais inativos" onClose={() => setDeletedOpen(false)}><DataTable
                headers={['Nome', 'Marca', 'Cor', 'Qtd', 'Ações']}>{deletedMateriais.map(material => <tr
                key={material.id}>
                <td>{material.nome}</td>
                <td>{material.marca || '-'}</td>
                <td>{material.cor || '-'}</td>
                <td>{material.quantidade}</td>
                <td className="actions-cell">
                    <div className="table-actions">
                        <button className="blue" onClick={() => setVisualizarMaterial(material)}>Visualizar</button>
                        <button onClick={() => restoreMaterial(material)}>Restaurar</button>
                    </div>
                </td>
            </tr>)}</DataTable></Drawer>}
        </>
    );
}

function Obras({obras, orcamentos, isAdmin, reload, showMessage}) {
    const [modalObra, setModalObra] = useState(null);
    const [visualizarObra, setVisualizarObra] = useState(null);
    const [modalError, setModalError] = useState('');
    const orcamentosDisponiveis = orcamentos.filter(orcamento =>
        (orcamento.tipoOrcamento || 'OBRA') === 'OBRA' && orcamento.status === 'CALCULADO' && !orcamento.obraId);
    const orcamentoSelecionado = modalObra?.id
        ? modalObra.orcamentoOrigem
        : orcamentosDisponiveis.find(orcamento => String(orcamento.id) === String(modalObra?.orcamentoId));

    const updateField = e => {
        const {name, value} = e.target;
        setModalError('');
        setModalObra({...modalObra, [name]: value});
    };

    async function saveObra(event) {
        event.preventDefault();
        setModalError('');

        try {
            const payload = {
                orcamentoId: modalObra.id
                    ? (modalObra.orcamentoOrigem?.id ?? null)
                    : Number(exigirCampoPreenchido(modalObra.orcamentoId, 'Orçamento')),
                nomeObra: exigirTextoComLetra(modalObra.nomeObra, 'Nome da obra', true),
                endereco: exigirTextoComLetra(modalObra.endereco, 'Endereço', true),
                descricao: exigirTextoComLetra(modalObra.descricao, 'Descrição da obra', true),
                dataInicio: modalObra.dataInicio || null,
                status: modalObra.id ? modalObra.status : 'CADASTRADA'
            };
            const method = modalObra.id ? 'PUT' : 'POST';
            const path = modalObra.id ? `/obras/${modalObra.id}` : '/obras';
            await request(path, {method, body: JSON.stringify(payload)});
            setModalObra(null);
            setModalError('');
            showMessage('Obra salva.');
            await reload();
        } catch (err) {
            setModalError(err.message);
        }
    }

    return (
        <>
            <div className="top">
                <div><h1>Obras</h1><p>Cadastro e acompanhamento das obras da empresa.</p></div>
                {isAdmin && <div className="toolbar-actions">
                    <button onClick={() => {
                        setModalError('');
                        setModalObra({...emptyObra});
                    }}>Nova obra
                    </button>
                </div>}</div>
            <DataTable headers={['Obra', 'Cliente', 'Status', 'Metragem', 'Valor contratado', 'Montador', 'Ações']}>
                {obras.map(obra => <tr key={obra.id}>
                    <td>{obra.nomeObra}</td>
                    <td>{obra.nomeCliente}</td>
                    <td><StatusBadge status={obra.status}/></td>
                    <td>{obra.metragemM2 ?? obra.metragem ?? 0} m²</td>
                    <td>R$ {money(obra.valorContratado)}</td>
                    <td>{obra.nomeMontador || 'Não contratado'}</td>
                    <td className="actions-cell">
                        <div className="table-actions">
                            <button className="blue" onClick={() => setVisualizarObra(obra)}>Visualizar</button>
                            {isAdmin &&
                                <button onClick={() => {
                                     setModalError('');
                                     setModalObra({...obra, orcamentoId: obra.orcamentoOrigem?.id || ''});
                                }}>Editar
                                </button>
                            }</div>
                    </td>
                </tr>)}
            </DataTable>
            {modalObra && <Modal><h2>{modalObra.id ? 'Editar obra' : 'Nova obra'}</h2>{modalError &&
                <div className="modal-error">{modalError}</div>}
                <form onSubmit={saveObra} noValidate>
                    <div className="modal-grid">
                        <div className="span2"><label>Orçamento *</label><select name="orcamentoId"
                                                                                 value={modalObra.orcamentoId || ''}
                                                                                 onChange={updateField}
                                                                                 disabled={Boolean(modalObra.id)}
                                                                                 required={!modalObra.id}>
                            <option value="">Selecione pelo nome do cliente</option>
                            {!modalObra.id && orcamentosDisponiveis.map(orcamento => <option key={orcamento.id}
                                                                                           value={orcamento.id}>
                                #{orcamento.id} — {orcamento.nomeCliente} — {Number(orcamento.areaObraM2 || 0).toLocaleString('pt-BR')} m² — R$ {money(orcamento.valorTotal)}
                            </option>)}
                            {modalObra.id && modalObra.orcamentoOrigem && <option value={modalObra.orcamentoOrigem.id}>
                                #{modalObra.orcamentoOrigem.id} — {modalObra.orcamentoOrigem.nomeCliente}
                            </option>}
                        </select></div>
                        {orcamentoSelecionado ? <div className="contract-summary span2">
                            <div><span>Cliente</span><b>{orcamentoSelecionado.nomeCliente}</b></div>
                            <div><span>Material previsto</span><b>{orcamentoSelecionado.nomeMaterial}</b></div>
                            <div><span>Metragem contratada</span><b>{Number(orcamentoSelecionado.areaObraM2 || 0).toLocaleString('pt-BR')} m²</b></div>
                            <div><span>Quantidade prevista</span><b>{orcamentoSelecionado.quantidadePecas} peças</b></div>
                            <div><span>Valor contratado</span><b>R$ {money(orcamentoSelecionado.valorTotal)}</b></div>
                        </div> : modalObra.id && <p className="muted-note span2">Registro antigo sem orçamento de origem.</p>}
                        <div><label>Nome da obra</label><input name="nomeObra" value={modalObra.nomeObra}
                                                               onChange={updateField} required/></div>
                        <div><label>Endereço</label><input name="endereco" value={modalObra.endereco || ''}
                                                           onChange={updateField} required/></div>
                        <div><label>Status</label><select name="status" value={modalObra.status} onChange={updateField}
                                                          disabled={!modalObra.id} required>
                            {statusObraDisponiveis(modalObra.status).map(status => <option key={status}>{status}</option>)}
                        </select></div>
                        <div className="span2"><label>Descrição</label><textarea name="descricao"
                                                                                 value={modalObra.descricao || ''}
                                                                                 onChange={updateField} required/></div>
                    </div>
                    <div className="modal-actions">
                        <button>Salvar</button>
                        <button type="button" className="secondary" onClick={() => {
                            setModalError('');
                            setModalObra(null);
                        }}>Cancelar
                        </button>
                    </div>
                </form>
            </Modal>}
            {visualizarObra && <Modal><h2>Detalhes da Obra</h2>
                <div className="list">
                    <div className="list-item"><b>Obra</b><span>{visualizarObra.nomeObra}</span></div>
                    <div className="list-item"><b>Cliente</b><span>{visualizarObra.nomeCliente}</span></div>
                    <div className="list-item"><b>Endereço</b><span>{visualizarObra.endereco || '-'}</span></div>
                    <div className="list-item"><b>Status</b><StatusBadge status={visualizarObra.status}/></div>
                    <div className="list-item"><b>Metragem</b><span>{visualizarObra.metragemM2 ?? visualizarObra.metragem ?? 0} m²</span></div>
                    <div className="list-item"><b>Valor contratado</b><span>R$ {money(visualizarObra.valorContratado)}</span></div>
                    <div className="list-item"><b>Montador</b><span>{visualizarObra.nomeMontador || 'Não contratado'}</span></div>
                    <div className="list-item"><b>Valor do montador</b><span>{visualizarObra.valorMontador != null ? `R$ ${money(visualizarObra.valorMontador)}` : '-'}</span></div>
                    <div className="list-item"><b>Finalização</b><span>{formatDateTime(visualizarObra.dataFinalizacao)}</span></div>
                    <div className="list-item"><b>Descrição</b><span>{visualizarObra.descricao || '-'}</span></div>
                    <div className="list-item"><b>Orçamento de origem</b><span>{visualizarObra.orcamentoOrigem ? `#${visualizarObra.orcamentoOrigem.id}` : 'Registro antigo sem orçamento'}</span></div>
                    {visualizarObra.orcamentoOrigem && <>
                        <div className="list-item"><b>Material previsto</b><span>{visualizarObra.orcamentoOrigem.nomeMaterial}</span></div>
                        <div className="list-item"><b>Quantidade prevista</b><span>{visualizarObra.orcamentoOrigem.quantidadePecas} peças</span></div>
                        <div className="list-item"><b>Valor contratado</b><span>R$ {money(visualizarObra.orcamentoOrigem.valorTotal)}</span></div>
                    </>}
                </div>
                <div className="modal-actions">
                    <button className="secondary" onClick={() => setVisualizarObra(null)}>Fechar</button>
                </div>
            </Modal>}
        </>
    );
}

function Movimentacoes({
                           materiais,
                           obras,
                           movs,
                           currentUser,
                           isAdmin,
                           reload,
                           showMessage,
                           materialSelecionadoSaida,
                           setMaterialSelecionadoSaida
                       }) {
    const [form, setForm] = useState({
        materialId: '',
        tipo: isAdmin ? 'ENTRADA' : 'SAIDA',
        quantidade: 1,
        obraId: '',
        observacao: ''
    });
    const [modalMov, setModalMov] = useState(null);
    const [modalError, setModalError] = useState('');
    const [movimentacaoBloqueada, setMovimentacaoBloqueada] = useState('');
    const obrasDisponiveis = obras.filter(obra =>
        ['CADASTRADA', 'EM_ANDAMENTO', 'PAUSADA'].includes(obra.status));

    useEffect(() => {
        if (materialSelecionadoSaida) {
            setForm(prev => ({...prev, materialId: materialSelecionadoSaida, tipo: 'SAIDA'}));
            setMaterialSelecionadoSaida(null);
        }
    }, [materialSelecionadoSaida, setMaterialSelecionadoSaida]);

    useEffect(() => {
        setForm(prev => ({...prev, tipo: isAdmin ? prev.tipo : 'SAIDA'}));
    }, [isAdmin]);

    function updateField(event) {
        const {name, value} = event.target;
        if (name === 'quantidade') {
            const valorLimpo = limparNumeroDigitado(value);
            if (valorLimpo === null) return;
            setForm({...form, [name]: valorLimpo});
            return;
        }
        setForm({...form, [name]: value});
    }

    async function saveMov(event) {
        event.preventDefault();
        try {
            const payload = {
                materialId: Number(exigirCampoPreenchido(form.materialId, 'Material')),
                obraId: form.obraId ? Number(form.obraId) : null,
                tipo: exigirCampoPreenchido(form.tipo, 'Tipo'),
                quantidade: inteiroMaiorQueZero(form.quantidade, 'Quantidade'),
                observacao: exigirTextoComLetra(form.observacao, 'Observação', true)
            };
            await request('/materiais/movimentacoes', {method: 'POST', body: JSON.stringify(payload)});
            showMessage('Movimentação registrada.');
            setForm({materialId: '', tipo: isAdmin ? 'ENTRADA' : 'SAIDA', quantidade: 1, obraId: '', observacao: ''});
            await reload();
        } catch (err) {
            if (err.message?.startsWith('Não é possível registrar uma movimentação para esta obra porque ela está com o status ')) {
                setMovimentacaoBloqueada(err.message);
                setForm(prev => ({...prev, obraId: ''}));
                try {
                    await reload();
                } catch (reloadError) {
                    showMessage(reloadError.message, 'error');
                }
                return;
            }
            showMessage(err.message, 'error');
        }
    }

    async function saveMovEdit(event) {
        event.preventDefault();
        setModalError('');
        try {
            const payload = {
                quantidade: inteiroMaiorQueZero(modalMov.quantidade, 'Quantidade'),
                observacao: exigirTextoComLetra(modalMov.observacao, 'Observação', true)
            };
            await request(`/materiais/movimentacoes/${modalMov.id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            setModalMov(null);
            showMessage('Movimentação atualizada e estoque recalculado.');
            await reload();
        } catch (err) {
            setModalError(err.message);
        }
    }

    function canEdit(mov) {
        return isAdmin || (mov.tipo === 'SAIDA' && mov.usuarioId === currentUser.id);
    }

    return (
        <>
            <div className="top">
                <div><h1>Entrada / Retirada</h1><p>Registro de movimentações dos materiais em estoque.</p></div>
            </div>
            <div className="split">
                <form className="panel" onSubmit={saveMov} noValidate>
                    <div className="grid">
                        <div className="span2"><label>Material</label><select name="materialId" required
                                                                              value={form.materialId}
                                                                              onChange={updateField}>
                            <option value="">Selecione</option>
                            {materiais.map(material => <option key={material.id} value={material.id}>{material.nome} -
                                estoque {material.quantidade}</option>)}</select></div>
                        <div><label>Tipo</label><select name="tipo" value={form.tipo} onChange={updateField} required>{isAdmin &&
                            <option>ENTRADA</option>}
                            <option>SAIDA</option>
                        </select></div>
                        <div><label>Quantidade</label><input name="quantidade" min="1" step="1" type="number"
                                                             value={form.quantidade}
                                                             onKeyDown={bloquearTeclaNumeroInvalida}
                                                             onPaste={bloquearColagemNegativa} onChange={updateField} required/>
                        </div>
                        <div className="span2"><label>Obra</label><select name="obraId" value={form.obraId}
                                                                          onChange={updateField}>
                            <option value="">Sem vínculo</option>
                            {obrasDisponiveis.map(obra => <option key={obra.id} value={obra.id}>{obra.nomeObra}</option>)}</select>
                        </div>
                        <div className="span2"><label>Observação</label><textarea name="observacao"
                                                                                  value={form.observacao}
                                                                                  onChange={updateField} required/></div>
                    </div>
                    <div className="actions">
                        <button>Registrar movimentação</button>
                    </div>
                </form>
                <div className="panel"><h2>Histórico</h2>
                    <div className="history">{movs.length ? [...movs]
                        .sort((a, b) => new Date(b.dataMovimentacao || 0) - new Date(a.dataMovimentacao || 0))
                        .map(mov => <div key={mov.id}>
                            <div className="history-head">
                                <b>{mov.tipo} — {mov.materialNome || ''} — {mov.quantidade} un.</b>
                                {canEdit(mov) && <button type="button" onClick={() => {
                                    setModalError('');
                                    setModalMov({
                                        id: mov.id,
                                        tipo: mov.tipo,
                                        materialNome: mov.materialNome || '',
                                        quantidade: mov.quantidade,
                                        observacao: mov.observacao || ''
                                    });
                                }}>Editar</button>}
                            </div>
                            <small>Registrado por: {mov.usuarioNome || '-'} | {formatDateTime(mov.dataMovimentacao)}</small>
                            {mov.obraNome && <small>Obra: {mov.obraNome}</small>}
                            <small>Observação: {mov.observacao || '-'}</small>
                            {mov.dataUltimaAlteracao ?
                                <small>Última alteração: {formatDateTime(mov.dataUltimaAlteracao)}</small> :
                                <small>Sem alterações</small>}
                        </div>) : <div className="empty">Nenhuma movimentação registrada.</div>}</div>
                </div>
            </div>
            {modalMov && <Modal>
                <h2>Editar movimentação</h2>
                <p className="muted-note">O tipo e o material são preservados. O estoque será recalculado automaticamente.</p>
                {modalError && <div className="modal-error">{modalError}</div>}
                <form onSubmit={saveMovEdit} noValidate>
                    <div className="modal-grid">
                        <div><label>Material</label><input value={modalMov.materialNome} readOnly/></div>
                        <div><label>Tipo</label><input value={modalMov.tipo} readOnly/></div>
                        <div><label>Quantidade</label><input type="number" min="1" step="1"
                                                              value={modalMov.quantidade}
                                                              onKeyDown={bloquearTeclaNumeroInvalida}
                                                              onPaste={bloquearColagemNegativa}
                                                              onChange={e => setModalMov({...modalMov, quantidade: e.target.value})}
                                                              required/></div>
                        <div className="span2"><label>Observação</label><textarea value={modalMov.observacao}
                                                                                   onChange={e => setModalMov({...modalMov, observacao: e.target.value})}
                                                                                   required/></div>
                    </div>
                    <div className="modal-actions">
                        <button>Salvar alteração</button>
                        <button type="button" className="secondary" onClick={() => setModalMov(null)}>Cancelar</button>
                    </div>
                </form>
            </Modal>}
            {movimentacaoBloqueada && <Modal>
                <h2>Movimentação não permitida</h2>
                <p>{movimentacaoBloqueada}</p>
                <div className="modal-actions">
                    <button type="button" onClick={() => setMovimentacaoBloqueada('')}>Entendi</button>
                </div>
            </Modal>}
        </>
    );
}


function novoOrcamentoObraVazio() {
    return {id: '', tipoOrcamento: 'OBRA', nomeCliente: '', larguraObraM: '', comprimentoObraM: '', materialId: ''};
}

function novoOrcamentoMontadorVazio() {
    return {id: '', tipoOrcamento: 'MONTADOR', obraId: '', nomeMontador: '', valorPorMetroQuadrado: ''};
}

function StatusOrcamentoBadge({status}) {
    const type = status === 'CONTRATADO' ? 'ok' : status === 'NAO_CONTRATADO' ? 'cancel' : 'admin';
    return <Badge type={type}>{status || 'LEGADO'}</Badge>;
}

function Orcamentos({orcamentos, materiais, obras, reload, showMessage, openConfirm}) {
    const [modalOrcamento, setModalOrcamento] = useState(null);
    const [modalError, setModalError] = useState('');
    const [calculo, setCalculo] = useState(null);
    const [visualizar, setVisualizar] = useState(null);
    const materiaisValidos = materiais.filter(material =>
        material.ativo !== false && Number(material.larguraPeca) > 0 &&
        Number(material.comprimentoPeca) > 0 && material.unidadeMedida && Number(material.areaPecaM2) > 0);
    const materialSelecionado = materiais.find(material =>
        String(material.id) === String(modalOrcamento?.materialId));

    const obraSelecionada = obras.find(obra => String(obra.id) === String(modalOrcamento?.obraId));

    function abrirNovo(tipo = 'OBRA') {
        setModalError('');
        setCalculo(null);
        setModalOrcamento(tipo === 'MONTADOR' ? novoOrcamentoMontadorVazio() : novoOrcamentoObraVazio());
    }

    function abrirEdicao(orcamento) {
        setModalError('');
        setCalculo(null);
        setModalOrcamento(orcamento.tipoOrcamento === 'MONTADOR' ? {
            id: orcamento.id, tipoOrcamento: 'MONTADOR', obraId: String(orcamento.obraId || ''),
            nomeMontador: orcamento.nomeMontador || '', valorPorMetroQuadrado: orcamento.valorPorMetroQuadrado ?? ''
        } : {
            id: orcamento.id, tipoOrcamento: 'OBRA', nomeCliente: orcamento.nomeCliente || '',
            larguraObraM: orcamento.larguraObraM ?? '', comprimentoObraM: orcamento.comprimentoObraM ?? '',
            materialId: orcamento.materialId ? String(orcamento.materialId) : ''
        });
    }

    function updateField(event) {
        const {name, value} = event.target;
        setModalError('');
        setCalculo(null);
        if (['larguraObraM', 'comprimentoObraM', 'valorPorMetroQuadrado'].includes(name)) {
            const valorLimpo = limparNumeroDigitado(value);
            if (valorLimpo === null) {
                setModalError('As dimensões da obra devem ser maiores que zero.');
                return;
            }
            setModalOrcamento({...modalOrcamento, [name]: valorLimpo});
            return;
        }
        setModalOrcamento({...modalOrcamento, [name]: value});
    }

    function montarPayload() {
        if (modalOrcamento.tipoOrcamento === 'MONTADOR') {
            return {
                obraId: Number(exigirCampoPreenchido(modalOrcamento.obraId, 'Obra')),
                nomeMontador: exigirTextoComLetra(modalOrcamento.nomeMontador, 'Nome do montador', true),
                valorPorMetroQuadrado: numeroMaiorQueZero(modalOrcamento.valorPorMetroQuadrado, 'Valor por m²')
            };
        }
        return {
            nomeCliente: exigirTextoComLetra(modalOrcamento.nomeCliente, 'Nome do cliente', true),
            larguraObraM: numeroMaiorQueZero(modalOrcamento.larguraObraM, 'Largura da obra'),
            comprimentoObraM: numeroMaiorQueZero(modalOrcamento.comprimentoObraM, 'Comprimento da obra'),
            materialId: Number(exigirCampoPreenchido(modalOrcamento.materialId, 'Material'))
        };
    }

    async function calcularOrcamento() {
        setModalError('');
        try {
            const endpoint = modalOrcamento.tipoOrcamento === 'MONTADOR'
                ? '/orcamentos/montador/calcular' : '/orcamentos/calcular';
            const resultado = await request(endpoint, {
                method: 'POST', body: JSON.stringify(montarPayload())
            });
            setCalculo(resultado);
        } catch (err) {
            setCalculo(null);
            setModalError(err.message);
        }
    }

    async function salvarOrcamento(event) {
        event.preventDefault();
        setModalError('');
        try {
            if (!calculo) throw new Error('Calcule o orçamento antes de salvar.');
            const montador = modalOrcamento.tipoOrcamento === 'MONTADOR';
            const path = montador
                ? (modalOrcamento.id ? '/orcamentos/montador/' + modalOrcamento.id : '/orcamentos/montador')
                : (modalOrcamento.id ? '/orcamentos/' + modalOrcamento.id : '/orcamentos');
            const salvo = await request(path, {
                method: modalOrcamento.id ? 'PUT' : 'POST',
                body: JSON.stringify(montarPayload())
            });
            setModalOrcamento(null);
            setCalculo(null);
            showMessage('Orçamento #' + salvo.id + ' salvo como CALCULADO. O estoque não foi alterado.');
            await reload();
        } catch (err) {
            setModalError(err.message);
        }
    }

    function marcarNaoContratado(orcamento) {
        openConfirm('Marcar como não contratado',
            'O orçamento de ' + (orcamento.nomeMontador || orcamento.nomeCliente) + ' continuará no histórico.',
            async () => {
                try {
                    await request('/orcamentos/' + orcamento.id + '/status', {
                        method: 'PATCH', body: JSON.stringify({status: 'NAO_CONTRATADO'})
                    });
                    showMessage('Orçamento marcado como NÃO CONTRATADO.');
                    await reload();
                } catch (err) {
                    showMessage(err.message, 'error');
                }
            });
    }

    function contratarMontador(orcamento) {
        openConfirm('Confirmar contratação do montador',
            `Contratar ${orcamento.nomeMontador} para a obra ${orcamento.nomeObra} por R$ ${money(orcamento.valorTotal)}? Os dados serão gravados na obra.`,
            async () => {
                try {
                    await request('/orcamentos/' + orcamento.id + '/status', {
                        method: 'PATCH', body: JSON.stringify({status: 'CONTRATADO'})
                    });
                    showMessage('Montador contratado e dados atualizados na obra.');
                    await reload();
                } catch (err) {
                    showMessage(err.message, 'error');
                }
            });
    }

    return (
        <>
            <div className="top">
                <div><h1>Orçamentos</h1><p>Previsões para obras e contratação de montadores, sem movimentar o estoque.</p></div>
                <div className="toolbar-actions"><button onClick={() => abrirNovo('OBRA')}>Novo de obra</button>
                    <button className="blue" onClick={() => abrirNovo('MONTADOR')}>Novo de montador</button></div>
            </div>
            <DataTable headers={['Tipo', 'Cliente / montador', 'Data', 'Obra', 'Valor total', 'Status', 'Ações']}>
                {orcamentos.map(orcamento => <tr key={orcamento.id}>
                    <td><Badge type={orcamento.tipoOrcamento === 'MONTADOR' ? 'user' : 'admin'}>{orcamento.tipoOrcamento || 'OBRA'}</Badge></td>
                    <td><b>{orcamento.tipoOrcamento === 'MONTADOR' ? orcamento.nomeMontador : (orcamento.nomeCliente || 'Registro legado')}</b><small className="table-subtitle">#{orcamento.id}</small></td>
                    <td>{formatDateTime(orcamento.dataCriacao)}</td>
                    <td>{orcamento.nomeObra || (orcamento.tipoOrcamento === 'OBRA' ? 'Ainda não vinculada' : '-')}</td>
                    <td><b>R$ {money(orcamento.valorTotal)}</b></td>
                    <td><StatusOrcamentoBadge status={orcamento.status}/></td>
                    <td className="actions-cell"><div className="table-actions">
                        <button className="blue" onClick={() => setVisualizar(orcamento)}>Visualizar</button>
                        {orcamento.status === 'CALCULADO' && (orcamento.tipoOrcamento === 'MONTADOR' || !orcamento.obraId) && <>
                            <button onClick={() => abrirEdicao(orcamento)}>Recalcular</button>
                            {orcamento.tipoOrcamento === 'MONTADOR' &&
                                <button onClick={() => contratarMontador(orcamento)}>Contratar</button>}
                            <button className="secondary" onClick={() => marcarNaoContratado(orcamento)}>Não contratado</button>
                        </>}
                    </div></td>
                </tr>)}
            </DataTable>

            {modalOrcamento && <Modal>
                <h2>{modalOrcamento.id ? 'Recalcular orçamento #' + modalOrcamento.id : `Novo orçamento de ${modalOrcamento.tipoOrcamento === 'MONTADOR' ? 'montador' : 'obra'}`}</h2>
                <p className="muted-note">Orçamentos são previsões e não movimentam o estoque.</p>
                {modalError && <div className="modal-error">{modalError}</div>}
                <form onSubmit={salvarOrcamento} noValidate>
                    {modalOrcamento.tipoOrcamento === 'OBRA' ? <>
                    <h3>Dados do cliente</h3>
                    <div className="modal-grid">
                        <div className="span2"><label>Nome do cliente *</label><input name="nomeCliente"
                            value={modalOrcamento.nomeCliente} onChange={updateField} required/></div>
                    </div>
                    <h3>Dimensões da obra</h3>
                    <div className="modal-grid">
                        <div><label>Largura (m) *</label><input name="larguraObraM" type="number" min="0.0001" step="0.01"
                            value={modalOrcamento.larguraObraM} onKeyDown={bloquearTeclaNumeroInvalida}
                            onPaste={bloquearColagemNegativa} onChange={updateField} required/></div>
                        <div><label>Comprimento (m) *</label><input name="comprimentoObraM" type="number" min="0.0001" step="0.01"
                            value={modalOrcamento.comprimentoObraM} onKeyDown={bloquearTeclaNumeroInvalida}
                            onPaste={bloquearColagemNegativa} onChange={updateField} required/></div>
                        <div className="span2"><label>Área calculada (m²)</label><input readOnly value={
                            Number(modalOrcamento.larguraObraM) > 0 && Number(modalOrcamento.comprimentoObraM) > 0
                                ? (Number(modalOrcamento.larguraObraM) * Number(modalOrcamento.comprimentoObraM)).toFixed(4) : ''
                        }/></div>
                    </div>
                    <h3>Material</h3>
                    <div className="modal-grid">
                        <div className="span2"><label>Selecionar material *</label><select name="materialId"
                            value={modalOrcamento.materialId} onChange={updateField} required>
                            <option value="">Selecione um material com dimensões cadastradas</option>
                            {materiaisValidos.map(material => <option key={material.id} value={material.id}>{material.nome}</option>)}
                        </select></div>
                    </div>
                    {materialSelecionado && <div className="contract-summary">
                        <div><span>Material</span><b>{materialSelecionado.nome}</b></div>
                        <div><span>Dimensão da peça</span><b>{materialSelecionado.larguraPeca} × {materialSelecionado.comprimentoPeca} {materialSelecionado.unidadeMedida}</b></div>
                        <div><span>Área da peça</span><b>{materialSelecionado.areaPecaM2} m²</b></div>
                        <div><span>Valor unitário</span><b>R$ {money(materialSelecionado.valorVenda)}</b></div>
                    </div>}
                    {materiaisValidos.length < materiais.length && <p className="muted-note">
                        Materiais sem dimensões não ficam disponíveis. Atualize o cadastro antes de usá-los.
                    </p>}
                    </> : <>
                        <h3>Obra e montador</h3>
                        <div className="modal-grid">
                            <div className="span2"><label>Obra *</label><select name="obraId" value={modalOrcamento.obraId}
                                onChange={updateField} disabled={Boolean(modalOrcamento.id)} required>
                                <option value="">Selecione uma obra</option>
                                {obras.filter(obra => !obra.orcamentoMontador || String(obra.id) === String(modalOrcamento.obraId))
                                    .map(obra => <option key={obra.id} value={obra.id}>{obra.nomeObra} — {obra.nomeCliente}</option>)}
                            </select></div>
                            <div><label>Nome do montador *</label><input name="nomeMontador" value={modalOrcamento.nomeMontador}
                                onChange={updateField} required/></div>
                            <div><label>Valor por m² (R$) *</label><input name="valorPorMetroQuadrado" type="number" min="0.01" step="0.01"
                                value={modalOrcamento.valorPorMetroQuadrado} onKeyDown={bloquearTeclaNumeroInvalida}
                                onPaste={bloquearColagemNegativa} onChange={updateField} required/></div>
                            <div className="span2"><label>Metragem da obra (m²)</label><input readOnly value={obraSelecionada?.metragemM2 ?? obraSelecionada?.metragem ?? ''}/></div>
                        </div>
                        {obraSelecionada && <div className="contract-summary">
                            <div><span>Obra</span><b>{obraSelecionada.nomeObra}</b></div>
                            <div><span>Cliente</span><b>{obraSelecionada.nomeCliente}</b></div>
                            <div><span>Metragem</span><b>{obraSelecionada.metragemM2 ?? obraSelecionada.metragem} m²</b></div>
                            <div><span>Valor previsto</span><b>R$ {money(Number((obraSelecionada.metragemM2 ?? obraSelecionada.metragem) || 0) * Number(modalOrcamento.valorPorMetroQuadrado || 0))}</b></div>
                        </div>}
                    </>}
                    <div className="modal-actions">
                        <button type="button" onClick={calcularOrcamento}>Calcular</button>
                        <button type="button" className="secondary" onClick={() => setModalOrcamento(null)}>Cancelar</button>
                    </div>
                    {calculo && <div className="budget-result">
                        <h3>Resultado do cálculo</h3>
                        {modalOrcamento.tipoOrcamento === 'OBRA' ? <div className="contract-summary">
                            <div><span>Cliente</span><b>{calculo.nomeCliente}</b></div>
                            <div><span>Material</span><b>{calculo.nomeMaterial}</b></div>
                            <div><span>Dimensão da peça</span><b>{calculo.itens?.[0] ? `${calculo.itens[0].larguraPeca} × ${calculo.itens[0].comprimentoPeca} ${calculo.itens[0].unidadeMedida}` : `${calculo.larguraPecaCm} cm × ${calculo.comprimentoPecaCm} cm`}</b></div>
                            <div><span>Área da peça</span><b>{calculo.areaPecaM2} m²</b></div>
                            <div><span>Dimensão da obra</span><b>{calculo.larguraObraM} m × {calculo.comprimentoObraM} m</b></div>
                            <div><span>Área total</span><b>{calculo.areaObraM2} m²</b></div>
                            <div><span>Quantidade necessária</span><b>{calculo.quantidadePecas} peças</b></div>
                            <div><span>Valor unitário</span><b>R$ {money(calculo.valorUnitario)}</b></div>
                            <div><span>Valor total da obra</span><b>R$ {money(calculo.valorTotal)}</b></div>
                        </div> : <div className="contract-summary">
                            <div><span>Obra</span><b>{calculo.nomeObra}</b></div>
                            <div><span>Montador</span><b>{calculo.nomeMontador}</b></div>
                            <div><span>Metragem</span><b>{calculo.metragemReferenciaM2} m²</b></div>
                            <div><span>Valor por m²</span><b>R$ {money(calculo.valorPorMetroQuadrado)}</b></div>
                            <div><span>Valor total</span><b>R$ {money(calculo.valorTotal)}</b></div>
                        </div>}
                        <div className="modal-actions"><button>Salvar orçamento</button></div>
                    </div>}
                </form>
            </Modal>}

            {visualizar && <Modal>
                <h2>{visualizar.nomeMontador || visualizar.nomeCliente || 'Orçamento'} <small>#{visualizar.id}</small></h2>
                <div className="list">
                    <div className="list-item"><b>Tipo</b><span>{visualizar.tipoOrcamento}</span></div>
                    {visualizar.tipoOrcamento === 'OBRA' ? <>
                    <div className="list-item"><b>Material</b><span>{visualizar.nomeMaterial || '-'}</span></div>
                    <div className="list-item"><b>Dimensão da peça</b><span>{visualizar.itens?.[0] ? `${visualizar.itens[0].larguraPeca} × ${visualizar.itens[0].comprimentoPeca} ${visualizar.itens[0].unidadeMedida}` : `${visualizar.larguraPecaCm || '-'} cm × ${visualizar.comprimentoPecaCm || '-'} cm`}</span></div>
                    <div className="list-item"><b>Área da peça</b><span>{visualizar.areaPecaM2 || '-'} m²</span></div>
                    <div className="list-item"><b>Dimensão da obra</b><span>{visualizar.larguraObraM || '-'} m × {visualizar.comprimentoObraM || '-'} m</span></div>
                    <div className="list-item"><b>Área total</b><span>{visualizar.areaObraM2 || '-'} m²</span></div>
                    <div className="list-item"><b>Quantidade</b><span>{visualizar.quantidadePecas || '-'} peças</span></div>
                    <div className="list-item"><b>Valor unitário usado</b><span>R$ {money(visualizar.valorUnitario)}</span></div>
                    </> : <>
                        <div className="list-item"><b>Obra</b><span>{visualizar.nomeObra || '-'}</span></div>
                        <div className="list-item"><b>Cliente</b><span>{visualizar.nomeCliente || '-'}</span></div>
                        <div className="list-item"><b>Montador</b><span>{visualizar.nomeMontador || '-'}</span></div>
                        <div className="list-item"><b>Metragem</b><span>{visualizar.metragemReferenciaM2 || '-'} m²</span></div>
                        <div className="list-item"><b>Valor por m²</b><span>R$ {money(visualizar.valorPorMetroQuadrado)}</span></div>
                    </>}
                    <div className="list-item"><b>Valor total</b><span>R$ {money(visualizar.valorTotal)}</span></div>
                    <div className="list-item"><b>Status</b><StatusOrcamentoBadge status={visualizar.status}/></div>
                </div>
                <div className="modal-actions"><button className="secondary" onClick={() => setVisualizar(null)}>Fechar</button></div>
            </Modal>}
        </>
    );
}


function App() {
    const [user, setUser] = useState(null);
    const [authReady, setAuthReady] = useState(false);
    const [page, setPage] = useState('dashboard');
    const [collapsed, setCollapsed] = useState(false);
    const [materialSelecionadoSaida, setMaterialSelecionadoSaida] = useState(null);
    const [message, setMessage] = useState('');
    const [messageType, setMessageType] = useState('success');
    const [confirm, setConfirm] = useState(null);
    const emptyData = {usuarios: [], materiais: [], obras: [], movs: [], orcamentos: []};
    const [data, setData] = useState(emptyData);
    const isAdmin = user?.tipoUsuario === 'ADMINISTRADOR';

    function showMessage(text, type = 'success') {
        setMessage(text);
        setMessageType(type);
        setTimeout(() => setMessage(''), 5200);
    }

    async function loadAll() {
        const [materiais, obras, movs, orcamentos] = await Promise.allSettled([
            request('/materiais'),
            request('/obras'),
            request('/materiais/movimentacoes'),
            request('/orcamentos')
        ]);
        let usuarios = {status: 'fulfilled', value: []};
        if (isAdmin) {
            [usuarios] = await Promise.allSettled([request('/usuarios')]);
        }
        setData(anterior => ({
            materiais: materiais.status === 'fulfilled' ? materiais.value : anterior.materiais,
            obras: obras.status === 'fulfilled' ? obras.value : anterior.obras,
            movs: movs.status === 'fulfilled' ? movs.value : anterior.movs,
            orcamentos: orcamentos.status === 'fulfilled' ? orcamentos.value : anterior.orcamentos,
            usuarios: usuarios.status === 'fulfilled' ? usuarios.value : anterior.usuarios
        }));
        const falha = [materiais, obras, movs, orcamentos, usuarios]
            .find(resultado => resultado.status === 'rejected');
        if (falha) throw falha.reason;
    }

    useEffect(() => {
        request('/auth/me')
            .then(usuario => setUser(usuario))
            .catch(() => setUser(null))
            .finally(() => setAuthReady(true));
    }, []);

    useEffect(() => {
        if (user) loadAll().catch(err => {
            if (err.status === 401) setUser(null);
            showMessage(err.message, 'error');
        });
    }, [user]);

    async function logout() {
        try {
            await request('/auth/logout', {method: 'POST'});
        } catch {
            // A sessão pode já ter expirado; o estado local ainda deve ser limpo.
        }
        setUser(null);
        setData(emptyData);
        setPage('dashboard');
    }

    function openConfirm(title, text, onConfirm) {
        setConfirm({title, text, onConfirm});
    }

    async function confirmAction() {
        const action = confirm.onConfirm;
        setConfirm(null);
        await action();
    }

    if (!authReady) return <div className="login"><div className="login-card"><section className="login-form"><h1>UniControl</h1><p>Carregando sessão...</p></section></div></div>;
    if (!user) return <Login onLogin={setUser}/>;

    return (
        <div className={`shell ${collapsed ? 'collapsed' : ''}`}>
            <Sidebar user={user} page={page} setPage={setPage} collapsed={collapsed} setCollapsed={setCollapsed}
                     onLogout={logout}/>
            <main>
                {message && <div className={messageType}>{message}</div>}
                {page === 'dashboard' && <Dashboard data={data} isAdmin={isAdmin}/>}
                {page === 'usuarios' && isAdmin &&
                    <Usuarios usuarios={data.usuarios} currentUser={user} reload={loadAll} showMessage={showMessage}
                              setCurrentUser={setUser} openConfirm={openConfirm}/>}
                {page === 'materiais' &&
                    <Materiais materiais={data.materiais} currentUser={user} isAdmin={isAdmin} reload={loadAll}
                               showMessage={showMessage} openConfirm={openConfirm} setPage={setPage}
                               setMaterialSelecionadoSaida={setMaterialSelecionadoSaida}/>}
                {page === 'movimentacoes' &&
                    <Movimentacoes materiais={data.materiais} obras={data.obras} movs={data.movs} currentUser={user}
                                   isAdmin={isAdmin} reload={loadAll} showMessage={showMessage}
                                   materialSelecionadoSaida={materialSelecionadoSaida}
                                   setMaterialSelecionadoSaida={setMaterialSelecionadoSaida}/>}
                {page === 'obras' && <Obras obras={data.obras} orcamentos={data.orcamentos} isAdmin={isAdmin}
                                             reload={loadAll} showMessage={showMessage}/>}
                {page === 'orcamentos' && <Orcamentos orcamentos={data.orcamentos} materiais={data.materiais}
                                                        obras={data.obras}
                                                        reload={loadAll} showMessage={showMessage}
                                                        openConfirm={openConfirm}/>}
                {confirm && <ConfirmModal title={confirm.title} text={confirm.text} onConfirm={confirmAction}
                                          onCancel={() => setConfirm(null)}/>}
            </main>
        </div>
    );
}

createRoot(document.getElementById('root')).render(<App/>);
