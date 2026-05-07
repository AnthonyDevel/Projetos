import sqlite3
import tkinter as tk
from tkinter import messagebox, ttk, filedialog
import os
from datetime import datetime
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.lib import colors
import hashlib
import json
from PIL import Image as PILImage, ImageTk
import qrcode
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4

# ==============================
# CONFIGURAÇÕES GLOBAIS
# ==============================
DB_CONN = None
DOCUMENT_TYPES = ["Others", "Judiciary", "Enterprise", "Business"]
JUDICIARY_SUBTYPES = ["Escritura", "Procuração", "Intimação", "Sentença", "Petição"]
CATEGORIES = ["Contas a Pagar", "Contabilidade", "Contas a Receber", "Geral", "Recursos Humanos", "Notas Fiscais"]
STATUS_OPTIONS = ["Armazenado", "Transferido", "Emitido", "Ecenerado", "Digitalizado"]
TEMP_CHART_FILE = "temp_chart.png"
DB_FILE_NAME = "sistema_documentos_v6.db"
NFE_FOLDER = "notas_fiscais"

# Criar pasta para notas fiscais se não existir
if not os.path.exists(NFE_FOLDER):
    os.makedirs(NFE_FOLDER)


# ==============================
# FUNÇÕES DO BANCO DE DADOS
# ==============================
def init_db():
    conn = sqlite3.connect(DB_FILE_NAME)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("PRAGMA foreign_keys = ON;")

    # Tabela de empresas
    cursor.execute("""CREATE TABLE IF NOT EXISTS companies (
        id INTEGER PRIMARY KEY, 
        name TEXT NOT NULL UNIQUE,
        cnpj TEXT,
        address TEXT,
        phone TEXT,
        email TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""")

    # Tabela de documentos
    cursor.execute("""CREATE TABLE IF NOT EXISTS documents (
        id INTEGER PRIMARY KEY, 
        name TEXT NOT NULL, 
        document_type TEXT NOT NULL, 
        category TEXT NOT NULL, 
        company_id INTEGER, 
        box_number TEXT,
        quantity INTEGER DEFAULT 1, 
        status TEXT DEFAULT 'Armazenado', 
        location TEXT,
        description TEXT,
        reference_number TEXT,
        creation_date TEXT,
        expiration_date TEXT,
        responsible_person TEXT,
        notes TEXT,
        digital_hash TEXT,
        scanned_copy_path TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL
    )""")

    # Tabela de transferências
    cursor.execute("""CREATE TABLE IF NOT EXISTS transfers (
        id INTEGER PRIMARY KEY, 
        document_id INTEGER NOT NULL, 
        action TEXT NOT NULL, 
        from_location TEXT, 
        to_location TEXT,
        responsible_person TEXT,
        transfer_reason TEXT,
        transfer_date TEXT DEFAULT CURRENT_TIMESTAMP,
        notes TEXT,
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
    )""")

    # Tabela de notas fiscais
    cursor.execute("""CREATE TABLE IF NOT EXISTS invoices (
        id INTEGER PRIMARY KEY,
        document_id INTEGER,
        invoice_number TEXT UNIQUE NOT NULL,
        invoice_type TEXT NOT NULL,
        series TEXT,
        value REAL,
        issue_date TEXT,
        due_date TEXT,
        issuer_name TEXT,
        issuer_cnpj TEXT,
        recipient_name TEXT,
        recipient_cnpj TEXT,
        description TEXT,
        digital_signature TEXT,
        qr_code_path TEXT,
        pdf_path TEXT,
        status TEXT DEFAULT 'Emitida',
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
    )""")

    # Tabela de usuários
    cursor.execute("""CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        full_name TEXT,
        email TEXT,
        role TEXT DEFAULT 'user',
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""")

    # Tabela de logs de atividades
    cursor.execute("""CREATE TABLE IF NOT EXISTS activity_logs (
        id INTEGER PRIMARY KEY,
        user_id INTEGER,
        action TEXT NOT NULL,
        description TEXT,
        timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id)
    )""")

    # Inserir usuário padrão se não existir
    cursor.execute("SELECT * FROM users WHERE username = 'Anthony'")
    if not cursor.fetchone():
        password_hash = hashlib.sha256("Pass_".encode()).hexdigest()
        cursor.execute(
            "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)",
            ("Anthony", password_hash, "Administrador", "admin")
        )

    conn.commit()
    return conn


def get_companies():
    if not DB_CONN: return []
    return DB_CONN.cursor().execute("SELECT id, name FROM companies ORDER BY name").fetchall()


def get_company_id_by_name(name):
    if not DB_CONN or not name: return None
    res = DB_CONN.cursor().execute("SELECT id FROM companies WHERE name = ?", (name,)).fetchone()
    return res['id'] if res else None


def log_activity(user_id, action, description=""):
    """Registra atividade no sistema"""
    if not DB_CONN: return
    DB_CONN.cursor().execute(
        "INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)",
        (user_id, action, description)
    )
    DB_CONN.commit()


# ==============================
# JANELA DE LOGIN E INICIALIZAÇÃO
# ==============================
class LoginWindow:
    def __init__(self, root):
        self.root = root
        self.root.title("Login - Sistema Dark Tech")
        self.root.geometry("400x300")
        self.root.resizable(False, False)

        # Configurar estilo dark
        self.root.configure(bg='#0a1928')

        # Frame central
        frame = tk.Frame(root, bg='#1a2b3c', padx=30, pady=30, relief='ridge', bd=2)
        frame.place(relx=0.5, rely=0.5, anchor='center')

        # Título
        tk.Label(frame, text="SISTEMA DE GESTÃO",
                 font=("Helvetica", 16, "bold"),
                 bg='#1a2b3c',
                 fg='#4a9eff').pack(pady=(0, 20))

        tk.Label(frame, text="Usuário:",
                 font=("Helvetica", 10),
                 bg='#1a2b3c',
                 fg='#e0e0e0').pack(anchor='w')

        self.user_entry = tk.Entry(frame, font=("Helvetica", 11),
                                   bg='#2c3e50', fg='white',
                                   insertbackground='white',
                                   relief='flat', bd=5)
        self.user_entry.pack(fill='x', pady=(0, 15))

        tk.Label(frame, text="Senha:",
                 font=("Helvetica", 10),
                 bg='#1a2b3c',
                 fg='#e0e0e0').pack(anchor='w')

        self.pass_entry = tk.Entry(frame, show="*", font=("Helvetica", 11),
                                   bg='#2c3e50', fg='white',
                                   insertbackground='white',
                                   relief='flat', bd=5)
        self.pass_entry.pack(fill='x', pady=(0, 20))

        tk.Button(frame, text="ENTRAR",
                  command=self.check_login,
                  bg='#4a9eff', fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat', bd=0,
                  cursor='hand2').pack(fill='x', pady=10)

        self.user_entry.focus_set()
        self.root.bind('<Return>', lambda e: self.check_login())

    def check_login(self):
        username = self.user_entry.get()
        password = self.pass_entry.get()
        password_hash = hashlib.sha256(password.encode()).hexdigest()

        cursor = DB_CONN.cursor()
        user = cursor.execute(
            "SELECT * FROM users WHERE username = ? AND password_hash = ?",
            (username, password_hash)
        ).fetchone()

        if user:
            self.root.destroy()
            open_main_app(user)
        else:
            messagebox.showerror("Erro", "Usuário ou senha incorretos.")


def open_main_app(user):
    app_root = tk.Tk()
    DocumentApp(app_root, user)
    app_root.protocol("WM_DELETE_WINDOW", lambda: on_closing(app_root))
    app_root.mainloop()


def on_closing(root):
    if DB_CONN: DB_CONN.close()
    if os.path.exists(TEMP_CHART_FILE): os.remove(TEMP_CHART_FILE)
    root.destroy()


