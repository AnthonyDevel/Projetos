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

# ==============================
# CONFIGURAÇÕES GLOBAIS
# ==============================
DB_CONN = None
DOCUMENT_TYPES = ["Hospital", "Judiciary", "Enterprise", "Business"]
JUDICIARY_SUBTYPES = ["Escritura", "Procuração", "Intimação", "Sentença", "Petição"]
CATEGORIES = ["Contas a Pagar", "Contabilidade", "Contas a Receber", "Geral", "Recursos Humanos"]
STATUS_OPTIONS = ["Armazenado", "Transferido", "Emitido", "Ecenerado"]
TEMP_CHART_FILE = "temp_chart.png"
DB_FILE_NAME = "sistema_documentos_v5.db"  # Atualizado para nova versão


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
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )""")

    # Tabela de documentos - COM CAMPOS EXPANDIDOS
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
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL
    )""")

    # Tabela de transferências - COM MAIS DETALHES
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

    # Tabela de usuários (para futuras expansões)
    cursor.execute("""CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
        username TEXT UNIQUE NOT NULL,
        full_name TEXT,
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
        self.root.title("Login")
        self.root.geometry("300x200")
        self.root.resizable(False, False)
        frame = tk.Frame(root, padx=20, pady=20)
        frame.pack(expand=True)
        tk.Label(frame, text="Usuário:").pack()
        self.user_entry = tk.Entry(frame)
        self.user_entry.pack(pady=5)
        tk.Label(frame, text="Senha:").pack()
        self.pass_entry = tk.Entry(frame, show="*")
        self.pass_entry.pack(pady=5)
        tk.Button(frame, text="Entrar", command=self.check_login, bg="#20B2AA", fg="white",
                  font=("Helvetica", 10, "bold")).pack(pady=10)
        self.user_entry.focus_set()
        self.root.bind('<Return>', lambda e: self.check_login())

    def check_login(self):
        if self.user_entry.get() == "Anthony" and self.pass_entry.get() == "Pass_":
            self.root.destroy()
            open_main_app()
        else:
            messagebox.showerror("Erro", "Usuário ou senha incorretos.")


def open_main_app():
    global DB_CONN
    DB_CONN = init_db()
    app_root = tk.Tk()
    DocumentApp(app_root)
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
    def __init__(self, root):
        self.root = root
        self.root.title("Sistema de Gerenciamento de Documentos")
        self.root.state('zoomed')
        self.colors = {"primary_bg": "#f0f0f0", "secondary_bg": "#dcdcdc", "accent_blue": "#87CEEB",
                       "accent_teal": "#20B2AA", "text": "#333333", "header": "#4682B4", "danger": "#C21807",
                       "row_alt": "#ffffff"}
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview.Heading", background=self.colors['accent_blue'], foreground="white",
                        font=("Helvetica", 10, "bold"))
        style.configure("Treeview", rowheight=25)
        style.map('Treeview', background=[('selected', self.colors['accent_teal'])])

        sidebar = tk.Frame(root, bg=self.colors['secondary_bg'], width=220, relief="raised", bd=2)
        sidebar.pack(side="left", fill="y", padx=5, pady=5)
        self.main_content = tk.Frame(root, bg=self.colors['primary_bg'])
        self.main_content.pack(side="right", fill="both", expand=True, padx=5, pady=5)
        self.current_frame = None

        tk.Label(sidebar, text="Menu", font=("Helvetica", 16, "bold"), bg=self.colors['secondary_bg'],
                 fg=self.colors['header']).pack(pady=20)

        buttons = {
            "Visualizar Documentos": PreviewDocumentsFrame,
            "Adicionar Documento": AddDocumentFrame,
            "Modificar Documento": ModifyDocumentFrame,
            "Transferir / Emitir": TransferDocumentFrame,
            "Excluir Documento": DeleteDocumentFrame,
            "Gerenciar Empresas": ManageCompaniesFrame,
            "Relatórios": ReportsFrame
        }
        for text, frame_class in buttons.items():
            tk.Button(sidebar, text=text, command=lambda fc=frame_class: self.show_frame(fc),
                      bg=self.colors['accent_blue'], fg="white", font=("Helvetica", 10), relief="flat").pack(fill="x",
                                                                                                             pady=5,
                                                                                                             padx=10)

        tk.Button(sidebar, text="Sair", command=lambda: on_closing(root), bg=self.colors['danger'], fg="white",
                  font=("Helvetica", 10, "bold"), relief="flat").pack(side="bottom", fill="x", pady=20, padx=10)
        self.show_frame(PreviewDocumentsFrame)

    def show_frame(self, frame_class):
        if self.current_frame: self.current_frame.destroy()
        self.current_frame = frame_class(self.main_content, self.colors)
        self.current_frame.pack(fill="both", expand=True)


# ==============================
# FRAMES DE CONTEÚDO ATUALIZADOS
# ==============================
class BaseFrame(tk.Frame):
    def __init__(self, master, colors, title):
        super().__init__(master, bg=colors['primary_bg'])
        self.colors = colors
        tk.Label(self, text=title, font=("Helvetica", 18, "bold"), bg=colors['primary_bg'], fg=colors['header']).pack(
            pady=20)


class PreviewDocumentsFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Visualização Geral de Documentos")
        controls_frame = tk.Frame(self, bg=colors['primary_bg'])
        controls_frame.pack(fill="x", padx=10, pady=(0, 5))
        tk.Button(controls_frame, text="🔄 Atualizar Lista", command=self.load_documents, bg=colors['accent_teal'],
                  fg='white').pack(side='left')

        tree_frame = tk.Frame(self)
        tree_frame.pack(fill="both", expand=True, padx=10, pady=5)
        cols = ("ID", "Nome", "Tipo", "Categoria", "Empresa", "Nº Caixa", "Qtd", "Status", "Localização", "Criado em")
        self.tree = ttk.Treeview(tree_frame, columns=cols, show="headings")
        vsb = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        hsb = ttk.Scrollbar(tree_frame, orient="horizontal", command=self.tree.xview)
        self.tree.configure(yscrollcommand=vsb.set, xscrollcommand=hsb.set)
        vsb.pack(side='right', fill='y')
        hsb.pack(side='bottom', fill='x')
        self.tree.pack(side='left', fill='both', expand=True)

        for col in cols: self.tree.heading(col, text=col)
        self.tree.column("ID", width=40, anchor='center')
        self.tree.column("Nome", width=250)
        self.tree.column("Tipo", width=120)
        self.tree.column("Categoria", width=120)
        self.tree.column("Empresa", width=150)
        self.tree.column("Nº Caixa", width=80, anchor='center')
        self.tree.column("Qtd", width=40, anchor='center')
        self.tree.column("Status", width=100)
        self.tree.column("Localização", width=150)
        self.tree.column("Criado em", width=100, anchor='center')

        self.tree.tag_configure('oddrow', background=colors['row_alt'])

        self.status_bar = tk.Label(self, text="", bd=1, relief=tk.SUNKEN, anchor=tk.W)
        self.status_bar.pack(side=tk.BOTTOM, fill=tk.X)
        self.load_documents()

    def load_documents(self):
        for item in self.tree.get_children(): self.tree.delete(item)
        query = """
            SELECT d.id, d.name, d.document_type, d.category, IFNULL(c.name, '-'),
                   d.box_number, d.quantity, d.status, d.location, strftime('%d/%m/%Y', d.created_at)
            FROM documents d LEFT JOIN companies c ON d.company_id = c.id ORDER BY d.id DESC"""

        rows = DB_CONN.cursor().execute(query).fetchall()
        for i, row in enumerate(rows):
            self.tree.insert("", "end", values=tuple(row), tags=('oddrow' if i % 2 else '',))
        self.status_bar.config(text=f"  Total de Documentos: {len(rows)}")


class AddDocumentFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Adicionar Novo Documento")
        form = tk.Frame(self, bg=colors['primary_bg'])
        form.pack(pady=10, padx=20, fill="both", expand=True)

        # Campos expandidos
        fields = [
            "Nome do Documento:", "Tipo Principal:", "Subtipo (Judiciário):", "Categoria:", "Empresa:",
            "Nº da Caixa:", "Quantidade:", "Status:", "Localização:", "Número de Referência:",
            "Data de Criação (DD/MM/AAAA):", "Data de Expiração (DD/MM/AAAA):", "Pessoa Responsável:",
            "Descrição:", "Observações:"
        ]

        self.entries = {}
        for i, field in enumerate(fields):
            tk.Label(form, text=field, bg=colors['primary_bg']).grid(row=i, column=0, sticky="w", pady=2, padx=5)

        # Linha 0
        self.entries['name'] = tk.Entry(form, width=50)
        self.entries['name'].grid(row=0, column=1, sticky="ew", columnspan=3)

        # Linha 1
        self.entries['main_type'] = ttk.Combobox(form, values=DOCUMENT_TYPES, state="readonly", width=20)
        self.entries['main_type'].grid(row=1, column=1, sticky="ew")
        self.entries['main_type'].bind("<<ComboboxSelected>>", self.update_subtypes)

        # Linha 2
        self.entries['subtype'] = ttk.Combobox(form, values=JUDICIARY_SUBTYPES, state="disabled", width=20)
        self.entries['subtype'].grid(row=2, column=1, sticky="ew")

        # Linha 3
        self.entries['category'] = ttk.Combobox(form, values=CATEGORIES, state="readonly", width=20)
        self.entries['category'].grid(row=3, column=1, sticky="ew")

        # Linha 4
        self.entries['company'] = ttk.Combobox(form, state="readonly", width=20)
        self.entries['company'].grid(row=4, column=1, sticky="ew")
        self.entries['company'].bind("<Button-1>", self.update_companies)

        # Linha 5-7
        self.entries['box_number'] = tk.Entry(form, width=20)
        self.entries['box_number'].grid(row=5, column=1, sticky="ew")

        self.entries['quantity'] = tk.Spinbox(form, from_=1, to=10000, width=20)
        self.entries['quantity'].grid(row=6, column=1, sticky="ew")

        self.entries['status'] = ttk.Combobox(form, values=STATUS_OPTIONS, state="readonly", width=20)
        self.entries['status'].set("Armazenado")
        self.entries['status'].grid(row=7, column=1, sticky="ew")

        # Linha 8-14 (campos adicionais)
        self.entries['location'] = tk.Entry(form, width=50)
        self.entries['location'].grid(row=8, column=1, sticky="ew", columnspan=3)

        self.entries['reference_number'] = tk.Entry(form, width=50)
        self.entries['reference_number'].grid(row=9, column=1, sticky="ew", columnspan=3)

        self.entries['creation_date'] = tk.Entry(form, width=20)
        self.entries['creation_date'].grid(row=10, column=1, sticky="ew")

        self.entries['expiration_date'] = tk.Entry(form, width=20)
        self.entries['expiration_date'].grid(row=11, column=1, sticky="ew")

        self.entries['responsible_person'] = tk.Entry(form, width=50)
        self.entries['responsible_person'].grid(row=12, column=1, sticky="ew", columnspan=3)

        self.entries['description'] = tk.Text(form, width=50, height=3)
        self.entries['description'].grid(row=13, column=1, sticky="ew", columnspan=3)

        self.entries['notes'] = tk.Text(form, width=50, height=3)
        self.entries['notes'].grid(row=14, column=1, sticky="ew", columnspan=3)

        # Configurar pesos das colunas
        form.columnconfigure(1, weight=1)

        tk.Button(self, text="Adicionar Documento", command=self.add_document, bg=colors['accent_teal'], fg="white",
                  font=("Helvetica", 12, "bold")).pack(pady=20)

        self.update_companies()

    def update_subtypes(self, e=None):
        is_jud = self.entries['main_type'].get() == "Judiciary"
        self.entries['subtype'].config(state="readonly" if is_jud else "disabled")
        if not is_jud: self.entries['subtype'].set('')

    def update_companies(self, e=None):
        self.entries['company']['values'] = [""] + [c['name'] for c in get_companies()]

    def add_document(self):
        main_type, subtype, cat = self.entries['main_type'].get(), self.entries['subtype'].get(), self.entries[
            'category'].get()
        doc_type = subtype if main_type == 'Judiciary' and subtype else main_type
        name = self.entries['name'].get().strip()

        if not name or not doc_type or not cat:
            return messagebox.showwarning("Inválido", "Nome, Tipo e Categoria são obrigatórios.")

        # Processar datas
        creation_date = self.parse_date(self.entries['creation_date'].get())
        expiration_date = self.parse_date(self.entries['expiration_date'].get())

        params = (
            name, doc_type, cat, get_company_id_by_name(self.entries['company'].get()),
            self.entries['box_number'].get(), self.entries['quantity'].get(),
            self.entries['status'].get(), self.entries['location'].get(),
            self.entries['description'].get("1.0", tk.END).strip(),
            self.entries['reference_number'].get(),
            creation_date, expiration_date,
            self.entries['responsible_person'].get(),
            self.entries['notes'].get("1.0", tk.END).strip()
        )

        try:
            cursor = DB_CONN.cursor()
            cursor.execute("""
                INSERT INTO documents 
                (name, document_type, category, company_id, box_number, quantity, status, location, 
                 description, reference_number, creation_date, expiration_date, responsible_person, notes) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, params)
            DB_CONN.commit()
            messagebox.showinfo("Sucesso", "Documento adicionado com todos os dados!")
            self.clear_form()
        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao adicionar documento: {str(e)}")

    def parse_date(self, date_str):
        """Converte data no formato DD/MM/AAAA para formato SQLite"""
        if not date_str.strip():
            return None
        try:
            return datetime.strptime(date_str, "%d/%m/%Y").strftime("%Y-%m-%d")
        except ValueError:
            return None

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

        self.entries['status'].set("Armazenado")


class ModifyDocumentFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Modificar Documento")
        self.selected_doc_id = None

        select_frame = tk.LabelFrame(self, text="1. Selecione um Documento", bg=colors['primary_bg'], padx=10, pady=10)
        select_frame.pack(pady=10, padx=10, fill="x")

        self.tree = ttk.Treeview(select_frame, columns=("ID", "Nome", "Status"), show="headings", height=5)
        for col in ("ID", "Nome", "Status"): self.tree.heading(col, text=col)
        self.tree.column("ID", width=50)
        self.tree.pack(fill="x", expand=True)
        self.tree.bind("<<TreeviewSelect>>", self.load_doc_details)

        self.edit_frame = tk.LabelFrame(self, text="2. Edite as Informações", bg=colors['primary_bg'], padx=10, pady=10)
        self.edit_frame.pack(pady=10, padx=10, fill="x")

        # Campos expandidos (similar ao AddDocumentFrame)
        fields = [
            "Nome:", "Tipo Principal:", "Subtipo:", "Categoria:", "Empresa:", "Nº Caixa:",
            "Quantidade:", "Status:", "Localização:", "Número de Referência:",
            "Data de Criação:", "Data de Expiração:", "Pessoa Responsável:",
            "Descrição:", "Observações:"
        ]

        self.entries = {}
        for i, field in enumerate(fields):
            tk.Label(self.edit_frame, text=field, bg=colors['primary_bg']).grid(
                row=i, column=0, sticky="w", padx=5, pady=2)

        # Configurar widgets de entrada
        self.entries['name'] = tk.Entry(self.edit_frame, width=50)
        self.entries['name'].grid(row=0, column=1, sticky="ew", columnspan=3)

        self.entries['main_type'] = ttk.Combobox(self.edit_frame, values=DOCUMENT_TYPES, state="readonly", width=20)
        self.entries['main_type'].grid(row=1, column=1, sticky="ew")
        self.entries['main_type'].bind("<<ComboboxSelected>>", self.update_subtypes)

        self.entries['subtype'] = ttk.Combobox(self.edit_frame, values=JUDICIARY_SUBTYPES, state="readonly", width=20)
        self.entries['subtype'].grid(row=2, column=1, sticky="ew")

        self.entries['category'] = ttk.Combobox(self.edit_frame, values=CATEGORIES, state="readonly", width=20)
        self.entries['category'].grid(row=3, column=1, sticky="ew")

        self.entries['company'] = ttk.Combobox(self.edit_frame, state="readonly", width=20)
        self.entries['company'].grid(row=4, column=1, sticky="ew")

        self.entries['box_number'] = tk.Entry(self.edit_frame, width=20)
        self.entries['box_number'].grid(row=5, column=1, sticky="ew")

        self.entries['quantity'] = tk.Spinbox(self.edit_frame, from_=1, to=10000, width=20)
        self.entries['quantity'].grid(row=6, column=1, sticky="ew")

        self.entries['status'] = ttk.Combobox(self.edit_frame, values=STATUS_OPTIONS, state="readonly", width=20)
        self.entries['status'].grid(row=7, column=1, sticky="ew")

        self.entries['location'] = tk.Entry(self.edit_frame, width=50)
        self.entries['location'].grid(row=8, column=1, sticky="ew", columnspan=3)

        self.entries['reference_number'] = tk.Entry(self.edit_frame, width=50)
        self.entries['reference_number'].grid(row=9, column=1, sticky="ew", columnspan=3)

        self.entries['creation_date'] = tk.Entry(self.edit_frame, width=20)
        self.entries['creation_date'].grid(row=10, column=1, sticky="ew")

        self.entries['expiration_date'] = tk.Entry(self.edit_frame, width=20)
        self.entries['expiration_date'].grid(row=11, column=1, sticky="ew")

        self.entries['responsible_person'] = tk.Entry(self.edit_frame, width=50)
        self.entries['responsible_person'].grid(row=12, column=1, sticky="ew", columnspan=3)

        self.entries['description'] = tk.Text(self.edit_frame, width=50, height=3)
        self.entries['description'].grid(row=13, column=1, sticky="ew", columnspan=3)

        self.entries['notes'] = tk.Text(self.edit_frame, width=50, height=3)
        self.entries['notes'].grid(row=14, column=1, sticky="ew", columnspan=3)

        self.edit_frame.columnconfigure(1, weight=1)

        self.save_button = tk.Button(self, text="Salvar Alterações", command=self.save_changes,
                                     bg=colors['accent_teal'], fg="white", font=("Helvetica", 12, "bold"))
        self.save_button.pack(pady=20)

        self.entries['company'].bind("<Button-1>", self.update_companies)
        self.load_documents_to_tree()
        self.toggle_form_state('disabled')

    def toggle_form_state(self, state):
        for widget in self.entries.values():
            widget.config(state=state)
        self.save_button.config(state=state)

    def load_documents_to_tree(self):
        for item in self.tree.get_children(): self.tree.delete(item)
        for row in DB_CONN.cursor().execute("SELECT id, name, status FROM documents ORDER BY id DESC").fetchall():
            self.tree.insert("", "end", values=tuple(row))

    def update_companies(self, event=None):
        self.entries['company']['values'] = [""] + [c['name'] for c in get_companies()]

    def update_subtypes(self, event=None):
        is_jud = self.entries['main_type'].get() == "Judiciary"
        self.entries['subtype'].config(state="readonly" if is_jud else "disabled")
        if not is_jud: self.entries['subtype'].set('')

    def load_doc_details(self, event=None):
        if not self.tree.selection(): return
        self.selected_doc_id = self.tree.item(self.tree.selection()[0])['values'][0]
        doc = DB_CONN.cursor().execute("SELECT * FROM documents WHERE id = ?", (self.selected_doc_id,)).fetchone()
        company = DB_CONN.cursor().execute("SELECT name FROM companies WHERE id = ?",
                                           (doc['company_id'],)).fetchone() if doc['company_id'] else None

        self.toggle_form_state('normal')
        self.clear_form()

        # Preencher campos
        self.entries['name'].insert(0, doc['name'])

        if doc['document_type'] in JUDICIARY_SUBTYPES:
            self.entries['main_type'].set("Judiciary")
            self.entries['subtype'].set(doc['document_type'])
        else:
            self.entries['main_type'].set(doc['document_type'])
            self.entries['subtype'].set('')

        self.update_subtypes()
        self.entries['category'].set(doc['category'])
        self.update_companies()
        self.entries['company'].set(company['name'] if company else '')
        self.entries['box_number'].insert(0, doc['box_number'] or '')
        self.entries['quantity'].delete(0, tk.END)
        self.entries['quantity'].insert(0, doc['quantity'])
        self.entries['status'].set(doc['status'])
        self.entries['location'].insert(0, doc['location'] or '')
        self.entries['reference_number'].insert(0, doc['reference_number'] or '')

        # Preencher datas
        if doc['creation_date']:
            self.entries['creation_date'].insert(0, datetime.strptime(doc['creation_date'], "%Y-%m-%d").strftime(
                "%d/%m/%Y"))
        if doc['expiration_date']:
            self.entries['expiration_date'].insert(0, datetime.strptime(doc['expiration_date'], "%Y-%m-%d").strftime(
                "%d/%m/%Y"))

        self.entries['responsible_person'].insert(0, doc['responsible_person'] or '')
        self.entries['description'].insert("1.0", doc['description'] or '')
        self.entries['notes'].insert("1.0", doc['notes'] or '')

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

    def save_changes(self):
        main_type, subtype = self.entries['main_type'].get(), self.entries['subtype'].get()
        doc_type = subtype if main_type == 'Judiciary' and subtype else main_type

        if not self.entries['name'].get() or not doc_type or not self.entries['category'].get():
            return messagebox.showwarning("Inválido", "Nome, Tipo e Categoria são obrigatórios.")

        # Processar datas
        creation_date = self.parse_date(self.entries['creation_date'].get())
        expiration_date = self.parse_date(self.entries['expiration_date'].get())

        query = """
            UPDATE documents SET 
            name=?, document_type=?, category=?, company_id=?, box_number=?, quantity=?, status=?, location=?,
            description=?, reference_number=?, creation_date=?, expiration_date=?, responsible_person=?, notes=?,
            updated_at=CURRENT_TIMESTAMP 
            WHERE id=?
        """

        params = (
            self.entries['name'].get(), doc_type, self.entries['category'].get(),
            get_company_id_by_name(self.entries['company'].get()), self.entries['box_number'].get(),
            self.entries['quantity'].get(), self.entries['status'].get(), self.entries['location'].get(),
            self.entries['description'].get("1.0", tk.END).strip(), self.entries['reference_number'].get(),
            creation_date, expiration_date, self.entries['responsible_person'].get(),
            self.entries['notes'].get("1.0", tk.END).strip(), self.selected_doc_id
        )

        try:
            DB_CONN.cursor().execute(query, params)
            DB_CONN.commit()
            messagebox.showinfo("Sucesso", "Documento atualizado com todos os dados!")
            self.load_documents_to_tree()
            self.toggle_form_state('disabled')
        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao atualizar documento: {str(e)}")

    def parse_date(self, date_str):
        """Converte data no formato DD/MM/AAAA para formato SQLite"""
        if not date_str.strip():
            return None
        try:
            return datetime.strptime(date_str, "%d/%m/%Y").strftime("%Y-%m-%d")
        except ValueError:
            return None


