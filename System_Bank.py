import tkinter as tk
from tkinter import messagebox, ttk # Importa ttk para widgets mais modernos

# Banco de dados de contas
accounts = []

def create_account_gui():
    name = entry_name.get()
    years = entry_age.get()
    cpf = entry_cpf.get()
    register = entry_register.get()

    if not all([name, years, cpf, register]):
        messagebox.showerror("Erro", "Todos os campos devem ser preenchidos!")
        return

    # Validar se o código de registro já existe
    if find_account_by_code(register):
        messagebox.showerror("Erro", "Código de registro já existe. Escolha outro.")
        return

    account = {
        "name": name,
        "years": years,
        "cpf": cpf,
        "register": register,
        "balance": 0.0
    }

    accounts.append(account)
    messagebox.showinfo("Sucesso", "Conta criada com sucesso!")
    clear_entries()
    update_account_treeview()

def find_account_by_code(register):
    for account in accounts:
        if account["register"] == register:
            return account
    return None

def deposit_gui():
    register = entry_deposit_register.get()
    amount_str = entry_deposit_amount.get()

    if not all([register, amount_str]):
        messagebox.showerror("Erro", "Código da conta e valor devem ser preenchidos!")
        return

    try:
        amount = float(amount_str)
        if amount <= 0:
            raise ValueError
    except ValueError:
        messagebox.showerror("Erro", "Valor de depósito inválido! Digite um número positivo.")
        return

    account = find_account_by_code(register)

    if account:
        account["balance"] += amount
        messagebox.showinfo("Sucesso", f"Depósito realizado com sucesso!\nSaldo atual: R${account['balance']:.2f}")
        clear_deposit_entries()
        update_account_treeview()
    else:
        messagebox.showerror("Erro", "Conta não encontrada!")

def cashout_gui():
    register = entry_cashout_register.get()
    amount_str = entry_cashout_amount.get()

    if not all([register, amount_str]):
        messagebox.showerror("Erro", "Código da conta e valor devem ser preenchidos!")
        return

    try:
        amount = float(amount_str)
        if amount <= 0:
            raise ValueError
    except ValueError:
        messagebox.showerror("Erro", "Valor de saque inválido! Digite um número positivo.")
        return

    account = find_account_by_code(register)

    if account:
        if amount > account["balance"]:
            messagebox.showerror("Erro", "Saldo insuficiente!")
        else:
            account["balance"] -= amount
            messagebox.showinfo("Sucesso", f"Saque realizado com sucesso!\nSaldo atual: R${account['balance']:.2f}")
            clear_cashout_entries()
            update_account_treeview()
    else:
        messagebox.showerror("Erro", "Conta não encontrada!")

def transfer_gui():
    from_code = entry_transfer_from.get()
    to_code = entry_transfer_to.get()
    amount_str = entry_transfer_amount.get()

    if not all([from_code, to_code, amount_str]):
        messagebox.showerror("Erro", "Todos os campos de transferência devem ser preenchidos!")
        return

    if from_code == to_code:
        messagebox.showerror("Erro", "Não é possível transferir para a mesma conta!")
        return

    try:
        amount = float(amount_str)
        if amount <= 0:
            raise ValueError
    except ValueError:
        messagebox.showerror("Erro", "Valor de transferência inválido! Digite um número positivo.")
        return

    from_account = find_account_by_code(from_code)
    to_account = find_account_by_code(to_code)

    if not from_account:
        messagebox.showerror("Erro", "Conta de origem não encontrada!")
        return
    if not to_account:
        messagebox.showerror("Erro", "Conta de destino não encontrada!")
        return

    if amount > from_account["balance"]:
        messagebox.showerror("Erro", "Saldo insuficiente para transferência!")
    else:
        from_account["balance"] -= amount
        to_account["balance"] += amount
        messagebox.showinfo("Sucesso", f"Transferência realizada com sucesso!\nSeu saldo: R${from_account['balance']:.2f}")
        clear_transfer_entries()
        update_account_treeview()

