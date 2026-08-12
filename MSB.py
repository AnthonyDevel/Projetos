# sistema_customtkinter.py
import customtkinter as ctk
import tkinter as tk
from tkinter import ttk, messagebox, simpledialog, filedialog
import sqlite3
from datetime import datetime, date
import json
import csv
import os
import traceback
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import pandas as pd
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib import colors

# Configuração de tema do customtkinter
ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")


# =============================================
# CLASSE PARA NOTESPLANO (EDITOR DE TEXTO)
# =============================================
class NotesPlano:
    def __init__(self, parent_frame):
        self.frame = ctk.CTkFrame(parent_frame, corner_radius=8)
        self.frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(self.frame, text="📝 NotesPlano - Editor de Texto", font=('Arial', 20, 'bold')).pack(pady=10)

        # Frame de botões
        frame_botoes = ctk.CTkFrame(self.frame)
        frame_botoes.pack(fill="x", pady=5)

        ctk.CTkButton(frame_botoes, text="📄 Novo", command=self.novo_arquivo, height=40).pack(side="left", padx=5)
        ctk.CTkButton(frame_botoes, text="📂 Abrir", command=self.abrir_arquivo, height=40).pack(side="left", padx=5)
        ctk.CTkButton(frame_botoes, text="💾 Salvar", command=self.salvar_arquivo, height=40).pack(side="left", padx=5)
        ctk.CTkButton(frame_botoes, text="💾 Salvar Como...", command=self.salvar_como_arquivo, height=40).pack(
            side="left", padx=5)

        # Área de texto
        self.textbox = ctk.CTkTextbox(self.frame, font=("Arial", 14), wrap="word")
        self.textbox.pack(fill="both", expand=True, padx=5, pady=5)

        self.filepath = None

    def novo_arquivo(self):
        if self.textbox.get("1.0", "end-1c").strip() and messagebox.askyesno("Confirmar",
                                                                             "Deseja salvar as alterações antes de criar um novo arquivo?"):
            self.salvar_arquivo()
        self.textbox.delete("1.0", "end")
        self.filepath = None

    def abrir_arquivo(self):
        try:
            filepath = filedialog.askopenfilename(
                defaultextension=".txt",
                filetypes=[("Text Files", "*.txt"), ("Markdown Files", "*.md"), ("All Files", "*.*")]
            )
            if filepath:
                with open(filepath, "r", encoding="utf-8") as f:
                    self.textbox.delete("1.0", "end")
                    self.textbox.insert("1.0", f.read())
                self.filepath = filepath
        except Exception as e:
            messagebox.showerror("Erro", f"Não foi possível abrir o arquivo: {e}")

    def salvar_arquivo(self):
        if not self.filepath:
            self.salvar_como_arquivo()
        else:
            try:
                with open(self.filepath, "w", encoding="utf-8") as f:
                    f.write(self.textbox.get("1.0", "end-1c"))
                messagebox.showinfo("Sucesso", "Arquivo salvo com sucesso!")
            except Exception as e:
                messagebox.showerror("Erro", f"Não foi possível salvar o arquivo: {e}")

    def salvar_como_arquivo(self):
        try:
            filepath = filedialog.asksaveasfilename(
                defaultextension=".txt",
                filetypes=[("Text Files", "*.txt"), ("Markdown Files", "*.md"), ("All Files", "*.*")]
            )
            if filepath:
                self.filepath = filepath
                self.salvar_arquivo()
        except Exception as e:
            messagebox.showerror("Erro", f"Não foi possível salvar o arquivo: {e}")


