package com.fluxodados.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dados_fluxo")
public class DadoFluxo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String autor;

	@Column(nullable = false, length = 4000)
	private String conteudo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusFluxo status = StatusFluxo.PENDENTE;

	@Column(length = 4000)
	private String resultado;

	@Column(nullable = false)
	private Instant criadoEm = Instant.now();

	private Instant processadoEm;

	public Long getId() {
		return id;
	}

	public String getAutor() {
		return autor;
	}

	public void setAutor(String autor) {
		this.autor = autor;
	}

	public String getConteudo() {
		return conteudo;
	}

	public void setConteudo(String conteudo) {
		this.conteudo = conteudo;
	}

	public StatusFluxo getStatus() {
		return status;
	}

	public void setStatus(StatusFluxo status) {
		this.status = status;
	}

	public String getResultado() {
		return resultado;
	}

	public void setResultado(String resultado) {
		this.resultado = resultado;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(Instant criadoEm) {
		this.criadoEm = criadoEm;
	}

	public Instant getProcessadoEm() {
		return processadoEm;
	}

	public void setProcessadoEm(Instant processadoEm) {
		this.processadoEm = processadoEm;
	}
}
