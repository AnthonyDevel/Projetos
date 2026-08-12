"""
Sabor Express - Servidor Central (Flask)
Versão robusta com tratamento de erros e inicialização automática.
"""

import json
import os
import uuid
import logging
from datetime import datetime
from flask import Flask, render_template, request, jsonify, session, redirect, url_for, send_from_directory
from models import Lanche, PedidoConfirmado, GerenciadorPedidos, ItemPedido

# Configuração de logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'chave-secreta-padrao')  # Use variável de ambiente em produção

# ----- Configurações -----
CARDAPIO_FILE = 'cardapio.json'
IMAGES_DIR = 'imagens_produtos'
CATEGORIAS_PADRAO = ['Combos', 'Hambúrgueres', 'Acompanhamentos', 'Entradas', 'Bebidas', 'Sobremesas']

# ----- Gerenciador global de pedidos -----
gerenciador = GerenciadorPedidos()

# ==================== FUNÇÕES AUXILIARES ====================

def inicializar_cardapio():
    """Cria o arquivo cardapio.json com dados padrão se não existir."""
    if not os.path.exists(CARDAPIO_FILE):
        produtos_padrao = [
            {
                "id": "1",
                "nome": "Clássico Burger",
                "preco": 25.90,
                "imagem": "imagens_produtos/default_burger.png",
                "descricao": "Hambúrguer 180g, queijo, alface e tomate",
                "categoria": "Hambúrgueres",
                "calorias": 850,
                "badge": "Mais Vendido"
            },
            {
                "id": "2",
                "nome": "Combo Família",
                "preco": 79.90,
                "imagem": "imagens_produtos/default_combo.png",
                "descricao": "4 hambúrgueres, 4 batatas, 4 refrigerantes",
                "categoria": "Combos",
                "calorias": 3200
            }
        ]
        os.makedirs(IMAGES_DIR, exist_ok=True)  # Garante que a pasta exista
        with open(CARDAPIO_FILE, 'w', encoding='utf-8') as f:
            json.dump(produtos_padrao, f, indent=4, ensure_ascii=False)
        logging.info("Cardápio padrão criado.")

def carregar_cardapio():
    """Carrega a lista de lanches do JSON. Retorna lista vazia em caso de erro."""
    try:
        with open(CARDAPIO_FILE, 'r', encoding='utf-8') as f:
            dados = json.load(f)
        return [Lanche.from_dict(item) for item in dados]
    except FileNotFoundError:
        logging.error(f"Arquivo {CARDAPIO_FILE} não encontrado.")
        return []
    except json.JSONDecodeError as e:
        logging.error(f"Erro ao decodificar JSON: {e}")
        return []

def salvar_cardapio(lanches):
    """Salva a lista de lanches no JSON."""
    try:
        with open(CARDAPIO_FILE, 'w', encoding='utf-8') as f:
            json.dump([l.to_dict() for l in lanches], f, indent=4, ensure_ascii=False)
    except IOError as e:
        logging.error(f"Erro ao salvar cardápio: {e}")

def get_cart():
    if 'cart' not in session:
        session['cart'] = {}
    return session['cart']

def save_cart(cart):
    session['cart'] = cart

def salvar_imagem(arquivo_imagem):
    """Salva imagem e retorna caminho relativo."""
    os.makedirs(IMAGES_DIR, exist_ok=True)
    ext = os.path.splitext(arquivo_imagem.filename)[1]
    nome_arquivo = f"produto_{uuid.uuid4().hex[:8]}{ext}"
    caminho = os.path.join(IMAGES_DIR, nome_arquivo)
    arquivo_imagem.save(caminho)
    return caminho

# ==================== ROTAS WEB ====================

@app.route('/')
def index():
    lanches = carregar_cardapio()
    return render_template('index.html', lanches=lanches)

@app.route('/checkout', methods=['GET', 'POST'])
def checkout():
    if request.method == 'POST':
        try:
            nome = request.form['nome']
            tipo_entrega = request.form['tipo_entrega']
            endereco = None
            if tipo_entrega == 'Entrega':
                logr = request.form.get('logradouro', '')
                num = request.form.get('numero', '')
                compl = request.form.get('complemento', '')
                bairro = request.form.get('bairro', '')
                cep = request.form.get('cep', '')
                # Monta endereço completo
                partes = [logr]
                if num: partes.append(num)
                if compl: partes.append(compl)
                if bairro: partes.append(bairro)
                if cep: partes.append(f"CEP: {cep}")
                endereco = " - ".join(partes)
            pagamento = request.form['pagamento']

            cart = get_cart()
            if not cart:
                return redirect(url_for('index'))

            lanches_dict = {l.id: l for l in carregar_cardapio()}
            itens = {}
            total = 0.0
            for pid, qtd in cart.items():
                l = lanches_dict[pid]
                item = ItemPedido(l, qtd)
                itens[pid] = item
                total += item.subtotal

            pedido = PedidoConfirmado(
                id_pedido=str(uuid.uuid4())[:8].upper(),
                cliente_nome=nome,
                itens=itens,
                total=total,
                tipo_entrega=tipo_entrega,
                endereco=endereco,
                pagamento=pagamento
            )
            gerenciador.adicionar(pedido)
            session.pop('cart', None)
            return redirect(url_for('acompanhar_pedido', pedido_id=pedido.id))

        except KeyError as e:
            logging.error(f"Campo obrigatório ausente: {e}")
            return "Erro: campo obrigatório faltando", 400

    return render_template('checkout.html')