# ==============================
# CLASSE PRINCIPAL DA APLICAÇÃO
# ==============================
class DocumentApp:
    def __init__(self, root, user):
        self.root = root
        self.user = user
        self.root.title("Sistema de Gerenciamento de Documentos - Dark Tech")
        self.root.state('zoomed')

        # Cores dark tech: azul escuro e cinza gelo
        self.colors = {
            "primary_bg": "#0a1928",  # Azul escuro profundo
            "secondary_bg": "#1a2b3c",  # Azul escuro médio
            "tertiary_bg": "#2c3e50",  # Azul escuro claro
            "accent_blue": "#4a9eff",  # Azul brilhante
            "accent_teal": "#64b5f6",  # Azul médio
            "text_primary": "#e0e0e0",  # Cinza gelo claro
            "text_secondary": "#b0b0b0",  # Cinza gelo médio
            "header": "#7cb9ff",  # Azul claro
            "danger": "#ff4444",  # Vermelho para ações críticas
            "success": "#4caf50",  # Verde para sucesso
            "warning": "#ffbb33",  # Amarelo para alertas
            "row_alt": "#1e2f40",  # Alternância de linhas
            "border": "#3a4b5c",  # Cor de borda
            "hover": "#3d5a73"  # Cor de hover
        }

        # Configurar estilo
        style = ttk.Style()
        style.theme_use("clam")

        # Configurar cores do tema
        style.configure("Treeview",
                        background=self.colors['secondary_bg'],
                        foreground=self.colors['text_primary'],
                        fieldbackground=self.colors['secondary_bg'],
                        borderwidth=0)

        style.configure("Treeview.Heading",
                        background=self.colors['tertiary_bg'],
                        foreground=self.colors['text_primary'],
                        font=("Helvetica", 10, "bold"),
                        relief="flat")

        style.map('Treeview',
                  background=[('selected', self.colors['accent_blue'])],
                  foreground=[('selected', 'white')])

        style.configure("TNotebook",
                        background=self.colors['primary_bg'],
                        borderwidth=0)

        style.configure("TNotebook.Tab",
                        background=self.colors['secondary_bg'],
                        foreground=self.colors['text_primary'],
                        padding=[10, 5])

        style.map("TNotebook.Tab",
                  background=[('selected', self.colors['accent_blue'])],
                  foreground=[('selected', 'white')])

        # Sidebar
        sidebar = tk.Frame(root, bg=self.colors['secondary_bg'], width=250, relief="ridge", bd=2)
        sidebar.pack(side="left", fill="y", padx=2, pady=2)

        # Main content
        self.main_content = tk.Frame(root, bg=self.colors['primary_bg'])
        self.main_content.pack(side="right", fill="both", expand=True, padx=2, pady=2)

        self.current_frame = None

        # Header do sidebar
        header_frame = tk.Frame(sidebar, bg=self.colors['tertiary_bg'], height=80)
        header_frame.pack(fill="x", pady=(0, 20))
        header_frame.pack_propagate(False)

        tk.Label(header_frame, text="DARK TECH",
                 font=("Helvetica", 18, "bold"),
                 bg=self.colors['tertiary_bg'],
                 fg=self.colors['accent_blue']).pack(expand=True)

        tk.Label(header_frame, text=f"Usuário: {self.user['full_name']}",
                 font=("Helvetica", 9),
                 bg=self.colors['tertiary_bg'],
                 fg=self.colors['text_secondary']).pack()

        # Menu buttons
        menu_items = [
            ("📋 VISUALIZAR", PreviewDocumentsFrame),
            ("➕ ADICIONAR", AddDocumentFrame),
            ("✏️ MODIFICAR", ModifyDocumentFrame),
            ("🔄 TRANSFERIR", TransferDocumentFrame),
            ("📄 DIGITALIZAR", ScanDocumentFrame),
            ("💰 NOTA FISCAL", InvoiceFrame),
            ("🏢 EMPRESAS", ManageCompaniesFrame),
            ("📊 RELATÓRIOS", ReportsFrame),
            ("🗑️ EXCLUIR", DeleteDocumentFrame)
        ]

        for text, frame_class in menu_items:
            btn = tk.Button(sidebar, text=text,
                            command=lambda fc=frame_class: self.show_frame(fc),
                            bg=self.colors['tertiary_bg'],
                            fg=self.colors['text_primary'],
                            font=("Helvetica", 10),
                            relief="flat",
                            bd=0,
                            cursor='hand2',
                            anchor='w',
                            padx=20)
            btn.pack(fill="x", pady=1)

            # Efeito hover
            btn.bind("<Enter>", lambda e, b=btn: b.config(bg=self.colors['hover']))
            btn.bind("<Leave>", lambda e, b=btn: b.config(bg=self.colors['tertiary_bg']))

        # Botão sair no final
        tk.Frame(sidebar, bg=self.colors['secondary_bg'], height=20).pack(fill="x", expand=True)

        exit_btn = tk.Button(sidebar, text="🚪 SAIR",
                             command=lambda: on_closing(root),
                             bg=self.colors['danger'],
                             fg="white",
                             font=("Helvetica", 10, "bold"),
                             relief="flat",
                             bd=0,
                             cursor='hand2')
        exit_btn.pack(fill="x", pady=10, padx=10)

        self.show_frame(PreviewDocumentsFrame)

    def show_frame(self, frame_class):
        if self.current_frame:
            self.current_frame.destroy()
        self.current_frame = frame_class(self.main_content, self.colors, self.user)
        self.current_frame.pack(fill="both", expand=True)


# ==============================
# FRAMES DE CONTEÚDO
# ==============================
class BaseFrame(tk.Frame):
    def __init__(self, master, colors, user, title):
        super().__init__(master, bg=colors['primary_bg'])
        self.colors = colors
        self.user = user

        # Header com título
        header = tk.Frame(self, bg=colors['tertiary_bg'], height=50)
        header.pack(fill="x", pady=(0, 20))
        header.pack_propagate(False)

        tk.Label(header, text=title,
                 font=("Helvetica", 16, "bold"),
                 bg=colors['tertiary_bg'],
                 fg=colors['header']).pack(expand=True)


class PreviewDocumentsFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "VISUALIZAÇÃO DE DOCUMENTOS")

        # Barra de ferramentas
        toolbar = tk.Frame(self, bg=colors['secondary_bg'], height=40)
        toolbar.pack(fill="x", padx=10, pady=(0, 10))
        toolbar.pack_propagate(False)

        # Botões de ação
        tk.Button(toolbar, text="🔄 Atualizar",
                  command=self.load_documents,
                  bg=colors['accent_blue'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5, pady=5)

        tk.Button(toolbar, text="🔍 Buscar",
                  command=self.search_documents,
                  bg=colors['accent_teal'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5, pady=5)

        tk.Button(toolbar, text="📊 Exportar",
                  command=self.export_to_excel,
                  bg=colors['success'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5, pady=5)

        # Campo de busca
        self.search_var = tk.StringVar()
        self.search_var.trace('w', lambda *args: self.filter_documents())

        search_frame = tk.Frame(toolbar, bg=colors['secondary_bg'])
        search_frame.pack(side='right', padx=10)

        tk.Label(search_frame, text="Buscar:",
                 bg=colors['secondary_bg'],
                 fg=colors['text_primary']).pack(side='left', padx=5)

        self.search_entry = tk.Entry(search_frame, textvariable=self.search_var,
                                     bg=colors['tertiary_bg'],
                                     fg=colors['text_primary'],
                                     insertbackground='white',
                                     relief='flat')
        self.search_entry.pack(side='left', padx=5)

        # Treeview com scrollbars
        tree_frame = tk.Frame(self, bg=colors['primary_bg'])
        tree_frame.pack(fill="both", expand=True, padx=10, pady=5)

        # Scrollbars
        vsb = tk.Scrollbar(tree_frame, orient="vertical")
        hsb = tk.Scrollbar(tree_frame, orient="horizontal")

        # Colunas
        cols = ("ID", "Nome", "Tipo", "Categoria", "Empresa", "Nº Caixa",
                "Qtd", "Status", "Localização", "Nº Ref", "Responsável", "Criado em")

        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings",
                                 yscrollcommand=vsb.set, xscrollcommand=hsb.set,
                                 height=20)

        vsb.config(command=self.tree.yview)
        hsb.config(command=self.tree.xview)

        # Configurar colunas
        column_widths = [50, 250, 120, 120, 150, 80, 50, 100, 150, 100, 150, 100]
        for col, width in zip(cols, column_widths):
            self.tree.heading(col, text=col)
            self.tree.column(col, width=width, anchor='center' if col in ["ID", "Qtd"] else 'w')

        # Grid layout
        self.tree.grid(row=0, column=0, sticky='nsew')
        vsb.grid(row=0, column=1, sticky='ns')
        hsb.grid(row=1, column=0, sticky='ew')

        tree_frame.grid_rowconfigure(0, weight=1)
        tree_frame.grid_columnconfigure(0, weight=1)

        # Tags para cores alternadas
        self.tree.tag_configure('oddrow', background=colors['row_alt'])
        self.tree.tag_configure('evenrow', background=colors['secondary_bg'])

        # Status bar
        self.status_bar = tk.Label(self, text="", bd=1, relief=tk.SUNKEN, anchor=tk.W,
                                   bg=colors['tertiary_bg'],
                                   fg=colors['text_secondary'])
        self.status_bar.pack(side=tk.BOTTOM, fill=tk.X)

        self.load_documents()

    def load_documents(self):
        for item in self.tree.get_children():
            self.tree.delete(item)

        query = """
            SELECT d.id, d.name, d.document_type, d.category, IFNULL(c.name, '-'),
                   d.box_number, d.quantity, d.status, d.location, 
                   IFNULL(d.reference_number, '-'), IFNULL(d.responsible_person, '-'),
                   strftime('%d/%m/%Y', d.created_at)
            FROM documents d 
            LEFT JOIN companies c ON d.company_id = c.id 
            ORDER BY d.id DESC
        """

        rows = DB_CONN.cursor().execute(query).fetchall()
        for i, row in enumerate(rows):
            tag = 'evenrow' if i % 2 == 0 else 'oddrow'
            self.tree.insert("", "end", values=tuple(row), tags=(tag,))

        self.status_bar.config(text=f"  Total de Documentos: {len(rows)}")

    def filter_documents(self):
        search_term = self.search_var.get().lower()
        if not search_term:
            self.load_documents()
            return

        for item in self.tree.get_children():
            self.tree.delete(item)

        query = """
            SELECT d.id, d.name, d.document_type, d.category, IFNULL(c.name, '-'),
                   d.box_number, d.quantity, d.status, d.location, 
                   IFNULL(d.reference_number, '-'), IFNULL(d.responsible_person, '-'),
                   strftime('%d/%m/%Y', d.created_at)
            FROM documents d 
            LEFT JOIN companies c ON d.company_id = c.id 
            WHERE LOWER(d.name) LIKE ? OR LOWER(d.reference_number) LIKE ? OR LOWER(c.name) LIKE ?
            ORDER BY d.id DESC
        """

        search_pattern = f"%{search_term}%"
        rows = DB_CONN.cursor().execute(query, (search_pattern, search_pattern, search_pattern)).fetchall()

        for i, row in enumerate(rows):
            tag = 'evenrow' if i % 2 == 0 else 'oddrow'
            self.tree.insert("", "end", values=tuple(row), tags=(tag,))

        self.status_bar.config(text=f"  Resultados: {len(rows)}")

    def search_documents(self):
        # Abrir janela de busca avançada
        search_window = tk.Toplevel(self)
        search_window.title("Busca Avançada")
        search_window.geometry("400x500")
        search_window.configure(bg=self.colors['secondary_bg'])

        # Campos de busca
        fields = ["Nome", "Tipo", "Categoria", "Empresa", "Status", "Nº Referência"]
        entries = {}

        for i, field in enumerate(fields):
            tk.Label(search_window, text=field + ":",
                     bg=self.colors['secondary_bg'],
                     fg=self.colors['text_primary']).grid(row=i, column=0, padx=10, pady=5, sticky='w')

            entry = tk.Entry(search_window, width=30,
                             bg=self.colors['tertiary_bg'],
                             fg=self.colors['text_primary'],
                             insertbackground='white')
            entry.grid(row=i, column=1, padx=10, pady=5)
            entries[field.lower()] = entry

        def perform_search():
            # Construir query dinâmica
            conditions = []
            params = []

            for field, entry in entries.items():
                value = entry.get().strip()
                if value:
                    if field == "nome":
                        conditions.append("d.name LIKE ?")
                    elif field == "tipo":
                        conditions.append("d.document_type LIKE ?")
                    elif field == "categoria":
                        conditions.append("d.category LIKE ?")
                    elif field == "empresa":
                        conditions.append("c.name LIKE ?")
                    elif field == "status":
                        conditions.append("d.status LIKE ?")
                    elif field == "nº referência":
                        conditions.append("d.reference_number LIKE ?")
                    params.append(f"%{value}%")

            where_clause = " AND ".join(conditions) if conditions else "1=1"

            query = f"""
                SELECT d.id, d.name, d.document_type, d.category, IFNULL(c.name, '-'),
                       d.box_number, d.quantity, d.status, d.location, 
                       IFNULL(d.reference_number, '-'), IFNULL(d.responsible_person, '-'),
                       strftime('%d/%m/%Y', d.created_at)
                FROM documents d 
                LEFT JOIN companies c ON d.company_id = c.id 
                WHERE {where_clause}
                ORDER BY d.id DESC
            """

            for item in self.tree.get_children():
                self.tree.delete(item)

            rows = DB_CONN.cursor().execute(query, params).fetchall()
            for i, row in enumerate(rows):
                tag = 'evenrow' if i % 2 == 0 else 'oddrow'
                self.tree.insert("", "end", values=tuple(row), tags=(tag,))

            self.status_bar.config(text=f"  Resultados: {len(rows)}")
            search_window.destroy()

        tk.Button(search_window, text="Buscar",
                  command=perform_search,
                  bg=self.colors['accent_blue'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').grid(row=len(fields), column=0, columnspan=2, pady=20)

    def export_to_excel(self):
        # Implementar exportação para Excel
        messagebox.showinfo("Info", "Exportação para Excel será implementada em breve.")


class AddDocumentFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "ADICIONAR DOCUMENTO")

        # Criar notebook para abas
        notebook = ttk.Notebook(self)
        notebook.pack(fill='both', expand=True, padx=10, pady=10)

        # Aba de informações básicas
        basic_frame = tk.Frame(notebook, bg=colors['primary_bg'])
        notebook.add(basic_frame, text="Informações Básicas")

        # Aba de informações detalhadas
        details_frame = tk.Frame(notebook, bg=colors['primary_bg'])
        notebook.add(details_frame, text="Detalhes")

        # Aba de digitalização
        scan_frame = tk.Frame(notebook, bg=colors['primary_bg'])
        notebook.add(scan_frame, text="Digitalização")

        self.entries = {}

        # ===== INFORMAÇÕES BÁSICAS =====
        basic_fields = [
            ("Nome do Documento:", "entry", 50),
            ("Tipo Principal:", "combobox", DOCUMENT_TYPES),
            ("Subtipo (Judiciário):", "combobox", JUDICIARY_SUBTYPES),
            ("Categoria:", "combobox", CATEGORIES),
            ("Empresa:", "combobox", []),
            ("Nº da Caixa:", "entry", 20),
            ("Quantidade:", "spinbox", (1, 10000)),
            ("Status:", "combobox", STATUS_OPTIONS),
            ("Localização:", "entry", 50)
        ]

        for i, (label, widget_type, widget_args) in enumerate(basic_fields):
            tk.Label(basic_frame, text=label,
                     bg=colors['primary_bg'],
                     fg=colors['text_primary']).grid(row=i, column=0, sticky='w', pady=5, padx=10)

            if widget_type == "entry":
                entry = tk.Entry(basic_frame, width=widget_args,
                                 bg=colors['tertiary_bg'],
                                 fg=colors['text_primary'],
                                 insertbackground='white',
                                 relief='flat')
                entry.grid(row=i, column=1, sticky='ew', padx=10, pady=5)
                self.entries[label] = entry

            elif widget_type == "combobox":
                combo = ttk.Combobox(basic_frame, values=widget_args, state="readonly", width=30)
                combo.grid(row=i, column=1, sticky='ew', padx=10, pady=5)
                self.entries[label] = combo

            elif widget_type == "spinbox":
                spin = tk.Spinbox(basic_frame, from_=widget_args[0], to=widget_args[1],
                                  bg=colors['tertiary_bg'],
                                  fg=colors['text_primary'],
                                  buttonbackground=colors['tertiary_bg'],
                                  relief='flat')
                spin.grid(row=i, column=1, sticky='ew', padx=10, pady=5)
                spin.delete(0, tk.END)
                spin.insert(0, "1")
                self.entries[label] = spin

        # Configurar grid
        basic_frame.columnconfigure(1, weight=1)

        # ===== DETALHES =====
        detail_fields = [
            ("Número de Referência:", "entry", 30),
            ("Data de Criação (DD/MM/AAAA):", "entry", 20),
            ("Data de Expiração (DD/MM/AAAA):", "entry", 20),
            ("Pessoa Responsável:", "entry", 30),
            ("Descrição:", "text", (50, 3)),
            ("Observações:", "text", (50, 3))
        ]

        for i, (label, widget_type, widget_args) in enumerate(detail_fields):
            tk.Label(details_frame, text=label,
                     bg=colors['primary_bg'],
                     fg=colors['text_primary']).grid(row=i, column=0, sticky='nw', pady=5, padx=10)

            if widget_type == "entry":
                entry = tk.Entry(details_frame, width=widget_args,
                                 bg=colors['tertiary_bg'],
                                 fg=colors['text_primary'],
                                 insertbackground='white',
                                 relief='flat')
                entry.grid(row=i, column=1, sticky='ew', padx=10, pady=5)
                self.entries[label] = entry

            elif widget_type == "text":
                text = tk.Text(details_frame, width=widget_args[0], height=widget_args[1],
                               bg=colors['tertiary_bg'],
                               fg=colors['text_primary'],
                               insertbackground='white',
                               relief='flat')
                text.grid(row=i, column=1, sticky='ew', padx=10, pady=5)
                self.entries[label] = text

        details_frame.columnconfigure(1, weight=1)

        # ===== DIGITALIZAÇÃO =====
        tk.Label(scan_frame, text="Arquivo Digitalizado:",
                 bg=colors['primary_bg'],
                 fg=colors['text_primary']).pack(anchor='w', padx=10, pady=5)

        scan_file_frame = tk.Frame(scan_frame, bg=colors['primary_bg'])
        scan_file_frame.pack(fill='x', padx=10, pady=5)

        self.scan_path = tk.StringVar()
        scan_entry = tk.Entry(scan_file_frame, textvariable=self.scan_path,
                              bg=colors['tertiary_bg'],
                              fg=colors['text_primary'],
                              state='readonly',
                              relief='flat')
        scan_entry.pack(side='left', fill='x', expand=True)

        tk.Button(scan_file_frame, text="📁 Selecionar",
                  command=self.select_scan_file,
                  bg=colors['accent_blue'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='right', padx=5)

        tk.Button(scan_frame, text="📷 Digitalizar",
                  command=self.scan_document,
                  bg=colors['accent_teal'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(pady=10)

        # Preview da imagem
        self.preview_label = tk.Label(scan_frame, bg=colors['secondary_bg'],
                                      text="Pré-visualização da digitalização")
        self.preview_label.pack(pady=10, padx=10, fill='both', expand=True)

        # Botão principal
        tk.Button(self, text="💾 SALVAR DOCUMENTO",
                  command=self.add_document,
                  bg=colors['success'],
                  fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat',
                  cursor='hand2').pack(pady=20)

        # Carregar empresas
        self.update_companies()

    def update_companies(self):
        companies = [c['name'] for c in get_companies()]
        self.entries["Empresa:"]['values'] = [""] + companies

    def select_scan_file(self):
        filename = filedialog.askopenfilename(
            title="Selecionar arquivo digitalizado",
            filetypes=[("Imagens", "*.png *.jpg *.jpeg *.pdf"), ("Todos", "*.*")]
        )
        if filename:
            self.scan_path.set(filename)
            self.show_preview(filename)

    def scan_document(self):
        messagebox.showinfo("Digitalização", "Função de digitalização será implementada com integração de scanner.")

    def show_preview(self, filename):
        try:
            if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
                img = PILImage.open(filename)
                img.thumbnail((300, 300))
                photo = ImageTk.PhotoImage(img)
                self.preview_label.config(image=photo, text='')
                self.preview_label.image = photo
        except Exception as e:
            messagebox.showerror("Erro", f"Não foi possível carregar a imagem: {str(e)}")

    def parse_date(self, date_str):
        if not date_str.strip():
            return None
        try:
            return datetime.strptime(date_str, "%d/%m/%Y").strftime("%Y-%m-%d")
        except ValueError:
            return None

    def add_document(self):
        # Validar campos obrigatórios
        name = self.entries["Nome do Documento:"].get().strip()
        main_type = self.entries["Tipo Principal:"].get()
        subtype = self.entries["Subtipo (Judiciário):"].get()
        category = self.entries["Categoria:"].get()

        doc_type = subtype if main_type == 'Judiciary' and subtype else main_type

        if not name or not doc_type or not category:
            return messagebox.showwarning("Inválido", "Nome, Tipo e Categoria são obrigatórios.")

        # Processar datas
        creation_date = self.parse_date(self.entries["Data de Criação (DD/MM/AAAA):"].get())
        expiration_date = self.parse_date(self.entries["Data de Expiração (DD/MM/AAAA):"].get())

        # Copiar arquivo digitalizado se existir
        scanned_copy = None
        if self.scan_path.get():
            import shutil
            filename = os.path.basename(self.scan_path.get())
            dest_path = os.path.join("scanned_docs", filename)
            os.makedirs("scanned_docs", exist_ok=True)
            shutil.copy2(self.scan_path.get(), dest_path)
            scanned_copy = dest_path

        params = (
            name, doc_type, category,
            get_company_id_by_name(self.entries["Empresa:"].get()),
            self.entries["Nº da Caixa:"].get(),
            self.entries["Quantidade:"].get(),
            self.entries["Status:"].get(),
            self.entries["Localização:"].get(),
            self.entries["Descrição:"].get("1.0", tk.END).strip() if "Descrição:" in self.entries else "",
            self.entries["Número de Referência:"].get() if "Número de Referência:" in self.entries else "",
            creation_date, expiration_date,
            self.entries["Pessoa Responsável:"].get() if "Pessoa Responsável:" in self.entries else "",
            self.entries["Observações:"].get("1.0", tk.END).strip() if "Observações:" in self.entries else "",
            None,  # digital_hash
            scanned_copy
        )

        try:
            cursor = DB_CONN.cursor()
            cursor.execute("""
                INSERT INTO documents 
                (name, document_type, category, company_id, box_number, quantity, status, location, 
                 description, reference_number, creation_date, expiration_date, responsible_person, notes,
                 digital_hash, scanned_copy_path) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, params)

            DB_CONN.commit()
            messagebox.showinfo("Sucesso", "Documento adicionado com sucesso!")
            self.clear_form()

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao adicionar documento: {str(e)}")

    def clear_form(self):
        for key, widget in self.entries.items():
            if isinstance(widget, tk.Entry):
                widget.delete(0, tk.END)
            elif isinstance(widget, ttk.Combobox):
                widget.set('')
            elif isinstance(widget, tk.Spinbox):
                widget.delete(0, tk.END)
                widget.insert(0, "1")
            elif isinstance(widget, tk.Text):
                widget.delete("1.0", tk.END)

        self.entries["Status:"].set("Armazenado")
        self.scan_path.set("")
        self.preview_label.config(image='', text="Pré-visualização da digitalização")


class ScanDocumentFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "DIGITALIZAR DOCUMENTO")

        # Frame de seleção de documento
        select_frame = tk.LabelFrame(self, text="Selecionar Documento",
                                     bg=colors['secondary_bg'],
                                     fg=colors['header'],
                                     padx=10, pady=10)
        select_frame.pack(fill='x', padx=10, pady=10)

        self.doc_combo = ttk.Combobox(select_frame, state="readonly", width=50)
        self.doc_combo.pack(side='left', padx=5)

        tk.Button(select_frame, text="Carregar",
                  command=self.load_document,
                  bg=colors['accent_blue'],
                  fg='white',
                  relief='flat').pack(side='left', padx=5)

        # Frame de digitalização
        scan_frame = tk.LabelFrame(self, text="Digitalização",
                                   bg=colors['secondary_bg'],
                                   fg=colors['header'],
                                   padx=10, pady=10)
        scan_frame.pack(fill='both', expand=True, padx=10, pady=10)

        # Opções de digitalização
        options_frame = tk.Frame(scan_frame, bg=colors['secondary_bg'])
        options_frame.pack(fill='x', pady=10)

        tk.Label(options_frame, text="Resolução:",
                 bg=colors['secondary_bg'],
                 fg=colors['text_primary']).pack(side='left', padx=5)

        self.resolution = ttk.Combobox(options_frame, values=["150 DPI", "300 DPI", "600 DPI"],
                                       state="readonly", width=10)
        self.resolution.set("300 DPI")
        self.resolution.pack(side='left', padx=5)

        tk.Label(options_frame, text="Cor:",
                 bg=colors['secondary_bg'],
                 fg=colors['text_primary']).pack(side='left', padx=5)

        self.color_mode = ttk.Combobox(options_frame, values=["Preto e Branco", "Escala de Cinza", "Colorido"],
                                       state="readonly", width=15)
        self.color_mode.set("Colorido")
        self.color_mode.pack(side='left', padx=5)

        # Botões de digitalização
        button_frame = tk.Frame(scan_frame, bg=colors['secondary_bg'])
        button_frame.pack(pady=10)

        tk.Button(button_frame, text="📷 Digitalizar",
                  command=self.scan_document,
                  bg=colors['accent_teal'],
                  fg='white',
                  font=("Helvetica", 11, "bold"),
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5)

        tk.Button(button_frame, text="💾 Salvar",
                  command=self.save_scan,
                  bg=colors['success'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5)

        # Preview
        preview_frame = tk.Frame(scan_frame, bg=colors['tertiary_bg'])
        preview_frame.pack(fill='both', expand=True, pady=10)

        self.preview_label = tk.Label(preview_frame, bg=colors['tertiary_bg'],
                                      text="Pré-visualização da digitalização")
        self.preview_label.pack(expand=True)

        self.current_scan = None
        self.load_documents()

    def load_documents(self):
        docs = DB_CONN.cursor().execute(
            "SELECT id, name FROM documents ORDER BY id DESC"
        ).fetchall()
        self.doc_combo['values'] = [f"{d['id']} - {d['name']}" for d in docs]

    def load_document(self):
        if not self.doc_combo.get():
            return
        doc_id = int(self.doc_combo.get().split(' - ')[0])
        doc = DB_CONN.cursor().execute(
            "SELECT * FROM documents WHERE id = ?", (doc_id,)
        ).fetchone()

        if doc and doc['scanned_copy_path'] and os.path.exists(doc['scanned_copy_path']):
            self.show_preview(doc['scanned_copy_path'])

    def scan_document(self):
        messagebox.showinfo("Digitalização", "Função de digitalização será implementada com integração de scanner.")

    def save_scan(self):
        if not self.current_scan:
            messagebox.showwarning("Aviso", "Nenhuma digitalização para salvar.")
            return

        if not self.doc_combo.get():
            messagebox.showwarning("Aviso", "Selecione um documento.")
            return

        doc_id = int(self.doc_combo.get().split(' - ')[0])

        # Salvar arquivo
        filename = filedialog.asksaveasfilename(
            defaultextension=".png",
            filetypes=[("PNG", "*.png"), ("JPEG", "*.jpg"), ("PDF", "*.pdf")]
        )

        if filename:
            # Atualizar caminho no banco
            DB_CONN.cursor().execute(
                "UPDATE documents SET scanned_copy_path = ? WHERE id = ?",
                (filename, doc_id)
            )
            DB_CONN.commit()
            messagebox.showinfo("Sucesso", "Digitalização salva com sucesso!")

    def show_preview(self, image_path):
        try:
            img = PILImage.open(image_path)
            img.thumbnail((400, 400))
            photo = ImageTk.PhotoImage(img)
            self.preview_label.config(image=photo, text='')
            self.preview_label.image = photo
        except Exception as e:
            messagebox.showerror("Erro", f"Não foi possível carregar a imagem: {str(e)}")


class InvoiceFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "NOTAS FISCAIS")

        # Notebook para abas
        notebook = ttk.Notebook(self)
        notebook.pack(fill='both', expand=True, padx=10, pady=10)

        # Aba de emissão
        issue_frame = tk.Frame(notebook, bg=colors['primary_bg'])
        notebook.add(issue_frame, text="Emitir Nota Fiscal")

        # Aba de consulta
        query_frame = tk.Frame(notebook, bg=colors['primary_bg'])
        notebook.add(query_frame, text="Consultar Notas")

        self.setup_issue_tab(issue_frame)
        self.setup_query_tab(query_frame)

    def setup_issue_tab(self, parent):
        # Frame de informações do documento
        doc_frame = tk.LabelFrame(parent, text="Documento Relacionado",
                                  bg=self.colors['secondary_bg'],
                                  fg=self.colors['header'])
        doc_frame.pack(fill='x', padx=10, pady=10)

        tk.Label(doc_frame, text="Documento:",
                 bg=self.colors['secondary_bg'],
                 fg=self.colors['text_primary']).grid(row=0, column=0, padx=5, pady=5)

        self.doc_combo = ttk.Combobox(doc_frame, state="readonly", width=50)
        self.doc_combo.grid(row=0, column=1, padx=5, pady=5)
        self.load_documents()

        # Frame de informações da nota fiscal
        nf_frame = tk.LabelFrame(parent, text="Dados da Nota Fiscal",
                                 bg=self.colors['secondary_bg'],
                                 fg=self.colors['header'])
        nf_frame.pack(fill='both', expand=True, padx=10, pady=10)

        # Campos
        fields = [
            ("Número da Nota:", "entry"),
            ("Série:", "entry"),
            ("Tipo:", "combobox", ["NF-e", "NFS-e", "CT-e"]),
            ("Valor (R$):", "entry"),
            ("Data de Emissão:", "entry"),
            ("Data de Vencimento:", "entry"),
            ("Emitente:", "entry"),
            ("CNPJ Emitente:", "entry"),
            ("Destinatário:", "entry"),
            ("CNPJ Destinatário:", "entry"),
            ("Descrição:", "text")
        ]

        self.nf_entries = {}

        for i, field_info in enumerate(fields):
            if len(field_info) == 2:
                label, widget_type = field_info
                widget_args = None
            else:
                label, widget_type, widget_args = field_info

            tk.Label(nf_frame, text=label,
                     bg=self.colors['secondary_bg'],
                     fg=self.colors['text_primary']).grid(row=i, column=0, sticky='w', padx=5, pady=5)

            if widget_type == "entry":
                entry = tk.Entry(nf_frame, width=40,
                                 bg=self.colors['tertiary_bg'],
                                 fg=self.colors['text_primary'],
                                 insertbackground='white')
                entry.grid(row=i, column=1, sticky='ew', padx=5, pady=5)
                self.nf_entries[label] = entry

            elif widget_type == "combobox":
                combo = ttk.Combobox(nf_frame, values=widget_args, state="readonly", width=38)
                combo.grid(row=i, column=1, sticky='ew', padx=5, pady=5)
                self.nf_entries[label] = combo

            elif widget_type == "text":
                text = tk.Text(nf_frame, width=40, height=3,
                               bg=self.colors['tertiary_bg'],
                               fg=self.colors['text_primary'],
                               insertbackground='white')
                text.grid(row=i, column=1, sticky='ew', padx=5, pady=5)
                self.nf_entries[label] = text

        nf_frame.columnconfigure(1, weight=1)

        # Botões
        button_frame = tk.Frame(parent, bg=self.colors['primary_bg'])
        button_frame.pack(pady=20)

        tk.Button(button_frame, text="💰 Emitir Nota Fiscal",
                  command=self.issue_invoice,
                  bg=self.colors['success'],
                  fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5)

        tk.Button(button_frame, text="📄 Gerar PDF",
                  command=self.generate_invoice_pdf,
                  bg=self.colors['accent_blue'],
                  fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5)

        tk.Button(button_frame, text="📱 Gerar QR Code",
                  command=self.generate_qr_code,
                  bg=self.colors['accent_teal'],
                  fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat',
                  cursor='hand2').pack(side='left', padx=5)

    def setup_query_tab(self, parent):
        # Treeview para mostrar notas fiscais
        tree_frame = tk.Frame(parent, bg=self.colors['primary_bg'])
        tree_frame.pack(fill='both', expand=True, padx=10, pady=10)

        # Scrollbars
        vsb = tk.Scrollbar(tree_frame, orient="vertical")
        hsb = tk.Scrollbar(tree_frame, orient="horizontal")

        # Colunas
        cols = ("ID", "Número", "Tipo", "Valor", "Emitente", "Destinatário", "Status", "Emissão")

        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings",
                                 yscrollcommand=vsb.set, xscrollcommand=hsb.set)

        vsb.config(command=self.tree.yview)
        hsb.config(command=self.tree.xview)

        for col in cols:
            self.tree.heading(col, text=col)
            self.tree.column(col, width=100 if col in ["ID", "Valor"] else 150)

        self.tree.grid(row=0, column=0, sticky='nsew')
        vsb.grid(row=0, column=1, sticky='ns')
        hsb.grid(row=1, column=0, sticky='ew')

        tree_frame.grid_rowconfigure(0, weight=1)
        tree_frame.grid_columnconfigure(0, weight=1)

        # Botão de atualizar
        tk.Button(parent, text="🔄 Atualizar Lista",
                  command=self.load_invoices,
                  bg=self.colors['accent_blue'],
                  fg='white',
                  relief='flat').pack(pady=10)

        self.load_invoices()

    def load_documents(self):
        docs = DB_CONN.cursor().execute(
            "SELECT id, name FROM documents ORDER BY id DESC"
        ).fetchall()
        self.doc_combo['values'] = [f"{d['id']} - {d['name']}" for d in docs]

    def load_invoices(self):
        for item in self.tree.get_children():
            self.tree.delete(item)

        invoices = DB_CONN.cursor().execute("""
            SELECT id, invoice_number, invoice_type, value, issuer_name, 
                   recipient_name, status, issue_date 
            FROM invoices ORDER BY id DESC
        """).fetchall()

        for inv in invoices:
            self.tree.insert("", "end", values=tuple(inv))

    def generate_qr_code(self):
        """Gera QR Code para a nota fiscal"""
        try:
            # Coletar dados da nota
            invoice_data = {
                'numero': self.nf_entries["Número da Nota:"].get(),
                'serie': self.nf_entries["Série:"].get(),
                'tipo': self.nf_entries["Tipo:"].get(),
                'valor': self.nf_entries["Valor (R$):"].get(),
                'emitente': self.nf_entries["Emitente:"].get(),
                'emitente_cnpj': self.nf_entries["CNPJ Emitente:"].get(),
                'destinatario': self.nf_entries["Destinatário:"].get(),
                'destinatario_cnpj': self.nf_entries["CNPJ Destinatário:"].get()
            }

            # Criar QR Code
            qr = qrcode.QRCode(version=1, box_size=10, border=5)
            qr.add_data(json.dumps(invoice_data, ensure_ascii=False))
            qr.make(fit=True)

            qr_image = qr.make_image(fill_color="black", back_color="white")

            # Salvar imagem
            filename = f"qr_invoice_{invoice_data['numero']}.png"
            filepath = os.path.join(NFE_FOLDER, filename)
            qr_image.save(filepath)

            # Mostrar QR Code
            self.show_qr_code(filepath)

            messagebox.showinfo("Sucesso", f"QR Code gerado: {filename}")

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao gerar QR Code: {str(e)}")

    def show_qr_code(self, qr_path):
        """Mostra o QR Code em uma nova janela"""
        qr_window = tk.Toplevel(self)
        qr_window.title("QR Code da Nota Fiscal")
        qr_window.geometry("400x450")
        qr_window.configure(bg=self.colors['secondary_bg'])

        img = PILImage.open(qr_path)
        img.thumbnail((300, 300))
        photo = ImageTk.PhotoImage(img)

        tk.Label(qr_window, image=photo, bg=self.colors['secondary_bg']).pack(pady=20)
        tk.Label(qr_window,
                 text="QR Code da Nota Fiscal\nEscaneie para validar",
                 bg=self.colors['secondary_bg'],
                 fg=self.colors['text_primary']).pack()

        qr_window.mainloop()

    def issue_invoice(self):
        """Emitir nota fiscal"""
        # Validar campos obrigatórios
        required_fields = ["Número da Nota:", "Tipo:", "Valor (R$):", "Emitente:", "CNPJ Emitente:"]

        for field in required_fields:
            if not self.nf_entries[field].get().strip():
                messagebox.showwarning("Inválido", f"Campo {field} é obrigatório.")
                return

        # Obter documento relacionado
        doc_id = None
        if self.doc_combo.get():
            doc_id = int(self.doc_combo.get().split(' - ')[0])

        # Gerar assinatura digital
        invoice_data = f"{self.nf_entries['Número da Nota:'].get()}{self.nf_entries['Valor (R$):'].get()}{datetime.now()}"
        digital_signature = hashlib.sha256(invoice_data.encode()).hexdigest()

        # Gerar QR Code
        qr_filename = f"qr_{self.nf_entries['Número da Nota:'].get()}.png"
        qr_path = os.path.join(NFE_FOLDER, qr_filename)

        # Inserir no banco
        try:
            cursor = DB_CONN.cursor()
            cursor.execute("""
                INSERT INTO invoices 
                (document_id, invoice_number, invoice_type, series, value, issue_date, 
                 due_date, issuer_name, issuer_cnpj, recipient_name, recipient_cnpj, 
                 description, digital_signature, qr_code_path, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                doc_id,
                self.nf_entries["Número da Nota:"].get(),
                self.nf_entries["Tipo:"].get(),
                self.nf_entries["Série:"].get(),
                float(self.nf_entries["Valor (R$):"].get().replace(',', '.')),
                self.nf_entries["Data de Emissão:"].get(),
                self.nf_entries["Data de Vencimento:"].get(),
                self.nf_entries["Emitente:"].get(),
                self.nf_entries["CNPJ Emitente:"].get(),
                self.nf_entries["Destinatário:"].get(),
                self.nf_entries["CNPJ Destinatário:"].get(),
                self.nf_entries["Descrição:"].get("1.0", tk.END).strip(),
                digital_signature,
                qr_path,
                "Emitida"
            ))
            DB_CONN.commit()

            messagebox.showinfo("Sucesso", "Nota fiscal emitida com sucesso!")
            self.clear_invoice_form()
            self.load_invoices()

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao emitir nota fiscal: {str(e)}")

    def generate_invoice_pdf(self):
        """Gerar PDF da nota fiscal"""
        try:
            # Coletar dados
            invoice_data = {
                'numero': self.nf_entries["Número da Nota:"].get(),
                'serie': self.nf_entries["Série:"].get(),
                'tipo': self.nf_entries["Tipo:"].get(),
                'valor': self.nf_entries["Valor (R$):"].get(),
                'emissao': self.nf_entries["Data de Emissão:"].get(),
                'vencimento': self.nf_entries["Data de Vencimento:"].get(),
                'emitente': self.nf_entries["Emitente:"].get(),
                'emitente_cnpj': self.nf_entries["CNPJ Emitente:"].get(),
                'destinatario': self.nf_entries["Destinatário:"].get(),
                'destinatario_cnpj': self.nf_entries["CNPJ Destinatário:"].get(),
                'descricao': self.nf_entries["Descrição:"].get("1.0", tk.END).strip()
            }

            # Nome do arquivo
            filename = f"NF_{invoice_data['numero']}.pdf"
            filepath = os.path.join(NFE_FOLDER, filename)

            # Criar PDF
            c = canvas.Canvas(filepath, pagesize=A4)
            width, height = A4

            # Cabeçalho
            c.setFont("Helvetica-Bold", 16)
            c.drawString(50, height - 50, "NOTA FISCAL")

            c.setFont("Helvetica", 12)
            c.drawString(50, height - 80, f"Número: {invoice_data['numero']}")
            c.drawString(50, height - 100, f"Série: {invoice_data['serie']}")
            c.drawString(50, height - 120, f"Tipo: {invoice_data['tipo']}")
            c.drawString(50, height - 140, f"Data de Emissão: {invoice_data['emissao']}")
            c.drawString(50, height - 160, f"Data de Vencimento: {invoice_data['vencimento']}")

            # Emitente
            c.setFont("Helvetica-Bold", 14)
            c.drawString(50, height - 200, "EMITENTE")
            c.setFont("Helvetica", 12)
            c.drawString(50, height - 220, f"Nome: {invoice_data['emitente']}")
            c.drawString(50, height - 240, f"CNPJ: {invoice_data['emitente_cnpj']}")

            # Destinatário
            c.setFont("Helvetica-Bold", 14)
            c.drawString(50, height - 280, "DESTINATÁRIO")
            c.setFont("Helvetica", 12)
            c.drawString(50, height - 300, f"Nome: {invoice_data['destinatario']}")
            c.drawString(50, height - 320, f"CNPJ: {invoice_data['destinatario_cnpj']}")

            # Valores
            c.setFont("Helvetica-Bold", 14)
            c.drawString(50, height - 360, "VALORES")
            c.setFont("Helvetica", 12)
            c.drawString(50, height - 380, f"Valor Total: R$ {invoice_data['valor']}")

            # Descrição
            c.setFont("Helvetica-Bold", 14)
            c.drawString(50, height - 420, "DESCRIÇÃO")
            c.setFont("Helvetica", 12)

            # Quebrar descrição em múltiplas linhas
            desc_lines = invoice_data['descricao'].split('\n')
            y = height - 440
            for line in desc_lines:
                if line.strip():
                    c.drawString(50, y, line[:80])  # Limitar tamanho da linha
                    y -= 20

            # QR Code
            if os.path.exists(os.path.join(NFE_FOLDER, f"qr_{invoice_data['numero']}.png")):
                c.drawImage(os.path.join(NFE_FOLDER, f"qr_{invoice_data['numero']}.png"),
                            50, y - 150, width=100, height=100)

            c.save()

            messagebox.showinfo("Sucesso", f"PDF gerado: {filename}")

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao gerar PDF: {str(e)}")

    def clear_invoice_form(self):
        for widget in self.nf_entries.values():
            if isinstance(widget, tk.Entry):
                widget.delete(0, tk.END)
            elif isinstance(widget, ttk.Combobox):
                widget.set('')
            elif isinstance(widget, tk.Text):
                widget.delete("1.0", tk.END)


class ModifyDocumentFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "MODIFICAR DOCUMENTO")

        # Similar ao AddDocumentFrame mas com carregamento de dados
        # (Implementação similar ao AddDocumentFrame com campos preenchidos)
        pass


class TransferDocumentFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "TRANSFERIR / EMITIR DOCUMENTO")

        # Frame de seleção
        select_frame = tk.LabelFrame(self, text="Selecionar Documento",
                                     bg=colors['secondary_bg'],
                                     fg=colors['header'])
        select_frame.pack(fill='x', padx=10, pady=10)

        self.doc_combo = ttk.Combobox(select_frame, state="readonly", width=50)
        self.doc_combo.pack(side='left', padx=5)

        tk.Button(select_frame, text="Carregar",
                  command=self.load_document,
                  bg=colors['accent_blue'],
                  fg='white',
                  relief='flat').pack(side='left', padx=5)

        # Frame de ação
        action_frame = tk.LabelFrame(self, text="Ação",
                                     bg=colors['secondary_bg'],
                                     fg=colors['header'])
        action_frame.pack(fill='x', padx=10, pady=10)

        self.action_var = tk.StringVar(value="Transferência")

        actions = ["Transferência", "Emissão", "Ecenerar", "Digitalizar"]
        for action in actions:
            tk.Radiobutton(action_frame, text=action, variable=self.action_var, value=action,
                           bg=colors['secondary_bg'],
                           fg=colors['text_primary'],
                           selectcolor=colors['secondary_bg'],
                           command=self.update_action_ui).pack(anchor='w', padx=20, pady=2)

        # Frame de detalhes
        self.details_frame = tk.Frame(action_frame, bg=colors['secondary_bg'])
        self.details_frame.pack(fill='x', padx=20, pady=10)

        # Campos comuns
        tk.Label(self.details_frame, text="Responsável:",
                 bg=colors['secondary_bg'],
                 fg=colors['text_primary']).grid(row=0, column=0, sticky='w')

        self.responsible_entry = tk.Entry(self.details_frame, width=40,
                                          bg=colors['tertiary_bg'],
                                          fg=colors['text_primary'],
                                          insertbackground='white')
        self.responsible_entry.grid(row=0, column=1, padx=5, pady=2)

        tk.Label(self.details_frame, text="Observações:",
                 bg=colors['secondary_bg'],
                 fg=colors['text_primary']).grid(row=1, column=0, sticky='w')

        self.notes_text = tk.Text(self.details_frame, width=40, height=3,
                                  bg=colors['tertiary_bg'],
                                  fg=colors['text_primary'],
                                  insertbackground='white')
        self.notes_text.grid(row=1, column=1, padx=5, pady=2)

        # Campo específico para transferência
        self.location_label = tk.Label(self.details_frame, text="Novo Local:",
                                       bg=colors['secondary_bg'],
                                       fg=colors['text_primary'])
        self.location_entry = tk.Entry(self.details_frame, width=40,
                                       bg=colors['tertiary_bg'],
                                       fg=colors['text_primary'],
                                       insertbackground='white')

        # Botão de confirmação
        tk.Button(self, text="Confirmar Ação",
                  command=self.confirm_action,
                  bg=colors['success'],
                  fg='white',
                  font=("Helvetica", 12, "bold"),
                  relief='flat',
                  cursor='hand2').pack(pady=20)

        self.current_doc = None
        self.load_documents()

    def load_documents(self):
        docs = DB_CONN.cursor().execute(
            "SELECT id, name FROM documents WHERE status NOT IN ('Emitido', 'Ecenerado') ORDER BY id DESC"
        ).fetchall()
        self.doc_combo['values'] = [f"{d['id']} - {d['name']}" for d in docs]

    def load_document(self):
        if not self.doc_combo.get():
            return
        doc_id = int(self.doc_combo.get().split(' - ')[0])
        self.current_doc = DB_CONN.cursor().execute(
            "SELECT * FROM documents WHERE id = ?", (doc_id,)
        ).fetchone()

    def update_action_ui(self):
        action = self.action_var.get()

        if action == "Transferência":
            self.location_label.grid(row=2, column=0, sticky='w')
            self.location_entry.grid(row=2, column=1, padx=5, pady=2)
        else:
            self.location_label.grid_remove()
            self.location_entry.grid_remove()

    def confirm_action(self):
        if not self.current_doc:
            messagebox.showwarning("Aviso", "Selecione um documento.")
            return

        action = self.action_var.get()
        responsible = self.responsible_entry.get().strip()
        notes = self.notes_text.get("1.0", tk.END).strip()

        if not responsible:
            messagebox.showwarning("Aviso", "Informe o responsável pela ação.")
            return

        # Mapear ação para novo status e local
        action_map = {
            "Transferência": ("Transferido", self.location_entry.get().strip()),
            "Emissão": ("Emitido", "Emitido para externo"),
            "Ecenerar": ("Ecenerado", "Enviado para descarte"),
            "Digitalizar": ("Digitalizado", self.current_doc['location'])
        }

        new_status, new_location = action_map[action]

        if action == "Transferência" and not new_location:
            messagebox.showwarning("Aviso", "Informe o novo local para transferência.")
            return

        # Registrar transferência
        cursor = DB_CONN.cursor()
        cursor.execute("""
            UPDATE documents SET status = ?, location = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """, (new_status, new_location, self.current_doc['id']))

        cursor.execute("""
            INSERT INTO transfers (document_id, action, from_location, to_location, 
                                  responsible_person, notes, transfer_date)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """, (
            self.current_doc['id'], action, self.current_doc['location'],
            new_location, responsible, notes
        ))

        DB_CONN.commit()
        messagebox.showinfo("Sucesso", f"Ação '{action}' realizada com sucesso!")

        # Limpar campos
        self.responsible_entry.delete(0, tk.END)
        self.notes_text.delete("1.0", tk.END)
        self.location_entry.delete(0, tk.END)
        self.load_documents()


class ManageCompaniesFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "GERENCIAR EMPRESAS")

        # Frame de adição
        add_frame = tk.LabelFrame(self, text="Adicionar Empresa",
                                  bg=colors['secondary_bg'],
                                  fg=colors['header'])
        add_frame.pack(fill='x', padx=10, pady=10)

        # Campos
        fields = ["Nome:", "CNPJ:", "Endereço:", "Telefone:", "Email:"]
        self.company_entries = {}

        for i, field in enumerate(fields):
            tk.Label(add_frame, text=field,
                     bg=colors['secondary_bg'],
                     fg=colors['text_primary']).grid(row=i, column=0, sticky='w', padx=5, pady=2)

            entry = tk.Entry(add_frame, width=50,
                             bg=colors['tertiary_bg'],
                             fg=colors['text_primary'],
                             insertbackground='white')
            entry.grid(row=i, column=1, sticky='ew', padx=5, pady=2)
            self.company_entries[field] = entry

        add_frame.columnconfigure(1, weight=1)

        tk.Button(add_frame, text="Adicionar Empresa",
                  command=self.add_company,
                  bg=colors['success'],
                  fg='white',
                  relief='flat').grid(row=len(fields), column=0, columnspan=2, pady=10)

        # Lista de empresas
        list_frame = tk.LabelFrame(self, text="Empresas Cadastradas",
                                   bg=colors['secondary_bg'],
                                   fg=colors['header'])
        list_frame.pack(fill='both', expand=True, padx=10, pady=10)

        # Treeview
        columns = ("ID", "Nome", "CNPJ", "Telefone", "Email")
        self.tree = ttk.Treeview(list_frame, columns=columns, show="headings", height=15)

        for col in columns:
            self.tree.heading(col, text=col)
            self.tree.column(col, width=100 if col == "ID" else 150)

        self.tree.pack(side='left', fill='both', expand=True)

        # Scrollbar
        scrollbar = ttk.Scrollbar(list_frame, orient="vertical", command=self.tree.yview)
        scrollbar.pack(side='right', fill='y')
        self.tree.configure(yscrollcommand=scrollbar.set)

        # Botão excluir
        tk.Button(self, text="Excluir Selecionada",
                  command=self.delete_company,
                  bg=colors['danger'],
                  fg='white',
                  relief='flat').pack(pady=10)

        self.load_companies()

    def load_companies(self):
        for item in self.tree.get_children():
            self.tree.delete(item)

        companies = DB_CONN.cursor().execute(
            "SELECT id, name, cnpj, phone, email FROM companies ORDER BY name"
        ).fetchall()

        for comp in companies:
            self.tree.insert("", "end", values=tuple(comp))

    def add_company(self):
        name = self.company_entries["Nome:"].get().strip()
        if not name:
            messagebox.showwarning("Inválido", "Nome é obrigatório.")
            return

        try:
            DB_CONN.cursor().execute("""
                INSERT INTO companies (name, cnpj, address, phone, email)
                VALUES (?, ?, ?, ?, ?)
            """, (
                name,
                self.company_entries["CNPJ:"].get().strip(),
                self.company_entries["Endereço:"].get().strip(),
                self.company_entries["Telefone:"].get().strip(),
                self.company_entries["Email:"].get().strip()
            ))
            DB_CONN.commit()

            # Limpar campos
            for entry in self.company_entries.values():
                entry.delete(0, tk.END)

            self.load_companies()
            messagebox.showinfo("Sucesso", "Empresa adicionada com sucesso!")

        except sqlite3.IntegrityError:
            messagebox.showerror("Erro", "Empresa já existe.")

    def delete_company(self):
        if not self.tree.selection():
            messagebox.showwarning("Seleção", "Selecione uma empresa.")
            return

        comp_id, comp_name = self.tree.item(self.tree.selection()[0])['values'][:2]

        # Verificar se há documentos associados
        count = DB_CONN.cursor().execute(
            "SELECT COUNT(*) FROM documents WHERE company_id = ?", (comp_id,)
        ).fetchone()[0]

        if count > 0:
            messagebox.showerror("Erro", f"Não é possível excluir '{comp_name}'. {count} documento(s) associado(s).")
            return

        if messagebox.askyesno("Confirmar", f"Tem certeza que deseja excluir '{comp_name}'?"):
            DB_CONN.cursor().execute("DELETE FROM companies WHERE id = ?", (comp_id,))
            DB_CONN.commit()
            self.load_companies()


class ReportsFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "RELATÓRIOS E GRÁFICOS")

        # Frame de controles
        controls = tk.Frame(self, bg=colors['secondary_bg'], height=50)
        controls.pack(fill='x', padx=10, pady=10)
        controls.pack_propagate(False)

        # Botões de relatório
        reports = [
            ("📊 Status dos Documentos", self.report_status),
            ("🏢 Documentos por Empresa", self.report_by_company),
            ("📅 Documentos por Mês", self.report_by_month),
            ("🔄 Movimentações", self.report_transfers),
            ("💰 Notas Fiscais", self.report_invoices)
        ]

        for text, command in reports:
            tk.Button(controls, text=text, command=command,
                      bg=colors['accent_blue'],
                      fg='white',
                      relief='flat',
                      cursor='hand2').pack(side='left', padx=5, pady=5)

        # Botão de exportar
        tk.Button(controls, text="📄 Exportar PDF",
                  command=self.export_to_pdf,
                  bg=colors['success'],
                  fg='white',
                  relief='flat',
                  cursor='hand2').pack(side='right', padx=5, pady=5)

        # Frame do gráfico
        self.chart_frame = tk.Frame(self, bg='white')
        self.chart_frame.pack(fill='both', expand=True, padx=10, pady=10)

        self.fig = plt.figure(figsize=(10, 6), facecolor='white')
        self.canvas = FigureCanvasTkAgg(self.fig, master=self.chart_frame)
        self.canvas.get_tk_widget().pack(fill='both', expand=True)

        self.current_report_data = None
        self.current_report_title = ""

    def report_status(self):
        """Relatório por status de documento"""
        cursor = DB_CONN.cursor()
        cursor.execute("""
            SELECT status, COUNT(*) as quantidade
            FROM documents
            GROUP BY status
            ORDER BY quantidade DESC
        """)

        data = cursor.fetchall()
        self.generate_chart(data, "Documentos por Status", 'pie')
        self.current_report_data = data
        self.current_report_title = "Documentos por Status"

    def report_by_company(self):
        """Relatório por empresa"""
        cursor = DB_CONN.cursor()
        cursor.execute("""
            SELECT c.name, COUNT(d.id) as quantidade
            FROM documents d
            JOIN companies c ON d.company_id = c.id
            GROUP BY c.name
            ORDER BY quantidade DESC
        """)

        data = cursor.fetchall()
        self.generate_chart(data, "Documentos por Empresa", 'bar')
        self.current_report_data = data
        self.current_report_title = "Documentos por Empresa"

    def report_by_month(self):
        """Relatório por mês"""
        cursor = DB_CONN.cursor()
        cursor.execute("""
            SELECT strftime('%Y-%m', created_at) as mes, COUNT(*) as quantidade
            FROM documents
            GROUP BY mes
            ORDER BY mes DESC
            LIMIT 12
        """)

        data = cursor.fetchall()
        self.generate_chart(data, "Documentos por Mês (últimos 12 meses)", 'line')
        self.current_report_data = data
        self.current_report_title = "Documentos por Mês"

    def report_transfers(self):
        """Relatório de movimentações"""
        cursor = DB_CONN.cursor()
        cursor.execute("""
            SELECT action, COUNT(*) as quantidade
            FROM transfers
            GROUP BY action
            ORDER BY quantidade DESC
        """)

        data = cursor.fetchall()
        self.generate_chart(data, "Movimentações de Documentos", 'pie')
        self.current_report_data = data
        self.current_report_title = "Movimentações"

    def report_invoices(self):
        """Relatório de notas fiscais"""
        cursor = DB_CONN.cursor()
        cursor.execute("""
            SELECT status, COUNT(*) as quantidade, SUM(value) as total
            FROM invoices
            GROUP BY status
        """)

        data = cursor.fetchall()
        self.generate_chart([(d[0], d[1]) for d in data], "Notas Fiscais por Status", 'pie')
        self.current_report_data = data
        self.current_report_title = "Notas Fiscais"

    def generate_chart(self, data, title, chart_type):
        """Gera gráfico baseado nos dados"""
        self.fig.clear()
        ax = self.fig.add_subplot(111)

        if not data:
            ax.text(0.5, 0.5, "Nenhum dado encontrado.", ha='center', va='center')
        else:
            labels = [str(d[0]) for d in data]
            values = [d[1] for d in data]

            if chart_type == 'pie':
                ax.pie(values, labels=labels, autopct='%1.1f%%', startangle=140)
                ax.set_title(title, pad=20)

            elif chart_type == 'bar':
                bars = ax.bar(labels, values, color=self.colors['accent_blue'])
                ax.set_title(title, pad=20)
                ax.set_xlabel('Categoria')
                ax.set_ylabel('Quantidade')
                plt.xticks(rotation=45, ha='right')

                # Adicionar valores nas barras
                for bar, value in zip(bars, values):
                    height = bar.get_height()
                    ax.text(bar.get_x() + bar.get_width() / 2., height,
                            f'{value}', ha='center', va='bottom')

            elif chart_type == 'line':
                ax.plot(labels, values, marker='o', color=self.colors['accent_blue'], linewidth=2)
                ax.set_title(title, pad=20)
                ax.set_xlabel('Período')
                ax.set_ylabel('Quantidade')
                plt.xticks(rotation=45, ha='right')

                # Adicionar valores nos pontos
                for i, (label, value) in enumerate(zip(labels, values)):
                    ax.annotate(f'{value}', (i, value), textcoords="offset points",
                                xytext=(0, 10), ha='center')

        self.fig.tight_layout()
        self.canvas.draw()

    def export_to_pdf(self):
        """Exporta relatório para PDF"""
        if not self.current_report_data:
            messagebox.showwarning("Aviso", "Nenhum relatório gerado para exportar.")
            return

        filepath = filedialog.asksaveasfilename(
            defaultextension=".pdf",
            filetypes=[("PDF", "*.pdf")],
            initialfile=f"Relatorio_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
        )

        if not filepath:
            return

        try:
            # Salvar gráfico temporariamente
            temp_chart = "temp_chart.png"
            self.fig.savefig(temp_chart, bbox_inches='tight', dpi=300)

            # Criar PDF
            doc = SimpleDocTemplate(filepath, pagesize=letter)
            styles = getSampleStyleSheet()
            story = []

            # Título
            story.append(Paragraph(self.current_report_title, styles['Title']))
            story.append(Spacer(1, 0.2 * inch))

            # Data
            story.append(Paragraph(
                f"Gerado em: {datetime.now().strftime('%d/%m/%Y %H:%M')}",
                styles['Normal']
            ))
            story.append(Spacer(1, 0.3 * inch))

            # Gráfico
            story.append(Image(temp_chart, width=6 * inch, height=4 * inch))
            story.append(Spacer(1, 0.3 * inch))

            # Tabela de dados
            story.append(Paragraph("Dados Detalhados", styles['Heading2']))
            story.append(Spacer(1, 0.1 * inch))

            # Criar tabela
            if self.current_report_title == "Notas Fiscais":
                table_data = [['Status', 'Quantidade', 'Valor Total (R$)']]
                for row in self.current_report_data:
                    table_data.append([str(row[0]), str(row[1]), f"R$ {row[2]:.2f}"])
            else:
                table_data = [['Categoria', 'Quantidade']]
                for row in self.current_report_data:
                    table_data.append([str(row[0]), str(row[1])])

            table = Table(table_data)
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor(self.colors['header'])),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 12),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
                ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f0f0f0')),
                ('GRID', (0, 0), (-1, -1), 1, colors.black),
                ('FONTSIZE', (0, 1), (-1, -1), 10),
            ]))

            story.append(table)

            # Gerar PDF
            doc.build(story)

            # Limpar arquivo temporário
            if os.path.exists(temp_chart):
                os.remove(temp_chart)

            messagebox.showinfo("Sucesso", "Relatório exportado com sucesso!")

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao exportar PDF: {str(e)}")


