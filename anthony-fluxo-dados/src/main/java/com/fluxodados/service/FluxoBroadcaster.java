package com.fluxodados.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fluxodados.web.DadoFluxoResponse;

@Component
public class FluxoBroadcaster {

	private final List<SseEmitter> clientes = new CopyOnWriteArrayList<>();

	public SseEmitter inscrever() {
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
		clientes.add(emitter);
		emitter.onCompletion(() -> clientes.remove(emitter));
		emitter.onTimeout(() -> clientes.remove(emitter));
		emitter.onError(ex -> clientes.remove(emitter));
		return emitter;
	}

	public void publicar(DadoFluxoResponse dado) {
		for (SseEmitter emitter : clientes) {
			try {
				emitter.send(SseEmitter.event().name("fluxo").data(dado));
			} catch (IOException ex) {
				clientes.remove(emitter);
			}
		}
	}
}
