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
 * Entidad que representa a un usuario del sistema.
 * 
 * Un usuario puede ser:
 * - paciente
 * - organizador
 * - administrador
 * 
 * Tambien almacena informacion relacionada con:
 * - colas asignadas
 * - turnos
 * - mensajes recibidos
 * - datos personales
 */
@Entity
@Data
@NoArgsConstructor
@NamedQueries({

    /**
     * Busca un usuario activo por username.
     */
    @NamedQuery(name = "User.byUsername", query = "SELECT u FROM User u "
        + "WHERE u.username = :username AND u.enabled = TRUE"),

    /**
     * Comprueba si ya existe un username en la BD.
     */
    @NamedQuery(name = "User.hasUsername", query = "SELECT COUNT(u) "
        + "FROM User u "
        + "WHERE u.username = :username"),

    /**
     * Obtiene las claves de los topics a los que pertenece el usuario.
     */
    @NamedQuery(name = "User.topics", query = "SELECT t.key "
        + "FROM Topic t JOIN t.members u "
        + "WHERE u.id = :id")
})
@Table(name = "Usuarios")
public class User implements Transferable<User.Transfer> {

  /**
   * Roles disponibles dentro del sistema.
   */
  public enum Role {
    PACIENTE, // pacientes que esperan pacientemente la cola
    ORGANIZADOR, // organizadores de colas
    ADMIN, // admin users
  }

  /**
   * Identificador unico del usuario.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  /**
   * Nombre de usuario unico.
   */
  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean enabled;

  /**
   * Roles del usuario almacenados como texto.
   * 
   * Puede contener varios roles separados por comas.
   */
  @Column(nullable = false)
  private String roles;

  @Column(nullable = true)
  private Integer posicion;

  /**
   * Lista de mensajes recibidos por el usuario.
   * 
   * Relacion uno a muchos con Message.
   */
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

  // Añadido como práctica para el examen
  private Integer numVecesLogin;

  @ManyToMany
  @JoinTable(name = "user_colas")
  private List<Cola> colasAsignadas = new ArrayList<>();

  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
  private List<Turno> turnos = new ArrayList<>();

  // Métodos auxiliares

  /**
   * Comprueba si el usuario posee un rol concreto.
   *
   * @param r rol a comprobar
   * @return true si el usuario tiene ese rol
   */
  public boolean hasRole(Role r) {
    if (roles == null)
      return false;
    return Arrays.asList(roles.split(",")).contains(r.name());
  }

  // Clase Transfer (DTO)
  /**
   * Clase DTO utilizada para transferir datos del usuario
   * hacia el frontend o respuestas JSON.
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Transfer {
    private long id;
    private String nombreCompleto;
    private String role;
    private String turno;
  }

  /**
   * Convierte la entidad User en un DTO Transfer.
   *
   * @return objeto Transfer serializable
   */
  @Override
  public Transfer toTransfer() {
    String nombreCompleto = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    return new Transfer(id, nombreCompleto.trim(), roles, turno);
  }

  public Integer getPosicion() { return posicion; }
  public void setPosicion(Integer posicion) { this.posicion = posicion; }

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
