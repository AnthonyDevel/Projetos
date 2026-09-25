package com.fluxodados.web;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fluxodados.service.FluxoBroadcaster;
import com.fluxodados.service.FluxoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dados")
public class FluxoController {

	private final FluxoService fluxoService;
	private final FluxoBroadcaster broadcaster;
	private final ScheduledExecutorService sseHeartbeat;

	public FluxoController(
			FluxoService fluxoService,
			FluxoBroadcaster broadcaster,
			ScheduledExecutorService sseHeartbeat
	) {
		this.fluxoService = fluxoService;
		this.broadcaster = broadcaster;
		this.sseHeartbeat = sseHeartbeat;
	}

	@GetMapping
	public List<DadoFluxoResponse> listar() {
		return fluxoService.listar();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DadoFluxoResponse inserir(@Valid @RequestBody NovoDadoRequest request) {
		return fluxoService.enfileirar(request);
	}

	@GetMapping("/stream")
	public SseEmitter stream() {
		SseEmitter emitter = broadcaster.inscrever();
		var heartbeat = sseHeartbeat.scheduleAtFixedRate(() -> {
			try {
				emitter.send(SseEmitter.event().comment("ping"));
			} catch (Exception ignored) {
				// o emissor é removido no onError/onCompletion
			}
		}, 15, 15, TimeUnit.SECONDS);
		emitter.onCompletion(() -> heartbeat.cancel(false));
		emitter.onTimeout(() -> heartbeat.cancel(false));
		emitter.onError(ex -> heartbeat.cancel(true));
		return emitter;
	}
}
