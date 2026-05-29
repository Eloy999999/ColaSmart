package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class AtencionLog {

  /**
   * Identificador unico del usuario.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  private Long userId;
  private String username;

  private Long personalId;
  private String usernamePersonal;

  private LocalDateTime horaEntradaCola;
  private LocalDateTime horaInicioAtencion;
  private LocalDateTime horaFinAtencion;

  private Long colaId;
  private String cola;
  private String lugar;

}
