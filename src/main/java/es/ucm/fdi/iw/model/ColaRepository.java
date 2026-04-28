package es.ucm.fdi.iw.model;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColaRepository extends JpaRepository<Cola, Long> {
    Optional<Cola> findByQrToken(String qrToken);

    List<Cola> findByTrabajadores_Id(Long id);
}