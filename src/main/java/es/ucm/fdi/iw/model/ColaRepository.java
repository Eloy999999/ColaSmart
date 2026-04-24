package es.ucm.fdi.iw.model;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaRepository extends JpaRepository<Cola, Long> {
    Optional<Cola> findByQrToken(String qrToken);
}