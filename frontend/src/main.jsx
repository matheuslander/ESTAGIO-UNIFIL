import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

const API = window.API_URL || "http://localhost:8080/api";

function money(value) {
  return Number(value || 0)
    .toFixed(2)
    .replace(".", ",");
}

function normalizarTexto(texto) {
  return String(texto || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "")
    .trim();
}

function materialEhSemelhante(a, b) {
  const nomeA = normalizarTexto(a?.nome);
  const nomeB = normalizarTexto(b?.nome);
  const marcaA = normalizarTexto(a?.marca);
  const marcaB = normalizarTexto(b?.marca);
  const corA = normalizarTexto(a?.cor);
  const corB = normalizarTexto(b?.cor);
  const descricaoA = normalizarTexto(a?.descricao);
  const descricaoB = normalizarTexto(b?.descricao);

  if (!nomeA || !nomeB || nomeA !== nomeB) return false;
  if (marcaA && marcaB && marcaA !== marcaB) return false;

  const mesmaCorPreenchida = !!corA && !!corB && corA === corB;
  const mesmaDescricaoPreenchida =
    !!descricaoA && !!descricaoB && descricaoA === descricaoB;

  return mesmaCorPreenchida || mesmaDescricaoPreenchida;
}

async function request(path, options = {}) {
  try {
    const response = await fetch(API + path, {
      headers: { "Content-Type": "application/json" },
      ...options,
    });

    if (!response.ok) {
      let message = "Erro na requisição.";
      try {
        const json = await response.json();
        message = json.erro || json.message || message;
      } catch {}
      throw new Error(message);
    }

    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  } catch (error) {
    if (error.message === "Failed to fetch") {
      throw new Error(
        "Não foi possível conectar ao backend. Confirme se o Spring Boot está rodando na porta 8080.",
      );
    }
    throw error;
  }
}

