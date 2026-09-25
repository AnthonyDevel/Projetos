package com.fluxodados.web;

import java.time.Instant;

import com.fluxodados.domain.DadoFluxo;
import com.fluxodados.domain.StatusFluxo;

public record DadoFluxoResponse(
		Long id,
		String autor,
		String conteudo,
		StatusFluxo status,
		String resultado,
		Instant criadoEm,
		Instant processadoEm
) {
	public static DadoFluxoResponse from(DadoFluxo dado) {
		return new DadoFluxoResponse(
				dado.getId(),
				dado.getAutor(),
				dado.getConteudo(),
				dado.getStatus(),
				dado.getResultado(),
				dado.getCriadoEm(),
				dado.getProcessadoEm()
		);
	}
}
