package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    USER, // normal users
    ADMIN, // admin users
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id; //autogenerado

  @Column(nullable = false)
  private Role role; // split by ',' to separate roles

  //Trabajadores (ADMIN)
  private String firstName;
  private String lastName;
  private String lugar;
  private String ocupacion;


  //Clientes (USER)
  @Column(nullable = false, unique = true)
  private String turno; //Se genera con el QR
  private int tiempoEstimado;
  private boolean prioridad;

  //Colas asignadas a un trabajador
   @ManyToMany
    @JoinTable(name = "user_colas")  // Trabajadores manejan colas
    private List<Cola> colasAsignadas = new ArrayList<>();

  //Un cliente puede tener varios turnos
  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Turno> turnos = new ArrayList<>(); 
    
/* 
  @Override
  public Transfer toTransfer() {
    StringBuilder gs = new StringBuilder();
    for (Topic g : groups) {
      gs.append(g.getName()).append(", ");
    } 
    return new Transfer(id, username, received.size(), sent.size(), gs.toString());
  }

  @Override
  public String toString() {
    return toTransfer().toString();
  } */
}
