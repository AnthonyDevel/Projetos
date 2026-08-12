import requests
from typing import List, Dict, Optional

BASE_URL = "http://192.168.18.2:5000"  # Ajuste para o IP do servidor Flask

class ApiClient:
    @staticmethod
    def _url(path):
        return f"{BASE_URL}{path}"

    # ---------- PRODUTOS ----------
    @staticmethod
    def get_produtos() -> List[Dict]:
        resp = requests.get(ApiClient._url("/api/produtos"))
        resp.raise_for_status()
        return resp.json()

    @staticmethod
    def criar_produto(dados: Dict, imagem_path: str) -> Dict:
        with open(imagem_path, 'rb') as img:
            files = {'imagem': img}
            resp = requests.post(ApiClient._url("/api/produtos"), data=dados, files=files)
        resp.raise_for_status()
        return resp.json()

    @staticmethod
    def atualizar_produto(produto_id: str, dados: Dict, imagem_path: Optional[str] = None) -> Dict:
        if imagem_path:
            with open(imagem_path, 'rb') as img:
                files = {'imagem': img}
                resp = requests.put(ApiClient._url(f"/api/produtos/{produto_id}"), data=dados, files=files)
        else:
            resp = requests.put(ApiClient._url(f"/api/produtos/{produto_id}"), json=dados)
        resp.raise_for_status()
        return resp.json()

    @staticmethod
    def excluir_produto(produto_id: str) -> bool:
        resp = requests.delete(ApiClient._url(f"/api/produtos/{produto_id}"))
        return resp.ok

    # ---------- PEDIDOS ----------
    @staticmethod
    def get_pedidos() -> List[Dict]:
        resp = requests.get(ApiClient._url("/api/pedidos"))
        resp.raise_for_status()
        return resp.json()

    @staticmethod
    def atualizar_status_pedido(pedido_id: str, novo_status: str) -> bool:
        resp = requests.post(ApiClient._url(f"/api/pedido/{pedido_id}/status"),
                             json={"status": novo_status})
        return resp.ok

    # ---------- IMAGENS ----------
    @staticmethod
    def get_url_imagem(caminho_relativo: str) -> str:
        # Retorna a URL completa para a imagem
        return f"{BASE_URL}/{caminho_relativo}"