package es.ucm.fdi.iw.model;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    List<User> findByPosicionBetweenOrderByPosicionDesc(int min, int max); // Encontrar los users entre min y max

    User findByPosicion(int posicion);

    @Query("""
        SELECT DISTINCT u
        FROM Cola c
        JOIN c.listaClientes u
        WHERE c.id = :colaId
          AND u.posicion = :posicion
    """)
    Optional<User> findTurnoActualByColaId(
        @Param("colaId") Long colaId,
        @Param("posicion") int posicion
    );

    @Query("""
        SELECT u
        FROM Cola c
        JOIN c.listaClientes u
        WHERE c.id = :colaId
          AND u.posicion BETWEEN :minPos AND :maxPos
        ORDER BY u.posicion DESC
    """)
    List<User> findAtendidosByColaId(
        @Param("colaId") Long colaId,
        @Param("minPos") int minPos,
        @Param("maxPos") int maxPos
    );
}
