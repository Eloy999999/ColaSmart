package es.ucm.fdi.iw.model;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

import lombok.Data;

/**
 * A group of users, with an associated chat.
 */
@Data
@Entity
public class Cola {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  private String nombre;
  private int capacidad;

  @ManyToOne
  private User encargado;

  @OneToMany
  private User listaClientes;
  private Time horario;
  private String lugar;
  private String turnoActual;
  private Boolean estado;
}