def update_account_treeview():
    # Limpa a Treeview
    for item in account_tree.get_children():
        account_tree.delete(item)

    if not accounts:
        account_tree.insert("", "end", values=("Nenhuma conta registrada.", "", ""))
    else:
        for acc in accounts:
            account_tree.insert("", "end", values=(acc['name'], acc['register'], f"R${acc['balance']:.2f}"))

def clear_entries():
    entry_name.delete(0, tk.END)
    entry_age.delete(0, tk.END)
    entry_cpf.delete(0, tk.END)
    entry_register.delete(0, tk.END)

def clear_deposit_entries():
    entry_deposit_register.delete(0, tk.END)
    entry_deposit_amount.delete(0, tk.END)

def clear_cashout_entries():
    entry_cashout_register.delete(0, tk.END)
    entry_cashout_amount.delete(0, tk.END)

def clear_transfer_entries():
    entry_transfer_from.delete(0, tk.END)
    entry_transfer_to.delete(0, tk.END)
    entry_transfer_amount.delete(0, tk.END)

# --- Configuração da janela principal ---
root = tk.Tk()
root.title("System Bank Pro")
root.geometry("900x750")
root.resizable(False, False) # Impede redimensionamento para manter o layout
root.configure(bg="#F0F0F0") # Fundo cinza claro para um visual mais neutro

# --- Estilos (usando ttk para um visual mais nativo e profissional) ---
style = ttk.Style()
style.theme_use("clam") # Tema moderno do ttk

# Cores
COLOR_PRIMARY_BLUE = "#003366" # Azul marinho profundo
COLOR_ACCENT_GOLD = "#DAA520" # Goldenrod para um dourado mais suave/bronze
COLOR_BACKGROUND_LIGHT = "#F5F5F5" # Cinza claro
COLOR_BACKGROUND_DARK = "#E0E0E0" # Cinza um pouco mais escuro para frames
COLOR_TEXT_DARK = "#333333" # Texto escuro
COLOR_BUTTON_HOVER = "#004080" # Azul mais escuro no hover

# Estilo para os Labels
style.configure("TLabel", background=COLOR_BACKGROUND_DARK, foreground=COLOR_TEXT_DARK, font=("Segoe UI", 10))
# Estilo específico para labels que não estão em LabelFrames (ex: títulos)
style.configure("Header.TLabel", background=COLOR_PRIMARY_BLUE, foreground="white", font=("Segoe UI", 16, "bold"))
style.configure("SubHeader.TLabel", background=COLOR_BACKGROUND_LIGHT, foreground=COLOR_PRIMARY_BLUE, font=("Segoe UI", 12, "bold"))


# Estilo para os Entry (campos de entrada)
style.configure("TEntry", fieldbackground="white", foreground=COLOR_TEXT_DARK, font=("Segoe UI", 10))

# Estilo para os Botões
style.configure("TButton",
                background=COLOR_PRIMARY_BLUE,
                foreground="white",
                font=("Segoe UI", 10, "bold"),
                padding=5,
                relief="flat")
style.map("TButton",
          background=[("active", COLOR_BUTTON_HOVER)],
          foreground=[("active", "white")])

# Estilo para os LabelFrames (bordas das seções)
style.configure("TLabelframe", background=COLOR_BACKGROUND_DARK, foreground=COLOR_PRIMARY_BLUE, font=("Segoe UI", 12, "bold"))
style.configure("TLabelframe.Label", background=COLOR_BACKGROUND_DARK, foreground=COLOR_PRIMARY_BLUE) # Cor do texto do labelframe

# Estilo para Treeview (Tabela de contas)
style.configure("Treeview",
                background="white",
                foreground=COLOR_TEXT_DARK,
                rowheight=25,
                fieldbackground="white",
                font=("Segoe UI", 9))
