import tkinter as tk
from tkinter import messagebox, ttk
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2Tk
import numpy as np

# Configuração global de estilo para os gráficos (tema escuro profissional)
plt.style.use('seaborn-v0_8-darkgrid')
COR_PRIMARIA = '#00d4ff'
COR_FUNDO_GRAF = '#2b2b2b'
COR_TEXTO_GRAF = '#f0f0f0'

class SimuladorFisicaPro:
    def __init__(self, root):
        self.root = root
        self.root.title("Physic Simulator")
        self.root.geometry("500x650")
        self.root.configure(bg='#1e1e1e')
        self.root.resizable(False, False)

        # Configura estilo das fonts
        self.fonte_titulo = ('Segoe UI', 20, 'bold')
        self.fonte_botao = ('Segoe UI', 11)
        self.fonte_label = ('Segoe UI', 10)

        self.criar_menu_principal()

    def limpar_tela(self):
        for widget in self.root.winfo_children():
            widget.destroy()

    def criar_tooltip(self, widget, texto):
        """Cria uma tooltip simples para um widget."""
        tooltip = tk.Toplevel(widget, bg='#333', padx=5, pady=2)
        tooltip.wm_overrideredirect(True)
        tooltip.wm_geometry("+0+0")
        label = tk.Label(tooltip, text=texto, bg='#333', fg='white', font=('Segoe UI', 9))
        label.pack()
        tooltip.withdraw()

        def enter(event):
            x, y, _, _ = widget.bbox("insert")
            x += widget.winfo_rootx() + 25
            y += widget.winfo_rooty() + 25
            tooltip.wm_geometry(f"+{x}+{y}")
            tooltip.deiconify()

        def leave(event):
            tooltip.withdraw()

        widget.bind('<Enter>', enter)
        widget.bind('<Leave>', leave)

    def criar_menu_principal(self):
        self.limpar_tela()

        # Cabeçalho
        tk.Label(self.root, text="⚛️ Physic", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(40, 10))
        tk.Label(self.root, text="Simulador de Física Interativo", font=self.fonte_label,
                 bg='#1e1e1e', fg='#aaa').pack(pady=(0, 30))

        # Frame para os botões
        frame_botoes = tk.Frame(self.root, bg='#1e1e1e')
        frame_botoes.pack(expand=True)

        botoes = [
            ("⚡ Lei de Ohm", self.gui_voltagem),
            ("📈 Movimento Uniforme", self.gui_tempo),
            ("🌊 Frequência & Ondas", self.gui_frequencia),
            ("⚖️ Força Gravitacional", self.gui_gravidade),
            ("🔋 Lei de Coulomb", self.gui_eletrostatica),
            ("🧲 Força Resultante", self.gui_resultante),
            ("❌ Sair", self.root.quit)
        ]

        for texto, comando in botoes:
            btn = tk.Button(frame_botoes, text=texto, command=comando,
                            font=self.fonte_botao, width=25, height=1,
                            bg='#333', fg='white', activebackground=COR_PRIMARIA,
                            activeforeground='black', relief='flat', bd=0, pady=8)
            btn.pack(pady=6)
            self.criar_tooltip(btn, f"Abrir simulador: {texto}")

        # Rodapé
        tk.Label(self.root, text="v2.0 • Interface aprimorada", font=('Segoe UI', 8),
                 bg='#1e1e1e', fg='#666').pack(side='bottom', pady=10)

    def preparar_janela_grafico(self, titulo_janela):
        """Cria uma toplevel com fundo escuro e figura matplotlib estilizada."""
        janela = tk.Toplevel(self.root)
        janela.title(titulo_janela)
        janela.geometry("750x600")
        janela.configure(bg='#1e1e1e')
        janela.resizable(True, True)

        # Figura com fundo escuro
        fig, ax = plt.subplots(figsize=(8, 5), dpi=100, facecolor='#2b2b2b')
        ax.set_facecolor('#2b2b2b')
        ax.tick_params(colors='white')
        ax.xaxis.label.set_color('white')
        ax.yaxis.label.set_color('white')
        ax.title.set_color('white')
        for spine in ax.spines.values():
            spine.set_color('#555')

        return janela, fig, ax

    def renderizar_grafico(self, fig, janela):
        """Exibe o gráfico na janela e adiciona barra de ferramentas."""
        canvas = FigureCanvasTkAgg(fig, master=janela)
        canvas.draw()

        # Barra de ferramentas do matplotlib (zoom, pan, salvar)
        toolbar = NavigationToolbar2Tk(canvas, janela)
        toolbar.update()
        toolbar.config(bg='#1e1e1e')
        # Estiliza os botões da toolbar, ignorando erros
        for child in toolbar.winfo_children():
            try:
                child.config(bg='#333', foreground='white', activebackground=COR_PRIMARIA)
            except tk.TclError:
                pass  # widget não suporta essas opções

        canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

    # ---------- MÉTODOS AUXILIARES ----------
    def criar_input(self, texto, tooltip=""):
        """Cria um campo de entrada estilizado com label."""
        frame = tk.Frame(self.root, bg='#1e1e1e')
        frame.pack(pady=5)

        label = tk.Label(frame, text=texto, font=self.fonte_label,
                         bg='#1e1e1e', fg='#ccc')
        label.pack(anchor='w')

        entrada = tk.Entry(frame, font=('Segoe UI', 11), justify='center',
                           bg='#2b2b2b', fg='white', insertbackground='white',
                           relief='flat', bd=0, highlightthickness=1,
                           highlightcolor=COR_PRIMARIA, highlightbackground='#555')
        entrada.pack(ipadx=5, ipady=5, fill='x')

        if tooltip:
            self.criar_tooltip(entrada, tooltip)

        return entrada

    # ---------- FUNÇÕES DE CADA SIMULADOR ----------
    def gui_voltagem(self):
        self.limpar_tela()
        tk.Label(self.root, text="⚡ Lei de Ohm", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="V = R · I", font=('Segoe UI', 14, 'italic'),
                 bg='#1e1e1e', fg='#aaa').pack()

        ent_r = self.criar_input("Resistência (Ω):", "Valor da resistência elétrica")
        ent_i = self.criar_input("Corrente (A):", "Corrente que passa pelo circuito")

        def calcular():
            try:
                r = float(ent_r.get())
                i = float(ent_i.get())
                v = r * i

                janela, fig, ax = self.preparar_janela_grafico("Lei de Ohm")
                correntes = np.linspace(0, i * 1.8, 100)
                voltagens = r * correntes

                ax.plot(correntes, voltagens, color=COR_PRIMARIA, lw=3, label=f'R = {r} Ω')
                ax.scatter([i], [v], color='#e74c3c', s=80, zorder=5, edgecolor='white')
                ax.annotate(f'Ponto de operação\n({i:.2f} A, {v:.2f} V)',
                            xy=(i, v), xytext=(i*0.3, v*0.7),
                            arrowprops=dict(arrowstyle='->', color='white', lw=1.5),
                            color='white', bbox=dict(boxstyle='round,pad=0.3', facecolor='#333'))

                ax.set_title("Característica Tensão x Corrente", fontsize=14)
                ax.set_xlabel("Corrente (A)")
                ax.set_ylabel("Tensão (V)")
                ax.grid(True, linestyle='--', alpha=0.5, color='#777')
                ax.legend()
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Valores inválidos. Use números separados por ponto.")

        tk.Button(self.root, text="▶ Calcular e Plotar", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()

    def gui_tempo(self):
        self.limpar_tela()
        tk.Label(self.root, text="📈 Movimento Uniforme", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="S = S₀ + v · t", font=('Segoe UI', 14, 'italic'),
                 bg='#1e1e1e', fg='#aaa').pack()

        ent_s0 = self.criar_input("Posição Inicial (m):", "Metros")
        ent_sf = self.criar_input("Posição Final (m):", "Metros")
        ent_v = self.criar_input("Velocidade (m/s):", "Constante")

        def calcular():
            try:
                s0 = float(ent_s0.get())
                sf = float(ent_sf.get())
                v = float(ent_v.get())
                if v == 0:
                    messagebox.showerror("Erro", "Velocidade não pode ser zero.")
                    return
                t_final = (sf - s0) / v
                if t_final < 0:
                    messagebox.showerror("Erro", "Posição final deve ser maior que inicial para v>0 (ou vice-versa).")
                    return

                janela, fig, ax = self.preparar_janela_grafico("Movimento Uniforme")
                tempos = np.linspace(0, t_final, 100)
                posicoes = s0 + v * tempos

                ax.plot(tempos, posicoes, color='#2ecc71', lw=3, label='Trajetória')
                ax.fill_between(tempos, s0, posicoes, alpha=0.2, color='#2ecc71')
                ax.scatter([0, t_final], [s0, sf], color='white', s=60, edgecolor='#2ecc71', zorder=5)
                ax.annotate(f'Início\n({0:.1f}s, {s0:.1f}m)', xy=(0, s0), xytext=(-0.05*t_final, s0+0.1*abs(sf-s0)),
                            color='white', bbox=dict(boxstyle='round', facecolor='#333'))
                ax.annotate(f'Fim\n({t_final:.1f}s, {sf:.1f}m)', xy=(t_final, sf), xytext=(t_final*0.8, sf-0.2*abs(sf-s0)),
                            color='white', bbox=dict(boxstyle='round', facecolor='#333'))

                ax.set_title(f"Deslocamento progressivo (ΔS = {sf-s0:.1f} m)", fontsize=14)
                ax.set_xlabel("Tempo (s)")
                ax.set_ylabel("Posição (m)")
                ax.grid(True, linestyle='--', alpha=0.5, color='#777')
                ax.legend()
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Valores inválidos.")

        tk.Button(self.root, text="▶ Simular", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()

    def gui_frequencia(self):
        self.limpar_tela()
        tk.Label(self.root, text="🌊 Oscilações e Ondas", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="f = 1 / T", font=('Segoe UI', 14, 'italic'),
                 bg='#1e1e1e', fg='#aaa').pack()

        ent_t = self.criar_input("Período (s):", "Tempo para um ciclo completo")

        def calcular():
            try:
                t = float(ent_t.get())
                if t <= 0:
                    messagebox.showerror("Erro", "Período deve ser positivo.")
                    return
                f = 1 / t

                janela, fig, ax = self.preparar_janela_grafico("Onda Senoidal")
                x = np.linspace(0, 3 * t, 1000)
                y = np.sin(2 * np.pi * f * x)

                ax.plot(x, y, color='#9b59b6', lw=2.5)
                ax.axvline(x=t, color='#e67e22', linestyle='--', linewidth=2, label=f'T = {t:.3f} s')
                ax.axvline(x=2*t, color='#e67e22', linestyle='--', linewidth=2)
                ax.axhline(y=0, color='white', linewidth=1, alpha=0.5)

                ax.set_title(f"Onda senoidal • Frequência: {f:.3f} Hz", fontsize=14)
                ax.set_xlabel("Tempo (s)")
                ax.set_ylabel("Amplitude (u.a.)")
                ax.set_ylim(-1.5, 1.5)
                ax.grid(True, alpha=0.3, color='#777')
                ax.legend()
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Período inválido.")

        tk.Button(self.root, text="▶ Gerar Onda", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()

    def gui_gravidade(self):
        self.limpar_tela()
        tk.Label(self.root, text="⚖️ Força Peso", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="F = m · g   (g = 9.806 m/s²)", font=('Segoe UI', 12, 'italic'),
                 bg='#1e1e1e', fg='#aaa').pack()

        ent_m = self.criar_input("Massa (kg):", "Valor da massa do corpo")

        def calcular():
            try:
                m = float(ent_m.get())
                g = 9.806
                peso = m * g

                janela, fig, ax = self.preparar_janela_grafico("Força Gravitacional")
                massas = np.linspace(0, m * 2, 50)
                pesos = massas * g

                ax.plot(massas, pesos, color='#27ae60', lw=3, label=f'g = {g} m/s²')
                ax.scatter([m], [peso], color='red', s=80, edgecolor='white', zorder=5)
                ax.annotate(f'({m} kg, {peso:.2f} N)', xy=(m, peso), xytext=(m*1.2, peso*0.8),
                            arrowprops=dict(arrowstyle='->', color='white'), color='white')

                ax.set_title("Relação Peso x Massa", fontsize=14)
                ax.set_xlabel("Massa (kg)")
                ax.set_ylabel("Peso (N)")
                ax.grid(True, linestyle='--', alpha=0.5, color='#777')
                ax.legend()
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Massa inválida.")

        tk.Button(self.root, text="▶ Calcular Peso", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()

    def gui_eletrostatica(self):
        self.limpar_tela()
        tk.Label(self.root, text="🔋 Lei de Coulomb", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="F = k·|q1·q2| / r²", font=('Segoe UI', 12, 'italic'),
                 bg='#1e1e1e', fg='#aaa').pack()

        ent_q1 = self.criar_input("Carga q1 (C):", "Valor em Coulombs (pode ser negativo)")
        ent_q2 = self.criar_input("Carga q2 (C):", "Valor em Coulombs")
        ent_d = self.criar_input("Distância inicial (m):", "Separação entre as cargas")

        def calcular():
            try:
                q1 = float(ent_q1.get())
                q2 = float(ent_q2.get())
                d = float(ent_d.get())
                if d <= 0:
                    messagebox.showerror("Erro", "Distância deve ser positiva.")
                    return
                k = 9e9
                f_inicial = k * abs(q1 * q2) / (d ** 2)

                janela, fig, ax = self.preparar_janela_grafico("Lei de Coulomb")
                distancias = np.linspace(d * 0.2, d * 5, 200)
                forcas = k * abs(q1 * q2) / (distancias ** 2)

                ax.plot(distancias, forcas, color='#f39c12', lw=3)
                ax.scatter([d], [f_inicial], color='red', s=80, edgecolor='white', label='Ponto inicial')
                ax.set_yscale('log')
                ax.set_xscale('log')  # Melhor visualização da lei inversa do quadrado
                ax.set_title("Força eletrostática vs Distância (escala log-log)", fontsize=14)
                ax.set_xlabel("Distância (m)")
                ax.set_ylabel("Força (N)")
                ax.grid(True, which='both', linestyle='--', alpha=0.5, color='#777')
                ax.legend()
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Valores inválidos.")

        tk.Button(self.root, text="▶ Simular Campo", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()

    def gui_resultante(self):
        self.limpar_tela()
        tk.Label(self.root, text="🧲 Força Resultante", font=self.fonte_titulo,
                 bg='#1e1e1e', fg=COR_PRIMARIA).pack(pady=(30, 10))
        tk.Label(self.root, text="Soma vetorial de até 3 forças", font=('Segoe UI', 12),
                 bg='#1e1e1e', fg='#aaa').pack()

        entradas = []
        for i in range(3):
            ent = self.criar_input(f"Força F{i+1} (N):", "Componente (positiva ou negativa)")
            entradas.append(ent)

        def calcular():
            try:
                f_vals = [float(e.get()) for e in entradas]
                res = sum(f_vals)

                janela, fig, ax = self.preparar_janela_grafico("Resultante de Forças")
                cores = ['#3498db', '#95a5a6', '#95a5a6', '#e74c3c']
                labels = ['F1', 'F2', 'F3', 'Resultante']
                valores = f_vals + [res]

                barras = ax.bar(labels, valores, color=cores, edgecolor='white', linewidth=1)
                ax.axhline(0, color='white', linewidth=1)
                ax.set_title(f"Soma vetorial: {res:.2f} N", fontsize=14)
                ax.set_ylabel("Força (N)")

                # Adicionar valores nas barras
                for bar in barras:
                    altura = bar.get_height()
                    ax.annotate(f'{altura:.1f}',
                                xy=(bar.get_x() + bar.get_width()/2, altura),
                                xytext=(0, 5 if altura >= 0 else -15),
                                textcoords="offset points",
                                ha='center', va='bottom' if altura >= 0 else 'top',
                                color='white', fontweight='bold')

                ax.grid(True, axis='y', linestyle='--', alpha=0.5, color='#777')
                self.renderizar_grafico(fig, janela)
            except ValueError:
                messagebox.showerror("Erro", "Valores inválidos. Use números.")

        tk.Button(self.root, text="▶ Calcular Resultante", command=calcular,
                  bg=COR_PRIMARIA, fg='black', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack(pady=20)
        tk.Button(self.root, text="🔙 Voltar", command=self.criar_menu_principal,
                  bg='#333', fg='white', font=self.fonte_botao,
                  relief='flat', padx=20, pady=8).pack()


if __name__ == "__main__":
    root = tk.Tk()
    app = SimuladorFisicaPro(root)
    root.mainloop()