@app.route('/acompanhar/<pedido_id>')
def acompanhar_pedido(pedido_id):
    """Página de acompanhamento em tempo real."""
    # Verifica se o pedido existe
    for p in gerenciador.obter_todos():
        if p.id == pedido_id:
            return render_template('acompanhar.html', pedido_id=pedido_id)
    return "Pedido não encontrado", 404

@app.route('/preparo')
def preparo():
    return render_template('preparo.html')

@app.route('/adicionar', methods=['GET', 'POST'])
def adicionar_produto_web():
    if request.method == 'POST':
        try:
            nome = request.form['nome']
            preco = float(request.form['preco'])
            descricao = request.form.get('descricao', '')
            categoria = request.form['categoria']
            calorias = int(request.form['calorias'])
            badge = request.form.get('badge') or None
            imagem = request.files['imagem']

            caminho_img = salvar_imagem(imagem)

            novo = Lanche(
                id=str(uuid.uuid4()),
                nome=nome,
                preco=preco,
                imagem=caminho_img,
                descricao=descricao,
                categoria=categoria,
                calorias=calorias,
                badge=badge
            )
            lanches = carregar_cardapio()
            lanches.append(novo)
            salvar_cardapio(lanches)
            return redirect(url_for('index'))
        except Exception as e:
            logging.error(f"Erro ao adicionar produto via web: {e}")
            return "Erro ao processar o formulário", 500

    return render_template('adicionar.html', categorias=CATEGORIAS_PADRAO)

# ==================== API - CARRINHO ====================

@app.route('/api/cart')
def api_cart():
    cart = get_cart()
    lanches_dict = {l.id: l for l in carregar_cardapio()}
    itens = []
    total = 0.0
    for pid, qtd in cart.items():
        l = lanches_dict.get(pid)
        if l:
            subtotal = l.preco * qtd
            itens.append({
                'id': pid,
                'nome': l.nome,
                'preco': l.preco,
                'imagem': l.imagem,
                'quantidade': qtd,
                'subtotal': subtotal
            })
            total += subtotal
    return jsonify({'itens': itens, 'total': total, 'qtd_total': sum(cart.values())})

@app.route('/api/cart/add', methods=['POST'])
def cart_add():
    data = request.get_json()
    if not data or 'id' not in data:
        return jsonify({'erro': 'ID do produto necessário'}), 400
    pid = data['id']
    qtd = int(data.get('quantidade', 1))
    cart = get_cart()
    cart[pid] = cart.get(pid, 0) + qtd
    save_cart(cart)
    return jsonify({'status': 'ok'})

@app.route('/api/cart/remove', methods=['POST'])
def cart_remove():
    data = request.get_json()
    if not data or 'id' not in data:
        return jsonify({'erro': 'ID necessário'}), 400
    pid = data['id']
    cart = get_cart()
    if pid in cart:
        del cart[pid]
        save_cart(cart)
    return jsonify({'status': 'ok'})

@app.route('/api/cart/clear', methods=['POST'])
def cart_clear():
    session.pop('cart', None)
    return jsonify({'status': 'ok'})

# ==================== API - PRODUTOS ====================

@app.route('/api/produtos')
def api_produtos():
    lanches = carregar_cardapio()
    return jsonify([l.to_dict() for l in lanches])

@app.route('/api/produtos', methods=['POST'])
def api_criar_produto():
    campos = ['nome', 'preco', 'categoria', 'calorias']
    for campo in campos:
        if campo not in request.form:
            return jsonify({'erro': f'Campo {campo} obrigatório'}), 400
    imagem = request.files.get('imagem')
    if not imagem or not imagem.filename:
        return jsonify({'erro': 'Imagem obrigatória'}), 400

    try:
        nome = request.form['nome']
        preco = float(request.form['preco'])
        descricao = request.form.get('descricao', '')
        categoria = request.form['categoria']
        calorias = int(request.form['calorias'])
        badge = request.form.get('badge') or None
        caminho_img = salvar_imagem(imagem)

        novo = Lanche(
            id=str(uuid.uuid4()),
            nome=nome,
            preco=preco,
            imagem=caminho_img,
            descricao=descricao,
            categoria=categoria,
            calorias=calorias,
            badge=badge
        )
        lanches = carregar_cardapio()
        lanches.append(novo)
        salvar_cardapio(lanches)
        return jsonify(novo.to_dict()), 201
    except ValueError as e:
        return jsonify({'erro': 'Preço ou calorias inválidos'}), 400

