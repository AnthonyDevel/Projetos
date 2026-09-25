package com.fluxodados.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fluxodados.domain.DadoFluxo;

public interface DadoFluxoRepository extends JpaRepository<DadoFluxo, Long> {

	List<DadoFluxo> findAllByOrderByCriadoEmDesc();
}
