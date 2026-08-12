# models.py
from dataclasses import dataclass, asdict
from typing import Optional, Dict, List
from datetime import datetime

@dataclass(frozen=True)
class Lanche:
    id: str
    nome: str
    preco: float
    imagem: str
    descricao: str
    categoria: str
    calorias: int
    badge: Optional[str] = None

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, data):
        return cls(**data)

class ItemPedido:
    def __init__(self, lanche: Lanche, quantidade: int):
        self.lanche = lanche
        self.quantidade = quantidade

    @property
    def subtotal(self):
        return self.lanche.preco * self.quantidade

class PedidoConfirmado:
    def __init__(self, id_pedido, cliente_nome, itens: Dict[str, ItemPedido],
                 total, tipo_entrega, endereco, pagamento):
        self.id = id_pedido
        self.cliente_nome = cliente_nome
        self.itens = itens
        self.total = total
        self.tipo_entrega = tipo_entrega
        self.endereco = endereco
        self.pagamento = pagamento
        self.status = "Preparando"
        self.data_hora = datetime.now().strftime("%H:%M")

    def atualizar_status(self, novo_status):
        self.status = novo_status

class GerenciadorPedidos:
    def __init__(self):
        self._pedidos: List[PedidoConfirmado] = []

    def adicionar(self, pedido):
        self._pedidos.append(pedido)

    def obter_todos(self):
        return self._pedidos

    def atualizar_status(self, pedido_id, novo_status):
        for p in self._pedidos:
            if p.id == pedido_id:
                p.atualizar_status(novo_status)
                return True
        return False