class DeleteDocumentFrame(BaseFrame):
    def __init__(self, master, colors, user):
        super().__init__(master, colors, user, "EXCLUIR DOCUMENTO")

        # Frame de seleção
        select_frame = tk.LabelFrame(self, text="Selecionar Documento para Excluir",
                                     bg=colors['secondary_bg'],
                                     fg=colors['header'])
        select_frame.pack(fill='x', padx=10, pady=10)

        self.doc_combo = ttk.Combobox(select_frame, state="readonly", width=70)
        self.doc_combo.pack(side='left', padx=5, pady=5)

        tk.Button(select_frame, text="Carregar Informações",
                  command=self.load_document_info,
                  bg=colors['accent_blue'],
                  fg='white',
                  relief='flat').pack(side='left', padx=5)

        # Frame de informações
        info_frame = tk.LabelFrame(self, text="Informações do Documento",
                                   bg=colors['secondary_bg'],
                                   fg=colors['header'])
        info_frame.pack(fill='both', expand=True, padx=10, pady=10)

        # Área de texto para mostrar informações
        self.info_text = tk.Text(info_frame, width=80, height=15,
                                 bg=colors['tertiary_bg'],
                                 fg=colors['text_primary'],
                                 state='disabled',
                                 wrap='word')
        self.info_text.pack(fill='both', expand=True, padx=10, pady=10)

        # Scrollbar
        scrollbar = ttk.Scrollbar(info_frame, orient="vertical", command=self.info_text.yview)
        scrollbar.pack(side='right', fill='y')
        self.info_text.configure(yscrollcommand=scrollbar.set)

        # Botão de exclusão
        self.delete_button = tk.Button(self, text="🗑️ EXCLUIR DOCUMENTO",
                                       command=self.delete_document,
                                       bg=colors['danger'],
                                       fg='white',
                                       font=("Helvetica", 14, "bold"),
                                       state='disabled',
                                       relief='flat',
                                       cursor='hand2')
        self.delete_button.pack(pady=20)

        self.current_doc = None
        self.load_documents()

    def load_documents(self):
        docs = DB_CONN.cursor().execute(
            "SELECT id, name FROM documents ORDER BY id DESC"
        ).fetchall()
        self.doc_combo['values'] = [f"{d['id']} - {d['name']}" for d in docs]

    def load_document_info(self):
        if not self.doc_combo.get():
            return

        doc_id = int(self.doc_combo.get().split(' - ')[0])

        # Buscar informações completas do documento
        doc = DB_CONN.cursor().execute("""
            SELECT d.*, c.name as company_name
            FROM documents d
            LEFT JOIN companies c ON d.company_id = c.id
            WHERE d.id = ?
        """, (doc_id,)).fetchone()

        if doc:
            self.current_doc = doc

            # Buscar transferências
            transfers = DB_CONN.cursor().execute("""
                SELECT * FROM transfers 
                WHERE document_id = ? 
                ORDER BY transfer_date DESC
            """, (doc_id,)).fetchall()

            # Buscar notas fiscais
            invoices = DB_CONN.cursor().execute("""
                SELECT * FROM invoices 
                WHERE document_id = ?
            """, (doc_id,)).fetchall()

            # Montar texto informativo
            info = f"""
{"=" * 60}
INFORMAÇÕES DO DOCUMENTO
{"=" * 60}

ID: {doc['id']}
Nome: {doc['name']}
Tipo: {doc['document_type']}
Categoria: {doc['category']}
Empresa: {doc['company_name'] if doc['company_name'] else '-'}
Nº Caixa: {doc['box_number'] if doc['box_number'] else '-'}
Quantidade: {doc['quantity']}
Status: {doc['status']}
Localização: {doc['location'] if doc['location'] else '-'}
Nº Referência: {doc['reference_number'] if doc['reference_number'] else '-'}
Responsável: {doc['responsible_person'] if doc['responsible_person'] else '-'}
Data de Criação: {doc['creation_date'] if doc['creation_date'] else '-'}
Data de Expiração: {doc['expiration_date'] if doc['expiration_date'] else '-'}
Data de Cadastro: {doc['created_at']}
Última Atualização: {doc['updated_at']}

Descrição:
{doc['description'] if doc['description'] else '-'}

Observações:
{doc['notes'] if doc['notes'] else '-'}

{"=" * 60}
HISTÓRICO DE TRANSFERÊNCIAS ({len(transfers)})
{"=" * 60}
"""

            for t in transfers:
                info += f"""
Data: {t['transfer_date']}
Ação: {t['action']}
De: {t['from_location'] if t['from_location'] else '-'}
Para: {t['to_location'] if t['to_location'] else '-'}
Responsável: {t['responsible_person'] if t['responsible_person'] else '-'}
Motivo: {t['transfer_reason'] if t['transfer_reason'] else '-'}
Observações: {t['notes'] if t['notes'] else '-'}
{'-' * 40}
"""

            if invoices:
                info += f"""

{"=" * 60}
NOTAS FISCAIS ASSOCIADAS ({len(invoices)})
{"=" * 60}
"""
                for inv in invoices:
                    info += f"""
Número: {inv['invoice_number']}
Tipo: {inv['invoice_type']}
Série: {inv['series']}
Valor: R$ {inv['value']:.2f}
Status: {inv['status']}
Emissão: {inv['issue_date']}
Vencimento: {inv['due_date']}
{'-' * 40}
"""

            # Mostrar no text widget
            self.info_text.config(state='normal')
            self.info_text.delete('1.0', tk.END)
            self.info_text.insert('1.0', info)
            self.info_text.config(state='disabled')

            # Habilitar botão de exclusão
            self.delete_button.config(state='normal')

    def delete_document(self):
        if not self.current_doc:
            return

        # Confirmação
        if not messagebox.askyesno(
                "Confirmar Exclusão",
                f"Tem certeza que deseja EXCLUIR PERMANENTEMENTE o documento:\n\n"
                f"'{self.current_doc['name']}' (ID: {self.current_doc['id']})\n\n"
                f"Esta ação não pode ser desfeita e removerá:\n"
                f"- Todas as transferências associadas\n"
                f"- Todas as notas fiscais associadas\n"
                f"- Arquivos digitalizados relacionados"
        ):
            return

        try:
            # Excluir arquivos digitalizados se existirem
            if self.current_doc['scanned_copy_path'] and os.path.exists(self.current_doc['scanned_copy_path']):
                os.remove(self.current_doc['scanned_copy_path'])

            # Excluir documentos do banco (as tabelas relacionadas serão excluídas por CASCADE)
            DB_CONN.cursor().execute("DELETE FROM documents WHERE id = ?", (self.current_doc['id'],))
            DB_CONN.commit()

            messagebox.showinfo("Sucesso", "Documento excluído com sucesso!")

            # Limpar interface
            self.info_text.config(state='normal')
            self.info_text.delete('1.0', tk.END)
            self.info_text.config(state='disabled')
            self.delete_button.config(state='disabled')
            self.current_doc = None
            self.load_documents()

        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao excluir documento: {str(e)}")


# ==============================
# INICIALIZAÇÃO DO PROGRAMA
# ==============================
if __name__ == "__main__":
    # Inicializar banco de dados
    DB_CONN = init_db()

    # Criar pasta para digitalizações
    if not os.path.exists("scanned_docs"):
        os.makedirs("scanned_docs")

    # Iniciar tela de login
    login_root = tk.Tk()
    login_root.configure(bg='#0a1928')
    LoginWindow(login_root)
    login_root.mainloop()