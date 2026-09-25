# Fluxo de Dados

Sistema em Java (Spring Boot) para várias pessoas, em computadores diferentes, inserirem dados pela web. Cada item entra numa fila e é tratado **online e de forma assíncrona**. Todos os navegadores conectados veem o status em tempo real.

## Como rodar

No computador que vai ser o servidor:

```bat
mvnw.cmd spring-boot:run
```

Abra no próprio PC: [http://localhost:8080](http://localhost:8080)

Nas outras máquinas da mesma rede, use o IP deste computador, por exemplo:

`http://192.168.0.10:8080`

Para descobrir o IP no Windows: `ipconfig` (procure IPv4).

Se o firewall bloquear a porta 8080, permita Java ou essa porta na rede privada.

## Como funciona

1. Alguém envia um dado pela página (nome + conteúdo).
2. O servidor grava o item como **PENDENTE** e responde na hora (`202 Accepted`).
3. Uma pool de threads trata o conteúdo em segundo plano (**PROCESSANDO** → **CONCLUIDO**).
4. Eventos SSE (`/api/dados/stream`) avisam todos os clientes conectados.

## API

- `GET /api/dados` — lista os itens
- `POST /api/dados` — `{ "autor": "Ana", "conteudo": "texto" }`
- `GET /api/dados/stream` — atualizações ao vivo