style.map('Treeview', background=[('selected', COLOR_ACCENT_GOLD)])
style.configure("Treeview.Heading",
                font=("Segoe UI", 10, "bold"),
                background=COLOR_PRIMARY_BLUE,
                foreground="white",
                relief="flat")


# --- Frame principal ---
main_frame = tk.Frame(root, bg=COLOR_BACKGROUND_LIGHT, padx=25, pady=25)
main_frame.pack(expand=True, fill="both")

# --- Cabeçalho ---
header_frame = tk.Frame(main_frame, bg=COLOR_PRIMARY_BLUE, padx=15, pady=10)
header_frame.pack(fill="x", pady=(0, 20))

# Os labels do cabeçalho são tk.Label porque o background já é o frame_header
tk.Label(header_frame, text="BANK ANTHONY DEVELOPER", font=("Segoe UI", 24, "bold"), fg="white", bg=COLOR_PRIMARY_BLUE).pack(pady=5)
tk.Label(header_frame, text="Developed by: Anthony Cavalcante", font=("Segoe UI", 12), fg=COLOR_ACCENT_GOLD, bg=COLOR_PRIMARY_BLUE).pack()

# --- Notebook para organizar as seções (abas) ---
notebook = ttk.Notebook(main_frame)
notebook.pack(expand=True, fill="both", pady=10)

# ==================== Aba Criar Conta ====================
create_tab = ttk.Frame(notebook, style="TLabelframe") # Note que a aba é um ttk.Frame
notebook.add(create_tab, text=" Criar Conta ")

create_account_frame = ttk.LabelFrame(create_tab, text="Dados da Nova Conta", style="TLabelframe")
create_account_frame.pack(padx=20, pady=20, fill="x")

# Todos os labels e entries dentro de create_account_frame agora são ttk.
ttk.Label(create_account_frame, text="Nome Completo:", style="TLabel").grid(row=0, column=0, sticky="w", pady=5, padx=5)
entry_name = ttk.Entry(create_account_frame, width=40, style="TEntry")
entry_name.grid(row=0, column=1, pady=5, padx=5)

ttk.Label(create_account_frame, text="Data de Nascimento (dd/mm/aaaa):", style="TLabel").grid(row=1, column=0, sticky="w", pady=5, padx=5)
entry_age = ttk.Entry(create_account_frame, width=40, style="TEntry")
entry_age.grid(row=1, column=1, pady=5, padx=5)

ttk.Label(create_account_frame, text="CPF:", style="TLabel").grid(row=2, column=0, sticky="w", pady=5, padx=5)
entry_cpf = ttk.Entry(create_account_frame, width=40, style="TEntry")
entry_cpf.grid(row=2, column=1, pady=5, padx=5)

ttk.Label(create_account_frame, text="Código de Registro (único):", style="TLabel").grid(row=3, column=0, sticky="w", pady=5, padx=5)
entry_register = ttk.Entry(create_account_frame, width=40, style="TEntry")
entry_register.grid(row=3, column=1, pady=5, padx=5)

ttk.Button(create_account_frame, text="Criar Conta", command=create_account_gui, style="TButton").grid(row=4, column=0, columnspan=2, pady=15)

# ==================== Aba Operações (Depósito, Saque, Transferência) ====================
operations_tab = ttk.Frame(notebook, style="TLabelframe")
notebook.add(operations_tab, text=" Operações ")

# --- Frame de Depósito ---
deposit_frame = ttk.LabelFrame(operations_tab, text="Realizar Depósito", style="TLabelframe")
deposit_frame.pack(padx=20, pady=10, fill="x")

ttk.Label(deposit_frame, text="Código da Conta:", style="TLabel").grid(row=0, column=0, sticky="w", pady=5, padx=5)
entry_deposit_register = ttk.Entry(deposit_frame, width=30, style="TEntry")
entry_deposit_register.grid(row=0, column=1, pady=5, padx=5)

