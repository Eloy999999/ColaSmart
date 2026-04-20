package es.ucm.fdi.iw.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    List<User> findByPosicionBetweenOrderByPosicionDesc(int min, int max); // Encontrar los users entre min y max

    User findByPosicion(int posicion);

}