# (Os outros frames - DeleteDocumentFrame, TransferDocumentFrame, ManageCompaniesFrame, ReportsFrame -
# permanecem os mesmos, apenas garantindo que usam tuple(row) para inserir dados no Treeview)

class DeleteDocumentFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Excluir Documento")
        self.selected_doc_info = {}

        select_frame = tk.LabelFrame(self, text="Selecione um Documento para Excluir", bg=colors['primary_bg'], padx=10,
                                     pady=10)
        select_frame.pack(pady=10, padx=10, fill="x")

        self.tree = ttk.Treeview(select_frame, columns=("ID", "Nome", "Empresa"), show="headings", height=10)
        for col in ("ID", "Nome", "Empresa"): self.tree.heading(col, text=col)
        self.tree.column("ID", width=50)
        self.tree.column("Nome", width=300)
        self.tree.pack(fill="x", expand=True)
        self.tree.bind("<<TreeviewSelect>>", self.on_doc_select)

        self.delete_button = tk.Button(self, text="Excluir Documento Selecionado", command=self.delete_document,
                                       bg=colors['danger'], fg="white", font=("Helvetica", 12, "bold"),
                                       state="disabled")
        self.delete_button.pack(pady=20)
        self.load_documents_to_tree()

    def load_documents_to_tree(self):
        for item in self.tree.get_children(): self.tree.delete(item)
        query = "SELECT d.id, d.name, IFNULL(c.name, '-') FROM documents d LEFT JOIN companies c ON d.company_id = c.id ORDER BY d.id DESC"
        for row in DB_CONN.cursor().execute(query).fetchall():
            self.tree.insert("", "end", values=tuple(row))

    def on_doc_select(self, event=None):
        if not self.tree.selection():
            self.selected_doc_info = {}
            self.delete_button.config(state="disabled")
            return

        values = self.tree.item(self.tree.selection()[0])['values']
        self.selected_doc_info = {'id': values[0], 'name': values[1]}
        self.delete_button.config(state="normal")

    def delete_document(self):
        if not self.selected_doc_info: return
        doc_id = self.selected_doc_info['id']
        doc_name = self.selected_doc_info['name']
        if messagebox.askyesno("Confirmar Exclusão",
                               f"Tem certeza que deseja excluir permanentemente o documento:\n\n'{doc_name}' (ID: {doc_id})?\n\nEsta ação não pode ser desfeita."):
            DB_CONN.cursor().execute("DELETE FROM documents WHERE id=?", (doc_id,))
            DB_CONN.commit()
            messagebox.showinfo("Sucesso", f"Documento '{doc_name}' foi excluído.")
            self.load_documents_to_tree()
            self.delete_button.config(state="disabled")


class TransferDocumentFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Transferir / Emitir Documento")
        self.selected_doc_info = {}
        select_frame = tk.LabelFrame(self, text="1. Selecione o Documento", bg=colors['primary_bg'], padx=10, pady=10)
        select_frame.pack(pady=10, padx=10, fill="x")
        self.tree = ttk.Treeview(select_frame, columns=("ID", "Nome", "Status", "Local Atual"), show="headings",
                                 height=6)
        for col in ("ID", "Nome", "Status", "Local Atual"): self.tree.heading(col, text=col)
        self.tree.column("ID", width=50)
        self.tree.pack(fill="x", expand=True)
        self.tree.bind("<<TreeviewSelect>>", self.on_doc_select)
        self.action_frame = tk.LabelFrame(self, text="2. Escolha e Confirme a Ação", bg=colors['primary_bg'], padx=10,
                                          pady=10)
        self.action_frame.pack(pady=10, padx=10, fill="x")
        self.action_var = tk.StringVar(value="Transferência Interna")
        for action in ["Transferência Interna", "Emissão", "Ecenerar"]:
            tk.Radiobutton(self.action_frame, text=action, variable=self.action_var, value=action,
                           command=self.update_ui, bg=colors['primary_bg']).pack(anchor='w')
        self.location_label = tk.Label(self.action_frame, text="Novo Local:", bg=colors['primary_bg'])
        self.location_entry = tk.Entry(self.action_frame, width=50)
        self.confirm_button = tk.Button(self, text="Confirmar Ação", command=self.confirm_action,
                                        bg=colors['accent_teal'], fg="white", font=("Helvetica", 12, "bold"))
        self.confirm_button.pack(pady=20)
        self.load_documents_to_tree()
        self.toggle_actions('disabled')

    def toggle_actions(self, state):
        for child in self.action_frame.winfo_children(): child.config(state=state)
        self.confirm_button.config(state=state)

    def load_documents_to_tree(self):
        for item in self.tree.get_children(): self.tree.delete(item)
        query = "SELECT id, name, status, location FROM documents WHERE status NOT IN ('Emitido', 'Ecenerado') ORDER BY id DESC"
        for row in DB_CONN.cursor().execute(query).fetchall():
            self.tree.insert("", "end", values=tuple(row))

    def on_doc_select(self, event=None):
        if not self.tree.selection():
            self.selected_doc_info = {};
            self.toggle_actions('disabled');
            return
        values = self.tree.item(self.tree.selection()[0])['values']
        self.selected_doc_info = {'id': values[0], 'location': values[3]}
        self.toggle_actions('normal')
        self.update_ui()

    def update_ui(self):
        is_transfer = self.action_var.get() == "Transferência Interna"
        if is_transfer and self.confirm_button['state'] == 'normal':
            self.location_label.pack(anchor='w', padx=20);
            self.location_entry.pack(anchor='w', padx=20, fill='x')
        else:
            self.location_label.pack_forget();
            self.location_entry.pack_forget()

    def confirm_action(self):
        action = self.action_var.get()
        new_loc = self.location_entry.get().strip() if action == "Transferência Interna" else None
        actions_map = {
            "Transferência Interna": ("Transferido", new_loc, new_loc),
            "Emissão": ("Emitido", "Emitido para Externo", "Externo"),
            "Ecenerar": ("Ecenerado", "Enviado para Descarte", "Descarte")
        }
        new_status, new_location, to_location = actions_map[action]
        from_loc = self.selected_doc_info.get('location', '')

        if action == "Transferência Interna" and not new_loc:
            return messagebox.showwarning("Inválido", "Novo local é obrigatório.")

        if messagebox.askyesno("Confirmar",
                               f"Confirma a ação '{action}' para o documento ID {self.selected_doc_info['id']}?"):
            cursor = DB_CONN.cursor()
            cursor.execute("UPDATE documents SET status=?, location=? WHERE id=?",
                           (new_status, new_location, self.selected_doc_info['id']))
            cursor.execute(
                "INSERT INTO transfers (document_id, action, from_location, to_location) VALUES (?, ?, ?, ?)",
                (self.selected_doc_info['id'], action, from_loc, to_location))
            DB_CONN.commit()
            messagebox.showinfo("Sucesso", "Ação registrada!")
            self.load_documents_to_tree()
            self.toggle_actions('disabled')


class ManageCompaniesFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Gerenciar Empresas")
        add_frame = tk.Frame(self, bg=colors['primary_bg'])
        add_frame.pack(pady=10, padx=10, fill="x")
        tk.Label(add_frame, text="Nome da Empresa:", bg=colors['primary_bg']).pack(side="left", padx=5)
        self.company_entry = tk.Entry(add_frame, width=40)
        self.company_entry.pack(side="left", expand=True, fill="x", padx=5)
        tk.Button(add_frame, text="Adicionar", command=self.add_company, bg=colors['accent_teal'], fg="white").pack(
            side="left")
        list_frame = tk.Frame(self, bg=colors['primary_bg'])
        list_frame.pack(pady=10, padx=10, fill="both", expand=True)
        self.tree = ttk.Treeview(list_frame, columns=("ID", "Nome"), show="headings")
        self.tree.heading("ID", text="ID")
        self.tree.heading("Nome", text="Nome")
        self.tree.column("ID", width=100)
        self.tree.pack(side="left", fill="both", expand=True)
        tk.Button(list_frame, text="Excluir Selecionada", command=self.delete_company, bg=colors['danger'],
                  fg="white").pack(side="left", padx=10, anchor="n")
        self.load_companies()

    def load_companies(self):
        for item in self.tree.get_children(): self.tree.delete(item)
        for comp in get_companies():
            self.tree.insert("", "end", values=tuple(comp))

    def add_company(self):
        name = self.company_entry.get().strip()
        if not name:
            return messagebox.showwarning("Inválido", "O nome não pode ser vazio.")
        try:
            DB_CONN.cursor().execute("INSERT INTO companies (name) VALUES (?)", (name,))
            DB_CONN.commit()
            self.company_entry.delete(0, tk.END)
            self.load_companies()
        except sqlite3.IntegrityError:
            messagebox.showerror("Erro", "Empresa já existe.")

    def delete_company(self):
        if not self.tree.selection():
            return messagebox.showwarning("Seleção", "Selecione uma empresa.")
        comp_id, comp_name = self.tree.item(self.tree.selection()[0])['values']
        docs_count = \
        DB_CONN.cursor().execute("SELECT COUNT(id) FROM documents WHERE company_id = ?", (comp_id,)).fetchone()[0]
        if docs_count > 0:
            return messagebox.showerror("Erro",
                                        f"Não é possível excluir '{comp_name}'. {docs_count} documento(s) associado(s).")
        if messagebox.askyesno("Confirmar", f"Tem certeza que deseja excluir '{comp_name}'?"):
            DB_CONN.cursor().execute("DELETE FROM companies WHERE id = ?", (comp_id,))
            DB_CONN.commit()
            self.load_companies()


