import sqlite3
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from datetime import datetime
import hashlib
import os
import uuid
import csv

# Cores e fontes (mantidas iguais)
COLOR_PRIMARY = "#2c3e50"
COLOR_SECONDARY = "#34495e"
COLOR_ACCENT = "#3498db"
COLOR_SUCCESS = "#27ae60"
COLOR_DANGER = "#e74c3c"
COLOR_BG = "#ecf0f1"
COLOR_TEXT = "#2c3e50"

FONT_MAIN = ("Segoe UI", 10)
FONT_BOLD = ("Segoe UI", 10, "bold")
FONT_TITLE = ("Segoe UI", 18, "bold")


class Database:
    def __init__(self, db_name="Databaseerp.db"):
        self.conn = sqlite3.connect(db_name)
        self.create_tables()
        self.migrar_tabela_usuarios()  # Garante compatibilidade com bancos antigos

    def create_tables(self):
        cursor = self.conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt BLOB NOT NULL,
                role TEXT DEFAULT 'user'
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS clientes (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                email TEXT,
                telefone TEXT,
                observacoes TEXT,
                data_cadastro TEXT NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS produtos (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                preco_venda REAL NOT NULL CHECK(preco_venda >= 0),
                estoque INTEGER NOT NULL CHECK(estoque >= 0)
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS transacoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT NOT NULL,
                valor REAL NOT NULL,
                descricao TEXT,
                data TEXT NOT NULL
            )
        """)
        # Nova tabela: Log de estoque
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS estoque_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                produto_id TEXT,
                tipo TEXT,          -- 'ENTRADA' (compra) ou 'SAIDA' (venda)
                quantidade INTEGER,
                motivo TEXT,
                data TEXT NOT NULL,
                FOREIGN KEY (produto_id) REFERENCES produtos(id)
            )
        """)

        # Admin padrão
        cursor.execute("SELECT * FROM usuarios WHERE username = 'admin'")
        if not cursor.fetchone():
            salt = os.urandom(32)
            senha = "admin123"
            hash_pw = hashlib.pbkdf2_hmac('sha256', senha.encode(), salt, 100000).hex()
            cursor.execute(
                "INSERT INTO usuarios (id, username, password_hash, salt, role) VALUES (?, ?, ?, ?, ?)",
                (str(uuid.uuid4()), "admin", hash_pw, salt, "admin")
            )

        self.conn.commit()

    def migrar_tabela_usuarios(self):
        cursor = self.conn.cursor()
        cursor.execute("PRAGMA table_info(usuarios)")
        colunas = [row[1] for row in cursor.fetchall()]
        if 'password' in colunas and 'password_hash' not in colunas:
            print("Migrando tabela usuarios...")
            cursor.execute("ALTER TABLE usuarios RENAME TO usuarios_old")
            cursor.execute("""
                CREATE TABLE usuarios (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt BLOB NOT NULL,
                    role TEXT DEFAULT 'user'
                )
            """)
            cursor.execute("SELECT id, username, password, role FROM usuarios_old")
            for row in cursor.fetchall():
                old_id, username, old_pw, role = row
                salt = os.urandom(32)
                new_hash = hashlib.pbkdf2_hmac('sha256', old_pw.encode(), salt, 100000).hex()
                cursor.execute(
                    "INSERT INTO usuarios VALUES (?, ?, ?, ?, ?)",
                    (old_id, username, new_hash, salt, role or 'user')
                )
            cursor.execute("DROP TABLE usuarios_old")
            self.conn.commit()
            print("Migração de senhas concluída.")

    def query(self, sql, params=(), fetch_all=True):
        cursor = self.conn.cursor()
        try:
            cursor.execute(sql, params)
            if sql.strip().upper().startswith(('INSERT', 'UPDATE', 'DELETE')):
                self.conn.commit()
                return cursor.lastrowid if 'INSERT' in sql.upper() else None
            return cursor.fetchall() if fetch_all else cursor.fetchone()
        except Exception as e:
            self.conn.rollback()
            raise e