ttk.Label(deposit_frame, text="Valor do Depósito (R$):", style="TLabel").grid(row=1, column=0, sticky="w", pady=5, padx=5)
entry_deposit_amount = ttk.Entry(deposit_frame, width=30, style="TEntry")
entry_deposit_amount.grid(row=1, column=1, pady=5, padx=5)

ttk.Button(deposit_frame, text="Depositar", command=deposit_gui, style="TButton").grid(row=2, column=0, columnspan=2, pady=10)

# --- Frame de Saque ---
cashout_frame = ttk.LabelFrame(operations_tab, text="Realizar Saque", style="TLabelframe")
cashout_frame.pack(padx=20, pady=10, fill="x")

ttk.Label(cashout_frame, text="Código da Conta:", style="TLabel").grid(row=0, column=0, sticky="w", pady=5, padx=5)
entry_cashout_register = ttk.Entry(cashout_frame, width=30, style="TEntry")
entry_cashout_register.grid(row=0, column=1, pady=5, padx=5)

ttk.Label(cashout_frame, text="Valor do Saque (R$):", style="TLabel").grid(row=1, column=0, sticky="w", pady=5, padx=5)
entry_cashout_amount = ttk.Entry(cashout_frame, width=30, style="TEntry")
entry_cashout_amount.grid(row=1, column=1, pady=5, padx=5)

ttk.Button(cashout_frame, text="Sacar", command=cashout_gui, style="TButton").grid(row=2, column=0, columnspan=2, pady=10)

# --- Frame de Transferência ---
transfer_frame = ttk.LabelFrame(operations_tab, text="Realizar Transferência", style="TLabelframe")
transfer_frame.pack(padx=20, pady=10, fill="x")

ttk.Label(transfer_frame, text="Cód. Conta Origem:", style="TLabel").grid(row=0, column=0, sticky="w", pady=5, padx=5)
entry_transfer_from = ttk.Entry(transfer_frame, width=30, style="TEntry")
entry_transfer_from.grid(row=0, column=1, pady=5, padx=5)

ttk.Label(transfer_frame, text="Cód. Conta Destino:", style="TLabel").grid(row=1, column=0, sticky="w", pady=5, padx=5)
entry_transfer_to = ttk.Entry(transfer_frame, width=30, style="TEntry")
entry_transfer_to.grid(row=1, column=1, pady=5, padx=5)

ttk.Label(transfer_frame, text="Valor da Transferência (R$):", style="TLabel").grid(row=2, column=0, sticky="w", pady=5, padx=5)
entry_transfer_amount = ttk.Entry(transfer_frame, width=30, style="TEntry")
entry_transfer_amount.grid(row=2, column=1, pady=5, padx=5)

ttk.Button(transfer_frame, text="Transferir", command=transfer_gui, style="TButton").grid(row=3, column=0, columnspan=2, pady=10)


# ==================== Aba Visualizar Contas ====================
view_accounts_tab = ttk.Frame(notebook, style="TLabelframe")
notebook.add(view_accounts_tab, text=" Visualizar Contas ")

accounts_list_frame = ttk.LabelFrame(view_accounts_tab, text="Contas Registradas", style="TLabelframe")
accounts_list_frame.pack(padx=20, pady=20, fill="both", expand=True)

# Usando Treeview para um formato de tabela
columns = ("Nome", "Código", "Saldo")
account_tree = ttk.Treeview(accounts_list_frame, columns=columns, show="headings", style="Treeview")
account_tree.pack(fill="both", expand=True)

for col in columns:
    account_tree.heading(col, text=col, anchor="w")
    account_tree.column(col, width=150, anchor="w")

# Adiciona scrollbar
scrollbar = ttk.Scrollbar(account_tree, orient="vertical", command=account_tree.yview)
account_tree.configure(yscrollcommand=scrollbar.set)
scrollbar.pack(side="right", fill="y")


ttk.Button(accounts_list_frame, text="Atualizar Lista", command=update_account_treeview, style="TButton").pack(pady=10)

# Inicializa a lista de contas ao abrir
update_account_treeview()

root.mainloop()