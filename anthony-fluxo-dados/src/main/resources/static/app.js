const form = document.getElementById("formulario");
const aviso = document.getElementById("aviso");
const lista = document.getElementById("lista");
const contador = document.getElementById("contador");
const statusEl = document.getElementById("status-conexao");
const itens = new Map();

function setStatus(texto, ok) {
  statusEl.textContent = texto;
  statusEl.classList.toggle("ok", ok === true);
  statusEl.classList.toggle("bad", ok === false);
}

function formatarData(iso) {
  if (!iso) return "";
  return new Date(iso).toLocaleString("pt-BR");
}

function render() {
  const dados = [...itens.values()].sort((a, b) => b.id - a.id);
  contador.textContent = `${dados.length} ${dados.length === 1 ? "item" : "itens"}`;
  if (!dados.length) {
    lista.innerHTML = '<p class="vazio">Nenhum dado ainda. Envie o primeiro item do fluxo.</p>';
    return;
  }
  lista.innerHTML = dados.map((dado) => `
    <article class="item">
      <div class="item-topo">
        <span class="autor">${escapeHtml(dado.autor)}</span>
        <span class="badge ${dado.status}">${dado.status}</span>
      </div>
      <p class="conteudo">${escapeHtml(dado.conteudo)}</p>
      <p class="resultado">${escapeHtml(dado.resultado || "")}</p>
      <p class="resultado">${formatarData(dado.criadoEm)}</p>
    </article>
  `).join("");
}

function escapeHtml(texto) {
  return String(texto)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function upsert(dado) {
  itens.set(dado.id, dado);
  render();
}

async function carregar() {
  const resposta = await fetch("/api/dados");
  const dados = await resposta.json();
  dados.forEach(upsert);
}

function conectarStream() {
  const source = new EventSource("/api/dados/stream");
  source.addEventListener("fluxo", (evento) => {
    upsert(JSON.parse(evento.data));
  });
  source.onopen = () => setStatus("Conectado ao vivo", true);
  source.onerror = () => setStatus("Reconectando...", false);
}

form.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  aviso.hidden = true;
  const botao = form.querySelector("button");
  botao.disabled = true;
  try {
    const resposta = await fetch("/api/dados", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        autor: document.getElementById("autor").value,
        conteudo: document.getElementById("conteudo").value
      })
    });
    if (!resposta.ok) {
      throw new Error("Não foi possível enfileirar o dado.");
    }
    const criado = await resposta.json();
    upsert(criado);
    document.getElementById("conteudo").value = "";
  } catch (erro) {
    aviso.hidden = false;
    aviso.textContent = erro.message;
  } finally {
    botao.disabled = false;
  }
});

carregar().then(conectarStream).catch(() => setStatus("Servidor indisponível", false));
render();