class ERPCore:
    def __init__(self):
        self.db = Database()

    def autenticar(self, username, password):
        user = self.db.query(
            "SELECT password_hash, salt FROM usuarios WHERE username = ?",
            (username,), fetch_all=False
        )
        if not user:
            return False
        hash_stored, salt = user
        hash_input = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000).hex()
        return hash_input == hash_stored

    def registrar_venda(self, produto_id, cliente_nome, quantidade, observacao=""):
        if quantidade <= 0:
            return False, "Quantidade inválida"
        prod = self.db.query(
            "SELECT nome, preco_venda, estoque FROM produtos WHERE id = ?",
            (produto_id,), fetch_all=False
        )
        if not prod:
            return False, "Produto não encontrado"
        nome_prod, preco, estoque = prod
        if estoque < quantidade:
            return False, f"Estoque insuficiente (disponível: {estoque})"

        valor_total = preco * quantidade
        novo_estoque = estoque - quantidade

        self.db.query("UPDATE produtos SET estoque = ? WHERE id = ?", (novo_estoque, produto_id))
        self.db.query(
            "INSERT INTO transacoes (tipo, valor, descricao, data) VALUES (?, ?, ?, ?)",
            ("ENTRADA", valor_total, f"Venda: {nome_prod} ×{quantidade} → {cliente_nome or 'Consumidor'} | Obs: {observacao}".strip(),
             datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        )
        # Log de estoque
        self.db.query(
            "INSERT INTO estoque_log (produto_id, tipo, quantidade, motivo, data) VALUES (?, ?, ?, ?, ?)",
            (produto_id, "SAIDA", quantidade, f"Venda para {cliente_nome or 'Consumidor'}", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        )
        return True, f"Venda de R$ {valor_total:,.2f} registrada"

    def registrar_compra(self, produto_id, quantidade, custo_unitario, fornecedor="", observacao=""):
        if quantidade <= 0 or custo_unitario < 0:
            return False, "Valores inválidos"
        prod = self.db.query(
            "SELECT nome, estoque FROM produtos WHERE id = ?",
            (produto_id,), fetch_all=False
        )
        if not prod:
            return False, "Produto não encontrado"
        nome_prod, estoque = prod

        valor_total = custo_unitario * quantidade
        novo_estoque = estoque + quantidade

        self.db.query("UPDATE produtos SET estoque = ? WHERE id = ?", (novo_estoque, produto_id))
        self.db.query(
            "INSERT INTO transacoes (tipo, valor, descricao, data) VALUES (?, ?, ?, ?)",
            ("SAIDA", valor_total, f"Compra: {nome_prod} +{quantidade} un | Fornecedor: {fornecedor} | Custo: R${custo_unitario:.2f} | Obs: {observacao}".strip(),
             datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        )
        # Log de estoque
        self.db.query(
            "INSERT INTO estoque_log (produto_id, tipo, quantidade, motivo, data) VALUES (?, ?, ?, ?, ?)",
            (produto_id, "ENTRADA", quantidade, f"Compra de {fornecedor or 'Fornecedor'}", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        )
        return True, f"Compra de R$ {valor_total:,.2f} registrada"

    def obter_dashboard_dados(self):
        saldo = self.db.query("SELECT COALESCE(SUM(CASE WHEN tipo='ENTRADA' THEN valor ELSE -valor END), 0) FROM transacoes", fetch_all=False)[0]
        total_clientes = self.db.query("SELECT COUNT(*) FROM clientes", fetch_all=False)[0]
        total_produtos = self.db.query("SELECT COUNT(*) FROM produtos", fetch_all=False)[0]
        estoque_baixo = self.db.query("SELECT * FROM produtos WHERE estoque <= 5 ORDER BY estoque", fetch_all=True)
        return {
            "saldo": saldo,
            "clientes": total_clientes,
            "produtos": total_produtos,
            "estoque_baixo": estoque_baixo
        }

    def listar_produtos(self):
        return self.db.query("SELECT id, nome, preco_venda, estoque FROM produtos ORDER BY nome")

    def adicionar_produto(self, nome, preco, estoque):
        if not nome.strip() or preco < 0 or estoque < 0:
            raise ValueError("Dados inválidos")
        pid = str(uuid.uuid4())[:8].upper()
        self.db.query("INSERT INTO produtos VALUES (?, ?, ?, ?)", (pid, nome.strip(), float(preco), int(estoque)))
        return pid

    def atualizar_produto(self, pid, nome, preco):
        self.db.query("UPDATE produtos SET nome = ?, preco_venda = ? WHERE id = ?", (nome.strip(), float(preco), pid))

    def excluir_produto(self, pid):
        self.db.query("DELETE FROM produtos WHERE id = ?", (pid,))

    def listar_clientes(self, termo=""):
        if termo:
            return self.db.query("SELECT id, nome, email, telefone, observacoes, data_cadastro FROM clientes WHERE nome LIKE ? OR id LIKE ? ORDER BY nome", (f"%{termo}%", f"%{termo}%"))
        return self.db.query("SELECT id, nome, email, telefone, observacoes, data_cadastro FROM clientes ORDER BY nome")

    def adicionar_cliente(self, nome, email, telefone, observacoes=""):
        if not nome.strip():
            raise ValueError("Nome é obrigatório")
        cid = str(uuid.uuid4())[:8].upper()
        data = datetime.now().strftime("%d/%m/%Y")
        self.db.query("INSERT INTO clientes VALUES (?, ?, ?, ?, ?, ?)", (cid, nome.strip(), email.strip() or None, telefone.strip() or None, observacoes.strip(), data))

    def atualizar_cliente(self, cid, nome, email, telefone, observacoes):
        self.db.query("UPDATE clientes SET nome=?, email=?, telefone=?, observacoes=? WHERE id=?", (nome.strip(), email.strip() or None, telefone.strip() or None, observacoes.strip(), cid))

    def excluir_cliente(self, cid):
        self.db.query("DELETE FROM clientes WHERE id = ?", (cid,))

    def listar_transacoes(self, data_inicio=None, data_fim=None):
        sql = "SELECT id, tipo, valor, descricao, data FROM transacoes WHERE 1=1"
        params = []
        if data_inicio:
            sql += " AND date(data) >= date(?)"
            params.append(data_inicio)
        if data_fim:
            sql += " AND date(data) <= date(?)"
            params.append(data_fim)
        sql += " ORDER BY data DESC LIMIT 200"
        return self.db.query(sql, tuple(params))

    def listar_estoque_log(self):
        return self.db.query("SELECT p.nome, l.tipo, l.quantidade, l.motivo, l.data FROM estoque_log l JOIN produtos p ON l.produto_id = p.id ORDER BY l.data DESC LIMIT 50")


class InterfaceERP:
    def __init__(self, root):
        self.core = ERPCore()
        self.root = root
        self.root.title("ERP Professional By: Anthony v1.2")
        self.root.geometry("1100x650")
        self.root.configure(bg=COLOR_BG)

        self.setup_styles()
        self.tela_login()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use('clam')
        style.configure("Treeview", font=FONT_MAIN, rowheight=25, background="white")
        style.configure("Treeview.Heading", font=FONT_BOLD)
        style.configure("TButton", font=FONT_BOLD)
        style.configure("Sidebar.TFrame", background=COLOR_PRIMARY)
        style.configure("Content.TFrame", background=COLOR_BG)
        style.map("Treeview", background=[('selected', COLOR_ACCENT)])

    def limpar_tela(self):
        for widget in self.root.winfo_children():
            widget.destroy()

    def tela_login(self):
        self.limpar_tela()
        frame = tk.Frame(self.root, bg="white", padx=40, pady=40, highlightbackground=COLOR_ACCENT, highlightthickness=2)
        frame.place(relx=0.5, rely=0.5, anchor="center")

        tk.Label(frame, text="LOGIN ERP", font=FONT_TITLE, bg="white", fg=COLOR_PRIMARY).pack(pady=(0, 20))

        tk.Label(frame, text="Usuário", bg="white", font=FONT_BOLD).pack(anchor="w")
        self.ent_user = ttk.Entry(frame, width=30)
        self.ent_user.pack(pady=(0, 10))
        self.ent_user.insert(0, "admin")

        tk.Label(frame, text="Senha", bg="white", font=FONT_BOLD).pack(anchor="w")
        self.ent_pass = ttk.Entry(frame, width=30, show="*")
        self.ent_pass.pack(pady=(0, 20))
        self.ent_pass.insert(0, "admin123")

        btn = tk.Button(frame, text="ENTRAR", bg=COLOR_ACCENT, fg="white", font=FONT_BOLD,
                        relief="flat", width=25, command=self.processar_login)
        btn.pack()

    def processar_login(self):
        u = self.ent_user.get().strip()
        p = self.ent_pass.get().strip()
        if self.core.autenticar(u, p):
            self.montar_layout_principal()
        else:
            messagebox.showerror("Erro", "Usuário ou senha inválidos!")

    def montar_layout_principal(self):
        self.limpar_tela()

        self.sidebar = ttk.Frame(self.root, style="Sidebar.TFrame", width=200)
        self.sidebar.pack(side="left", fill="y")
        self.sidebar.pack_propagate(False)

        tk.Label(self.sidebar, text="ERP ANTHONY DEVELOPER", font=FONT_TITLE, bg=COLOR_PRIMARY, fg="white", pady=20).pack()

        menus = [
            ("Dashboard", self.view_dashboard),
            ("Estoque", self.view_estoque),
            ("Clientes", self.view_clientes),
            ("Vendas/Financeiro", self.view_financeiro),
            ("Sair", self.tela_login)
        ]

        for texto, comando in menus:
            btn = tk.Button(self.sidebar, text=texto, font=FONT_BOLD, bg=COLOR_SECONDARY, fg="white",
                            relief="flat", pady=10, cursor="hand2", command=comando)
            btn.pack(fill="x", padx=5, pady=2)

        self.conteudo = ttk.Frame(self.root, style="Content.TFrame")
        self.conteudo.pack(side="right", fill="both", expand=True, padx=20, pady=20)

        self.view_dashboard()

    # Dashboard com alerta de estoque baixo
    def view_dashboard(self):
        for w in self.conteudo.winfo_children():
            w.destroy()

        dados = self.core.obter_dashboard_dados()

        tk.Label(self.conteudo, text="Dashboard Informativo", font=FONT_TITLE, bg=COLOR_BG).pack(anchor="w", pady=10)

        cards_frame = ttk.Frame(self.conteudo)
        cards_frame.pack(fill="x", pady=10)

        def criar_card(parent, titulo, valor, cor):
            f = tk.Frame(parent, bg="white", width=260, height=110, highlightbackground=cor, highlightthickness=2)
            f.pack(side="left", padx=12)
            f.pack_propagate(False)
            tk.Label(f, text=titulo, bg="white", font=FONT_BOLD).pack(pady=8)
            tk.Label(f, text=valor, bg="white", font=FONT_TITLE, fg=cor).pack()

        criar_card(cards_frame, "Saldo em Caixa", f"R$ {dados['saldo']:,.2f}", COLOR_SUCCESS)
        criar_card(cards_frame, "Total Clientes", dados['clientes'], COLOR_ACCENT)
        criar_card(cards_frame, "Itens no Estoque", dados['produtos'], COLOR_PRIMARY)

        if dados['estoque_baixo']:
            tk.Label(self.conteudo, text="⚠ Produtos com Estoque Baixo:", fg=COLOR_DANGER, bg=COLOR_BG, font=FONT_BOLD).pack(anchor="w", pady=5)
            tree_baixo = ttk.Treeview(self.conteudo, columns=("Nome", "Estoque"), show="headings", height=5)
            tree_baixo.heading("Nome", text="Nome")
            tree_baixo.heading("Estoque", text="Estoque")
            tree_baixo.column("Nome", width=200)
            tree_baixo.column("Estoque", width=80, anchor="center")
            for p in dados['estoque_baixo']:
                tree_baixo.insert("", "end", values=(p[1], p[3]))
            tree_baixo.pack(fill="x", pady=5)

    # Estoque (adicionado botão de compra)
    def view_estoque(self):
        for w in self.conteudo.winfo_children():
            w.destroy()

        top = ttk.Frame(self.conteudo)
        top.pack(fill="x", pady=10)

        tk.Label(top, text="Gestão de Estoque", font=FONT_TITLE, bg=COLOR_BG).pack(side="left")
        ttk.Button(top, text="+ Novo Produto", command=self.modal_produto).pack(side="right", padx=5)
        ttk.Button(top, text="+ Comprar Estoque", command=self.modal_compra).pack(side="right", padx=5)
        ttk.Button(top, text="Atualizar", command=lambda: self.atualizar_tree_estoque(tree)).pack(side="right", padx=5)

        colunas = ("ID", "Nome", "Preço R$", "Estoque")
        tree = ttk.Treeview(self.conteudo, columns=colunas, show="headings")
        for col in colunas:
            tree.heading(col, text=col)
        tree.column("ID", width=80, anchor="center")
        tree.column("Preço R$", width=100, anchor="e")
        tree.column("Estoque", width=80, anchor="center")

        self.atualizar_tree_estoque(tree)
        tree.pack(fill="both", expand=True, pady=5)

        btn_frame = ttk.Frame(self.conteudo)
        btn_frame.pack(fill="x", pady=8)
        ttk.Button(btn_frame, text="Editar selecionado", command=lambda: self.editar_produto(tree)).pack(side="left", padx=5)
        ttk.Button(btn_frame, text="Excluir selecionado", command=lambda: self.excluir_produto_tree(tree)).pack(side="left")

    def atualizar_tree_estoque(self, tree):
        for item in tree.get_children():
            tree.delete(item)
        for p in self.core.listar_produtos():
            tree.insert("", "end", values=(p[0], p[1], f"{p[2]:,.2f}", p[3]))

    def modal_produto(self, pid=None, valores=None):
        win = tk.Toplevel(self.root)
        win.title("Editar Produto" if pid else "Novo Produto")
        win.geometry("340x340")

        tk.Label(win, text="Nome do Produto:").pack(pady=(15,5))
        e_nome = ttk.Entry(win, width=38)
        e_nome.pack()

        tk.Label(win, text="Preço de Venda (R$):").pack(pady=(15,5))
        e_preco = ttk.Entry(win, width=38)
        e_preco.pack()

        tk.Label(win, text="Estoque Inicial:").pack(pady=(15,5))
        e_qtd = ttk.Entry(win, width=38)
        e_qtd.pack()

        if valores:
            e_nome.insert(0, valores[1])
            e_preco.insert(0, str(valores[2]))
            e_qtd.insert(0, str(valores[3]))
            e_qtd.config(state="disabled")

        def salvar():
            try:
                nome = e_nome.get().strip()
                preco = float(e_preco.get().replace(",", "."))
                if pid:
                    self.core.atualizar_produto(pid, nome, preco)
                else:
                    qtd = int(e_qtd.get())
                    self.core.adicionar_produto(nome, preco, qtd)
                messagebox.showinfo("OK", "Produto salvo")
                win.destroy()
                self.view_estoque()
            except Exception as e:
                messagebox.showerror("Erro", str(e))

        ttk.Button(win, text="Salvar", command=salvar).pack(pady=25)

    def editar_produto(self, tree):
        sel = tree.selection()
        if not sel: return messagebox.showwarning("Atenção", "Selecione um produto")
        valores = tree.item(sel[0])["values"]
        self.modal_produto(valores[0], valores)

    def excluir_produto_tree(self, tree):
        sel = tree.selection()
        if not sel: return
        pid = tree.item(sel[0])["values"][0]
        if messagebox.askyesno("Confirmação", f"Excluir {pid}?"):
            self.core.excluir_produto(pid)
            self.atualizar_tree_estoque(tree)

    # Novo: Modal de Compra de Estoque
    def modal_compra(self):
        win = tk.Toplevel(self.root)
        win.title("Registrar Compra de Estoque")
        win.geometry("400x420")

        tk.Label(win, text="ID do Produto:").pack(pady=(15,5))
        e_pid = ttk.Entry(win, width=45)
        e_pid.pack()

        tk.Label(win, text="Quantidade:").pack(pady=(12,5))
        e_qtd = ttk.Entry(win, width=45)
        e_qtd.pack()

        tk.Label(win, text="Custo Unitário (R$):").pack(pady=(12,5))
        e_custo = ttk.Entry(win, width=45)
        e_custo.pack()

        tk.Label(win, text="Fornecedor (opcional):").pack(pady=(12,5))
        e_fornecedor = ttk.Entry(win, width=45)
        e_fornecedor.pack()

        tk.Label(win, text="Observação:").pack(pady=(12,5))
        e_obs = tk.Text(win, width=45, height=4)
        e_obs.pack()

        def confirmar():
            try:
                qtd = int(e_qtd.get())
                custo = float(e_custo.get().replace(",", "."))
                sucesso, msg = self.core.registrar_compra(
                    e_pid.get().strip(),
                    qtd,
                    custo,
                    e_fornecedor.get().strip(),
                    e_obs.get("1.0", tk.END).strip()
                )
                if sucesso:
                    messagebox.showinfo("Sucesso", msg)
                    win.destroy()
                    self.view_estoque()
                else:
                    messagebox.showerror("Erro", msg)
            except ValueError as ve:
                messagebox.showerror("Erro", f"Formato inválido: {ve}")

        ttk.Button(win, text="Registrar Compra", command=confirmar).pack(pady=25)

    # Clientes com observações
    def view_clientes(self):
        for w in self.conteudo.winfo_children():
            w.destroy()

        tk.Label(self.conteudo, text="Gestão de Clientes", font=FONT_TITLE, bg=COLOR_BG).pack(anchor="w", pady=10)

        top = ttk.Frame(self.conteudo)
        top.pack(fill="x", pady=5)

        ttk.Label(top, text="Buscar:").pack(side="left")
        ent_busca = ttk.Entry(top, width=40)
        ent_busca.pack(side="left", padx=8)

        ttk.Button(top, text="Buscar", command=lambda: self.atualizar_tree_clientes(tree, ent_busca.get())).pack(side="left")
        ttk.Button(top, text="+ Novo Cliente", command=self.modal_cliente).pack(side="right")

        colunas = ("ID", "Nome", "Email", "Telefone", "Obs.", "Cadastro")
        tree = ttk.Treeview(self.conteudo, columns=colunas, show="headings")
        for col in colunas:
            tree.heading(col, text=col)
        tree.column("ID", width=80)
        tree.column("Obs.", width=150)
        tree.column("Cadastro", width=110)

        self.atualizar_tree_clientes(tree)
        tree.pack(fill="both", expand=True, pady=5)

        btn_frame = ttk.Frame(self.conteudo)
        btn_frame.pack(fill="x", pady=8)
        ttk.Button(btn_frame, text="Editar selecionado", command=lambda: self.editar_cliente(tree)).pack(side="left", padx=5)
        ttk.Button(btn_frame, text="Excluir selecionado", command=lambda: self.excluir_cliente_tree(tree)).pack(side="left")

    def atualizar_tree_clientes(self, tree, termo=""):
        for item in tree.get_children():
            tree.delete(item)
        for c in self.core.listar_clientes(termo):
            obs_curta = (c[4][:30] + "...") if c[4] and len(c[4]) > 30 else (c[4] or "")
            tree.insert("", "end", values=(c[0], c[1], c[2], c[3], obs_curta, c[5]))

    def modal_cliente(self, cid=None, valores=None):
        win = tk.Toplevel(self.root)
        win.title("Editar Cliente" if cid else "Novo Cliente")
        win.geometry("420x450")

        tk.Label(win, text="Nome:").pack(pady=(15,5))
        e_nome = ttk.Entry(win, width=48)
        e_nome.pack()

        tk.Label(win, text="E-mail:").pack(pady=(12,5))
        e_email = ttk.Entry(win, width=48)
        e_email.pack()

        tk.Label(win, text="Telefone:").pack(pady=(12,5))
        e_tel = ttk.Entry(win, width=48)
        e_tel.pack()

        tk.Label(win, text="Observações:").pack(pady=(12,5))
        e_obs = tk.Text(win, width=48, height=6)
        e_obs.pack()

        if valores:
            e_nome.insert(0, valores[1])
            e_email.insert(0, valores[2] or "")
            e_tel.insert(0, valores[3] or "")
            if valores[4]:
                e_obs.insert("1.0", valores[4])

        def salvar():
            try:
                obs = e_obs.get("1.0", tk.END).strip()
                if cid:
                    self.core.atualizar_cliente(cid, e_nome.get(), e_email.get(), e_tel.get(), obs)
                else:
                    self.core.adicionar_cliente(e_nome.get(), e_email.get(), e_tel.get(), obs)
                messagebox.showinfo("Sucesso", "Cliente salvo")
                win.destroy()
                self.view_clientes()
            except Exception as e:
                messagebox.showerror("Erro", str(e))

        ttk.Button(win, text="Salvar", command=salvar).pack(pady=25)

    def editar_cliente(self, tree):
        sel = tree.selection()
        if not sel: return messagebox.showwarning("Atenção", "Selecione um cliente")
        valores = tree.item(sel[0])["values"]
        # Busca observação completa
        cliente_completo = self.core.db.query("SELECT * FROM clientes WHERE id = ?", (valores[0],), fetch_all=False)
        self.modal_cliente(valores[0], cliente_completo)

    def excluir_cliente_tree(self, tree):
        sel = tree.selection()
        if not sel: return
        cid = tree.item(sel[0])["values"][0]
        if messagebox.askyesno("Confirmação", "Excluir cliente?"):
            self.core.excluir_cliente(cid)
            self.atualizar_tree_clientes(tree)

    # Financeiro com filtro de data + export CSV
    def view_financeiro(self):
        for w in self.conteudo.winfo_children():
            w.destroy()

        top = ttk.Frame(self.conteudo)
        top.pack(fill="x", pady=10)

        tk.Label(top, text="Histórico Financeiro", font=FONT_TITLE, bg=COLOR_BG).pack(side="left", padx=5)

        ttk.Label(top, text="De:").pack(side="left", padx=(20,5))
        self.ent_data_ini = ttk.Entry(top, width=12)
        self.ent_data_ini.pack(side="left")
        self.ent_data_ini.insert(0, datetime.now().strftime("%Y-%m-01"))

        ttk.Label(top, text="Até:").pack(side="left", padx=(10,5))
        self.ent_data_fim = ttk.Entry(top, width=12)
        self.ent_data_fim.pack(side="left")
        self.ent_data_fim.insert(0, datetime.now().strftime("%Y-%m-%d"))

        ttk.Button(top, text="Filtrar", command=lambda: self.atualizar_tree_financeiro(tree)).pack(side="left", padx=10)
        ttk.Button(top, text="Exportar CSV", command=self.exportar_csv).pack(side="right", padx=5)
        ttk.Button(top, text="Registrar Venda", command=self.modal_venda).pack(side="right", padx=5)

        colunas = ("ID", "Tipo", "Valor", "Descrição", "Data")
        tree = ttk.Treeview(self.conteudo, columns=colunas, show="headings")
        for col in colunas:
            tree.heading(col, text=col)
        tree.column("ID", width=60, anchor="center")
        tree.column("Tipo", width=80, anchor="center")
        tree.column("Valor", width=100, anchor="e")
        tree.column("Data", width=140)

        self.atualizar_tree_financeiro(tree)
        tree.pack(fill="both", expand=True, pady=5)

    def atualizar_tree_financeiro(self, tree):
        for item in tree.get_children():
            tree.delete(item)
        data_ini = self.ent_data_ini.get().strip() or None
        data_fim = self.ent_data_fim.get().strip() or None
        transacoes = self.core.listar_transacoes(data_ini, data_fim)
        for t in transacoes:
            tag = "entrada" if t[1] == "ENTRADA" else "saida"
            tree.insert("", "end", values=(t[0], t[1], f"{t[2]:,.2f}", t[3], t[4]), tags=(tag,))
        tree.tag_configure("entrada", foreground=COLOR_SUCCESS)
        tree.tag_configure("saida", foreground=COLOR_DANGER)

    def modal_venda(self):
        win = tk.Toplevel(self.root)
        win.title("Registrar Venda")
        win.geometry("400x380")

        tk.Label(win, text="ID do Produto:").pack(pady=(15,5))
        e_pid = ttk.Entry(win, width=45)
        e_pid.pack()

        tk.Label(win, text="Nome do Cliente (opcional):").pack(pady=(12,5))
        e_cli = ttk.Entry(win, width=45)
        e_cli.pack()

        tk.Label(win, text="Quantidade:").pack(pady=(12,5))
        e_qtd = ttk.Entry(win, width=45)
        e_qtd.pack()

        tk.Label(win, text="Observação:").pack(pady=(12,5))
        e_obs = tk.Text(win, width=45, height=5)
        e_obs.pack()

        def confirmar():
            try:
                qtd = int(e_qtd.get())
                obs = e_obs.get("1.0", tk.END).strip()
                sucesso, msg = self.core.registrar_venda(e_pid.get().strip(), e_cli.get().strip(), qtd, obs)
                if sucesso:
                    messagebox.showinfo("Sucesso", msg)
                    win.destroy()
                    self.view_financeiro()
                else:
                    messagebox.showerror("Erro", msg)
            except ValueError:
                messagebox.showerror("Erro", "Quantidade inválida")

        ttk.Button(win, text="Finalizar Venda", command=confirmar).pack(pady=25)

    def exportar_csv(self):
        arquivo = filedialog.asksaveasfilename(defaultextension=".csv", filetypes=[("CSV", "*.csv")])
        if not arquivo:
            return
        transacoes = self.core.listar_transacoes()
        with open(arquivo, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(["ID", "Tipo", "Valor", "Descrição", "Data"])
            for t in transacoes:
                writer.writerow(t)
        messagebox.showinfo("Exportado", f"Transações exportadas para:\n{arquivo}")


if __name__ == "__main__":
    root = tk.Tk()
    app = InterfaceERP(root)
    root.mainloop()