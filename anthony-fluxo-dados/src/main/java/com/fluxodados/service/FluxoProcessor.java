package com.fluxodados.service;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fluxodados.domain.DadoFluxo;
import com.fluxodados.domain.StatusFluxo;
import com.fluxodados.repo.DadoFluxoRepository;
import com.fluxodados.web.DadoFluxoResponse;

@Service
public class FluxoProcessor {

	private final DadoFluxoRepository repository;
	private final FluxoBroadcaster broadcaster;

	public FluxoProcessor(DadoFluxoRepository repository, FluxoBroadcaster broadcaster) {
		this.repository = repository;
		this.broadcaster = broadcaster;
	}

	@Async("fluxoExecutor")
	public void processar(Long id) {
		atualizar(id, StatusFluxo.PROCESSANDO, "Processando em segundo plano...", null);
		try {
			int esperaMs = ThreadLocalRandom.current().nextInt(800, 2500);
			Thread.sleep(esperaMs);
			DadoFluxo dado = repository.findById(id).orElseThrow();
			String resultado = tratar(dado.getConteudo(), esperaMs);
			atualizar(id, StatusFluxo.CONCLUIDO, resultado, Instant.now());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			atualizar(id, StatusFluxo.ERRO, "Processamento interrompido.", Instant.now());
		} catch (Exception ex) {
			atualizar(id, StatusFluxo.ERRO, "Falha no tratamento: " + ex.getMessage(), Instant.now());
		}
	}

	private String tratar(String conteudo, int esperaMs) {
		String normalizado = conteudo.replaceAll("\\s+", " ").trim();
		int caracteres = normalizado.length();
		int palavras = normalizado.isBlank() ? 0 : normalizado.split(" ").length;
		return "Tratado online: %d palavra(s), %d caractere(s), %s. Tempo de fila ~%d ms."
				.formatted(palavras, caracteres, normalizado.toUpperCase(Locale.ROOT), esperaMs);
	}

	private void atualizar(Long id, StatusFluxo status, String resultado, Instant processadoEm) {
		DadoFluxo dado = repository.findById(id).orElseThrow();
		dado.setStatus(status);
		dado.setResultado(resultado);
		if (processadoEm != null) {
			dado.setProcessadoEm(processadoEm);
		}
		repository.save(dado);
		broadcaster.publicar(DadoFluxoResponse.from(dado));
	}
}