@app.route('/api/produtos/<produto_id>', methods=['PUT'])
def api_editar_produto(produto_id):
    lanches = carregar_cardapio()
    for i, l in enumerate(lanches):
        if l.id == produto_id:
            dados = request.form if request.form else request.get_json()
            if not dados:
                return jsonify({'erro': 'Dados inválidos'}), 400

            nova_imagem = l.imagem
            if 'imagem' in request.files:
                arquivo = request.files['imagem']
                if arquivo.filename:
                    nova_imagem = salvar_imagem(arquivo)

            try:
                lanches[i] = Lanche(
                    id=produto_id,
                    nome=dados.get('nome', l.nome),
                    preco=float(dados.get('preco', l.preco)),
                    imagem=nova_imagem,
                    descricao=dados.get('descricao', l.descricao),
                    categoria=dados.get('categoria', l.categoria),
                    calorias=int(dados.get('calorias', l.calorias)),
                    badge=dados.get('badge', l.badge)
                )
                salvar_cardapio(lanches)
                return jsonify(lanches[i].to_dict())
            except (ValueError, TypeError):
                return jsonify({'erro': 'Dados numéricos inválidos'}), 400

    return jsonify({'erro': 'Produto não encontrado'}), 404

@app.route('/api/produtos/<produto_id>', methods=['DELETE'])
def api_excluir_produto(produto_id):
    lanches = carregar_cardapio()
    novo_lanches = [l for l in lanches if l.id != produto_id]
    if len(novo_lanches) == len(lanches):
        return jsonify({'erro': 'Produto não encontrado'}), 404
    salvar_cardapio(novo_lanches)
    return jsonify({'ok': True})

# ==================== API - PEDIDOS ====================

@app.route('/api/pedidos')
def api_pedidos():
    pedidos = []
    for p in gerenciador.obter_todos():
        itens_resumo = []
        for item in p.itens.values():
            itens_resumo.append({
                'nome': item.lanche.nome,
                'quantidade': item.quantidade,
                'subtotal': item.subtotal
            })
        pedidos.append({
            'id': p.id,
            'cliente': p.cliente_nome,
            'total': p.total,
            'tipo_entrega': p.tipo_entrega,
            'status': p.status,
            'data_hora': p.data_hora,
            'itens': itens_resumo,
            'pagamento': p.pagamento,    # ← CORRIGIDO: incluído campo pagamento
            'endereco': p.endereco       # ← CORRIGIDO: incluído campo endereco
        })
    return jsonify(pedidos)

@app.route('/api/pedido/<pedido_id>', methods=['GET'])
def api_detalhe_pedido(pedido_id):
    """Retorna detalhes completos de um pedido específico."""
    for p in gerenciador.obter_todos():
        if p.id == pedido_id:
            itens_resumo = []
            for item in p.itens.values():
                itens_resumo.append({
                    'nome': item.lanche.nome,
                    'quantidade': item.quantidade,
                    'subtotal': item.subtotal
                })
            return jsonify({
                'id': p.id,
                'cliente': p.cliente_nome,
                'total': p.total,
                'tipo_entrega': p.tipo_entrega,
                'status': p.status,
                'data_hora': p.data_hora,
                'itens': itens_resumo,
                'endereco': p.endereco,
                'pagamento': p.pagamento
            })
    return jsonify({'erro': 'Pedido não encontrado'}), 404

@app.route('/api/pedido/<pedido_id>/status', methods=['POST'])
def api_atualizar_status(pedido_id):
    dados = request.get_json()
    if not dados or 'status' not in dados:
        return jsonify({'erro': 'Status não informado'}), 400
    if gerenciador.atualizar_status(pedido_id, dados['status']):
        return jsonify({'ok': True})
    return jsonify({'erro': 'Pedido não encontrado'}), 404

# ==================== ARQUIVOS ESTÁTICOS ====================

@app.route('/imagens_produtos/<path:filename>')
def imagem(filename):
    return send_from_directory(IMAGES_DIR, filename)

# ==================== INICIALIZAÇÃO ====================

if __name__ == '__main__':
    os.makedirs(IMAGES_DIR, exist_ok=True)
    inicializar_cardapio()
    app.run(debug=True, host='0.0.0.0', port=5000)