class ReportsFrame(BaseFrame):
    def __init__(self, master, colors):
        super().__init__(master, colors, "Relatórios e Gráficos")
        self.current_report = {}
        controls = tk.Frame(self, bg=colors['primary_bg'])
        controls.pack(pady=10, fill="x")
        tk.Button(controls, text="Movimentações", command=self.report_transfers).pack(side="left", padx=5)
        tk.Button(controls, text="Docs por Empresa", command=self.report_docs_by_company).pack(side="left", padx=5)
        self.export_button = tk.Button(self, text="Exportar para PDF", command=self.export_to_pdf, state="disabled",
                                       bg=colors['accent_blue'], fg="white")
        self.export_button.pack(pady=10)
        self.chart_frame = tk.Frame(self, bg="white")
        self.chart_frame.pack(fill="both", expand=True, pady=10, padx=10)
        self.fig = plt.figure()
        self.canvas = FigureCanvasTkAgg(self.fig, master=self.chart_frame)

    def generate_chart(self, cursor, title, chart_type):
        data = cursor.fetchall()
        headers = [d[0] for d in cursor.description]
        self.fig.clear()
        ax = self.fig.add_subplot(111)
        self.canvas.get_tk_widget().pack_forget()
        if not data:
            ax.text(0.5, 0.5, "Nenhum dado encontrado.", ha="center")
        else:
            labels, values = zip(*data)
            if chart_type == 'pie':
                ax.pie(values, labels=labels, autopct='%1.1f%%', startangle=140)
            elif chart_type == 'bar':
                ax.bar(labels, values, color=self.colors['accent_teal'])
                plt.xticks(rotation=30, ha="right")
            ax.set_title(title, pad=20)
        self.fig.tight_layout()
        self.canvas.draw()
        self.canvas.get_tk_widget().pack(fill="both", expand=True)
        self.export_button.config(state="normal" if data else "disabled")
        self.current_report = {'title': title, 'data': data, 'headers': headers}
        if data:
            self.fig.savefig(TEMP_CHART_FILE, bbox_inches='tight')

    def report_transfers(self):
        self.generate_chart(DB_CONN.cursor().execute("SELECT action, COUNT(*) FROM transfers GROUP BY action"),
                            "Distribuição de Movimentações", 'pie')

    def report_docs_by_company(self):
        self.generate_chart(DB_CONN.cursor().execute(
            "SELECT c.name, SUM(d.quantity) FROM documents d JOIN companies c ON d.company_id=c.id GROUP BY c.name ORDER BY SUM(d.quantity) DESC"),
            "Documentos por Empresa", 'bar')

    def export_to_pdf(self):
        filepath = filedialog.asksaveasfilename(defaultextension=".pdf", initialfile="Relatorio.pdf")
        if not filepath: return
        doc = SimpleDocTemplate(filepath, pagesize=letter)
        styles = getSampleStyleSheet()
        story = [Paragraph(self.current_report['title'], styles['Title']),
                 Paragraph(f"Gerado em: {datetime.now():%d/%m/%Y %H:%M}", styles['Normal']), Spacer(1, 0.25 * inch),
                 Image(TEMP_CHART_FILE, width=6 * inch, height=4 * inch), Spacer(1, 0.25 * inch),
                 Paragraph("Dados Tabulados", styles['h2'])]
        table_data = [self.current_report['headers']] + [[str(item) for item in row] for row in
                                                         self.current_report['data']]
        t = Table(table_data)
        t.setStyle(TableStyle([('BACKGROUND', (0, 0), (-1, 0), colors.HexColor(self.colors['header'])),
                               ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                               ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                               ('GRID', (0, 0), (-1, -1), 1, colors.black)]))
        story.append(t)
        try:
            doc.build(story)
            messagebox.showinfo("Sucesso", "Relatório PDF gerado!")
        except Exception as e:
            messagebox.showerror("Erro ao Gerar PDF", str(e))


# ==============================
# INICIALIZAÇÃO DO PROGRAMA
# ==============================
if __name__ == "__main__":
    login_root = tk.Tk()
    LoginWindow(login_root)
    login_root.mainloop()