function EyeIcon({ closed = false }) {
  if (closed) {
    return (
      <svg viewBox="0 0 24 24">
        <path d="M3 3l18 18" />
        <path d="M10.6 10.6A3 3 0 0 0 13.4 13.4" />
        <path d="M9.9 4.3A10.8 10.8 0 0 1 12 4c6.5 0 10.5 8 10.5 8a18.7 18.7 0 0 1-3.1 4.2" />
        <path d="M6.1 6.1C3.2 8 1.5 12 1.5 12s4 7 10.5 7a10.7 10.7 0 0 0 5.1-1.3" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24">
      <path d="M1.5 12s4-7 10.5-7 10.5 7 10.5 7-4 7-10.5 7S1.5 12 1.5 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg className="sidebar-user-icon" viewBox="0 0 24 24">
      <path d="M20 21a8 8 0 0 0-16 0" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function PasswordInput({
  name,
  value,
  onChange,
  placeholder = "Digite sua senha",
}) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="password-wrapper">
      <input
        name={name}
        type={visible ? "text" : "password"}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
      />
      <button
        type="button"
        className="toggle-password"
        title={visible ? "Ocultar senha" : "Mostrar senha"}
        onClick={() => setVisible(!visible)}
      >
        <EyeIcon closed={visible} />
      </button>
    </div>
  );
}

function Badge({ children, type = "neutral" }) {
  return <span className={`badge ${type}`}>{children}</span>;
}

function StatusBadge({ status }) {
  const type =
    status === "CANCELADA"
      ? "cancel"
      : status === "FINALIZADA"
        ? "ok"
        : status === "EM_ANDAMENTO"
          ? "admin"
          : "neutral";
  return <Badge type={type}>{status || "-"}</Badge>;
}

function Modal({ children }) {
  return (
    <div className="modal-bg">
      <div className="modal">{children}</div>
    </div>
  );
}

function ConfirmModal({ title, text, onConfirm, onCancel }) {
  return (
    <Modal>
      <div className="confirm-card">
        <h2>{title}</h2>
        <p>{text}</p>
        <div className="modal-actions">
          <button className="danger" onClick={onConfirm}>
            Confirmar
          </button>
          <button className="secondary" onClick={onCancel}>
            Cancelar
          </button>
        </div>
      </div>
    </Modal>
  );
}

function DataTable({ headers, children }) {
  const hasRows = React.Children.count(children) > 0;
  return (
    <div className="table">
      <table>
        <thead>
          <tr>
            {headers.map((header) => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {hasRows ? (
            children
          ) : (
            <tr>
              <td colSpan={headers.length}>
                <div className="empty">Nenhum registro encontrado.</div>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function Login({ onLogin }) {
  const [form, setForm] = useState({ login: "", senha: "" });
  const [error, setError] = useState("");

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    try {
      const usuario = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify(form),
      });
      setForm({ login: "", senha: "" });
      onLogin(usuario);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="login">
      <form className="login-card" onSubmit={handleSubmit}>
        <section className="login-brand">
          <img src="/logo-uniao.png" alt="UniControl" />
        </section>
        <section className="login-form">
          <h1>UniControl</h1>
          <p>Sistema de Gestão de Estoque e Obras</p>
          <div>
            <label>Usuário</label>
            <input
              name="login"
              autoComplete="off"
              placeholder="Digite seu usuário"
              value={form.login}
              onChange={updateField}
            />
          </div>
          <div>
            <label>Senha</label>
            <PasswordInput
              name="senha"
              value={form.senha}
              onChange={updateField}
            />
          </div>
          <button>Entrar</button>
          {error && <div className="error">{error}</div>}
        </section>
      </form>
    </div>
  );
}

function Sidebar({ user, page, setPage, collapsed, setCollapsed, onLogout }) {
  const isAdmin = user?.tipoUsuario === "ADMINISTRADOR";
  const items = [
    { id: "dashboard", label: "Dashboard", icon: "▦", show: true },
    { id: "usuarios", label: "Usuários", icon: <UserIcon />, show: isAdmin },
    { id: "materiais", label: "Materiais", icon: "▣", show: true },
    { id: "movimentacoes", label: "Entrada / Retirada", icon: "↕", show: true },
    { id: "obras", label: "Obras", icon: "⌂", show: true },
  ];

  return (
    <aside className="sidebar">
      <div className="brand">
        <img src="/logo-uniao.png" alt="União Acabamentos" />
        <button
          className="toggle"
          title="Recolher menu"
          onClick={() => setCollapsed(!collapsed)}
        >
          ☰
        </button>
      </div>
      <div className="profile">
        <img
          className="profile-avatar"
          src="/logo-uniao.png"
          alt="União Acabamentos"
        />
        <div>
          <b>{user.nome}</b>
          <small>{isAdmin ? "Administrador" : "Usuário comum"}</small>
        </div>
      </div>
      <div className="nav">
        {items
          .filter((item) => item.show)
          .map((item) => (
            <button
              key={item.id}
              className={page === item.id ? "active" : ""}
              onClick={() => setPage(item.id)}
            >
              <span className="ico">{item.icon}</span>
              <span className="label">{item.label}</span>
            </button>
          ))}
      </div>
      <button className="logout" onClick={onLogout}>
        <span className="ico">⎋</span>
        <span className="label">Sair</span>
      </button>
    </aside>
  );
}

function Dashboard({ data, isAdmin }) {
  const retiradas = {};
  data.movs
    .filter((m) => m.tipo === "SAIDA")
    .forEach((m) => {
      const nome = m.material?.nome || "Material";
      retiradas[nome] = (retiradas[nome] || 0) + (m.quantidade || 0);
    });
  const topMateriais = Object.entries(retiradas)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3);
  const ultimasObras = [...data.obras]
    .sort((a, b) => (b.id || 0) - (a.id || 0))
    .slice(0, 3);

  return (
    <>
      <div className="top">
        <div>
          <h1>UniControl</h1>
          <p>Controle de materiais, obras e estoque.</p>
        </div>
        <Badge type={isAdmin ? "admin" : "user"}>
          {isAdmin ? "Administrador" : "Usuário comum"}
        </Badge>
      </div>
      <div className="cards">
        <div className="card">
          <h3>Materiais</h3>
          <div className="metric">{data.materiais.length}</div>
        </div>
        <div className="card">
          <h3>Estoque baixo</h3>
          <div className="metric">
            {
              data.materiais.filter(
                (m) => (m.quantidade || 0) <= (m.estoqueMinimo || 0),
              ).length
            }
          </div>
        </div>
        <div className="card">
          <h3>Obras</h3>
          <div className="metric">{data.obras.length}</div>
        </div>
      </div>
      <div className="dash-grid">
        <div className="panel">
          <h2>Principais materiais usados</h2>
          <div className="list">
            {topMateriais.length ? (
              topMateriais.map(([nome, qtd]) => (
                <div className="list-item" key={nome}>
                  <div>
                    <b>{nome}</b>
                  </div>
                  <Badge type="ok">{qtd} un.</Badge>
                </div>
              ))
            ) : (
              <div className="empty">Sem retiradas registradas.</div>
            )}
          </div>
        </div>
        <div className="panel">
          <h2>Últimas obras</h2>
          <div className="list">
            {ultimasObras.length ? (
              ultimasObras.map((obra) => (
                <div className="list-item" key={obra.id}>
                  <div>
                    <b>{obra.nomeObra}</b>
                    <small>{obra.nomeCliente || ""}</small>
                  </div>
                  <StatusBadge status={obra.status} />
                </div>
              ))
            ) : (
              <div className="empty">Nenhuma obra cadastrada.</div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

const emptyUser = {
  id: "",
  nome: "",
  login: "",
  senha: "",
  tipoUsuario: "USUARIO_COMUM",
  ativo: true,
};
const emptyMaterial = {
  id: "",
  nome: "",
  marca: "",
  cor: "",
  quantidade: 0,
  estoqueMinimo: 0,
  valorCusto: 0,
  valorVenda: 0,
  descricao: "",
};
const emptyObra = {
  id: "",
  nomeObra: "",
  nomeCliente: "",
  endereco: "",
  metragem: 0,
  status: "CADASTRADA",
  descricao: "",
};

function Usuarios({ usuarios, currentUser, reload, showMessage, openConfirm }) {
  const [modalUser, setModalUser] = useState(null);
  const updateField = (e) =>
    setModalUser({ ...modalUser, [e.target.name]: e.target.value });

  async function saveUser(event) {
    event.preventDefault();
    try {
      const method = modalUser.id ? "PUT" : "POST";
      const path = modalUser.id
        ? `/usuarios/${modalUser.id}?usuarioLogadoId=${currentUser.id}`
        : `/usuarios?usuarioLogadoId=${currentUser.id}`;
      await request(path, {
        method,
        body: JSON.stringify({ ...modalUser, ativo: modalUser.ativo ?? true }),
      });
      setModalUser(null);
      showMessage("Usuário salvo.");
      await reload();
    } catch (err) {
      showMessage(err.message, "error");
    }
  }

  async function toggleUser(usuario) {
    try {
      await request(
        `/usuarios/${usuario.id}/status?usuarioLogadoId=${currentUser.id}&ativo=${!usuario.ativo}`,
        { method: "PATCH" },
      );
      showMessage("Status alterado.");
      await reload();
    } catch (err) {
      showMessage(err.message, "error");
    }
  }

  function deleteUser(usuario) {
    openConfirm(
      "Excluir usuário",
      "Deseja realmente excluir este usuário?",
      async () => {
        try {
          await request(
            `/usuarios/${usuario.id}?usuarioLogadoId=${currentUser.id}`,
            { method: "DELETE" },
          );
          showMessage("Usuário excluído.");
          await reload();
        } catch (err) {
          showMessage(err.message, "error");
        }
      },
    );
  }

  return (
    <>
      <div className="top">
        <div>
          <h1>Usuários</h1>
          <p>Cadastro e controle de permissões do sistema.</p>
        </div>
        <button onClick={() => setModalUser(emptyUser)}>Novo usuário</button>
      </div>
      <DataTable headers={["Nome", "Login", "Perfil", "Status", "Ações"]}>
        {usuarios.map((usuario) => (
          <tr key={usuario.id}>
            <td>{usuario.nome}</td>
            <td>{usuario.login}</td>
            <td>
              <Badge
                type={
                  usuario.tipoUsuario === "ADMINISTRADOR" ? "admin" : "user"
                }
              >
                {usuario.tipoUsuario}
              </Badge>
            </td>
            <td>{usuario.ativo ? "Ativo" : "Inativo"}</td>
            <td className="actions-cell">
              <div className="table-actions">
                <button onClick={() => setModalUser({ ...usuario, senha: "" })}>
                  Editar
                </button>
                <button
                  className="secondary"
                  onClick={() => toggleUser(usuario)}
                >
                  {usuario.ativo ? "Inativar" : "Ativar"}
                </button>
                <button className="danger" onClick={() => deleteUser(usuario)}>
                  Excluir
                </button>
              </div>
            </td>
          </tr>
        ))}
      </DataTable>
      {modalUser && (
        <Modal>
          <h2>{modalUser.id ? "Editar usuário" : "Novo usuário"}</h2>
          <form onSubmit={saveUser}>
            <div className="modal-grid">
              <div>
                <label>Nome</label>
                <input
                  name="nome"
                  value={modalUser.nome}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Login</label>
                <input
                  name="login"
                  value={modalUser.login}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Senha</label>
                <PasswordInput
                  name="senha"
                  value={modalUser.senha || ""}
                  onChange={updateField}
                  placeholder={
                    modalUser.id
                      ? "Deixe em branco para manter"
                      : "Digite a senha"
                  }
                />
              </div>
              <div>
                <label>Perfil</label>
                <select
                  name="tipoUsuario"
                  value={modalUser.tipoUsuario}
                  onChange={updateField}
                >
                  <option>USUARIO_COMUM</option>
                  <option>ADMINISTRADOR</option>
                </select>
              </div>
            </div>
            <div className="modal-actions">
              <button>Salvar</button>
              <button
                type="button"
                className="secondary"
                onClick={() => setModalUser(null)}
              >
                Cancelar
              </button>
            </div>
          </form>
        </Modal>
      )}
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
  setMaterialSelecionadoSaida,
}) {
  const [modalMaterial, setModalMaterial] = useState(null);
  const [modalError, setModalError] = useState("");
  const updateField = (e) => {
    setModalError("");
    setModalMaterial({ ...modalMaterial, [e.target.name]: e.target.value });
  };

  const materialDuplicado = modalMaterial
    ? materiais.find(
        (material) =>
          String(material.id) !== String(modalMaterial.id || "") &&
          materialEhSemelhante(material, modalMaterial),
      )
    : null;

  async function saveMaterial(event) {
    event.preventDefault();
    setModalError("");

    if (materialDuplicado) {
      setModalError(
        "Já existe um material cadastrado com o mesmo nome e a mesma cor ou descrição. Materiais iguais só podem ser cadastrados quando a cor e a descrição forem diferentes.",
      );
      return;
    }

    try {
      const method = modalMaterial.id ? "PUT" : "POST";
      const path = modalMaterial.id
        ? `/materiais/${modalMaterial.id}?usuarioLogadoId=${currentUser.id}`
        : `/materiais?usuarioLogadoId=${currentUser.id}`;
      await request(path, {
        method,
        body: JSON.stringify({
          ...modalMaterial,
          quantidade: Number(modalMaterial.quantidade || 0),
          estoqueMinimo: Number(modalMaterial.estoqueMinimo || 0),
          valorCusto: Number(modalMaterial.valorCusto || 0),
          valorVenda: Number(modalMaterial.valorVenda || 0),
        }),
      });
      setModalMaterial(null);
      setModalError("");
      showMessage("Material salvo.");
      await reload();
    } catch (err) {
      setModalError(err.message);
    }
  }

  function deleteMaterial(material) {
    openConfirm(
      "Excluir material",
      "Deseja realmente excluir este material?",
      async () => {
        try {
          await request(
            `/materiais/${material.id}?usuarioLogadoId=${currentUser.id}`,
            { method: "DELETE" },
          );
          showMessage("Material excluído.");
          await reload();
        } catch (err) {
          showMessage(err.message, "error");
        }
      },
    );
  }

  function movimentar(material) {
    setMaterialSelecionadoSaida(String(material.id));
    setPage("movimentacoes");
  }

  return (
    <>
      <div className="top">
        <div>
          <h1>Materiais</h1>
          <p>Cadastro, consulta e controle das quantidades em estoque.</p>
        </div>
        {isAdmin && (
          <button
            onClick={() => {
              setModalError("");
              setModalMaterial(emptyMaterial);
            }}
          >
            Novo material
          </button>
        )}
      </div>
      <DataTable headers={["Nome", "Marca", "Cor", "Qtd", "Status", "Ações"]}>
        {materiais.map((material) => (
          <tr key={material.id}>
            <td>{material.nome}</td>
            <td>{material.marca || ""}</td>
            <td>{material.cor || ""}</td>
            <td>{material.quantidade}</td>
            <td>
              {material.quantidade <= 0 ? (
                <Badge type="cancel">Sem material</Badge>
              ) : material.quantidade <= material.estoqueMinimo ? (
                <Badge type="low">Estoque baixo</Badge>
              ) : (
                <Badge type="ok">Disponível</Badge>
              )}
            </td>
            <td className="actions-cell">
              <div className="table-actions">
                {isAdmin ? (
                  <>
                    <button
                      onClick={() => {
                        setModalError("");
                        setModalMaterial({ ...material });
                      }}
                    >
                      Editar
                    </button>
                    <button
                      className="blue"
                      onClick={() => movimentar(material)}
                    >
                      Movimentar
                    </button>
                    <button
                      className="danger"
                      onClick={() => deleteMaterial(material)}
                    >
                      Excluir
                    </button>
                  </>
                ) : (
                  <button className="blue" onClick={() => movimentar(material)}>
                    Registrar retirada
                  </button>
                )}
              </div>
            </td>
          </tr>
        ))}
      </DataTable>
      {modalMaterial && (
        <Modal>
          <h2>{modalMaterial.id ? "Editar material" : "Novo material"}</h2>
          {(modalError || materialDuplicado) && (
            <div className="modal-error">
              {modalError ||
                "Já existe um material cadastrado com o mesmo nome e a mesma cor ou descrição."}
            </div>
          )}
          <form onSubmit={saveMaterial}>
            <div className="modal-grid">
              {["nome", "marca", "cor"].map((field) => (
                <div key={field}>
                  <label>
                    {field === "nome"
                      ? "Nome"
                      : field === "marca"
                        ? "Marca"
                        : "Cor"}
                  </label>
                  <input
                    name={field}
                    value={modalMaterial[field] || ""}
                    onChange={updateField}
                  />
                </div>
              ))}
              <div>
                <label>Quantidade</label>
                <input
                  name="quantidade"
                  type="number"
                  value={modalMaterial.quantidade}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Estoque mínimo</label>
                <input
                  name="estoqueMinimo"
                  type="number"
                  value={modalMaterial.estoqueMinimo}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Valor custo</label>
                <input
                  name="valorCusto"
                  type="number"
                  step="0.01"
                  value={modalMaterial.valorCusto}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Valor venda</label>
                <input
                  name="valorVenda"
                  type="number"
                  step="0.01"
                  value={modalMaterial.valorVenda}
                  onChange={updateField}
                />
              </div>
              <div className="span2">
                <label>Descrição</label>
                <textarea
                  name="descricao"
                  value={modalMaterial.descricao || ""}
                  onChange={updateField}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button disabled={!!materialDuplicado}>Salvar</button>
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setModalError("");
                  setModalMaterial(null);
                }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}

function Obras({
  obras,
  currentUser,
  isAdmin,
  reload,
  showMessage,
  openConfirm,
}) {
  const [modalObra, setModalObra] = useState(null);
  const [visualizarObra, setVisualizarObra] = useState(null);
  const [modalError, setModalError] = useState("");
  const updateField = (e) => {
    setModalError("");
    setModalObra({ ...modalObra, [e.target.name]: e.target.value });
  };

  const obraDuplicada = modalObra
    ? obras.find(
        (obra) =>
          String(obra.id) !== String(modalObra.id || "") &&
          obra.status !== "FINALIZADA" &&
          normalizarTexto(obra.nomeObra) ===
            normalizarTexto(modalObra.nomeObra) &&
          normalizarTexto(obra.nomeCliente) ===
            normalizarTexto(modalObra.nomeCliente) &&
          normalizarTexto(obra.endereco) ===
            normalizarTexto(modalObra.endereco) &&
          normalizarTexto(obra.descricao) ===
            normalizarTexto(modalObra.descricao),
      )
    : null;

  async function saveObra(event) {
    event.preventDefault();
    setModalError("");

    if (obraDuplicada) {
      setModalError(
        "Já existe uma obra semelhante cadastrada em aberto. Só é permitido repetir uma obra quando a anterior estiver finalizada.",
      );
      return;
    }

    try {
      const method = modalObra.id ? "PUT" : "POST";
      const path = modalObra.id
        ? `/obras/${modalObra.id}?usuarioLogadoId=${currentUser.id}`
        : `/obras?usuarioLogadoId=${currentUser.id}`;
      await request(path, {
        method,
        body: JSON.stringify({
          ...modalObra,
          metragem: Number(modalObra.metragem || 0),
          valorOrcamento: 0,
        }),
      });
      setModalObra(null);
      setModalError("");
      showMessage("Obra salva.");
      await reload();
    } catch (err) {
      setModalError(err.message);
    }
  }

  function deleteObra(obra) {
    openConfirm(
      "Excluir obra",
      "Deseja realmente excluir esta obra?",
      async () => {
        try {
          await request(`/obras/${obra.id}?usuarioLogadoId=${currentUser.id}`, {
            method: "DELETE",
          });
          showMessage("Obra excluída.");
          await reload();
        } catch (err) {
          showMessage(err.message, "error");
        }
      },
    );
  }

  return (
    <>
      <div className="top">
        <div>
          <h1>Obras</h1>
          <p>Cadastro e acompanhamento das obras da empresa.</p>
        </div>
        {isAdmin && (
          <button
            onClick={() => {
              setModalError("");
              setModalObra(emptyObra);
            }}
          >
            Nova obra
          </button>
        )}
      </div>
      <DataTable headers={["Obra", "Cliente", "Status", "Metragem", "Ações"]}>
        {obras.map((obra) => (
          <tr key={obra.id}>
            <td>{obra.nomeObra}</td>
            <td>{obra.nomeCliente}</td>
            <td>
              <StatusBadge status={obra.status} />
            </td>
            <td>{obra.metragem || 0}</td>
            <td className="actions-cell">
              <div className="table-actions">
                <button
                  className="blue"
                  onClick={() => setVisualizarObra(obra)}
                >
                  Visualizar
                </button>
                {isAdmin && (
                  <>
                    <button
                      onClick={() => {
                        setModalError("");
                        setModalObra({ ...obra });
                      }}
                    >
                      Editar
                    </button>
                    <button className="danger" onClick={() => deleteObra(obra)}>
                      Excluir
                    </button>
                  </>
                )}
              </div>
            </td>
          </tr>
        ))}
      </DataTable>
      {modalObra && (
        <Modal>
          <h2>{modalObra.id ? "Editar obra" : "Nova obra"}</h2>
          {(modalError || obraDuplicada) && (
            <div className="modal-error">
              {modalError ||
                "Já existe uma obra semelhante cadastrada em aberto. Só é permitido repetir quando a anterior estiver finalizada."}
            </div>
          )}
          <form onSubmit={saveObra}>
            <div className="modal-grid">
              <div>
                <label>Nome da obra</label>
                <input
                  name="nomeObra"
                  value={modalObra.nomeObra}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Cliente</label>
                <input
                  name="nomeCliente"
                  value={modalObra.nomeCliente}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Endereço</label>
                <input
                  name="endereco"
                  value={modalObra.endereco || ""}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Metragem</label>
                <input
                  name="metragem"
                  type="number"
                  step="0.01"
                  value={modalObra.metragem || 0}
                  onChange={updateField}
                />
              </div>
              <div>
                <label>Status</label>
                <select
                  name="status"
                  value={modalObra.status}
                  onChange={updateField}
                >
                  <option>CADASTRADA</option>
                  <option>EM_ANDAMENTO</option>
                  <option>FINALIZADA</option>
                  <option>CANCELADA</option>
                </select>
              </div>
              <div className="span2">
                <label>Descrição</label>
                <textarea
                  name="descricao"
                  value={modalObra.descricao || ""}
                  onChange={updateField}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button disabled={!!obraDuplicada}>Salvar</button>
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setModalError("");
                  setModalObra(null);
                }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </Modal>
      )}
      {visualizarObra && (
        <Modal>
          <h2>Detalhes da Obra</h2>
          <div className="list">
            <div className="list-item">
              <b>Obra</b>
              <span>{visualizarObra.nomeObra}</span>
            </div>
            <div className="list-item">
              <b>Cliente</b>
              <span>{visualizarObra.nomeCliente}</span>
            </div>
            <div className="list-item">
              <b>Endereço</b>
              <span>{visualizarObra.endereco || "-"}</span>
            </div>
            <div className="list-item">
              <b>Status</b>
              <StatusBadge status={visualizarObra.status} />
            </div>
            <div className="list-item">
              <b>Metragem</b>
              <span>{visualizarObra.metragem || 0} m²</span>
            </div>
            <div className="list-item">
              <b>Descrição</b>
              <span>{visualizarObra.descricao || "-"}</span>
            </div>
          </div>
          <div className="modal-actions">
            <button
              className="secondary"
              onClick={() => setVisualizarObra(null)}
            >
              Fechar
            </button>
          </div>
        </Modal>
      )}
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
  setMaterialSelecionadoSaida,
}) {
  const [form, setForm] = useState({
    materialId: "",
    tipo: isAdmin ? "ENTRADA" : "SAIDA",
    quantidade: 1,
    obraId: "",
    observacao: "",
  });

  useEffect(() => {
    if (materialSelecionadoSaida) {
      setForm((prev) => ({
        ...prev,
        materialId: materialSelecionadoSaida,
        tipo: "SAIDA",
      }));
      setMaterialSelecionadoSaida(null);
    }
  }, [materialSelecionadoSaida, setMaterialSelecionadoSaida]);

  useEffect(() => {
    setForm((prev) => ({ ...prev, tipo: isAdmin ? prev.tipo : "SAIDA" }));
  }, [isAdmin]);

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  async function saveMov(event) {
    event.preventDefault();
    try {
      await request("/materiais/movimentacoes", {
        method: "POST",
        body: JSON.stringify({
          materialId: Number(form.materialId),
          usuarioId: currentUser.id,
          obraId: form.obraId ? Number(form.obraId) : null,
          tipo: form.tipo,
          quantidade: Number(form.quantidade),
          observacao: form.observacao,
        }),
      });
      showMessage("Movimentação registrada.");
      setForm({
        materialId: "",
        tipo: isAdmin ? "ENTRADA" : "SAIDA",
        quantidade: 1,
        obraId: "",
        observacao: "",
      });
      await reload();
    } catch (err) {
      showMessage(err.message, "error");
    }
  }

  return (
    <>
      <div className="top">
        <div>
          <h1>Entrada / Retirada</h1>
          <p>Registro de movimentações dos materiais em estoque.</p>
        </div>
      </div>
      <div className="split">
        <form className="panel" onSubmit={saveMov}>
          <div className="grid">
            <div className="span2">
              <label>Material</label>
              <select
                name="materialId"
                required
                value={form.materialId}
                onChange={updateField}
              >
                <option value="">Selecione</option>
                {materiais.map((material) => (
                  <option key={material.id} value={material.id}>
                    {material.nome} - estoque {material.quantidade}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label>Tipo</label>
              <select name="tipo" value={form.tipo} onChange={updateField}>
                {isAdmin && <option>ENTRADA</option>}
                <option>SAIDA</option>
              </select>
            </div>
            <div>
              <label>Quantidade</label>
              <input
                name="quantidade"
                min="1"
                type="number"
                value={form.quantidade}
                onChange={updateField}
              />
            </div>
            <div className="span2">
              <label>Obra</label>
              <select name="obraId" value={form.obraId} onChange={updateField}>
                <option value="">Sem vínculo</option>
                {obras.map((obra) => (
                  <option key={obra.id} value={obra.id}>
                    {obra.nomeObra}
                  </option>
                ))}
              </select>
            </div>
            <div className="span2">
              <label>Observação</label>
              <textarea
                name="observacao"
                value={form.observacao}
                onChange={updateField}
              />
            </div>
          </div>
          <div className="actions">
            <button>Registrar movimentação</button>
          </div>
        </form>
        <div className="panel">
          <h2>Histórico</h2>
          <div className="history">
            {movs.length ? (
              [...movs].reverse().map((mov) => (
                <div key={mov.id}>
                  <b>{mov.tipo}</b> - {mov.material?.nome || ""} -{" "}
                  {mov.quantidade} un.
                  <br />
                  <small>
                    {mov.usuario?.nome || ""}
                    {mov.obra?.nomeObra ? ` | Obra: ${mov.obra.nomeObra}` : ""}
                  </small>
                </div>
              ))
            ) : (
              <div className="empty">Nenhuma movimentação registrada.</div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

function App() {
  const [user, setUser] = useState(null);
  const [page, setPage] = useState("dashboard");
  const [collapsed, setCollapsed] = useState(false);
  const [materialSelecionadoSaida, setMaterialSelecionadoSaida] =
    useState(null);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("success");
  const [confirm, setConfirm] = useState(null);
  const [data, setData] = useState({
    usuarios: [],
    materiais: [],
    obras: [],
    movs: [],
  });
  const isAdmin = user?.tipoUsuario === "ADMINISTRADOR";

  function showMessage(text, type = "success") {
    setMessage(text);
    setMessageType(type);
    setTimeout(() => setMessage(""), 5200);
  }

  async function loadAll() {
    const [materiais, obras, movs] = await Promise.all([
      request("/materiais"),
      request("/obras"),
      request("/materiais/movimentacoes"),
    ]);
    let usuarios = [];
    if (isAdmin) {
      usuarios = await request(`/usuarios?usuarioLogadoId=${user.id}`);
    }
    setData({ materiais, obras, movs, usuarios });
  }

  useEffect(() => {
    if (user) loadAll().catch((err) => showMessage(err.message, "error"));
  }, [user]);

  function openConfirm(title, text, onConfirm) {
    setConfirm({ title, text, onConfirm });
  }
  async function confirmAction() {
    const action = confirm.onConfirm;
    setConfirm(null);
    await action();
  }

  if (!user) return <Login onLogin={setUser} />;

  return (
    <div className={`shell ${collapsed ? "collapsed" : ""}`}>
      <Sidebar
        user={user}
        page={page}
        setPage={setPage}
        collapsed={collapsed}
        setCollapsed={setCollapsed}
        onLogout={() => {
          setUser(null);
          setPage("dashboard");
        }}
      />
      <main>
        {message && <div className={messageType}>{message}</div>}
        {page === "dashboard" && <Dashboard data={data} isAdmin={isAdmin} />}
        {page === "usuarios" && isAdmin && (
          <Usuarios
            usuarios={data.usuarios}
            currentUser={user}
            reload={loadAll}
            showMessage={showMessage}
            openConfirm={openConfirm}
          />
        )}
        {page === "materiais" && (
          <Materiais
            materiais={data.materiais}
            currentUser={user}
            isAdmin={isAdmin}
            reload={loadAll}
            showMessage={showMessage}
            openConfirm={openConfirm}
            setPage={setPage}
            setMaterialSelecionadoSaida={setMaterialSelecionadoSaida}
          />
        )}
        {page === "movimentacoes" && (
          <Movimentacoes
            materiais={data.materiais}
            obras={data.obras}
            movs={data.movs}
            currentUser={user}
            isAdmin={isAdmin}
            reload={loadAll}
            showMessage={showMessage}
            materialSelecionadoSaida={materialSelecionadoSaida}
            setMaterialSelecionadoSaida={setMaterialSelecionadoSaida}
          />
        )}
        {page === "obras" && (
          <Obras
            obras={data.obras}
            currentUser={user}
            isAdmin={isAdmin}
            reload={loadAll}
            showMessage={showMessage}
            openConfirm={openConfirm}
          />
        )}
        {confirm && (
          <ConfirmModal
            title={confirm.title}
            text={confirm.text}
            onConfirm={confirmAction}
            onCancel={() => setConfirm(null)}
          />
        )}
      </main>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
