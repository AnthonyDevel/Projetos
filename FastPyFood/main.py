"""
Sabor Express – Painel de Gestão Profissional
Dashboard inteligente, análises em tempo real e interface premium.
"""

import customtkinter as ctk
from customtkinter import CTkTabview
import tkinter.messagebox as tkmb
import tkinter as tk
from tkinter import filedialog
from datetime import datetime, timedelta
from typing import Dict, Optional
import threading
from PIL import Image

from api_client import ApiClient
from image_utils import ImageCache

# ============================================================
# CORES & ESTILOS (paleta profissional)
# ============================================================
PRIMARY = "#FF6A00"
PRIMARY_HOVER = "#E55D00"
SECONDARY = "#1E1E1E"
BACKGROUND = "#121212"
SURFACE = "#1E1E1E"
SURFACE_VARIANT = "#2C2C2C"
TEXT_PRIMARY = "#FFFFFF"
TEXT_SECONDARY = "#B3B3B3"
SUCCESS = "#4CAF50"
SUCCESS_HOVER = "#45A049"
DANGER = "#F44336"
DANGER_HOVER = "#D32F2F"
WARNING = "#FFC107"
INFO = "#2196F3"
BORDER = "#3A3A3A"
CHART_COLORS = ["#FF6A00", "#FF4081", "#00E5FF", "#76FF03", "#FFD740"]

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")

# ============================================================
# WIDGETS ANIMADOS
# ============================================================
class AnimatedLabel(ctk.CTkLabel):
    """Label que anima a mudança de número."""
    def __init__(self, master, prefix="", suffix="", **kwargs):
        super().__init__(master, **kwargs)
        self.prefix = prefix
        self.suffix = suffix
        self._target = 0.0
        self._current = 0.0
        self._job = None
        self.configure(text=f"{self.prefix}0{self.suffix}")

    def set_value(self, value: float, animate: bool = True):
        self._target = value
        if animate:
            self._animate()
        else:
            self._current = value
            self._update_text()

    def _animate(self):
        if self._job:
            self.after_cancel(self._job)
        self._step = (self._target - self._current) * 0.2
        if abs(self._step) < 0.1:
            self._current = self._target
        else:
            self._current += self._step
        self._update_text()
        if abs(self._current - self._target) > 0.1:
            self._job = self.after(16, self._animate)

    def _update_text(self):
        self.configure(text=f"{self.prefix}{self._current:.2f}{self.suffix}")

