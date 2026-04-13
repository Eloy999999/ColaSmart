package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An authorized user of the system.
 */
@Entity
@Data
@NoArgsConstructor
@NamedQueries({
    @NamedQuery(name = "User.byUsername", query = "SELECT u FROM User u "
        + "WHERE u.username = :username AND u.enabled = TRUE"),
    @NamedQuery(name = "User.hasUsername", query = "SELECT COUNT(u) "
        + "FROM User u "
        + "WHERE u.username = :username"),
    @NamedQuery(name = "User.topics", query = "SELECT t.key "
        + "FROM Topic t JOIN t.members u "
        + "WHERE u.id = :id")
})
@Table(name = "Usuarios")
public class User implements Transferable<User.Transfer> {

  public enum Role {
    PACIENTE, // pacientes que esperan pacientemente la cola
    ORGANIZADOR, // organizadores de colas
    ADMIN, // admin users
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false)
  private String roles;

  @Column(nullable = true)
  private Integer posicion;

  @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
  private List<Message> received = new ArrayList<>();

  @Column(nullable = true, unique = false)
  private String firstName;
  private String lastName;
  private String lugar;
  private String ocupacion;
  private String turno;
  private Integer tiempoEstimado;
  private Integer prioridad;

  @ManyToMany
  @JoinTable(name = "user_colas")
  private List<Cola> colasAsignadas = new ArrayList<>();

  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
  private List<Turno> turnos = new ArrayList<>();

  // Métodos auxiliares
  public boolean hasRole(Role r) {
    if (roles == null)
      return false;
    return Arrays.asList(roles.split(",")).contains(r.name());
  }

  // Clase Transfer (DTO)
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Transfer {
    private long id;
    private String nombreCompleto;
    private String role;
    private String turno;
  }

  @Override
  public Transfer toTransfer() {
    String nombreCompleto = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    return new Transfer(id, nombreCompleto.trim(), roles, turno);
  }

  // Si el proyecto necesita getReceived() para mensajes:
  // private List<Message> received = new ArrayList<>();
  // public List<Message> getReceived() { return received; }
  /*
   * @Override
   * public Transfer toTransfer() {
   * StringBuilder gs = new StringBuilder();
   * for (Topic g : groups) {
   * gs.append(g.getName()).append(", ");
   * }
   * return new Transfer(id, username, received.size(), sent.size(),
   * gs.toString());
   * }
   * 
   * @Override
   * public String toString() {
   * return toTransfer().toString();
   * }
   */
}