# =============================================
# CLASSE PARA TABLESPLANO (PLANILHA)
# =============================================
class TablesPlano:
    def __init__(self, parent_frame):
        self.frame = ctk.CTkFrame(parent_frame, corner_radius=8)
        self.frame.pack(fill="both", expand=True, padx=20, pady=20)
        ctk.CTkLabel(self.frame, text="📄 TablesPlano - Planilha", font=('Arial', 20, 'bold')).pack(pady=10)

        # Controles
        frame_controles = ctk.CTkFrame(self.frame)
        frame_controles.pack(fill="x", pady=5)

        ctk.CTkButton(frame_controles, text="➕ Adicionar Linha", command=self.adicionar_linha).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="➖ Remover Linha", command=self.remover_linha).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="➕ Adicionar Coluna", command=self.adicionar_coluna).pack(side="left",
                                                                                                      padx=5)
        ctk.CTkButton(frame_controles, text="➖ Remover Coluna", command=self.remover_coluna).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="📂 Importar CSV", command=self.importar_csv).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="📊 Exportar CSV", command=self.exportar_csv).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="🧮 Calcular Soma", command=self.calcular_soma).pack(side="left", padx=5)

        # Frame para entrada de dados
        frame_entrada = ctk.CTkFrame(self.frame)
        frame_entrada.pack(fill="x", pady=5)

        ctk.CTkLabel(frame_entrada, text="Inserir dados:").pack(side="left", padx=5)
        self.entrada_dados = ctk.CTkEntry(frame_entrada, width=300,
                                          placeholder_text="Digite os dados separados por vírgula")
        self.entrada_dados.pack(side="left", padx=5)
        self.entrada_dados.bind("<Return>", self.inserir_dados_linha)

        ctk.CTkButton(frame_entrada, text="Inserir", command=self.inserir_dados_linha).pack(side="left", padx=5)

        # Tabela
        self.tree_frame = ctk.CTkFrame(self.frame)
        self.tree_frame.pack(fill="both", expand=True, pady=5)

        # Colunas Padrão
        self.columns = ("A", "B", "C", "D", "E")
        self.tree = ttk.Treeview(self.tree_frame, columns=self.columns, show="headings")

        for col in self.columns:
            self.tree.heading(col, text=col)
            self.tree.column(col, width=150)

        self.tree.pack(fill="both", expand=True, side="left")

        scrollbar = ttk.Scrollbar(self.tree_frame, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=scrollbar.set)
        scrollbar.pack(side="right", fill="y")

        # Adicionar bind para edição
        self.tree.bind("<Double-1>", self.editar_celula)

    def adicionar_linha(self):
        self.tree.insert("", "end", values=("", "", "", "", ""))

    def remover_linha(self):
        selected_item = self.tree.selection()
        if selected_item:
            self.tree.delete(selected_item)
        else:
            messagebox.showwarning("Aviso", "Selecione uma linha para remover.")

    def adicionar_coluna(self):
        nova_coluna = f"Col{len(self.columns) + 1}"
        self.columns = self.columns + (nova_coluna,)
        self.tree.config(columns=self.columns)
        self.tree.heading(nova_coluna, text=nova_coluna)
        self.tree.column(nova_coluna, width=150)

    def remover_coluna(self):
        if len(self.columns) > 1:
            self.columns = self.columns[:-1]
            self.tree.config(columns=self.columns)
        else:
            messagebox.showwarning("Aviso", "Não é possível remover todas as colunas.")

    def inserir_dados_linha(self, event=None):
        dados = self.entrada_dados.get().split(',')
        if dados and dados[0].strip():
            # Preenche com valores vazios se houver menos dados que colunas
            valores = [d.strip() for d in dados]
            while len(valores) < len(self.columns):
                valores.append("")
            self.tree.insert("", "end", values=valores[:len(self.columns)])
            self.entrada_dados.delete(0, "end")

    def calcular_soma(self):
        try:
            coluna = simpledialog.askstring("Calcular Soma", "Digite a letra da coluna para somar (A, B, C, etc.):")
            if coluna and coluna.upper() in self.columns:
                col_index = self.columns.index(coluna.upper())
                total = 0
                count = 0
                for item in self.tree.get_children():
                    valor = self.tree.item(item)['values'][col_index]
                    try:
                        total += float(valor) if valor else 0
                        count += 1
                    except ValueError:
                        continue
                messagebox.showinfo("Soma", f"Soma da coluna {coluna}: {total:.2f}\nItens somados: {count}")
        except Exception as e:
            messagebox.showerror("Erro", f"Erro ao calcular soma: {e}")

    def exportar_csv(self):
        filepath = filedialog.asksaveasfilename(defaultextension=".csv", filetypes=[("CSV files", "*.csv")])
        if not filepath:
            return

        try:
            with open(filepath, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(self.columns)
                for iid in self.tree.get_children():
                    writer.writerow(self.tree.item(iid)['values'])
            messagebox.showinfo("Sucesso", f"Planilha exportada como {os.path.basename(filepath)}")
        except Exception as e:
            messagebox.showerror("Erro", f"Falha na exportação: {e}")

    def importar_csv(self):
        filepath = filedialog.askopenfilename(filetypes=[("CSV files", "*.csv")])
        if not filepath:
            return

        try:
            # Limpa tabela atual
            for i in self.tree.get_children():
                self.tree.delete(i)

            with open(filepath, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                header = next(reader)

                # Atualiza as colunas
                self.columns = tuple(header)
                self.tree.config(columns=self.columns)

                for col in self.columns:
                    self.tree.heading(col, text=col)
                    self.tree.column(col, width=120)

                for row in reader:
                    self.tree.insert('', 'end', values=row)

            messagebox.showinfo("Sucesso", "CSV importado com sucesso.")
        except Exception as e:
            messagebox.showerror("Erro", f"Falha na importação: {e}")

    def editar_celula(self, event):
        item_id = self.tree.identify_row(event.y)
        column_id = self.tree.identify_column(event.x)

        if not item_id or not column_id:
            return

        col_index = int(column_id.replace("#", "")) - 1

        x, y, width, height = self.tree.bbox(item_id, column_id)

        entry = ctk.CTkEntry(self.tree_frame)

        current_value = self.tree.item(item_id, 'values')[col_index]
        entry.insert(0, current_value)
        entry.place(x=x, y=y, width=width, height=height)
        entry.focus()

        def on_focus_out(event):
            new_value = entry.get()
            current_values = list(self.tree.item(item_id, 'values'))
            current_values[col_index] = new_value
            self.tree.item(item_id, values=tuple(current_values))
            entry.destroy()

        entry.bind("<Return>", on_focus_out)
        entry.bind("<FocusOut>", on_focus_out)


# =============================================
# CLASSES PARA ÁREA FINANCEIRA
# =============================================

class ContaPagarReceberDialog(simpledialog.Dialog):
    def __init__(self, parent, tipo="Pagar"):
        self.tipo = tipo
        self.resultado = None
        super().__init__(parent, title=f"Adicionar Conta a {tipo}")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Descrição:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Valor:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Data Vencimento:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Categoria:").grid(row=3, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Centro de Custo:").grid(row=4, column=0, sticky="w", pady=5)

        self.descricao = ctk.CTkEntry(container, width=300)
        self.valor = ctk.CTkEntry(container, width=300)
        self.data_vencimento = ctk.CTkEntry(container, width=300)
        self.data_vencimento.insert(0, date.today().isoformat())
        self.categoria = ctk.CTkComboBox(container,
                                         values=['Fornecedores', 'Salários', 'Aluguel', 'Impostos', 'Serviços',
                                                 'Outros'], width=280)
        self.centro_custo = ctk.CTkComboBox(container,
                                            values=['Administrativo', 'Vendas', 'Produção', 'TI', 'Marketing', 'RH'],
                                            width=280)

        self.descricao.grid(row=0, column=1, padx=5, pady=5)
        self.valor.grid(row=1, column=1, padx=5, pady=5)
        self.data_vencimento.grid(row=2, column=1, padx=5, pady=5)
        self.categoria.grid(row=3, column=1, padx=5, pady=5)
        self.centro_custo.grid(row=4, column=1, padx=5, pady=5)

        return self.descricao

    def apply(self):
        if not all([self.descricao.get(), self.valor.get(), self.data_vencimento.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            valor = float(self.valor.get())
            self.resultado = (
                self.descricao.get(),
                self.tipo,
                valor,
                self.data_vencimento.get(),
                date.today().isoformat(),
                'Pendente',
                0.0,  # multa
                0.0,  # juros
                self.categoria.get(),
                self.centro_custo.get()
            )
        except ValueError:
            messagebox.showerror("Erro", "Valor deve ser um número!")


class ImportarExtratoDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Importar Extrato Bancário")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Selecionar arquivo:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Banco:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Conta:").grid(row=2, column=0, sticky="w", pady=5)

        self.arquivo_path = ctk.CTkEntry(container, width=250)
        self.banco = ctk.CTkComboBox(container,
                                     values=['Banco do Brasil', 'Itaú', 'Bradesco', 'Santander', 'Caixa', 'Nubank'],
                                     width=280)
        self.conta = ctk.CTkEntry(container, width=300)

        btn_procurar = ctk.CTkButton(container, text="Procurar", command=self.procurar_arquivo, width=80)

        self.arquivo_path.grid(row=0, column=1, padx=5, pady=5)
        btn_procurar.grid(row=0, column=2, padx=5, pady=5)
        self.banco.grid(row=1, column=1, padx=5, pady=5)
        self.conta.grid(row=2, column=1, padx=5, pady=5)

        return self.arquivo_path

    def procurar_arquivo(self):
        filepath = filedialog.askopenfilename(
            filetypes=[("CSV files", "*.csv"), ("TXT files", "*.txt"), ("All files", "*.*")]
        )
        if filepath:
            self.arquivo_path.delete(0, "end")
            self.arquivo_path.insert(0, filepath)

    def apply(self):
        if not all([self.arquivo_path.get(), self.banco.get(), self.conta.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos!")
            return

        self.resultado = (self.arquivo_path.get(), self.banco.get(), self.conta.get())


# =============================================
# DIÁLOGOS ORIGINAIS
# =============================================

class AdicionarFuncionarioDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Adicionar Funcionário")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Nome:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Cargo:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Departamento:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Salário:").grid(row=3, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Email:").grid(row=4, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Telefone:").grid(row=5, column=0, sticky="w", pady=5)

        self.nome = ctk.CTkEntry(container, width=300)
        self.cargo = ctk.CTkEntry(container, width=300)
        self.departamento = ctk.CTkComboBox(container,
                                            values=['Administrativo', 'Vendas', 'Produção', 'TI', 'RH', 'Financeiro'],
                                            width=280)
        self.salario = ctk.CTkEntry(container, width=300)
        self.email = ctk.CTkEntry(container, width=300)
        self.telefone = ctk.CTkEntry(container, width=300)

        self.nome.grid(row=0, column=1, padx=5, pady=5)
        self.cargo.grid(row=1, column=1, padx=5, pady=5)
        self.departamento.grid(row=2, column=1, padx=5, pady=5)
        self.salario.grid(row=3, column=1, padx=5, pady=5)
        self.email.grid(row=4, column=1, padx=5, pady=5)
        self.telefone.grid(row=5, column=1, padx=5, pady=5)

        return self.nome

    def apply(self):
        if not all([self.nome.get(), self.cargo.get(), self.departamento.get(), self.salario.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            salario = float(self.salario.get())
            self.resultado = (
                self.nome.get(),
                self.cargo.get(),
                self.departamento.get(),
                salario,
                date.today().isoformat(),
                self.email.get(),
                self.telefone.get()
            )
        except ValueError:
            messagebox.showerror("Erro", "Salário deve ser um número válido!")


class AdicionarClienteDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Adicionar Cliente")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Nome:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Email:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Telefone:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Endereço:").grid(row=3, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Tipo:").grid(row=4, column=0, sticky="w", pady=5)

        self.nome = ctk.CTkEntry(container, width=300)
        self.email = ctk.CTkEntry(container, width=300)
        self.telefone = ctk.CTkEntry(container, width=300)
        self.endereco = ctk.CTkEntry(container, width=300)
        self.tipo = ctk.CTkComboBox(container, values=['Pessoa Física', 'Pessoa Jurídica'], width=280)
        self.tipo.set("Pessoa Física")

        self.nome.grid(row=0, column=1, padx=5, pady=5)
        self.email.grid(row=1, column=1, padx=5, pady=5)
        self.telefone.grid(row=2, column=1, padx=5, pady=5)
        self.endereco.grid(row=3, column=1, padx=5, pady=5)
        self.tipo.grid(row=4, column=1, padx=5, pady=5)

        return self.nome

    def apply(self):
        if not self.nome.get():
            messagebox.showerror("Erro", "Nome é obrigatório!")
            return

        self.resultado = (
            self.nome.get(),
            self.email.get(),
            self.telefone.get(),
            self.endereco.get(),
            date.today().isoformat(),
            self.tipo.get()
        )


class AdicionarProdutoDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Adicionar Produto")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Nome:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Categoria:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Preço:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Custo:").grid(row=3, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Estoque:").grid(row=4, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Estoque Mínimo:").grid(row=5, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Descrição:").grid(row=6, column=0, sticky="w", pady=5)

        self.nome = ctk.CTkEntry(container, width=300)
        self.categoria = ctk.CTkComboBox(container,
                                         values=['Eletrônicos', 'Roupas', 'Alimentação', 'Livros', 'Casa', 'Esportes'],
                                         width=280)
        self.preco = ctk.CTkEntry(container, width=300)
        self.custo = ctk.CTkEntry(container, width=300)
        self.estoque = ctk.CTkEntry(container, width=300)
        self.estoque_minimo = ctk.CTkEntry(container, width=300)
        self.descricao = ctk.CTkTextbox(container, width=300, height=80)

        self.nome.grid(row=0, column=1, padx=5, pady=5)
        self.categoria.grid(row=1, column=1, padx=5, pady=5)
        self.preco.grid(row=2, column=1, padx=5, pady=5)
        self.custo.grid(row=3, column=1, padx=5, pady=5)
        self.estoque.grid(row=4, column=1, padx=5, pady=5)
        self.estoque_minimo.grid(row=5, column=1, padx=5, pady=5)
        self.descricao.grid(row=6, column=1, padx=5, pady=5)

        return self.nome

    def apply(self):
        if not all([self.nome.get(), self.categoria.get(), self.preco.get(), self.custo.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            preco = float(self.preco.get())
            custo = float(self.custo.get())
            estoque = int(self.estoque.get() or 0)
            estoque_minimo = int(self.estoque_minimo.get() or 0)

            if estoque < 0 or estoque_minimo < 0:
                messagebox.showerror("Erro", "Estoque não pode ser negativo!")
                return

            self.resultado = (
                self.nome.get(),
                self.categoria.get(),
                preco,
                custo,
                estoque,
                estoque_minimo,
                self.descricao.get("1.0", "end-1c").strip()
            )
        except ValueError:
            messagebox.showerror("Erro", "Valores numéricos inválidos!")


class AdicionarFornecedorDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Adicionar Fornecedor")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Nome:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="CNPJ:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Telefone:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Email:").grid(row=3, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Endereço:").grid(row=4, column=0, sticky="w", pady=5)

        self.nome = ctk.CTkEntry(container, width=300)
        self.cnpj = ctk.CTkEntry(container, width=300)
        self.telefone = ctk.CTkEntry(container, width=300)
        self.email = ctk.CTkEntry(container, width=300)
        self.endereco = ctk.CTkEntry(container, width=300)

        self.nome.grid(row=0, column=1, padx=5, pady=5)
        self.cnpj.grid(row=1, column=1, padx=5, pady=5)
        self.telefone.grid(row=2, column=1, padx=5, pady=5)
        self.email.grid(row=3, column=1, padx=5, pady=5)
        self.endereco.grid(row=4, column=1, padx=5, pady=5)

        return self.nome

    def apply(self):
        if not self.nome.get():
            messagebox.showerror("Erro", "Nome é obrigatório!")
            return

        self.resultado = (
            self.nome.get(),
            self.cnpj.get(),
            self.telefone.get(),
            self.email.get(),
            self.endereco.get(),
            date.today().isoformat()
        )


class NovaVendaDialog(simpledialog.Dialog):
    def __init__(self, parent, sistema):
        self.sistema = sistema
        self.resultado = None
        super().__init__(parent, title="Nova Venda")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        clientes = self.sistema.obter_clientes()
        produtos = self.sistema.obter_produtos()

        self.clientes_dict = {cliente[1]: cliente[0] for cliente in clientes}
        self.produtos_dict = {f"{produto[1]} - R$ {produto[3]:.2f}": produto[0] for produto in produtos}

        ctk.CTkLabel(container, text="Cliente:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Produto:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Quantidade:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Forma Pagamento:").grid(row=3, column=0, sticky="w", pady=5)

        self.cliente = ctk.CTkComboBox(container, values=list(self.clientes_dict.keys()), width=280)
        self.produto = ctk.CTkComboBox(container, values=list(self.produtos_dict.keys()), width=280)
        self.quantidade = ctk.CTkEntry(container, width=300)
        self.forma_pagamento = ctk.CTkComboBox(container, values=['Dinheiro', 'Cartão', 'PIX', 'Boleto'], width=280)

        self.cliente.grid(row=0, column=1, padx=5, pady=5)
        self.produto.grid(row=1, column=1, padx=5, pady=5)
        self.quantidade.grid(row=2, column=1, padx=5, pady=5)
        self.forma_pagamento.grid(row=3, column=1, padx=5, pady=5)

        return self.cliente

    def apply(self):
        if not all([self.cliente.get(), self.produto.get(), self.quantidade.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            cliente_id = self.clientes_dict[self.cliente.get()]
            produto_id = self.produtos_dict[self.produto.get()]
            quantidade = int(self.quantidade.get())

            conn = sqlite3.connect(self.sistema.db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT preco FROM produtos WHERE id = ?", (produto_id,))
            row = cursor.fetchone()
            conn.close()
            if not row:
                messagebox.showerror("Erro", "Produto não encontrado!")
                return

            preco = row[0]
            valor_total = preco * quantidade

            self.resultado = (
                cliente_id,
                produto_id,
                quantidade,
                preco,
                valor_total,
                date.today().isoformat(),
                self.forma_pagamento.get() or 'Dinheiro'
            )
        except (ValueError, KeyError):
            messagebox.showerror("Erro", "Dados inválidos!")


class NovaDespesaDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Nova Despesa")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Descrição:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Categoria:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Valor:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Forma Pagamento:").grid(row=3, column=0, sticky="w", pady=5)

        self.descricao = ctk.CTkEntry(container, width=300)
        self.categoria = ctk.CTkComboBox(container,
                                         values=['Salários', 'Aluguel', 'Energia', 'Água', 'Internet', 'Manutenção',
                                                 'Outros'], width=280)
        self.valor = ctk.CTkEntry(container, width=300)
        self.forma_pagamento = ctk.CTkComboBox(container, values=['Dinheiro', 'Cartão', 'PIX', 'Boleto'], width=280)

        self.descricao.grid(row=0, column=1, padx=5, pady=5)
        self.categoria.grid(row=1, column=1, padx=5, pady=5)
        self.valor.grid(row=2, column=1, padx=5, pady=5)
        self.forma_pagamento.grid(row=3, column=1, padx=5, pady=5)

        return self.descricao

    def apply(self):
        if not all([self.descricao.get(), self.categoria.get(), self.valor.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            valor = float(self.valor.get())
            self.resultado = (
                self.descricao.get(),
                self.categoria.get(),
                valor,
                date.today().isoformat(),
                self.forma_pagamento.get() or 'Dinheiro'
            )
        except ValueError:
            messagebox.showerror("Erro", "Valor deve ser um número!")


# =============================================
# DIÁLOGOS PARA RH
# =============================================

class EmitirHoleriteDialog(simpledialog.Dialog):
    def __init__(self, parent, sistema):
        self.sistema = sistema
        self.resultado = None
        super().__init__(parent, title="Emitir Holerite")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Funcionário:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Mês/Ano:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Horas Trabalhadas:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Faltas:").grid(row=3, column=0, sticky="w", pady=5)

        funcionarios = self.sistema.obter_funcionarios()
        self.funcionarios_dict = {f"{func[1]} - {func[2]}": func[0] for func in funcionarios}

        self.funcionario = ctk.CTkComboBox(container, values=list(self.funcionarios_dict.keys()), width=280)
        self.mes_ano = ctk.CTkEntry(container, width=300)
        self.mes_ano.insert(0, datetime.now().strftime("%m/%Y"))
        self.horas_trabalhadas = ctk.CTkEntry(container, width=300)
        self.horas_trabalhadas.insert(0, "220")
        self.faltas = ctk.CTkEntry(container, width=300)
        self.faltas.insert(0, "0")

        self.funcionario.grid(row=0, column=1, padx=5, pady=5)
        self.mes_ano.grid(row=1, column=1, padx=5, pady=5)
        self.horas_trabalhadas.grid(row=2, column=1, padx=5, pady=5)
        self.faltas.grid(row=3, column=1, padx=5, pady=5)

        return self.funcionario

    def apply(self):
        if not all([self.funcionario.get(), self.mes_ano.get(), self.horas_trabalhadas.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            funcionario_id = self.funcionarios_dict[self.funcionario.get()]
            horas = int(self.horas_trabalhadas.get())
            faltas = int(self.faltas.get())

            self.resultado = (funcionario_id, self.mes_ano.get(), horas, faltas)
        except (ValueError, KeyError):
            messagebox.showerror("Erro", "Dados inválidos!")


class MarcarEntrevistaDialog(simpledialog.Dialog):
    def __init__(self, parent):
        self.resultado = None
        super().__init__(parent, title="Marcar Entrevista")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Candidato:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Cargo:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Data/Hora:").grid(row=2, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Entrevistador:").grid(row=3, column=0, sticky="w", pady=5)

        self.candidato = ctk.CTkEntry(container, width=300)
        self.cargo = ctk.CTkComboBox(container,
                                     values=['Analista', 'Gerente', 'Desenvolvedor', 'Vendedor', 'Assistente'],
                                     width=280)
        self.data_hora = ctk.CTkEntry(container, width=300)
        self.data_hora.insert(0, datetime.now().strftime("%d/%m/%Y %H:%M"))
        self.entrevistador = ctk.CTkEntry(container, width=300)

        self.candidato.grid(row=0, column=1, padx=5, pady=5)
        self.cargo.grid(row=1, column=1, padx=5, pady=5)
        self.data_hora.grid(row=2, column=1, padx=5, pady=5)
        self.entrevistador.grid(row=3, column=1, padx=5, pady=5)

        return self.candidato

    def apply(self):
        if not all([self.candidato.get(), self.cargo.get(), self.data_hora.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        self.resultado = (
            self.candidato.get(),
            self.cargo.get(),
            self.data_hora.get(),
            self.entrevistador.get(),
            date.today().isoformat()
        )


class DemitirFuncionarioDialog(simpledialog.Dialog):
    def __init__(self, parent, sistema):
        self.sistema = sistema
        self.resultado = None
        super().__init__(parent, title="Demitir Funcionário")

    def body(self, frame):
        container = ctk.CTkFrame(frame)
        container.pack(fill="both", expand=True, padx=10, pady=10)

        ctk.CTkLabel(container, text="Funcionário:").grid(row=0, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Data Demissão:").grid(row=1, column=0, sticky="w", pady=5)
        ctk.CTkLabel(container, text="Motivo:").grid(row=2, column=0, sticky="w", pady=5)

        funcionarios = self.sistema.obter_funcionarios()
        self.funcionarios_dict = {f"{func[1]} - {func[2]}": func[0] for func in funcionarios}

        self.funcionario = ctk.CTkComboBox(container, values=list(self.funcionarios_dict.keys()), width=280)
        self.data_demissao = ctk.CTkEntry(container, width=300)
        self.data_demissao.insert(0, date.today().isoformat())
        self.motivo = ctk.CTkComboBox(container,
                                      values=['Rescisão voluntária', 'Rescisão por justa causa',
                                              'Término de contrato', 'Reestruturação'], width=280)

        self.funcionario.grid(row=0, column=1, padx=5, pady=5)
        self.data_demissao.grid(row=1, column=1, padx=5, pady=5)
        self.motivo.grid(row=2, column=1, padx=5, pady=5)

        return self.funcionario

    def apply(self):
        if not all([self.funcionario.get(), self.data_demissao.get()]):
            messagebox.showerror("Erro", "Preencha todos os campos obrigatórios!")
            return

        try:
            funcionario_id = self.funcionarios_dict[self.funcionario.get()]
            self.resultado = (funcionario_id, self.data_demissao.get(), self.motivo.get())
        except KeyError:
            messagebox.showerror("Erro", "Funcionário inválido!")


# =============================================
# SISTEMA PRINCIPAL COMPLETO
# =============================================

class SistemaEmpresarial:
    def __init__(self, root):
        self.root = root
        self.root.title("Sistema de Gestão Empresarial - v2.1")
        self.root.geometry("1400x800")

        # Configurar estilo para o Treeview
        style = ttk.Style()
        style.theme_use("default")
        style.configure("Treeview",
                        background="#2b2b2b",
                        foreground="white",
                        rowheight=25,
                        fieldbackground="#2b2b2b",
                        bordercolor="#333333",
                        borderwidth=0)
        style.map('Treeview', background=[('selected', '#22559b')])
        style.configure("Treeview.Heading",
                        background="#565b5e",
                        foreground="white",
                        relief="flat")
        style.map("Treeview.Heading",
                  background=[('active', '#3484F0')])

        try:
            self.root.iconbitmap(default=None)
        except Exception:
            pass

        self.db_path = "empresa.db"
        self.inicializar_banco_dados()
        self.criar_interface()

    def inicializar_banco_dados(self):
        """Inicializa o banco de dados com todas as tabelas necessárias"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # Tabela de Funcionários (atualizada para RH)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS funcionarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cargo TEXT NOT NULL,
                departamento TEXT NOT NULL,
                salario REAL NOT NULL,
                data_admissao DATE NOT NULL,
                email TEXT UNIQUE,
                telefone TEXT,
                ativo INTEGER DEFAULT 1,
                data_demissao DATE,
                motivo_demissao TEXT
            )
        ''')

        # Novas tabelas para RH
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS holerites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                funcionario_id INTEGER,
                mes_ano TEXT NOT NULL,
                horas_trabalhadas INTEGER,
                faltas INTEGER,
                salario_bruto REAL,
                descontos REAL,
                salario_liquido REAL,
                data_emissao DATE,
                FOREIGN KEY (funcionario_id) REFERENCES funcionarios (id)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS entrevistas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                candidato TEXT NOT NULL,
                cargo TEXT NOT NULL,
                data_hora TEXT NOT NULL,
                entrevistador TEXT,
                data_cadastro DATE,
                status TEXT DEFAULT 'Agendada'
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS verbas_rescisorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                funcionario_id INTEGER,
                data_demissao DATE,
                saldo_salario REAL,
                ferias_proporcionais REAL,
                decimo_terceiro REAL,
                multa_fgts REAL,
                total_verba REAL,
                data_calculo DATE,
                FOREIGN KEY (funcionario_id) REFERENCES funcionarios (id)
            )
        ''')

        # Tabelas originais
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                email TEXT UNIQUE,
                telefone TEXT,
                endereco TEXT,
                data_cadastro DATE NOT NULL,
                tipo TEXT DEFAULT 'Pessoa Física'
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS produtos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                categoria TEXT NOT NULL,
                preco REAL NOT NULL,
                custo REAL NOT NULL,
                estoque INTEGER NOT NULL,
                estoque_minimo INTEGER DEFAULT 0,
                descricao TEXT,
                ativo INTEGER DEFAULT 1
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS vendas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_id INTEGER,
                produto_id INTEGER,
                quantidade INTEGER NOT NULL,
                valor_unitario REAL NOT NULL,
                valor_total REAL NOT NULL,
                data_venda DATE NOT NULL,
                forma_pagamento TEXT DEFAULT 'Dinheiro',
                status TEXT DEFAULT 'Concluída',
                FOREIGN KEY (cliente_id) REFERENCES clientes (id),
                FOREIGN KEY (produto_id) REFERENCES produtos (id)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS despesas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descricao TEXT NOT NULL,
                categoria TEXT NOT NULL,
                valor REAL NOT NULL,
                data_despesa DATE NOT NULL,
                forma_pagamento TEXT DEFAULT 'Dinheiro'
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS fornecedores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cnpj TEXT UNIQUE,
                telefone TEXT,
                email TEXT,
                endereco TEXT,
                data_cadastro DATE NOT NULL
            )
        ''')

        # NOVAS TABELAS PARA ÁREA FINANCEIRA
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS contas_pagar_receber (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descricao TEXT NOT NULL,
                tipo TEXT NOT NULL,
                valor REAL NOT NULL,
                data_vencimento DATE NOT NULL,
                data_emissao DATE NOT NULL,
                status TEXT DEFAULT 'Pendente',
                multa REAL DEFAULT 0,
                juros REAL DEFAULT 0,
                categoria TEXT,
                centro_custo TEXT,
                data_pagamento DATE,
                valor_pago REAL
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS fluxo_caixa (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data DATE NOT NULL,
                descricao TEXT NOT NULL,
                tipo TEXT NOT NULL,
                valor REAL NOT NULL,
                categoria TEXT,
                centro_custo TEXT
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS extratos_bancarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                banco TEXT NOT NULL,
                conta TEXT NOT NULL,
                data DATE NOT NULL,
                descricao TEXT NOT NULL,
                valor REAL NOT NULL,
                tipo TEXT NOT NULL,
                categoria TEXT,
                conciliado INTEGER DEFAULT 0
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS centros_custo (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                departamento TEXT,
                orcamento REAL,
                responsavel TEXT
            )
        ''')

        conn.commit()
        conn.close()

    def criar_interface(self):
        """Cria a interface gráfica principal com customtkinter"""
        # Frame superior
        frame_superior = ctk.CTkFrame(self.root, height=100, corner_radius=0)
        frame_superior.pack(fill="x", padx=10, pady=5)

        lbl_titulo = ctk.CTkLabel(frame_superior, text="🏢 ANTHONY DEVELOPER-SOFTWARE BUSINESS",
                                  font=('Arial', 24, 'bold'))
        lbl_titulo.pack(pady=20)

        # Menu lateral
        frame_menu = ctk.CTkFrame(self.root, width=250, corner_radius=0)
        frame_menu.pack(side="left", fill="y", padx=5, pady=5)

        botoes_menu = [
            ("📊 DASHBOARD", self.mostrar_dashboard),
            ("📝 NOTESPLANO", self.mostrar_notesplano),
            ("📄 TABLESPLANO", self.mostrar_tablesplano),
            ("💰 FINANCEIRO", self.mostrar_financeiro),
            ("👥 RH", self.mostrar_rh),
            ("👥 FUNCIONÁRIOS", self.mostrar_funcionarios),
            ("👥 CLIENTES", self.mostrar_clientes),
            ("📦 PRODUTOS", self.mostrar_produtos),
            ("🏭 FORNECEDORES", self.mostrar_fornecedores),
            ("💰 VENDAS", self.mostrar_vendas),
            ("💸 DESPESAS", self.mostrar_despesas),
            ("📈 RELATÓRIOS", self.mostrar_relatorios),
            ("💾 BACKUP", self.mostrar_backup),
            ("⚙️ CONFIGURAÇÕES", self.mostrar_configuracoes)
        ]

        for texto, comando in botoes_menu:
            btn = ctk.CTkButton(frame_menu, text=texto, command=comando,
                                font=('Arial', 12, 'bold'), height=45)
            btn.pack(pady=5, padx=10, fill="x")

        # Área principal de conteúdo
        self.frame_conteudo = ctk.CTkFrame(self.root, corner_radius=8)
        self.frame_conteudo.pack(side="right", fill="both", expand=True, padx=5, pady=5)

        self.mostrar_dashboard()

    def limpar_conteudo(self):
        for widget in self.frame_conteudo.winfo_children():
            widget.destroy()

    # =============================================
    # MÉTODOS PARA NOTESPLANO E TABLESPLANO
    # =============================================

    def mostrar_notesplano(self):
        self.limpar_conteudo()
        self.notes_app = NotesPlano(self.frame_conteudo)

    def mostrar_tablesplano(self):
        self.limpar_conteudo()
        self.tables_app = TablesPlano(self.frame_conteudo)

    # =============================================
    # NOVO MÓDULO FINANCEIRO
    # =============================================

    def mostrar_financeiro(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="💰 ÁREA FINANCEIRA", font=('Arial', 20, 'bold')).pack(pady=10)

        # Abas para organização
        tabview = ctk.CTkTabview(frame, width=1200, height=600)
        tabview.pack(pady=10, padx=10, fill="both", expand=True)

        tabview.add("Contas")
        tabview.add("Fluxo de Caixa")
        tabview.add("Integração Bancária")
        tabview.add("Centro de Custos")

        # ABA 1: CONTAS A PAGAR E RECEBER
        frame_contas = tabview.tab("Contas")

        # Botões de controle
        frame_controles = ctk.CTkFrame(frame_contas)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Conta a Pagar",
                      command=lambda: self.adicionar_conta("Pagar")).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="➕ Conta a Receber",
                      command=lambda: self.adicionar_conta("Receber")).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="✅ Marcar como Pago",
                      command=self.marcar_conta_paga).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="📊 Relatório",
                      command=self.gerar_relatorio_contas).pack(side="left", padx=5)

        # Tabela de contas
        columns = ('ID', 'Descrição', 'Tipo', 'Valor', 'Vencimento', 'Status', 'Categoria', 'Centro Custo')
        self.tree_contas = ttk.Treeview(frame_contas, columns=columns, show='headings', height=15)

        for col in columns:
            self.tree_contas.heading(col, text=col)
            self.tree_contas.column(col, width=120)

        scrollbar = ttk.Scrollbar(frame_contas, orient=tk.VERTICAL, command=self.tree_contas.yview)
        self.tree_contas.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.tree_contas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_contas()

        # ABA 2: FLUXO DE CAIXA
        frame_fluxo = tabview.tab("Fluxo de Caixa")

        # Controles do fluxo de caixa
        frame_fluxo_controles = ctk.CTkFrame(frame_fluxo)
        frame_fluxo_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_fluxo_controles, text="📊 Gráfico Diário",
                      command=lambda: self.gerar_grafico_fluxo('diario')).pack(side="left", padx=5)
        ctk.CTkButton(frame_fluxo_controles, text="📊 Gráfico Semanal",
                      command=lambda: self.gerar_grafico_fluxo('semanal')).pack(side="left", padx=5)
        ctk.CTkButton(frame_fluxo_controles, text="📊 Gráfico Mensal",
                      command=lambda: self.gerar_grafico_fluxo('mensal')).pack(side="left", padx=5)

        # Frame para gráfico
        self.frame_grafico_fluxo = ctk.CTkFrame(frame_fluxo)
        self.frame_grafico_fluxo.pack(fill="both", expand=True, pady=10)

        # ABA 3: INTEGRAÇÃO BANCÁRIA
        frame_bancario = tabview.tab("Integração Bancária")

        ctk.CTkButton(frame_bancario, text="📁 Importar Extrato",
                      command=self.importar_extrato, width=200, height=50).pack(pady=10)
        ctk.CTkButton(frame_bancario, text="🔄 Conciliação Automática",
                      command=self.conciliacao_automatica, width=200, height=50).pack(pady=10)

        # ABA 4: CENTRO DE CUSTOS
        frame_centro_custos = tabview.tab("Centro de Custos")

        # Controles centro de custos
        frame_cc_controles = ctk.CTkFrame(frame_centro_custos)
        frame_cc_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_cc_controles, text="➕ Adicionar Centro",
                      command=self.adicionar_centro_custo).pack(side="left", padx=5)
        ctk.CTkButton(frame_cc_controles, text="📊 Relatório por Centro",
                      command=self.relatorio_centro_custos).pack(side="left", padx=5)

        # Tabela centro de custos
        columns_cc = ('ID', 'Nome', 'Departamento', 'Orçamento', 'Responsável')
        self.tree_centro_custos = ttk.Treeview(frame_centro_custos, columns=columns_cc, show='headings', height=10)

        for col in columns_cc:
            self.tree_centro_custos.heading(col, text=col)
            self.tree_centro_custos.column(col, width=120)

        scrollbar_cc = ttk.Scrollbar(frame_centro_custos, orient=tk.VERTICAL, command=self.tree_centro_custos.yview)
        self.tree_centro_custos.configure(yscroll=scrollbar_cc.set)
        scrollbar_cc.pack(side=tk.RIGHT, fill=tk.Y)
        self.tree_centro_custos.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_centro_custos()

    def adicionar_conta(self, tipo):
        dialog = ContaPagarReceberDialog(self.root, tipo)
        if dialog.resultado:
            self.salvar_conta(dialog.resultado)
            self.popular_contas()

    def salvar_conta(self, dados):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO contas_pagar_receber 
                (descricao, tipo, valor, data_vencimento, data_emissao, status, multa, juros, categoria, centro_custo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', dados)
            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", f"✅ Conta adicionada com sucesso!")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro ao salvar conta: {str(e)}")

    def popular_contas(self):
        for item in self.tree_contas.get_children():
            self.tree_contas.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, descricao, tipo, valor, data_vencimento, status, categoria, centro_custo
            FROM contas_pagar_receber
            ORDER BY data_vencimento
        ''')
        contas = cursor.fetchall()
        conn.close()

        for conta in contas:
            conta_list = list(conta)
            conta_list[3] = f"R$ {conta_list[3]:.2f}"
            # Colorir based on status and type
            if conta[5] == 'Pago':
                self.tree_contas.insert('', tk.END, values=conta_list, tags=('pago',))
            elif conta[2] == 'Pagar':
                self.tree_contas.insert('', tk.END, values=conta_list, tags=('pagar',))
            else:
                self.tree_contas.insert('', tk.END, values=conta_list, tags=('receber',))

        self.tree_contas.tag_configure('pago', background='#27ae60')
        self.tree_contas.tag_configure('pagar', background='#e74c3c')
        self.tree_contas.tag_configure('receber', background='#3498db')

    def marcar_conta_paga(self):
        selected_item = self.tree_contas.selection()
        if not selected_item:
            messagebox.showwarning("Aviso", "Selecione uma conta para marcar como paga.")
            return

        conta_id = self.tree_contas.item(selected_item[0])['values'][0]
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                UPDATE contas_pagar_receber 
                SET status = 'Pago', data_pagamento = ?, valor_pago = valor
                WHERE id = ?
            ''', (date.today().isoformat(), conta_id))
            conn.commit()
            conn.close()
            self.popular_contas()
            messagebox.showinfo("Sucesso", "✅ Conta marcada como paga!")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro ao atualizar conta: {str(e)}")

    def gerar_relatorio_contas(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # Totais
        cursor.execute("SELECT SUM(valor) FROM contas_pagar_receber WHERE tipo = 'Pagar' AND status = 'Pendente'")
        total_pagar = cursor.fetchone()[0] or 0

        cursor.execute("SELECT SUM(valor) FROM contas_pagar_receber WHERE tipo = 'Receber' AND status = 'Pendente'")
        total_receber = cursor.fetchone()[0] or 0

        cursor.execute("SELECT COUNT(*) FROM contas_pagar_receber WHERE data_vencimento < ? AND status = 'Pendente'",
                       (date.today().isoformat(),))
        contas_vencidas = cursor.fetchone()[0]

        conn.close()

        relatorio = f"""
        📊 RELATÓRIO DE CONTAS
        ======================
        Total a Pagar: R$ {total_pagar:.2f}
        Total a Receber: R$ {total_receber:.2f}
        Saldo: R$ {total_receber - total_pagar:.2f}
        Contas Vencidas: {contas_vencidas}
        """
        messagebox.showinfo("Relatório de Contas", relatorio)

    def gerar_grafico_fluxo(self, periodo):
        # Limpa o frame do gráfico
        for widget in self.frame_grafico_fluxo.winfo_children():
            widget.destroy()

        conn = sqlite3.connect(self.db_path)

        if periodo == 'diario':
            query = """
                SELECT date(data), SUM(CASE WHEN tipo = 'Receita' THEN valor ELSE -valor END)
                FROM fluxo_caixa
                WHERE date(data) >= date('now', '-30 days')
                GROUP BY date(data)
                ORDER BY date(data)
            """
            titulo = "Fluxo de Caixa - Últimos 30 Dias"
        elif periodo == 'semanal':
            query = """
                SELECT strftime('%Y-%W', data), SUM(CASE WHEN tipo = 'Receita' THEN valor ELSE -valor END)
                FROM fluxo_caixa
                WHERE data >= date('now', '-3 months')
                GROUP BY strftime('%Y-%W', data)
                ORDER BY strftime('%Y-%W', data)
            """
            titulo = "Fluxo de Caixa - Semanal (3 Meses)"
        else:  # mensal
            query = """
                SELECT strftime('%Y-%m', data), SUM(CASE WHEN tipo = 'Receita' THEN valor ELSE -valor END)
                FROM fluxo_caixa
                GROUP BY strftime('%Y-%m', data)
                ORDER BY strftime('%Y-%m', data)
            """
            titulo = "Fluxo de Caixa - Mensal"

        df = pd.read_sql_query(query, conn)
        conn.close()

        if df.empty:
            ctk.CTkLabel(self.frame_grafico_fluxo, text="Sem dados para exibir",
                         font=('Arial', 16)).pack(expand=True)
            return

        fig, ax = plt.subplots(figsize=(10, 6), facecolor="#2b2b2b")

        # Gráfico de barras
        bars = ax.bar(df.iloc[:, 0], df.iloc[:, 1],
                      color=['#27ae60' if x >= 0 else '#e74c3c' for x in df.iloc[:, 1]])

        ax.set_facecolor("#2b2b2b")
        ax.set_title(titulo, color="white", fontsize=16)
        ax.set_ylabel("Saldo (R$)", color="white")
        ax.tick_params(axis='x', labelcolor='white', rotation=45)
        ax.tick_params(axis='y', labelcolor='white')

        # Adicionar valores nas barras
        for bar in bars:
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width() / 2., height,
                    f'R$ {height:.0f}', ha='center', va='bottom' if height >= 0 else 'top',
                    color='white', fontweight='bold')

        plt.tight_layout()

        canvas = FigureCanvasTkAgg(fig, self.frame_grafico_fluxo)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True)

    def importar_extrato(self):
        dialog = ImportarExtratoDialog(self.root)
        if dialog.resultado:
            arquivo, banco, conta = dialog.resultado
            try:
                # Simulação de importação - implementar conforme formato do banco
                df = pd.read_csv(arquivo)
                messagebox.showinfo("Sucesso", f"Extrato importado com sucesso!\n{len(df)} transações processadas.")
            except Exception as e:
                messagebox.showerror("Erro", f"Erro na importação: {str(e)}")

    def conciliacao_automatica(self):
        try:
            conn = sqlite3.connect(self.db_path)

            # Simulação de conciliação
            cursor = conn.cursor()
            cursor.execute('''
                UPDATE contas_pagar_receber 
                SET status = 'Pago', data_pagamento = ?
                WHERE status = 'Pendente' AND data_vencimento < ?
            ''', (date.today().isoformat(), date.today().isoformat()))

            linhas_afetadas = cursor.rowcount
            conn.commit()
            conn.close()

            messagebox.showinfo("Conciliação",
                                f"Conciliação automática realizada!\n{linhas_afetadas} contas marcadas como pagas.")
        except Exception as e:
            messagebox.showerror("Erro", f"Erro na conciliação: {str(e)}")

    def adicionar_centro_custo(self):
        nome = simpledialog.askstring("Centro de Custo", "Nome do centro de custo:")
        if nome:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO centros_custo (nome, departamento, orcamento, responsavel)
                    VALUES (?, ?, ?, ?)
                ''', (nome, 'Geral', 0.0, ''))
                conn.commit()
                conn.close()
                self.popular_centro_custos()
                messagebox.showinfo("Sucesso", "✅ Centro de custo adicionado!")
            except Exception as e:
                messagebox.showerror("Erro", f"❌ Erro ao adicionar: {str(e)}")

    def popular_centro_custos(self):
        for item in self.tree_centro_custos.get_children():
            self.tree_centro_custos.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('SELECT id, nome, departamento, orcamento, responsavel FROM centros_custo')
        centros = cursor.fetchall()
        conn.close()

        for centro in centros:
            centro_list = list(centro)
            centro_list[3] = f"R$ {centro_list[3]:.2f}" if centro_list[3] else "R$ 0.00"
            self.tree_centro_custos.insert('', tk.END, values=centro_list)

    def relatorio_centro_custos(self):
        conn = sqlite3.connect(self.db_path)

        # Gastos por centro de custo
        query = """
            SELECT centro_custo, SUM(valor) as total
            FROM contas_pagar_receber 
            WHERE tipo = 'Pagar' AND status = 'Pago'
            GROUP BY centro_custo
        """
        df = pd.read_sql_query(query, conn)
        conn.close()

        if df.empty:
            messagebox.showinfo("Relatório", "Sem dados de gastos por centro de custo.")
            return

        relatorio = "📊 GASTOS POR CENTRO DE CUSTO\n\n"
        for _, row in df.iterrows():
            relatorio += f"{row['centro_custo']}: R$ {row['total']:.2f}\n"

        messagebox.showinfo("Relatório Centro de Custos", relatorio)

    # =============================================
    # MÓDULO RH
    # =============================================

    def mostrar_rh(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="👥 RECURSOS HUMANOS", font=('Arial', 20, 'bold')).pack(pady=10)

        # Abas para organização
        tabview = ctk.CTkTabview(frame, width=1200, height=600)
        tabview.pack(pady=10, padx=10, fill="both", expand=True)

        tabview.add("Funcionalidades")
        tabview.add("Relatórios")
        tabview.add("Documentos")

        # ABA 1: FUNCIONALIDADES
        frame_func = tabview.tab("Funcionalidades")

        # Botões principais do RH
        botoes_rh = [
            ("📋 Emitir Holerite", self.emitir_holerite),
            ("📅 Marcar Entrevista", self.marcar_entrevista),
            ("🚪 Demitir Funcionário", self.demitir_funcionario),
            ("💰 Calcular Verbas", self.calcular_verbas_rescisorias),
            ("🛡️ Seguro Desemprego", self.emitir_seguro_desemprego),
            ("🏦 FGTS", self.consultar_fgts),
            ("👥 Contratações", self.mostrar_contratacoes),
            ("📊 Folha Pagamento", self.mostrar_folha_pagamento)
        ]

        for i, (texto, comando) in enumerate(botoes_rh):
            row = i // 4
            col = i % 4
            btn = ctk.CTkButton(frame_func, text=texto, command=comando, width=280, height=80)
            btn.grid(row=row, column=col, padx=5, pady=5, sticky="nsew")
            frame_func.grid_columnconfigure(col, weight=1)
            frame_func.grid_rowconfigure(row, weight=1)

        # ABA 2: RELATÓRIOS
        frame_relatorios = tabview.tab("Relatórios")

        # Gráficos e relatórios
        frame_graficos = ctk.CTkFrame(frame_relatorios)
        frame_graficos.pack(fill="both", expand=True, padx=10, pady=10)

        # Botões para gerar relatórios
        botoes_relatorios = [
            ("📊 Gráfico Folha Pagamento", self.gerar_grafico_folha),
            ("👥 Distribuição por Cargo", self.gerar_grafico_cargos),
            ("📈 Evolução Salarial", self.gerar_grafico_evolucao),
            ("💸 Custos por Departamento", self.gerar_grafico_departamentos)
        ]

        for i, (texto, comando) in enumerate(botoes_relatorios):
            btn = ctk.CTkButton(frame_graficos, text=texto, command=comando, width=250, height=50)
            btn.grid(row=i // 2, column=i % 2, padx=10, pady=10, sticky="nsew")
            frame_graficos.grid_columnconfigure(i % 2, weight=1)

        # Área para exibir gráficos
        self.frame_grafico_rh = ctk.CTkFrame(frame_graficos, height=300)
        self.frame_grafico_rh.grid(row=2, column=0, columnspan=2, padx=10, pady=10, sticky="nsew")
        frame_graficos.grid_rowconfigure(2, weight=1)

        # ABA 3: DOCUMENTOS
        frame_docs = tabview.tab("Documentos")

        botoes_docs = [
            ("📄 Exportar PDF", self.exportar_rh_pdf),
            ("📊 Exportar Excel", self.exportar_rh_excel),
            ("🖨️ Imprimir Relatórios", self.imprimir_relatorios_rh),
            ("💾 Backup RH", self.backup_rh)
        ]

        for i, (texto, comando) in enumerate(botoes_docs):
            btn = ctk.CTkButton(frame_docs, text=texto, command=comando, width=280, height=80)
            btn.grid(row=i // 2, column=i % 2, padx=10, pady=10, sticky="nsew")
            frame_docs.grid_columnconfigure(i % 2, weight=1)

    def emitir_holerite(self):
        dialog = EmitirHoleriteDialog(self.root, self)
        if dialog.resultado:
            funcionario_id, mes_ano, horas, faltas = dialog.resultado

            # Obter dados do funcionário
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT nome, salario FROM funcionarios WHERE id = ?", (funcionario_id,))
            row = cursor.fetchone()
            if not row:
                messagebox.showerror("Erro", "Funcionário não encontrado!")
                conn.close()
                return

            nome, salario_bruto = row

            # Cálculos simplificados
            valor_hora = salario_bruto / 220
            salario_proporcional = valor_hora * horas
            desconto_faltas = valor_hora * faltas * 2
            inss = salario_bruto * 0.08
            irrf = salario_bruto * 0.075

            descontos = inss + irrf + desconto_faltas
            salario_liquido = salario_proporcional - descontos

            # Salvar holerite
            cursor.execute('''
                INSERT INTO holerites (funcionario_id, mes_ano, horas_trabalhadas, faltas,
                                     salario_bruto, descontos, salario_liquido, data_emissao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (funcionario_id, mes_ano, horas, faltas, salario_bruto, descontos, salario_liquido, date.today()))

            conn.commit()
            conn.close()

            # Mostrar recibo
            recibo = f"""
            📋 HOLERITE - {mes_ano}
            Funcionário: {nome}
            ===============================
            Salário Bruto: R$ {salario_bruto:.2f}
            Horas Trabalhadas: {horas}
            Faltas: {faltas}
            -------------------------------
            DESCONTOS:
            INSS: R$ {inss:.2f}
            IRRF: R$ {irrf:.2f}
            Faltas: R$ {desconto_faltas:.2f}
            Total Descontos: R$ {descontos:.2f}
            -------------------------------
            SALÁRIO LÍQUIDO: R$ {salario_liquido:.2f}
            """

            messagebox.showinfo("Holerite Emitido", recibo)

    def marcar_entrevista(self):
        dialog = MarcarEntrevistaDialog(self.root)
        if dialog.resultado:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO entrevistas (candidato, cargo, data_hora, entrevistador, data_cadastro)
                VALUES (?, ?, ?, ?, ?)
            ''', dialog.resultado)
            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", "✅ Entrevista agendada com sucesso!")

    def demitir_funcionario(self):
        dialog = DemitirFuncionarioDialog(self.root, self)
        if dialog.resultado:
            funcionario_id, data_demissao, motivo = dialog.resultado

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                UPDATE funcionarios
                SET ativo = 0, data_demissao = ?, motivo_demissao = ?
                WHERE id = ?
            ''', (data_demissao, motivo, funcionario_id))
            conn.commit()
            conn.close()

            messagebox.showinfo("Sucesso", "✅ Funcionário demitido com sucesso!")

    def calcular_verbas_rescisorias(self):
        funcionarios = self.obter_funcionarios()
        if not funcionarios:
            messagebox.showinfo("Info", "Nenhum funcionário cadastrado!")
            return

        funcionario_str = simpledialog.askstring("Verbas Rescisórias",
                                                 "Digite o ID do funcionário:")
        if not funcionario_str:
            return

        try:
            funcionario_id = int(funcionario_str)
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT nome, salario, data_admissao FROM funcionarios WHERE id = ?", (funcionario_id,))
            func = cursor.fetchone()

            if not func:
                messagebox.showerror("Erro", "Funcionário não encontrado!")
                return

            nome, salario, data_admissao = func

            # Cálculos simplificados das verbas
            saldo_salario = salario / 30 * 15
            ferias_proporcionais = salario / 12 * 6
            decimo_terceiro = salario / 12 * 6
            multa_fgts = salario * 0.4

            total_verba = saldo_salario + ferias_proporcionais + decimo_terceiro + multa_fgts

            verbas = f"""
            💰 VERBAS RESCISÓRIAS - {nome}
            ===============================
            Saldo de Salário: R$ {saldo_salario:.2f}
            Férias Proporcionais: R$ {ferias_proporcionais:.2f}
            Décimo Terceiro: R$ {decimo_terceiro:.2f}
            Multa FGTS: R$ {multa_fgts:.2f}
            -------------------------------
            TOTAL: R$ {total_verba:.2f}
            """

            # Salvar cálculo
            cursor.execute('''
                INSERT INTO verbas_rescisorias
                (funcionario_id, data_demissao, saldo_salario, ferias_proporcionais,
                 decimo_terceiro, multa_fgts, total_verba, data_calculo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (funcionario_id, date.today(), saldo_salario, ferias_proporcionais,
                  decimo_terceiro, multa_fgts, total_verba, date.today()))

            conn.commit()
            conn.close()

            messagebox.showinfo("Verbas Rescisórias", verbas)

        except ValueError:
            messagebox.showerror("Erro", "ID inválido!")

    def emitir_seguro_desemprego(self):
        messagebox.showinfo("Seguro Desemprego",
                            "📄 Documentação para seguro desemprego gerada!\n"
                            "Encaminhe ao funcionário para protocolo na Caixa Econômica.")

    def consultar_fgts(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT SUM(salario) FROM funcionarios WHERE ativo = 1")
        total_folha = cursor.fetchone()[0] or 0
        fgts_mensal = total_folha * 0.08

        messagebox.showinfo("Consulta FGTS",
                            f"🏦 INFORMAÇÕES FGTS\n"
                            f"Folha Mensal: R$ {total_folha:.2f}\n"
                            f"Depósito Mensal (8%): R$ {fgts_mensal:.2f}\n"
                            f"Total Estimado Anual: R$ {fgts_mensal * 12:.2f}")

    def mostrar_contratacoes(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT candidato, cargo, data_hora, status
            FROM entrevistas ORDER BY data_hora DESC
        ''')
        entrevistas = cursor.fetchall()
        conn.close()

        if not entrevistas:
            messagebox.showinfo("Contratações", "Nenhuma entrevista agendada.")
            return

        relatorio = "📅 ENTREVISTAS AGENDADAS\n\n"
        for ent in entrevistas:
            relatorio += f"👤 {ent[0]} - {ent[1]}\n"
            relatorio += f"   📅 {ent[2]} - Status: {ent[3]}\n\n"

        messagebox.showinfo("Contratações", relatorio)

    def mostrar_folha_pagamento(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT f.nome, f.departamento, f.salario, h.salario_liquido, h.mes_ano
            FROM funcionarios f
            LEFT JOIN holerites h ON f.id = h.funcionario_id
            WHERE f.ativo = 1
        ''')
        folha = cursor.fetchall()
        conn.close()

        if not folha:
            messagebox.showinfo("Folha de Pagamento", "Nenhum dado de folha encontrado.")
            return

        total_bruto = sum(func[2] for func in folha)
        total_liquido = sum(func[3] or 0 for func in folha if func[3])

        relatorio = f"💰 FOLHA DE PAGAMENTO\n\n"
        relatorio += f"Total Bruto: R$ {total_bruto:.2f}\n"
        relatorio += f"Total Líquido: R$ {total_liquido:.2f}\n"
        relatorio += f"Total Descontos: R$ {total_bruto - total_liquido:.2f}\n\n"

        for func in folha:
            relatorio += f"👤 {func[0]} - {func[1]}\n"
            relatorio += f"   💰 Bruto: R$ {func[2]:.2f} | Líquido: R$ {func[3] or func[2]:.2f}\n"

        messagebox.showinfo("Folha de Pagamento", relatorio)

    # GRÁFICOS E RELATÓRIOS RH
    def gerar_grafico_folha(self):
        self.limpar_grafico_rh()

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT departamento, SUM(salario)
            FROM funcionarios WHERE ativo = 1
            GROUP BY departamento
        ''')
        dados = cursor.fetchall()
        conn.close()

        if not dados:
            messagebox.showinfo("Info", "Nenhum dado disponível!")
            return

        departamentos = [d[0] for d in dados]
        valores = [d[1] for d in dados]

        fig, ax = plt.subplots(figsize=(8, 6), facecolor="#2b2b2b")
        ax.pie(valores, labels=departamentos, autopct='%1.1f%%', startangle=90,
               textprops={'color': "w"})
        ax.set_title('Distribuição da Folha por Departamento', color="w")

        canvas = FigureCanvasTkAgg(fig, self.frame_grafico_rh)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True)

    def gerar_grafico_cargos(self):
        self.limpar_grafico_rh()

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT cargo, COUNT(*)
            FROM funcionarios WHERE ativo = 1
            GROUP BY cargo
        ''')
        dados = cursor.fetchall()
        conn.close()

        if not dados:
            messagebox.showinfo("Info", "Nenhum dado disponível!")
            return

        cargos = [d[0] for d in dados]
        quantidades = [d[1] for d in dados]

        fig, ax = plt.subplots(figsize=(8, 6), facecolor="#2b2b2b")
        ax.bar(cargos, quantidades)
        ax.set_title('Distribuição de Funcionários por Cargo', color="w")
        ax.set_ylabel('Quantidade', color="w")
        ax.tick_params(axis='x', colors='w')
        ax.tick_params(axis='y', colors='w')
        plt.xticks(rotation=45)

        canvas = FigureCanvasTkAgg(fig, self.frame_grafico_rh)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True)

    def gerar_grafico_evolucao(self):
        self.limpar_grafico_rh()

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT strftime('%Y-%m', data_admissao), AVG(salario)
            FROM funcionarios
            GROUP BY strftime('%Y-%m', data_admissao)
            ORDER BY strftime('%Y-%m', data_admissao)
        ''')
        dados = cursor.fetchall()
        conn.close()

        if not dados:
            messagebox.showinfo("Info", "Nenhum dado disponível!")
            return

        meses = [d[0] for d in dados]
        salarios = [d[1] for d in dados]

        fig, ax = plt.subplots(figsize=(8, 6), facecolor="#2b2b2b")
        ax.plot(meses, salarios, marker='o')
        ax.set_title('Evolução Salarial Média', color="w")
        ax.set_ylabel('Salário Médio (R$)', color="w")
        ax.tick_params(axis='x', colors='w')
        ax.tick_params(axis='y', colors='w')
        plt.xticks(rotation=45)

        canvas = FigureCanvasTkAgg(fig, self.frame_grafico_rh)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True)

    def gerar_grafico_departamentos(self):
        self.limpar_grafico_rh()

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT departamento, COUNT(*), AVG(salario)
            FROM funcionarios WHERE ativo = 1
            GROUP BY departamento
        ''')
        dados = cursor.fetchall()
        conn.close()

        if not dados:
            messagebox.showinfo("Info", "Nenhum dado disponível!")
            return

        departamentos = [d[0] for d in dados]
        funcionarios = [d[1] for d in dados]
        salarios = [d[2] for d in dados]

        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 6), facecolor="#2b2b2b")

        ax1.bar(departamentos, funcionarios)
        ax1.set_title('Funcionários por Departamento', color="w")
        ax1.set_ylabel('Quantidade', color="w")
        ax1.tick_params(axis='x', colors='w')
        ax1.tick_params(axis='y', colors='w')

        ax2.bar(departamentos, salarios)
        ax2.set_title('Salário Médio por Departamento', color="w")
        ax2.set_ylabel('Salário Médio (R$)', color="w")
        ax2.tick_params(axis='x', colors='w')
        ax2.tick_params(axis='y', colors='w')

        plt.tight_layout()
        canvas = FigureCanvasTkAgg(fig, self.frame_grafico_rh)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True)

    def limpar_grafico_rh(self):
        for widget in self.frame_grafico_rh.winfo_children():
            widget.destroy()

    # EXPORTAÇÃO RH
    def exportar_rh_pdf(self):
        try:
            filename = f"relatorio_rh_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
            doc = SimpleDocTemplate(filename, pagesize=letter)
            styles = getSampleStyleSheet()
            story = []

            title = Paragraph("RELATÓRIO DE RECURSOS HUMANOS", styles['Title'])
            story.append(title)
            story.append(Spacer(1, 12))

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                SELECT nome, cargo, departamento, salario, data_admissao
                FROM funcionarios WHERE ativo = 1
            ''')
            funcionarios = cursor.fetchall()

            data = [['Nome', 'Cargo', 'Departamento', 'Salário', 'Admissão']]
            for func in funcionarios:
                data.append([func[0], func[1], func[2], f"R$ {func[3]:.2f}", func[4]])

            table = Table(data)
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.grey),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 12),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
                ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
                ('GRID', (0, 0), (-1, -1), 1, colors.black)
            ]))

            story.append(table)
            doc.build(story)
            conn.close()

            messagebox.showinfo("Sucesso", f"✅ PDF exportado como {filename}")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro na exportação: {str(e)}")

    def exportar_rh_excel(self):
        try:
            conn = sqlite3.connect(self.db_path)

            df_funcionarios = pd.read_sql_query('''
                SELECT nome, cargo, departamento, salario, data_admissao
                FROM funcionarios WHERE ativo = 1
            ''', conn)

            df_folha = pd.read_sql_query('''
                SELECT f.nome, f.salario as bruto, h.salario_liquido as liquido, h.mes_ano
                FROM funcionarios f
                LEFT JOIN holerites h ON f.id = h.funcionario_id
                WHERE f.ativo = 1
            ''', conn)

            filename = f"relatorio_rh_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"

            with pd.ExcelWriter(filename) as writer:
                df_funcionarios.to_excel(writer, sheet_name='Funcionários', index=False)
                df_folha.to_excel(writer, sheet_name='Folha_Pagamento', index=False)

            conn.close()
            messagebox.showinfo("Sucesso", f"✅ Excel exportado como {filename}")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro na exportação: {str(e)}")

    def imprimir_relatorios_rh(self):
        messagebox.showinfo("Impressão", "🖨️ Relatórios enviados para impressão!")

    def backup_rh(self):
        try:
            dados = {}
            tabelas_rh = ['funcionarios', 'holerites', 'entrevistas', 'verbas_rescisorias']

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            for tabela in tabelas_rh:
                cursor.execute(f"SELECT * FROM {tabela}")
                colunas = [desc[0] for desc in cursor.description]
                registros = cursor.fetchall()
                dados[tabela] = [dict(zip(colunas, registro)) for registro in registros]

            conn.close()

            filename = f"backup_rh_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(dados, f, indent=2, ensure_ascii=False)

            messagebox.showinfo("Sucesso", f"✅ Backup RH salvo como {filename}")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro no backup: {str(e)}")

    # =============================================
    # MÉTODOS ORIGINAIS
    # =============================================

    # === DASHBOARD ===
    def mostrar_dashboard(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        lbl_titulo = ctk.CTkLabel(frame, text="📊 DASHBOARD PRINCIPAL",
                                  font=('Arial', 24, 'bold'))
        lbl_titulo.pack(pady=10)

        # Métricas rápidas
        frame_metricas = ctk.CTkFrame(frame)
        frame_metricas.pack(fill="x", pady=20)

        metricas = [
            ("👥 Funcionários", str(self.contar_funcionarios())),
            ("👥 Clientes", str(self.contar_clientes())),
            ("📦 Produtos", str(self.contar_produtos())),
            ("💰 Vendas Mês", f"R$ {self.vendas_mes():.2f}"),
            ("💸 Despesas Mês", f"R$ {self.despesas_mes():.2f}"),
            ("🏭 Fornecedores", str(self.contar_fornecedores()))
        ]

        cards_container = ctk.CTkFrame(frame_metricas)
        cards_container.pack(fill="x", padx=10, pady=10)
        for titulo, valor in metricas:
            card = ctk.CTkFrame(cards_container, width=200, height=100, corner_radius=10)
            card.pack(side="left", padx=8, pady=8, expand=True, fill="both")

            lbl_valor = ctk.CTkLabel(card, text=valor, font=('Arial', 20, 'bold'))
            lbl_valor.pack(pady=(12, 2))

            lbl_titulo_card = ctk.CTkLabel(card, text=titulo, font=('Arial', 12))
            lbl_titulo_card.pack(pady=(0, 10))

        # Frame para últimas movimentações
        frame_inferior = ctk.CTkFrame(frame)
        frame_inferior.pack(fill="both", expand=True, pady=10)

        # Últimas vendas
        frame_vendas = ctk.CTkFrame(frame_inferior, corner_radius=8)
        frame_vendas.pack(side="left", fill="both", expand=True, padx=10, pady=10)

        lbl_lv = ctk.CTkLabel(frame_vendas, text="🛒 ÚLTIMAS VENDAS", font=('Arial', 12, 'bold'))
        lbl_lv.pack(pady=8)

        columns_vendas = ('Data', 'Cliente', 'Produto', 'Valor')
        tree_vendas = ttk.Treeview(frame_vendas, columns=columns_vendas, show='headings', height=8)
        for col in columns_vendas:
            tree_vendas.heading(col, text=col)
        tree_vendas.pack(fill="both", expand=True, padx=10, pady=10)

        # Últimas despesas
        frame_despesas = ctk.CTkFrame(frame_inferior, corner_radius=8)
        frame_despesas.pack(side="right", fill="both", expand=True, padx=10, pady=10)

        lbl_ld = ctk.CTkLabel(frame_despesas, text="💸 ÚLTIMAS DESPESAS", font=('Arial', 12, 'bold'))
        lbl_ld.pack(pady=8)

        columns_despesas = ('Data', 'Descrição', 'Categoria', 'Valor')
        tree_despesas = ttk.Treeview(frame_despesas, columns=columns_despesas, show='headings', height=8)
        for col in columns_despesas:
            tree_despesas.heading(col, text=col)
        tree_despesas.pack(fill="both", expand=True, padx=10, pady=10)

        # Popular dados
        self.popular_ultimas_vendas(tree_vendas)
        self.popular_ultimas_despesas(tree_despesas)

    def popular_ultimas_vendas(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT v.data_venda, c.nome, p.nome, v.valor_total
            FROM vendas v
            JOIN clientes c ON v.cliente_id = c.id
            JOIN produtos p ON v.produto_id = p.id
            ORDER BY v.data_venda DESC LIMIT 10
        ''')
        vendas = cursor.fetchall()
        conn.close()

        for venda in vendas:
            venda_list = list(venda)
            venda_list[3] = f"R$ {venda[3]:.2f}"
            tree.insert('', tk.END, values=venda_list)

    def popular_ultimas_despesas(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT data_despesa, descricao, categoria, valor
            FROM despesas
            ORDER BY data_despesa DESC LIMIT 10
        ''')
        despesas = cursor.fetchall()
        conn.close()

        for despesa in despesas:
            despesa_list = list(despesa)
            despesa_list[3] = f"R$ {despesa[3]:.2f}"
            tree.insert('', tk.END, values=despesa_list)

    # === FUNÇÕES AUXILIARES DE CONSULTA ===
    def contar_funcionarios(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM funcionarios WHERE ativo = 1")
        count = cursor.fetchone()[0]
        conn.close()
        return count

    def contar_clientes(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM clientes")
        count = cursor.fetchone()[0]
        conn.close()
        return count

    def contar_produtos(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM produtos WHERE ativo = 1")
        count = cursor.fetchone()[0]
        conn.close()
        return count

    def contar_fornecedores(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM fornecedores")
        count = cursor.fetchone()[0]
        conn.close()
        return count

    def vendas_mes(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        mes_atual = datetime.now().strftime('%m')
        ano_atual = datetime.now().strftime('%Y')
        cursor.execute(
            "SELECT SUM(valor_total) FROM vendas WHERE strftime('%m', data_venda) = ? AND strftime('%Y', data_venda) = ?",
            (mes_atual, ano_atual))
        total = cursor.fetchone()[0] or 0
        conn.close()
        return total

    def despesas_mes(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        mes_atual = datetime.now().strftime('%m')
        ano_atual = datetime.now().strftime('%Y')
        cursor.execute(
            "SELECT SUM(valor) FROM despesas WHERE strftime('%m', data_despesa) = ? AND strftime('%Y', data_despesa) = ?",
            (mes_atual, ano_atual))
        total = cursor.fetchone()[0] or 0
        conn.close()
        return total

    def obter_funcionarios(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, nome, cargo, departamento, salario, data_admissao, email FROM funcionarios WHERE ativo = 1")
        funcionarios = cursor.fetchall()
        conn.close()
        return funcionarios

    def obter_clientes(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT id, nome, email, telefone, tipo, data_cadastro FROM clientes")
        clientes = cursor.fetchall()
        conn.close()
        return clientes

    def obter_produtos(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, nome, categoria, preco, custo, estoque, estoque_minimo FROM produtos WHERE ativo = 1")
        produtos = cursor.fetchall()
        conn.close()
        return produtos

    def obter_fornecedores(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT id, nome, cnpj, telefone, email, data_cadastro FROM fornecedores")
        fornecedores = cursor.fetchall()
        conn.close()
        return fornecedores

    # === MÓDULO FUNCIONÁRIOS ===
    def mostrar_funcionarios(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="👥 GERENCIAR FUNCIONÁRIOS", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame, corner_radius=0)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Adicionar Funcionário", command=self.adicionar_funcionario,
                      height=40).pack(side="left", padx=5)
        ctk.CTkButton(frame_controles, text="📊 Relatório", command=self.gerar_relatorio_funcionarios, height=40).pack(
            side="left", padx=5)

        columns = ('ID', 'Nome', 'Cargo', 'Departamento', 'Salário', 'Admissão', 'Email')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=140)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_funcionarios(tree)

    def popular_funcionarios(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        funcionarios = self.obter_funcionarios()
        for func in funcionarios:
            func_list = list(func)
            func_list[4] = f"R$ {func_list[4]:.2f}"
            tree.insert('', tk.END, values=func_list)

    def adicionar_funcionario(self):
        dialog = AdicionarFuncionarioDialog(self.root)
        if dialog.resultado:
            self.salvar_funcionario(dialog.resultado)
            self.mostrar_funcionarios()

    def salvar_funcionario(self, dados):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO funcionarios (nome, cargo, departamento, salario, data_admissao, email, telefone)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            ''', dados)
            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", "✅ Funcionário adicionado com sucesso!")
        except sqlite3.IntegrityError:
            messagebox.showerror("Erro", "❌ Email já cadastrado!")

    def gerar_relatorio_funcionarios(self):
        funcionarios = self.obter_funcionarios()
        total_salarios = sum(func[4] for func in funcionarios) if funcionarios else 0

        relatorio = f"RELATÓRIO DE FUNCIONÁRIOS\n"
        relatorio += f"Total de Funcionários: {len(funcionarios)}\n"
        relatorio += f"Folha de Pagamento Total: R$ {total_salarios:.2f}\n\n"

        for func in funcionarios:
            relatorio += f"ID: {func[0]} | Nome: {func[1]} | Cargo: {func[2]} | Salário: R$ {func[4]:.2f}\n"

        messagebox.showinfo("Relatório de Funcionários", relatorio)

    # === MÓDULO CLIENTES ===
    def mostrar_clientes(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="👥 GERENCIAR CLIENTES", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Adicionar Cliente", command=self.adicionar_cliente, height=40).pack(
            side="left", padx=5)

        columns = ('ID', 'Nome', 'Email', 'Telefone', 'Tipo', 'Data Cadastro')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=140)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_clientes(tree)

    def popular_clientes(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        clientes = self.obter_clientes()
        for cliente in clientes:
            tree.insert('', tk.END, values=cliente)

    def adicionar_cliente(self):
        dialog = AdicionarClienteDialog(self.root)
        if dialog.resultado:
            self.salvar_cliente(dialog.resultado)
            self.mostrar_clientes()

    def salvar_cliente(self, dados):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO clientes (nome, email, telefone, endereco, data_cadastro, tipo)
                VALUES (?, ?, ?, ?, ?, ?)
            ''', dados)
            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", "✅ Cliente adicionado com sucesso!")
        except sqlite3.IntegrityError:
            messagebox.showerror("Erro", "❌ Email já cadastrado!")

    # === MÓDULO PRODUTOS ===
    def mostrar_produtos(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="📦 GERENCIAR PRODUTOS", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Adicionar Produto", command=self.adicionar_produto, height=40).pack(
            side="left", padx=5)
        ctk.CTkButton(frame_controles, text="⚠️ Produtos com Estoque Baixo", command=self.mostrar_estoque_baixo,
                      height=40).pack(side="left", padx=5)

        columns = ('ID', 'Nome', 'Categoria', 'Preço', 'Custo', 'Estoque', 'Estoque Mínimo')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=120)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_produtos(tree)

    def popular_produtos(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        produtos = self.obter_produtos()
        for produto in produtos:
            produto_list = list(produto)
            produto_list[3] = f"R$ {produto_list[3]:.2f}"
            produto_list[4] = f"R$ {produto_list[4]:.2f}"

            if produto[5] <= produto[6]:
                tree.insert('', tk.END, values=produto_list, tags=('estoque_baixo',))
            else:
                tree.insert('', tk.END, values=produto_list)

        tree.tag_configure('estoque_baixo', background='#ff5733', foreground='black')

    def adicionar_produto(self):
        dialog = AdicionarProdutoDialog(self.root)
        if dialog.resultado:
            self.salvar_produto(dialog.resultado)
            self.mostrar_produtos()

    def salvar_produto(self, dados):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO produtos (nome, categoria, preco, custo, estoque, estoque_minimo, descricao)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', dados)
        conn.commit()
        conn.close()
        messagebox.showinfo("Sucesso", "✅ Produto adicionado com sucesso!")

    def mostrar_estoque_baixo(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT nome, estoque, estoque_minimo FROM produtos
            WHERE estoque <= estoque_minimo AND ativo = 1
        ''')
        produtos = cursor.fetchall()
        conn.close()

        if not produtos:
            messagebox.showinfo("Estoque", "✅ Nenhum produto com estoque baixo!")
            return

        relatorio = "⚠️ PRODUTOS COM ESTOQUE BAIXO:\n\n"
        for produto in produtos:
            relatorio += f"📦 {produto[0]} - Estoque: {produto[1]} (Mínimo: {produto[2]})\n"

        messagebox.showwarning("Estoque Baixo", relatorio)

    # === MÓDULO FORNECEDORES ===
    def mostrar_fornecedores(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="🏭 GERENCIAR FORNECEDORES", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Adicionar Fornecedor", command=self.adicionar_fornecedor,
                      height=40).pack(side="left", padx=5)

        columns = ('ID', 'Nome', 'CNPJ', 'Telefone', 'Email', 'Data Cadastro')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=140)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_fornecedores(tree)

    def popular_fornecedores(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        fornecedores = self.obter_fornecedores()
        for fornecedor in fornecedores:
            tree.insert('', tk.END, values=fornecedor)

    def adicionar_fornecedor(self):
        dialog = AdicionarFornecedorDialog(self.root)
        if dialog.resultado:
            self.salvar_fornecedor(dialog.resultado)
            self.mostrar_fornecedores()

    def salvar_fornecedor(self, dados):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO fornecedores (nome, cnpj, telefone, email, endereco, data_cadastro)
                VALUES (?, ?, ?, ?, ?, ?)
            ''', dados)
            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", "✅ Fornecedor adicionado com sucesso!")
        except sqlite3.IntegrityError:
            messagebox.showerror("Erro", "❌ CNPJ já cadastrado!")

    # === MÓDULO VENDAS ===
    def mostrar_vendas(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="💰 GERENCIAR VENDAS", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Nova Venda", command=self.nova_venda, height=40).pack(side="left",
                                                                                                     padx=5)

        columns = ('ID', 'Data', 'Cliente', 'Produto', 'Quantidade', 'Valor Total', 'Forma Pagamento')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=130)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_vendas(tree)

    def popular_vendas(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT v.id, v.data_venda, c.nome, p.nome, v.quantidade, v.valor_total, v.forma_pagamento
            FROM vendas v
            JOIN clientes c ON v.cliente_id = c.id
            JOIN produtos p ON v.produto_id = p.id
            ORDER BY v.data_venda DESC
        ''')
        vendas = cursor.fetchall()
        conn.close()

        for venda in vendas:
            venda_list = list(venda)
            venda_list[5] = f"R$ {venda_list[5]:.2f}"
            tree.insert('', tk.END, values=venda_list)

    def nova_venda(self):
        dialog = NovaVendaDialog(self.root, self)
        if dialog.resultado:
            self.registrar_venda(dialog.resultado)
            self.mostrar_vendas()

    def registrar_venda(self, dados):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("SELECT estoque FROM produtos WHERE id = ?", (dados[1],))
        row = cursor.fetchone()
        if not row:
            messagebox.showerror("Erro", "Produto não encontrado!")
            conn.close()
            return

        estoque_atual = row[0]

        if estoque_atual < dados[2]:
            messagebox.showerror("Erro", "❌ Estoque insuficiente!")
            conn.close()
            return

        cursor.execute('''
            INSERT INTO vendas (cliente_id, produto_id, quantidade, valor_unitario, valor_total, data_venda, forma_pagamento)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', dados)

        cursor.execute('UPDATE produtos SET estoque = estoque - ? WHERE id = ?', (dados[2], dados[1]))

        conn.commit()
        conn.close()
        messagebox.showinfo("Sucesso", "✅ Venda registrada com sucesso!")

    # === MÓDULO DESPESAS ===
    def mostrar_despesas(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="💸 GERENCIAR DESPESAS", font=('Arial', 20, 'bold')).pack(pady=10)

        frame_controles = ctk.CTkFrame(frame)
        frame_controles.pack(fill="x", pady=10)

        ctk.CTkButton(frame_controles, text="➕ Nova Despesa", command=self.nova_despesa, height=40).pack(side="left",
                                                                                                         padx=5)

        columns = ('ID', 'Data', 'Descrição', 'Categoria', 'Valor', 'Forma Pagamento')
        tree = ttk.Treeview(frame, columns=columns, show='headings', height=20)

        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=130)

        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscroll=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.popular_despesas(tree)

    def popular_despesas(self, tree):
        for item in tree.get_children():
            tree.delete(item)

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, data_despesa, descricao, categoria, valor, forma_pagamento
            FROM despesas
            ORDER BY data_despesa DESC
        ''')
        despesas = cursor.fetchall()
        conn.close()

        for despesa in despesas:
            despesa_list = list(despesa)
            despesa_list[4] = f"R$ {despesa_list[4]:.2f}"
            tree.insert('', tk.END, values=despesa_list)

    def nova_despesa(self):
        dialog = NovaDespesaDialog(self.root)
        if dialog.resultado:
            self.registrar_despesa(dialog.resultado)
            self.mostrar_despesas()

    def registrar_despesa(self, dados):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO despesas (descricao, categoria, valor, data_despesa, forma_pagamento)
            VALUES (?, ?, ?, ?, ?)
        ''', dados)
        conn.commit()
        conn.close()
        messagebox.showinfo("Sucesso", "✅ Despesa registrada com sucesso!")

    # =======================================================
    # === MÓDULO RELATÓRIOS ===
    # =======================================================

    def mostrar_relatorios(self):
        """Cria a nova interface de relatórios com abas, gráficos e tabelas."""
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="📈 RELATÓRIOS E ESTATÍSTICAS", font=('Arial', 20, 'bold')).pack(pady=10)

        tabview = ctk.CTkTabview(frame, width=1100, height=600)
        tabview.pack(pady=10, padx=10, fill="both", expand=True)

        tabview.add("Financeiro")
        tabview.add("Vendas")
        tabview.add("Estoque")
        tabview.add("Despesas")
        tabview.add("Funcionários")

        self.criar_aba_financeiro(tabview.tab("Financeiro"))
        self.criar_aba_vendas(tabview.tab("Vendas"))
        self.criar_aba_estoque(tabview.tab("Estoque"))
        self.criar_aba_despesas(tabview.tab("Despesas"))
        self.criar_aba_funcionarios(tabview.tab("Funcionários"))

    def criar_aba_financeiro(self, tab):
        """Cria o conteúdo da aba de relatórios financeiros."""
        # Frame para cards de resumo
        frame_cards = ctk.CTkFrame(tab, fg_color="transparent")
        frame_cards.pack(fill="x", padx=10, pady=10)

        vendas = self.vendas_mes()
        despesas = self.despesas_mes()
        lucro = vendas - despesas

        metricas = [
            ("💰 Vendas no Mês", f"R$ {vendas:.2f}", "#27ae60"),
            ("💸 Despesas no Mês", f"R$ {despesas:.2f}", "#c0392b"),
            ("💵 Lucro / Prejuízo", f"R$ {lucro:.2f}", "#2980b9")
        ]

        for i, (titulo, valor, cor) in enumerate(metricas):
            card = ctk.CTkFrame(frame_cards, corner_radius=8, border_color=cor, border_width=2)
            card.grid(row=0, column=i, padx=10, pady=10, sticky="nsew")
            frame_cards.grid_columnconfigure(i, weight=1)

            lbl_valor = ctk.CTkLabel(card, text=valor, font=('Arial', 24, 'bold'))
            lbl_valor.pack(pady=(15, 5), padx=20)
            lbl_titulo = ctk.CTkLabel(card, text=titulo, font=('Arial', 14))
            lbl_titulo.pack(pady=(0, 15), padx=20)

        # Frame para o gráfico
        frame_grafico = ctk.CTkFrame(tab, corner_radius=8)
        frame_grafico.pack(fill="both", expand=True, padx=10, pady=10)

        # Gráfico de barras
        fig, ax = plt.subplots(facecolor="#333333")
        bars = ax.bar(['Vendas', 'Despesas', 'Lucro'], [vendas, despesas, lucro],
                      color=['#27ae60', '#c0392b', '#2980b9'])
        ax.set_facecolor("#2b2b2b")
        ax.set_title(f"Resumo Financeiro de {datetime.now().strftime('%B/%Y')}", color="white", fontsize=16)
        ax.set_ylabel("Valor (R$)", color="white")
        ax.tick_params(axis='y', labelcolor='white')
        ax.tick_params(axis='x', labelcolor='white', labelsize=12)
        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)

        for bar in bars:
            yval = bar.get_height()
            ax.text(bar.get_x() + bar.get_width() / 2.0, yval, f'R$ {yval:.2f}', va='bottom', ha='center',
                    color="white")

        canvas = FigureCanvasTkAgg(fig, master=frame_grafico)
        canvas.draw()
        canvas.get_tk_widget().pack(fill="both", expand=True, padx=5, pady=5)

    def criar_aba_vendas(self, tab):
        """Cria o conteúdo da aba de relatórios de vendas."""
        # Conectar ao DB
        conn = sqlite3.connect(self.db_path)
        mes_atual = datetime.now().strftime('%m')
        ano_atual = datetime.now().strftime('%Y')
        df_vendas = pd.read_sql_query(f"""
            SELECT p.categoria, SUM(v.quantidade) as Unidades, SUM(v.valor_total) as Total
            FROM vendas v
            JOIN produtos p ON v.produto_id = p.id
            WHERE strftime('%m', v.data_venda) = '{mes_atual}' AND strftime('%Y', v.data_venda) = '{ano_atual}'
            GROUP BY p.categoria
            ORDER BY Total DESC
        """, conn)
        conn.close()

        # Frame principal
        frame_principal = ctk.CTkFrame(tab, fg_color="transparent")
        frame_principal.pack(fill="both", expand=True, padx=10, pady=10)
        frame_principal.grid_columnconfigure(0, weight=1)
        frame_principal.grid_columnconfigure(1, weight=1)
        frame_principal.grid_rowconfigure(0, weight=1)

        # Frame do gráfico
        frame_grafico = ctk.CTkFrame(frame_principal, corner_radius=8)
        frame_grafico.grid(row=0, column=0, sticky="nsew", padx=5)

        # Frame da tabela
        frame_tabela = ctk.CTkFrame(frame_principal, corner_radius=8)
        frame_tabela.grid(row=0, column=1, sticky="nsew", padx=5)

        # Tabela
        ctk.CTkLabel(frame_tabela, text="Vendas por Categoria (Mês Atual)", font=('Arial', 14, 'bold')).pack(pady=10)
        cols = ('Categoria', 'Unidades', 'Valor Total')
        tree = ttk.Treeview(frame_tabela, columns=cols, show='headings')
        for col in cols:
            tree.heading(col, text=col)
            tree.column(col, anchor="center")
        tree.pack(fill="both", expand=True, padx=10, pady=10)

        for index, row in df_vendas.iterrows():
            tree.insert("", "end", values=(row['categoria'], row['Unidades'], f"R$ {row['Total']:.2f}"))

        # Gráfico
        if not df_vendas.empty:
            fig, ax = plt.subplots(facecolor="#333333")
            ax.pie(df_vendas['Total'], labels=df_vendas['categoria'], autopct='%1.1f%%', startangle=140,
                   textprops={'color': "white"})
            ax.set_title("Distribuição de Vendas por Categoria", color="white", fontsize=16)
            canvas = FigureCanvasTkAgg(fig, master=frame_grafico)
            canvas.draw()
            canvas.get_tk_widget().pack(fill="both", expand=True, padx=5, pady=5)
        else:
            ctk.CTkLabel(frame_grafico, text="Sem dados de vendas para exibir.", font=('Arial', 16)).pack(expand=True)

    def criar_aba_estoque(self, tab):
        """Cria o conteúdo da aba de relatórios de estoque."""
        conn = sqlite3.connect(self.db_path)
        df_estoque = pd.read_sql_query("""
            SELECT nome, estoque, estoque_minimo, preco, (estoque * preco) as ValorTotal
            FROM produtos WHERE ativo = 1
            ORDER BY ValorTotal DESC
        """, conn)
        conn.close()

        # Cards
        frame_cards = ctk.CTkFrame(tab, fg_color="transparent")
        frame_cards.pack(fill="x", padx=10, pady=10)
        total_itens = df_estoque['estoque'].sum()
        valor_total_estoque = df_estoque['ValorTotal'].sum()
        ctk.CTkLabel(frame_cards, text=f"Itens em Estoque: {total_itens}", font=('Arial', 16, 'bold')).pack(side="left",
                                                                                                            padx=20)
        ctk.CTkLabel(frame_cards, text=f"Valor Total do Estoque: R$ {valor_total_estoque:.2f}",
                     font=('Arial', 16, 'bold')).pack(side="right", padx=20)

        # Tabela
        frame_tabela = ctk.CTkFrame(tab, corner_radius=8)
        frame_tabela.pack(fill="both", expand=True, padx=10, pady=10)
        cols = ('Produto', 'Estoque Atual', 'Estoque Mínimo', 'Valor Unitário', 'Valor Total')
        tree = ttk.Treeview(frame_tabela, columns=cols, show='headings')
        for col in cols:
            tree.heading(col, text=col)
            tree.column(col, anchor="center")
        tree.pack(fill="both", expand=True, padx=10, pady=10)
        tree.tag_configure('baixo', background='#c0392b', foreground='white')

        for index, row in df_estoque.iterrows():
            tag = 'baixo' if row['estoque'] <= row['estoque_minimo'] else ''
            valores = (
                row['nome'], row['estoque'], row['estoque_minimo'], f"R$ {row['preco']:.2f}",
                f"R$ {row['ValorTotal']:.2f}")
            tree.insert("", "end", values=valores, tags=(tag,))

    def criar_aba_despesas(self, tab):
        """Cria o conteúdo da aba de relatórios de despesas."""
        conn = sqlite3.connect(self.db_path)
        mes_atual = datetime.now().strftime('%m')
        ano_atual = datetime.now().strftime('%Y')
        df_despesas = pd.read_sql_query(f"""
            SELECT categoria, SUM(valor) as Total
            FROM despesas
            WHERE strftime('%m', data_despesa) = '{mes_atual}' AND strftime('%Y', data_despesa) = '{ano_atual}'
            GROUP BY categoria
            ORDER BY Total DESC
        """, conn)
        conn.close()

        frame_principal = ctk.CTkFrame(tab, fg_color="transparent")
        frame_principal.pack(fill="both", expand=True, padx=10, pady=10)
        frame_principal.grid_columnconfigure(0, weight=1)
        frame_principal.grid_columnconfigure(1, weight=1)
        frame_principal.grid_rowconfigure(0, weight=1)

        frame_grafico = ctk.CTkFrame(frame_principal, corner_radius=8)
        frame_grafico.grid(row=0, column=0, sticky="nsew", padx=5)
        frame_tabela = ctk.CTkFrame(frame_principal, corner_radius=8)
        frame_tabela.grid(row=0, column=1, sticky="nsew", padx=5)

        # Tabela de Despesas
        ctk.CTkLabel(frame_tabela, text="Despesas por Categoria (Mês Atual)", font=('Arial', 14, 'bold')).pack(pady=10)
        cols = ('Categoria', 'Valor Total')
        tree = ttk.Treeview(frame_tabela, columns=cols, show='headings')
        tree.heading('Categoria', text='Categoria')
        tree.heading('Valor Total', text='Valor Total')
        tree.column('Categoria', anchor="center")
        tree.column('Valor Total', anchor="center")
        tree.pack(fill="both", expand=True, padx=10, pady=10)
        for index, row in df_despesas.iterrows():
            tree.insert("", "end", values=(row['categoria'], f"R$ {row['Total']:.2f}"))

        if not df_despesas.empty:
            fig, ax = plt.subplots(facecolor="#333333")
            ax.pie(df_despesas['Total'], labels=df_despesas['categoria'], autopct='%1.1f%%', startangle=140,
                   textprops={'color': "white"})
            ax.set_title("Distribuição de Despesas por Categoria", color="white", fontsize=16)
            canvas = FigureCanvasTkAgg(fig, master=frame_grafico)
            canvas.draw()
            canvas.get_tk_widget().pack(fill="both", expand=True, padx=5, pady=5)
        else:
            ctk.CTkLabel(frame_grafico, text="Sem dados de despesas para exibir.", font=('Arial', 16)).pack(expand=True)

    def criar_aba_funcionarios(self, tab):
        """Cria o conteúdo da aba de relatórios de funcionários."""
        funcionarios = self.obter_funcionarios()
        total_funcionarios = len(funcionarios)
        folha_pagamento = sum(f[4] for f in funcionarios)

        frame_cards = ctk.CTkFrame(tab, fg_color="transparent")
        frame_cards.pack(fill="x", padx=10, pady=10)
        ctk.CTkLabel(frame_cards, text=f"Total de Funcionários Ativos: {total_funcionarios}",
                     font=('Arial', 16, 'bold')).pack(side="left", padx=20)
        ctk.CTkLabel(frame_cards, text=f"Folha de Pagamento Mensal: R$ {folha_pagamento:.2f}",
                     font=('Arial', 16, 'bold')).pack(side="right", padx=20)

        frame_tabela = ctk.CTkFrame(tab, corner_radius=8)
        frame_tabela.pack(fill="both", expand=True, padx=10, pady=10)

        cols = ('ID', 'Nome', 'Cargo', 'Departamento', 'Salário')
        tree = ttk.Treeview(frame_tabela, columns=cols, show='headings')
        for col in cols:
            tree.heading(col, text=col)
        tree.pack(fill="both", expand=True, padx=10, pady=10)

        for func in funcionarios:
            valores = (func[0], func[1], func[2], func[3], f"R$ {func[4]:.2f}")
            tree.insert("", "end", values=valores)

    # === MÓDULO BACKUP ===
    def mostrar_backup(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="💾 BACKUP E EXPORTAÇÃO", font=('Arial', 20, 'bold')).pack(pady=10)

        opcoes = [
            ("💾 Backup Completo (JSON)", self.backup_completo_json),
            ("📊 Exportar para CSV", self.exportar_csv),
            ("🔄 Restaurar Backup", self.restaurar_backup),
            ("📋 Log do Sistema", self.mostrar_log)
        ]

        for i, (texto, comando) in enumerate(opcoes):
            btn = ctk.CTkButton(frame, text=texto, command=comando, width=300, height=50)
            btn.grid(row=i // 2, column=i % 2, padx=10, pady=10, sticky="nsew")
            frame.grid_columnconfigure(i % 2, weight=1)

    def backup_completo_json(self):
        try:
            dados = {}
            tabelas = ['funcionarios', 'clientes', 'produtos', 'vendas', 'despesas', 'fornecedores']

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            for tabela in tabelas:
                cursor.execute(f"SELECT * FROM {tabela}")
                colunas = [desc[0] for desc in cursor.description]
                registros = cursor.fetchall()
                dados[tabela] = [dict(zip(colunas, registro)) for registro in registros]

            conn.close()

            filename = f"backup_empresa_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(dados, f, indent=2, ensure_ascii=False)

            messagebox.showinfo("Sucesso", f"✅ Backup salvo como {filename}")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro no backup: {str(e)}")

    def exportar_csv(self):
        try:
            tabela = simpledialog.askstring("Exportar CSV",
                                            "Digite o nome da tabela (funcionarios, clientes, produtos, etc.):")
            if not tabela:
                return

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute(f"SELECT * FROM {tabela}")

            colunas = [desc[0] for desc in cursor.description]
            registros = cursor.fetchall()
            conn.close()

            filename = f"export_{tabela}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
            with open(filename, 'w', newline='', encoding='utf-8') as f:
                writer = csv.writer(f)
                writer.writerow(colunas)
                writer.writerows(registros)

            messagebox.showinfo("Sucesso", f"✅ Dados exportados como {filename}")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro na exportação: {str(e)}")

    def restaurar_backup(self):
        filepath = filedialog.askopenfilename(
            filetypes=[("JSON files", "*.json")]
        )
        if not filepath:
            return

        if not messagebox.askyesno("Confirmação", "⚠️ Isso substituirá todos os dados atuais! Continuar?"):
            return

        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                dados = json.load(f)

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            for tabela, registros in dados.items():
                if registros:
                    # Limpa a tabela
                    cursor.execute(f"DELETE FROM {tabela}")

                    # Insere os registros
                    colunas = list(registros[0].keys())
                    placeholders = ','.join(['?' for _ in colunas])

                    for registro in registros:
                        valores = [registro[col] for col in colunas]
                        cursor.execute(f"INSERT INTO {tabela} ({','.join(colunas)}) VALUES ({placeholders})", valores)

            conn.commit()
            conn.close()
            messagebox.showinfo("Sucesso", "✅ Backup restaurado com sucesso!")
        except Exception as e:
            messagebox.showerror("Erro", f"❌ Erro na restauração: {str(e)}")

    def mostrar_log(self):
        log = f"📋 LOG DO SISTEMA - {datetime.now().strftime('%d/%m/%Y %H:%M')}\n\n"
        log += f"👥 Funcionários: {self.contar_funcionarios()}\n"
        log += f"👥 Clientes: {self.contar_clientes()}\n"
        log += f"📦 Produtos: {self.contar_produtos()}\n"
        log += f"💰 Vendas do Mês: R$ {self.vendas_mes():.2f}\n"
        log += f"💸 Despesas do Mês: R$ {self.despesas_mes():.2f}\n"
        log += f"🏭 Fornecedores: {self.contar_fornecedores()}\n"

        messagebox.showinfo("Log do Sistema", log)

    # === MÓDULO CONFIGURAÇÕES ===
    def mostrar_configuracoes(self):
        self.limpar_conteudo()

        frame = ctk.CTkFrame(self.frame_conteudo, corner_radius=8)
        frame.pack(fill="both", expand=True, padx=20, pady=20)

        ctk.CTkLabel(frame, text="⚙️ CONFIGURAÇÕES DO SISTEMA", font=('Arial', 20, 'bold')).pack(pady=10)

        opcoes = [
            ("🔄 Reiniciar Banco de Dados", self.reiniciar_banco),
            ("🗑️ Limpar Dados", self.limpar_dados),
            ("ℹ️ Sobre o Sistema", self.mostrar_sobre)
        ]

        for texto, comando in opcoes:
            btn = ctk.CTkButton(frame, text=texto, command=comando, width=300, height=50)
            btn.pack(pady=10)

    def reiniciar_banco(self):
        if messagebox.askyesno("Confirmação", "⚠️ Isso apagará todos os dados! Continuar?"):
            try:
                if os.path.exists(self.db_path):
                    os.remove(self.db_path)
                self.inicializar_banco_dados()
                messagebox.showinfo("Sucesso", "✅ Banco de dados reiniciado com sucesso!")
            except Exception as e:
                messagebox.showerror("Erro", f"❌ Erro: {str(e)}")

    def limpar_dados(self):
        if messagebox.askyesno("Confirmação", "⚠️ Isso apagará todos os registros! Continuar?"):
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()
                tabelas = ['vendas', 'despesas', 'funcionarios', 'clientes', 'produtos', 'fornecedores',
                           'holerites', 'entrevistas', 'verbas_rescisorias', 'contas_pagar_receber',
                           'fluxo_caixa', 'extratos_bancarios', 'centros_custo']

                for tabela in tabelas:
                    cursor.execute(f"DELETE FROM {tabela}")

                conn.commit()
                conn.close()
                messagebox.showinfo("Sucesso", "✅ Dados limpos com sucesso!")
            except Exception as e:
                messagebox.showerror("Erro", f"❌ Erro: {str(e)}")

    def mostrar_sobre(self):
        sobre = """
        🏢 SISTEMA DE GESTÃO EMPRESARIAL

        By Anthony Cavalcante Da Silva

        📊 Funcionalidades:
        - Gestão de Funcionários e RH
        - Gestão de Clientes e Fornecedores
        - Controle de Produtos e Estoque
        - Registro de Vendas e Despesas
        - Relatórios Visuais e Financeiros
        - Backup de Dados
        - Módulo Financeiro completo
        - NotesPlano e TablesPlano

        💡 Desenvolvido para auxiliar na administração empresarial
        """
        messagebox.showinfo("Sobre o Sistema", sobre)


# =============================================
# EXECUÇÃO PRINCIPAL
# =============================================

def main():
    try:
        root = ctk.CTk()
        app = SistemaEmpresarial(root)
        root.mainloop()
    except Exception as e:
        print("Erro ao executar o sistema:", e)
        traceback.print_exc()


if __name__ == "__main__":
    main()