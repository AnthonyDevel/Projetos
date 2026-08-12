// ==================== CARRINHO ====================
async function atualizarCarrinho() {
    const resp = await fetch('/api/cart');
    const data = await resp.json();
    const container = document.getElementById('itens-carrinho');
    const totalEl = document.getElementById('total');
    const badgeEls = document.querySelectorAll('.cart-badge');

    if (data.itens.length === 0) {
        container.innerHTML = '<p style="color:#999; text-align:center;">Adicione itens</p>';
    } else {
        container.innerHTML = data.itens.map(item => `
            <div class="cart-item">
                <img src="${item.imagem.startsWith('http') ? item.imagem : '/' + item.imagem}" alt="${item.nome}"
                     onerror="this.onerror=null;this.src='/static/imagens_produtos/placeholder.png';">
                <div class="item-details">
                    <div class="item-name">${item.nome}</div>
                    <div class="item-qty">${item.quantidade}x R$ ${item.preco.toFixed(2)}</div>
                </div>
                <span class="item-price">R$ ${item.subtotal.toFixed(2)}</span>
                <button class="btn-remove" onclick="removerItem('${item.id}')">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </div>
        `).join('');
    }
    totalEl.textContent = `R$ ${data.total.toFixed(2)}`;
    badgeEls.forEach(el => el.textContent = data.qtd_total);
}

async function adicionar(id) {
    await fetch('/api/cart/add', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({id: id})
    });
    atualizarCarrinho();
    // Abre o modal do carrinho automaticamente ao adicionar um item
    document.getElementById('cart-modal').classList.add('show');
}

async function removerItem(id) {
    await fetch('/api/cart/remove', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({id: id})
    });
    atualizarCarrinho();
}

async function limparCarrinho() {
    if (confirm('Deseja realmente limpar o carrinho?')) {
        await fetch('/api/cart/clear', {method: 'POST'});
        atualizarCarrinho();
        document.getElementById('cart-modal').classList.remove('show');
    }
}

function finalizar() {
    window.location.href = '/checkout';
}

// ==================== MODAL DO CARRINHO ====================
function toggleCarrinho() {
    const modal = document.getElementById('cart-modal');
    modal.classList.toggle('show');
    if (modal.classList.contains('show')) {
        atualizarCarrinho();
    }
}

// Fechar modal do carrinho clicando no X
document.getElementById('cart-modal')?.addEventListener('click', function(e) {
    if (e.target === this) this.classList.remove('show');
});

// ==================== ACOMPANHAR PEDIDO ====================
function abrirAcompanharPedido() {
    document.getElementById('track-modal').classList.add('show');
}

function fecharAcompanharPedido() {
    document.getElementById('track-modal').classList.remove('show');
}

function abrirPedido() {
    const id = document.getElementById('pedido-id-input').value.trim().toUpperCase();
    if (id) {
        window.location.href = `/acompanhar/${id}`;
    } else {
        alert('Digite o número do pedido.');
    }
}

// Fechar modal de acompanhamento clicando fora
document.getElementById('track-modal')?.addEventListener('click', function(e) {
    if (e.target === this) this.classList.remove('show');
});

// ==================== FILTROS ====================
function filtrar(categoria = 'Todos') {
    const cards = document.querySelectorAll('.product-card');
    const termo = document.getElementById('busca').value.toLowerCase();

    // Atualizar botões de categoria
    document.querySelectorAll('.categories button:not(.btn-novo)').forEach(btn => {
        const btnCat = btn.innerText.trim();
        btn.classList.remove('active');
        if (btnCat === categoria || (categoria === 'Todos' && btnCat === 'Todos')) {
            btn.classList.add('active');
        }
    });

    cards.forEach(card => {
        const cat = card.dataset.categoria;
        const nome = card.dataset.nome.toLowerCase();
        const matchCat = categoria === 'Todos' || cat === categoria;
        const matchBusca = nome.includes(termo);
        card.style.display = (matchCat && matchBusca) ? '' : 'none';
    });
}

// ==================== INICIALIZAÇÃO ====================
document.addEventListener('DOMContentLoaded', () => {
    atualizarCarrinho();

    // Carrinho flutuante (mobile) também aciona o modal
    const floatingCart = document.getElementById('floating-cart');
    if (floatingCart) {
        floatingCart.addEventListener('click', toggleCarrinho);
    }

    // Fecha modais ao pressionar ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.getElementById('cart-modal').classList.remove('show');
            document.getElementById('track-modal').classList.remove('show');
        }
    });
});

// Forçar filtro inicial (caso DOM já esteja pronto)
if (document.readyState === 'interactive' || document.readyState === 'complete') {
    atualizarCarrinho();
}