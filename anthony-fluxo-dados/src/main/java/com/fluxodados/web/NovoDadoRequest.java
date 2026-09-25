package com.fluxodados.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoDadoRequest(
		@NotBlank(message = "Informe o seu nome")
		@Size(max = 80)
		String autor,

		@NotBlank(message = "Informe o conteúdo")
		@Size(max = 4000)
		String conteudo
) {
}
