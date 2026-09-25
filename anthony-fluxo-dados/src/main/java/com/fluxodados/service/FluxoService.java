package com.fluxodados.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fluxodados.domain.DadoFluxo;
import com.fluxodados.domain.StatusFluxo;
import com.fluxodados.repo.DadoFluxoRepository;
import com.fluxodados.web.DadoFluxoResponse;
import com.fluxodados.web.NovoDadoRequest;

@Service
public class FluxoService {

	private final DadoFluxoRepository repository;
	private final FluxoBroadcaster broadcaster;
	private final FluxoProcessor processor;

	public FluxoService(DadoFluxoRepository repository, FluxoBroadcaster broadcaster, FluxoProcessor processor) {
		this.repository = repository;
		this.broadcaster = broadcaster;
		this.processor = processor;
	}

	@Transactional
	public DadoFluxoResponse enfileirar(NovoDadoRequest request) {
		DadoFluxo dado = new DadoFluxo();
		dado.setAutor(request.autor().trim());
		dado.setConteudo(request.conteudo().trim());
		dado.setStatus(StatusFluxo.PENDENTE);
		dado.setCriadoEm(Instant.now());
		dado.setResultado("Na fila de processamento.");
		DadoFluxo salvo = repository.save(dado);
		DadoFluxoResponse resposta = DadoFluxoResponse.from(salvo);
		broadcaster.publicar(resposta);

		Long id = salvo.getId();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				processor.processar(id);
			}
		});
		return resposta;
	}

	public List<DadoFluxoResponse> listar() {
		return repository.findAllByOrderByCriadoEmDesc().stream()
				.map(DadoFluxoResponse::from)
				.toList();
	}
}
