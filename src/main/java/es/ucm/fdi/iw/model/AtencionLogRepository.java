package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtencionLogRepository extends JpaRepository<AtencionLog, Long> {

    List<AtencionLog> findTop50ByOrderByHoraEntradaColaDesc();

    Optional<AtencionLog> findByUserId(long id);

    Optional<AtencionLog> findTopByPersonalIdAndHoraFinAtencionIsNull(long id);

    List<AtencionLog> findTop50ByHoraInicioAtencionIsNotNullAndHoraFinAtencionIsNullOrderByHoraEntradaColaDesc();

    long countByHoraFinAtencionIsNotNull();

    long countByHoraFinAtencionIsNotNullAndHoraFinAtencionAfter(LocalDateTime desde);

    @Query("""
        SELECT l
        FROM AtencionLog l
        WHERE l.personalId IS NOT NULL
          AND l.horaInicioAtencion IS NOT NULL
          AND l.horaFinAtencion IS NOT NULL
        ORDER BY l.horaInicioAtencion DESC
    """)
    List<AtencionLog> findHistorialAtencionesTerminadas();

    @Query("""
        SELECT 
            l.personalId AS personalId,
            l.usernamePersonal AS usernamePersonal,
            COUNT(l) AS total,
            SUM(CASE WHEN l.horaFinAtencion >= :desde THEN 1 ELSE 0 END) AS ultimas24h
        FROM AtencionLog l
        WHERE l.personalId IS NOT NULL
          AND l.horaFinAtencion IS NOT NULL
        GROUP BY l.personalId, l.usernamePersonal
        ORDER BY total DESC
    """)
    List<ResumenGestor> resumenPorGestor(@Param("desde") LocalDateTime desde);

    interface ResumenGestor {
        Long getPersonalId();
        String getUsernamePersonal();
        Long getTotal();
        Long getUltimas24h();
    }
}