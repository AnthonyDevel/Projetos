import io
import requests
from PIL import Image
import customtkinter as ctk

class ImageCache:
    _cache = {}

    @classmethod
    def get_image(cls, url: str, size: tuple) -> ctk.CTkImage:
        key = (url, size)
        if key not in cls._cache:
            try:
                resp = requests.get(url, timeout=5)
                resp.raise_for_status()
                img = Image.open(io.BytesIO(resp.content))
                img = img.resize(size, Image.LANCZOS)
                # Converter para CTkImage
                ctk_img = ctk.CTkImage(light_image=img, dark_image=img, size=size)
                cls._cache[key] = ctk_img
            except Exception as e:
                print(f"Erro ao carregar imagem {url}: {e}")
                cls._cache[key] = None
        return cls._cache[key]