class SimpleBarChart(ctk.CTkFrame):
    """Gráfico de barras simplificado."""
    def __init__(self, master, width=400, height=250, **kwargs):
        super().__init__(master, width=width, height=height, **kwargs)
        self.canvas = tk.Canvas(self, bg=SURFACE_VARIANT, highlightthickness=0)
        self.canvas.pack(fill="both", expand=True)

    def set_data(self, data: dict):
        self.canvas.delete("all")
        if not data:
            return
        w = self.canvas.winfo_width() or 400
        h = self.canvas.winfo_height() or 250
        margin = 40
        bar_area = w - 2 * margin
        num_bars = len(data)
        bar_width = max(20, bar_area // num_bars - 5)
        max_value = max(data.values()) if data else 1
        if max_value == 0:          # <-- EVITA DIVISÃO POR ZERO
            max_value = 1

        for i, (label, value) in enumerate(data.items()):
            x = margin + i * (bar_width + 5) + bar_width/2
            bar_h = (value / max_value) * (h - 80)
            y0 = h - 40 - bar_h
            y1 = h - 40
            color = CHART_COLORS[i % len(CHART_COLORS)]
            self.canvas.create_rectangle(x - bar_width/2, y0, x + bar_width/2, y1, fill=color, outline="")
            self.canvas.create_text(x, y0 - 10, text=str(value), fill=TEXT_PRIMARY, font=("Poppins", 11, "bold"))
            self.canvas.create_text(x, h - 20, text=label, fill=TEXT_SECONDARY, font=("Poppins", 10), angle=0)
        self.canvas.create_line(margin, h-40, w-margin, h-40, fill=TEXT_SECONDARY)

class StatusIndicator(ctk.CTkFrame):
    def __init__(self, master, status, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.status = status
        colors = {"Preparando": WARNING, "Pronto": SUCCESS, "Saiu para entrega": PRIMARY, "Entregue": TEXT_SECONDARY}
        self.dot = ctk.CTkLabel(self, text="●", font=("Arial", 14), text_color=colors.get(status, TEXT_SECONDARY))
        self.dot.pack(side="left", padx=(0,5))
        self.label = ctk.CTkLabel(self, text=status, font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY)
        self.label.pack(side="left")

    def atualizar(self, novo_status):
        colors = {"Preparando": WARNING, "Pronto": SUCCESS, "Saiu para entrega": PRIMARY, "Entregue": TEXT_SECONDARY}
        self.dot.configure(text_color=colors.get(novo_status, TEXT_SECONDARY))
        self.label.configure(text=novo_status)

# ============================================================
# JANELA PRINCIPAL
# ============================================================
class GestaoApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("Sabor Express – Centro de Gestão")
        self.geometry("1366x800")
        self.minsize(1200, 700)
        self.protocol("WM_DELETE_WINDOW", self._on_close)

        self.grid_rowconfigure(0, weight=1)
        self.grid_columnconfigure(0, weight=1)

        # Abas (corrigido argumento duplicado)
        self.tabview = CTkTabview(
            self,
            fg_color=BACKGROUND,
            segmented_button_fg_color=SURFACE_VARIANT,
            segmented_button_selected_color=PRIMARY,
            segmented_button_unselected_color=SURFACE_VARIANT,
            text_color=TEXT_PRIMARY,
            text_color_disabled=TEXT_SECONDARY
        )
        self.tabview.grid(row=0, column=0, sticky="nsew", padx=5, pady=5)

        self.tab_dash = self.tabview.add("📊 Dashboard")
        self.tab_pedidos = self.tabview.add("🛒 Pedidos")
        self.tab_cardapio = self.tabview.add("🍔 Cardápio")

        self._criar_dashboard()
        self._criar_pedidos()
        self._criar_cardapio()

        # Status bar
        self.status_bar = ctk.CTkFrame(self, fg_color=SURFACE, height=30, corner_radius=0)
        self.status_bar.grid(row=1, column=0, sticky="ew")
        self.lbl_status = ctk.CTkLabel(self.status_bar, text="🟢 Conectado ao servidor", font=ctk.CTkFont(size=11),
                                       text_color=TEXT_SECONDARY)
        self.lbl_status.pack(side="left", padx=15, pady=2)
        self.lbl_hora = ctk.CTkLabel(self.status_bar, text="", font=ctk.CTkFont(size=11), text_color=TEXT_SECONDARY)
        self.lbl_hora.pack(side="right", padx=15, pady=2)

        self.pedidos_cache = []
        self.produtos_cache = []
        self.update_interval = 10000

        self.carregar_dados()
        self.atualizar_relogio()
        self.agendar_atualizacoes()

    # ============================================================
    # DASHBOARD
    # ============================================================
    def _criar_dashboard(self):
        frame = self.tab_dash
        frame.grid_columnconfigure(0, weight=1)
        frame.grid_rowconfigure(1, weight=1)

        header = ctk.CTkFrame(frame, fg_color=SURFACE, height=50)
        header.grid(row=0, column=0, sticky="ew", padx=10, pady=10)
        header.grid_columnconfigure(1, weight=1)
        ctk.CTkLabel(header, text="Visão Geral", font=ctk.CTkFont(size=22, weight="bold"),
                     text_color=TEXT_PRIMARY).grid(row=0, column=0, padx=15, pady=10)
        self.periodo_var = ctk.StringVar(value="Hoje")
        ctk.CTkOptionMenu(header, values=["Hoje", "Ontem", "Últimos 7 dias", "Este mês"],
                          variable=self.periodo_var, command=lambda _: self.atualizar_dashboard(),
                          fg_color=SURFACE_VARIANT, button_color=PRIMARY, text_color=TEXT_PRIMARY,
                          font=ctk.CTkFont(size=13)).grid(row=0, column=1, sticky="e", padx=15)
        btn_refresh = ctk.CTkButton(header, text="Atualizar", fg_color=PRIMARY, hover_color=PRIMARY_HOVER,
                                    command=self.carregar_dados, width=100)
        btn_refresh.grid(row=0, column=2, padx=15)

        body = ctk.CTkFrame(frame, fg_color="transparent")
        body.grid(row=1, column=0, sticky="nsew", padx=10, pady=5)
        body.grid_columnconfigure(0, weight=3)
        body.grid_columnconfigure(1, weight=2)
        body.grid_rowconfigure(0, weight=1)

        col_left = ctk.CTkFrame(body, fg_color="transparent")
        col_left.grid(row=0, column=0, sticky="nsew", padx=(0,5))
        col_left.grid_rowconfigure(1, weight=1)
        col_left.grid_columnconfigure((0,1,2,3), weight=1)

        self.card_total = self._criar_card_kpi(col_left, "Total de Pedidos", "0", "📦", PRIMARY, 0, 0)
        self.card_preparando = self._criar_card_kpi(col_left, "Preparando", "0", "🔥", WARNING, 0, 1)
        self.card_prontos = self._criar_card_kpi(col_left, "Prontos", "0", "✅", SUCCESS, 0, 2)
        self.card_faturamento = self._criar_card_kpi(col_left, "Faturamento", "R$ 0.00", "💰", INFO, 0, 3)

        self.chart_frame = ctk.CTkFrame(col_left, fg_color=SURFACE, corner_radius=15)
        self.chart_frame.grid(row=1, column=0, columnspan=4, sticky="nsew", padx=5, pady=10)
        self.bar_chart = SimpleBarChart(self.chart_frame, height=250)
        self.bar_chart.pack(fill="both", expand=True, padx=15, pady=15)

        col_right = ctk.CTkFrame(body, fg_color="transparent")
        col_right.grid(row=0, column=1, sticky="nsew", padx=(5,0))
        col_right.grid_rowconfigure(0, weight=1)
        col_right.grid_rowconfigure(1, weight=1)

        self.recent_frame = ctk.CTkFrame(col_right, fg_color=SURFACE, corner_radius=15)
        self.recent_frame.grid(row=0, column=0, sticky="nsew", pady=(0,5))
        ctk.CTkLabel(self.recent_frame, text="Últimos Pedidos", font=ctk.CTkFont(size=14, weight="bold"),
                     text_color=TEXT_PRIMARY).pack(pady=10, padx=10, anchor="w")
        self.recent_list = ctk.CTkScrollableFrame(self.recent_frame, fg_color="transparent", height=150)
        self.recent_list.pack(fill="both", expand=True, padx=5, pady=5)

        self.pie_frame = ctk.CTkFrame(col_right, fg_color=SURFACE, corner_radius=15)
        self.pie_frame.grid(row=1, column=0, sticky="nsew", pady=(5,0))
        self.pie_chart_canvas = tk.Canvas(self.pie_frame, bg=SURFACE, highlightthickness=0)
        self.pie_chart_canvas.pack(fill="both", expand=True, padx=10, pady=10)

    def _criar_card_kpi(self, parent, title, value, icon, color, row, col):
        card = ctk.CTkFrame(parent, fg_color=SURFACE, corner_radius=15, height=100)
        card.grid(row=row, column=col, padx=5, pady=5, sticky="nsew")
        card.grid_propagate(False)
        ctk.CTkLabel(card, text=icon, font=("Arial", 28)).grid(row=0, column=0, padx=10, pady=(10,0), sticky="w")
        ctk.CTkLabel(card, text=title, font=ctk.CTkFont(size=11), text_color=TEXT_SECONDARY).grid(row=1, column=0, padx=10, sticky="w")
        if "R$" in value:
            anim = AnimatedLabel(card, prefix="R$ ", font=ctk.CTkFont(size=22, weight="bold"), text_color=color)
        else:
            anim = AnimatedLabel(card, font=ctk.CTkFont(size=22, weight="bold"), text_color=color)
        anim.set_value(0, animate=False)
        anim.grid(row=2, column=0, padx=10, pady=(0,10), sticky="w")
        return anim

    def atualizar_dashboard(self):
        filtrados = self._filtrar_pedidos_por_periodo(self.pedidos_cache)
        total = len(filtrados)
        preparando = sum(1 for p in filtrados if p['status'] == 'Preparando')
        prontos = sum(1 for p in filtrados if p['status'] == 'Pronto')
        faturamento = sum(p['total'] for p in filtrados)

        self.card_total.set_value(total, animate=True)
        self.card_preparando.set_value(preparando, animate=True)
        self.card_prontos.set_value(prontos, animate=True)
        self.card_faturamento.set_value(faturamento, animate=True)

        status_counts = {"Preparando": 0, "Pronto": 0, "Saiu para entrega": 0, "Entregue": 0}
        for p in filtrados:
            status_counts[p['status']] = status_counts.get(p['status'], 0) + 1
        self.bar_chart.set_data(status_counts)
        self._desenhar_pizza(status_counts)

        for w in self.recent_list.winfo_children():
            w.destroy()
        recentes = sorted(filtrados, key=lambda p: p.get('data_hora', ''), reverse=True)[:5]
        for p in recentes:
            item = ctk.CTkFrame(self.recent_list, fg_color=SURFACE_VARIANT, corner_radius=8)
            item.pack(fill="x", padx=5, pady=2)
            ctk.CTkLabel(item, text=f"#{p['id']} - {p['cliente']}", font=ctk.CTkFont(size=12, weight="bold"),
                         text_color=TEXT_PRIMARY).pack(side="left", padx=10, pady=5)
            ctk.CTkLabel(item, text=f"R$ {p['total']:.2f}", font=ctk.CTkFont(size=12), text_color=SUCCESS).pack(side="right", padx=10)

    def _filtrar_pedidos_por_periodo(self, pedidos):
        periodo = self.periodo_var.get()
        hoje = datetime.now().date()
        if periodo == "Hoje":
            return [p for p in pedidos if self._extrair_data(p) == hoje]
        elif periodo == "Ontem":
            ontem = hoje - timedelta(days=1)
            return [p for p in pedidos if self._extrair_data(p) == ontem]
        elif periodo == "Últimos 7 dias":
            inicio = hoje - timedelta(days=7)
            return [p for p in pedidos if inicio <= self._extrair_data(p) <= hoje]
        elif periodo == "Este mês":
            return [p for p in pedidos if self._extrair_data(p).month == hoje.month]
        return pedidos

    def _extrair_data(self, pedido):
        try:
            data_str = pedido.get('data', pedido.get('data_hora', '').split(' ')[0])
            return datetime.strptime(data_str, "%Y-%m-%d").date()
        except:
            return datetime.now().date()

    def _desenhar_pizza(self, data):
        canvas = self.pie_chart_canvas
        canvas.delete("all")
        w = canvas.winfo_width() or 200
        h = canvas.winfo_height() or 200
        cx, cy, r = w//2, h//2, min(w,h)//2 - 20
        total = sum(data.values())
        if total == 0:
            canvas.create_text(cx, cy, text="Sem dados", fill=TEXT_SECONDARY)
            return
        start_angle = 90
        colors = {"Preparando": WARNING, "Pronto": SUCCESS, "Saiu para entrega": PRIMARY, "Entregue": TEXT_SECONDARY}
        for status, count in data.items():
            if count == 0:
                continue
            extent = count / total * 360
            canvas.create_arc(cx - r, cy - r, cx + r, cy + r, start=start_angle, extent=extent,
                              fill=colors.get(status, "#888"), outline="")
            leg_x = cx + r + 30
            leg_y = 50 + list(data.keys()).index(status) * 30
            canvas.create_rectangle(leg_x, leg_y, leg_x+15, leg_y+15, fill=colors.get(status, "#888"), outline="")
            canvas.create_text(leg_x+70, leg_y+7, text=f"{status} ({count})", anchor="w", fill=TEXT_PRIMARY,
                               font=("Poppins", 9))
            start_angle += extent

    # ============================================================
    # ABA PEDIDOS
    # ============================================================
    def _criar_pedidos(self):
        frame = self.tab_pedidos
        frame.grid_rowconfigure(0, weight=0)
        frame.grid_rowconfigure(1, weight=1)
        frame.grid_columnconfigure(0, weight=1)

        toolbar = ctk.CTkFrame(frame, fg_color=SURFACE, height=50)
        toolbar.grid(row=0, column=0, sticky="ew", padx=10, pady=10)
        ctk.CTkLabel(toolbar, text="Gerenciamento de Pedidos", font=ctk.CTkFont(size=18, weight="bold"),
                     text_color=TEXT_PRIMARY).pack(side="left", padx=15, pady=10)
        self.pedido_filtro_status = ctk.StringVar(value="Todos")
        ctk.CTkOptionMenu(toolbar, values=["Todos", "Preparando", "Pronto", "Saiu para entrega", "Entregue"],
                          variable=self.pedido_filtro_status, command=lambda _: self.atualizar_lista_pedidos(),
                          fg_color=SURFACE_VARIANT, button_color=PRIMARY).pack(side="right", padx=15)

        self.scroll_pedidos = ctk.CTkScrollableFrame(frame, fg_color="transparent")
        self.scroll_pedidos.grid(row=1, column=0, sticky="nsew", padx=10, pady=10)
        self.scroll_pedidos.grid_columnconfigure(0, weight=1)

    def atualizar_lista_pedidos(self):
        pedidos = self.pedidos_cache
        filtro = self.pedido_filtro_status.get()
        if filtro != "Todos":
            pedidos = [p for p in pedidos if p['status'] == filtro]
        pedidos.sort(key=lambda p: p.get('data_hora', ''), reverse=True)

        for w in self.scroll_pedidos.winfo_children():
            w.destroy()
        if not pedidos:
            ctk.CTkLabel(self.scroll_pedidos, text="Nenhum pedido encontrado.", text_color=TEXT_SECONDARY).pack(pady=30)
            return

        for i, pedido in enumerate(pedidos):
            self._criar_card_pedido(pedido, i)

    def _criar_card_pedido(self, pedido, row):
        card = ctk.CTkFrame(self.scroll_pedidos, fg_color=SURFACE, corner_radius=15, border_width=1, border_color=BORDER)
        card.grid(row=row, column=0, sticky="ew", padx=5, pady=5)
        card.grid_columnconfigure(1, weight=1)

        header = ctk.CTkFrame(card, fg_color="transparent")
        header.pack(fill="x", padx=15, pady=10)
        ctk.CTkLabel(header, text=f"#{pedido['id']}", font=ctk.CTkFont(size=14, weight="bold"),
                     text_color=PRIMARY).pack(side="left")
        ind = StatusIndicator(header, pedido['status'])
        ind.pack(side="left", padx=10)
        ctk.CTkLabel(header, text=pedido['cliente'], font=ctk.CTkFont(size=13),
                     text_color=TEXT_PRIMARY).pack(side="left", padx=10)
        ctk.CTkLabel(header, text=f"R$ {pedido['total']:.2f}", font=ctk.CTkFont(size=14, weight="bold"),
                     text_color=SUCCESS).pack(side="right")

        detalhes = ctk.CTkFrame(card, fg_color=SURFACE_VARIANT, corner_radius=10)
        itens_texto = "\n".join([f"• {i['quantidade']}x {i['nome']} - R$ {i.get('subtotal',0):.2f}" for i in pedido['itens']])
        ctk.CTkLabel(detalhes, text=itens_texto, font=ctk.CTkFont(size=12), justify="left",
                     text_color=TEXT_SECONDARY).pack(anchor="w", padx=15, pady=5)
        info = f"{'🚚' if pedido['tipo_entrega']=='Entrega' else '🏪'} {pedido['tipo_entrega']} | {pedido['pagamento']} | 🕒 {pedido['data_hora']}"
        ctk.CTkLabel(detalhes, text=info, font=ctk.CTkFont(size=12), text_color=TEXT_SECONDARY).pack(anchor="w", padx=15, pady=2)

        acoes = ctk.CTkFrame(card, fg_color="transparent")
        acoes.pack(fill="x", padx=10, pady=10)
        transicoes = {"Preparando": ["Pronto"], "Pronto": ["Saiu para entrega"], "Saiu para entrega": ["Entregue"], "Entregue": []}
        for ns in transicoes.get(pedido['status'], []):
            ctk.CTkButton(acoes, text=ns, width=100, fg_color=PRIMARY if ns != "Entregue" else INFO,
                          hover_color=PRIMARY_HOVER, command=lambda pid=pedido['id'], st=ns: self._mudar_status(pid, st)).pack(side="left", padx=5)

        expand_btn = ctk.CTkButton(card, text="▼ Detalhes", width=100, fg_color="transparent",
                                   text_color=TEXT_SECONDARY, command=lambda d=detalhes, b=None: self._toggle_detalhes(d, b))
        expand_btn.pack(side="bottom", padx=10, pady=5)
        detalhes.pack_forget()
        # Armazena o botão para callback
        expand_btn.configure(command=lambda d=detalhes, b=expand_btn: self._toggle_detalhes(d, b))

    def _toggle_detalhes(self, frame, btn):
        if frame.winfo_ismapped():
            frame.pack_forget()
            btn.configure(text="▼ Detalhes")
        else:
            frame.pack(fill="x", padx=10, pady=5, before=btn.master.winfo_children()[-1])
            btn.configure(text="▲ Ocultar")

    def _mudar_status(self, pedido_id, novo_status):
        try:
            ApiClient.atualizar_status_pedido(pedido_id, novo_status)
            self.carregar_dados()
        except Exception as e:
            tkmb.showerror("Erro", str(e))

    # ============================================================
    # ABA CARDÁPIO
    # ============================================================
    def _criar_cardapio(self):
        frame = self.tab_cardapio
        frame.grid_rowconfigure(0, weight=0)
        frame.grid_rowconfigure(1, weight=1)
        frame.grid_columnconfigure(0, weight=1)

        toolbar = ctk.CTkFrame(frame, fg_color=SURFACE, height=50)
        toolbar.grid(row=0, column=0, sticky="ew", padx=10, pady=10)
        toolbar.grid_columnconfigure(1, weight=1)
        ctk.CTkLabel(toolbar, text="Cardápio", font=ctk.CTkFont(size=18, weight="bold"),
                     text_color=TEXT_PRIMARY).grid(row=0, column=0, padx=15, pady=10)
        self.search_cardapio = ctk.CTkEntry(toolbar, placeholder_text="Buscar produto...", fg_color=SURFACE_VARIANT,
                                            border_color=BORDER, text_color=TEXT_PRIMARY)
        self.search_cardapio.grid(row=0, column=1, padx=10, sticky="ew")
        self.search_cardapio.bind("<KeyRelease>", lambda e: self.filtrar_cardapio())
        ctk.CTkButton(toolbar, text="➕ Novo", fg_color=SUCCESS, hover_color=SUCCESS_HOVER,
                      command=self._abrir_form_produto).grid(row=0, column=2, padx=10)

        self.grid_cardapio = ctk.CTkScrollableFrame(frame, fg_color="transparent")
        self.grid_cardapio.grid(row=1, column=0, sticky="nsew", padx=10, pady=5)

        self.categoria_filtro_var = ctk.StringVar(value="Todas")
        cat_frame = ctk.CTkFrame(frame, fg_color="transparent")
        cat_frame.grid(row=2, column=0, sticky="ew", padx=10, pady=5)
        ctk.CTkLabel(cat_frame, text="Categoria:", text_color=TEXT_SECONDARY).pack(side="left", padx=5)
        ctk.CTkOptionMenu(cat_frame, values=["Todas"] + ['Combos', 'Hambúrgueres', 'Acompanhamentos', 'Entradas', 'Bebidas', 'Sobremesas'],
                          variable=self.categoria_filtro_var, command=lambda _: self.filtrar_cardapio(),
                          fg_color=SURFACE_VARIANT, button_color=PRIMARY).pack(side="left", padx=5)

    def atualizar_lista_cardapio(self):
        try:
            self.produtos_cache = ApiClient.get_produtos()
        except Exception as e:
            tkmb.showerror("Erro", f"Falha ao carregar produtos: {e}")
            return
        self.filtrar_cardapio()

    def filtrar_cardapio(self):
        termo = self.search_cardapio.get().lower() if hasattr(self, 'search_cardapio') else ""
        categoria = self.categoria_filtro_var.get() if hasattr(self, 'categoria_filtro_var') else "Todas"
        produtos = self.produtos_cache
        if categoria != "Todas":
            produtos = [p for p in produtos if p.get('categoria', '') == categoria]
        if termo:
            produtos = [p for p in produtos if termo in p['nome'].lower() or termo in p.get('descricao', '').lower()]
        for w in self.grid_cardapio.winfo_children():
            w.destroy()
        if not produtos:
            ctk.CTkLabel(self.grid_cardapio, text="Nenhum produto encontrado.", text_color=TEXT_SECONDARY).pack(pady=30)
            return
        for i, prod in enumerate(produtos):
            self._criar_card_produto(prod, i)

    def _criar_card_produto(self, produto, index):
        card = ctk.CTkFrame(self.grid_cardapio, fg_color=SURFACE, corner_radius=15, border_width=1, border_color=BORDER)
        card.pack(fill="x", padx=5, pady=5)
        card.grid_columnconfigure(1, weight=1)

        img_url = ApiClient.get_url_imagem(produto['imagem'])
        img = ImageCache.get_image(img_url, (80, 60))
        if img:
            lbl_img = ctk.CTkLabel(card, image=img, text="")
            lbl_img.grid(row=0, column=0, rowspan=2, padx=10, pady=10, sticky="w")
        else:
            ctk.CTkLabel(card, text="🍔", font=("Arial", 30)).grid(row=0, column=0, rowspan=2, padx=10, pady=10)

        ctk.CTkLabel(card, text=produto['nome'], font=ctk.CTkFont(size=14, weight="bold"),
                     text_color=TEXT_PRIMARY).grid(row=0, column=1, sticky="w", padx=5, pady=(5,0))
        ctk.CTkLabel(card, text=produto.get('descricao', '')[:80], font=ctk.CTkFont(size=11),
                     text_color=TEXT_SECONDARY, wraplength=500).grid(row=1, column=1, sticky="w", padx=5, pady=(0,5))
        ctk.CTkLabel(card, text=f"R$ {produto['preco']:.2f}", font=ctk.CTkFont(size=14, weight="bold"),
                     text_color=SUCCESS).grid(row=0, column=2, rowspan=2, padx=10)

        btn_frame = ctk.CTkFrame(card, fg_color="transparent")
        btn_frame.grid(row=0, column=3, rowspan=2, padx=10)
        ctk.CTkButton(btn_frame, text="✏️", width=40, fg_color=PRIMARY, hover_color=PRIMARY_HOVER,
                      command=lambda p=produto: self._abrir_form_produto(p)).pack(side="left", padx=2)
        ctk.CTkButton(btn_frame, text="🗑️", width=40, fg_color=DANGER, hover_color=DANGER_HOVER,
                      command=lambda p=produto: self._excluir_produto(p['id'])).pack(side="left", padx=2)

    def _abrir_form_produto(self, produto_existente=None):
        janela = ctk.CTkToplevel(self)
        janela.title("Editar Produto" if produto_existente else "Novo Produto")
        janela.geometry("480x680")
        janela.configure(fg_color=BACKGROUND)
        janela.transient(self)
        janela.grab_set()

        # Variáveis
        nome_var = ctk.StringVar(value=produto_existente['nome'] if produto_existente else "")
        preco_var = ctk.StringVar(value=str(produto_existente['preco']) if produto_existente else "")
        desc_var = ctk.StringVar(value=produto_existente.get('descricao', '') if produto_existente else "")
        calorias_var = ctk.StringVar(value=str(produto_existente['calorias']) if produto_existente else "")
        badge_var = ctk.StringVar(value=produto_existente.get('badge', '') if produto_existente else "")
        categoria_var = ctk.StringVar(value=produto_existente.get('categoria', 'Combos') if produto_existente else "Combos")
        img_path_var = ctk.StringVar()

        # Layout
        form = ctk.CTkFrame(janela, fg_color="transparent")
        form.pack(fill="both", expand=True, padx=20, pady=20)

        campos = [("Nome", nome_var), ("Preço", preco_var), ("Descrição", desc_var),
                  ("Calorias", calorias_var), ("Badge", badge_var)]
        row = 0
        for label, var in campos:
            ctk.CTkLabel(form, text=label, text_color=TEXT_PRIMARY).grid(row=row, column=0, padx=10, pady=8, sticky="e")
            ctk.CTkEntry(form, textvariable=var, fg_color=SURFACE_VARIANT, border_color=BORDER, text_color=TEXT_PRIMARY).grid(row=row, column=1, padx=10, pady=8, sticky="ew")
            row += 1

        # Categoria
        ctk.CTkLabel(form, text="Categoria", text_color=TEXT_PRIMARY).grid(row=row, column=0, padx=10, pady=8, sticky="e")
        ctk.CTkOptionMenu(form, values=['Combos','Hambúrgueres','Acompanhamentos','Entradas','Bebidas','Sobremesas'],
                          variable=categoria_var, fg_color=SURFACE_VARIANT, button_color=PRIMARY, text_color=TEXT_PRIMARY).grid(row=row, column=1, padx=10, pady=8, sticky="ew")
        row += 1

        # Imagem
        ctk.CTkLabel(form, text="Imagem", text_color=TEXT_PRIMARY).grid(row=row, column=0, padx=10, pady=8, sticky="e")
        img_frame = ctk.CTkFrame(form, fg_color="transparent")
        img_frame.grid(row=row, column=1, padx=10, pady=8, sticky="w")
        ctk.CTkButton(img_frame, text="Selecionar arquivo...", command=lambda: self._escolher_imagem(img_path_var, preview_label),
                      fg_color=PRIMARY).pack(side="left", padx=5)
        preview_label = ctk.CTkLabel(img_frame, text="Nenhuma imagem", text_color=TEXT_SECONDARY)
        preview_label.pack(side="left", padx=10)
        if produto_existente:
            preview_label.configure(text="Imagem atual (selecione para alterar)")
        row += 1

        # Botões
        btn_frame = ctk.CTkFrame(janela, fg_color="transparent")
        btn_frame.pack(fill="x", padx=20, pady=20)
        ctk.CTkButton(btn_frame, text="Cancelar", fg_color="transparent", border_width=1, border_color=BORDER,
                      text_color=TEXT_SECONDARY, command=janela.destroy).pack(side="left", padx=10)
        def salvar():
            try:
                dados = {
                    "nome": nome_var.get(),
                    "preco": float(preco_var.get()),
                    "descricao": desc_var.get(),
                    "categoria": categoria_var.get(),
                    "calorias": int(calorias_var.get()),
                    "badge": badge_var.get() or None
                }
            except ValueError:
                tkmb.showerror("Erro", "Preço e calorias devem ser números válidos.")
                return
            imagem = img_path_var.get()
            if not produto_existente and not imagem:
                tkmb.showerror("Erro", "Selecione uma imagem para o novo produto.")
                return
            try:
                if produto_existente:
                    ApiClient.atualizar_produto(produto_existente['id'], dados, imagem if imagem else None)
                else:
                    ApiClient.criar_produto(dados, imagem)
            except Exception as e:
                tkmb.showerror("Erro", f"Falha ao salvar: {e}")
                return
            janela.destroy()
            self.atualizar_lista_cardapio()
            self.atualizar_dashboard()
        ctk.CTkButton(btn_frame, text="Salvar", fg_color=SUCCESS, hover_color=SUCCESS_HOVER, command=salvar).pack(side="right", padx=10)

    def _escolher_imagem(self, var, label):
        path = filedialog.askopenfilename(filetypes=[("Imagens", "*.png *.jpg *.jpeg")])
        if path:
            var.set(path)
            label.configure(text="Imagem selecionada: " + path.split("/")[-1])

    def _excluir_produto(self, produto_id):
        if tkmb.askyesno("Confirmar", "Remover este produto permanentemente?"):
            try:
                ApiClient.excluir_produto(produto_id)
                self.atualizar_lista_cardapio()
            except Exception as e:
                tkmb.showerror("Erro", str(e))

    # ============================================================
    # ATUALIZAÇÕES E DADOS
    # ============================================================
    def carregar_dados(self):
        threading.Thread(target=self._carregar_dados_thread, daemon=True).start()

    def _carregar_dados_thread(self):
        try:
            self.pedidos_cache = ApiClient.get_pedidos()
            self.produtos_cache = ApiClient.get_produtos()
            self.after(0, self._atualizar_ui)
            self.after(0, lambda: self.lbl_status.configure(text="🟢 Online"))
        except Exception as e:
            self.after(0, lambda: self.lbl_status.configure(text="🔴 Offline"))
            print("Erro ao carregar dados:", e)

    def _atualizar_ui(self):
        self.atualizar_dashboard()
        self.atualizar_lista_pedidos()
        self.atualizar_lista_cardapio()

    def agendar_atualizacoes(self):
        self.carregar_dados()
        self.after(self.update_interval, self.agendar_atualizacoes)

    def atualizar_relogio(self):
        self.lbl_hora.configure(text=datetime.now().strftime("%H:%M:%S"))
        self.after(1000, self.atualizar_relogio)

    def _on_close(self):
        self.destroy()

if __name__ == "__main__":
    app = GestaoApp()
    app.mainloop()