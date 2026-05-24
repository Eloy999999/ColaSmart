# ColaSmart
## Descripción

ColaSmart es una plataforma web para gestionar turnos y colas virtuales en servicios presenciales como secretarías, tutorías o laboratorios. Los usuarios se ponen en cola escaneando un QR desde su móvil, reciben notificaciones en tiempo real y evitan esperas innecesarias. El personal organiza la atención y prioriza casos. Menos colas físicas, menos estrés y una experiencia más eficiente para todos.

## Roles y tareas

**• Administrador**: usuario con control del sistema.

Lo maneja todo; las colas, al personal de atención y a los usuarios que esperan en las colas. 

Para cada cola puede ver su ID, el personal que está asignado, el tiempo estimado por consulta en esa cola, el aforo máximo y su área o sala. Además, puede hacer dos acciones, que son borrarla y modificarla. Al modificarla, se pueden cambiar todos los aspectos mencionados anteriormente que se pueden ver de la cola, además de cuando empieza y cuando termina. Por último, respecto a colas, también puede crear colas, estableciendo los parámetros mencionados desde el principio en la nueva cola a crear.

Para cada miembro del personal, y eso incluye tambien todos los admins, se puede ver su ID, su nombre completo real, su nombre de usuario y su rol (Admin u organizador). También como acciones sobre cada miembro, puede asignarle colas, eliminarlo y editarlo, donde se puede cambiar su nombre, apellidos, rol, nombre de usuario y contraseña. También se pueden crear nuevos miembros de personal, pudiendo ya ponerle desde el inicio todo lo que se puede editar del personal. Además, al crear un nuevo miembro del personal, al presionar el botón "Lorem", se autorrellenan los campos de nombre, apellidos y rol.

Para cada usuario esperando en alguna cola, se puede ver su ID, su usuario (caracteres aleatorios para asegurar el anonimato), ID de la cola en el que está el usuario, el nombre de la cola en la que está y su posición en la cola. Esta es relativa al turno que se está atendiendo; si ya fue atendido, aparece negativo, de menos a más negativo el que fue atendido después a antes, y se guardan para tener un registro de qué usuarios y en qué colas estuvieron. Si su posición es la 0, está siendo atendido justo, y si es positivo, es su puesto en la cola. Se pueden llevar a cabo dos acciones: poner primero en la cola a un usuario que esté esperando y no haya sido o esté siendo atendido y eliminar a un usuario.

A parte de lo anterior, los administradores también pueden hacer seguimiento de todas las colas (estén o no asignados). Como el seguimiento de las colas es la función principal del rol de los trabajadores, se explicará cómo es el seguimiento de las colas dentro del rol de personal de atención.

**• Personal de atención**: responsable de la gestión de turnos en una cola específica.

Tiene asignada colas, desde cero hasta las que se le asignen, pueden ser muchas las colas asignadas, a las cuales a parte de ver toda la información sobre los datos de cada cola, puede abrir ocerrarla, también puede hacer que se muestre el panel qr de una cola a la que luego los usuarios la escanean, y puede ver los detalles del seguimiento de la cola, los cuales se citan a continuación.

En el seguimiento de una cola, se puede ver qué usuario está siendo atendido, a qué hora se empezó a atender y cuánto lleva siendo atendido. También se puede ver la última persona que fue atendida, su hora de inicio de atención, la de fin de atención y el tiempo total que fue atendido. También se puede ver la lista de espera, donde se muestran los usuarios que están esperando a ser atendidos, y su número de turno. Cuando el miembro del personal esté listo, podrá atender al siguiente cliente de la cola que está haciendo seguimiento, en el puesto que esté (el puesto puede editarse, por si el trabajador va a puestos distintos).

**• Usuario/Cliente**: Persona que solicita turno y está interesada en un servicio.

El usuario verá en pantalla la lista de los seis últimos usuarios que fueron llamados, el usuario que está siendo atendido y un código QR. Cuando el usuario lo escanee, este verá una nueva vista con información del tipo de servicio al que se metío en la cola, así como su posición en la cola y el tiempo estimado de espera hasta que el usuario sea atendido. En grande puede ver su código, de manera que pueda conservar su anonimato, al ser difícil de memorizar, y en cualquier momento podrá abandonar la cola, en caso de que ya no esté interesado en esperar allí.

## Vistas de la aplicación

Al iniciar la web, se mostrará una vista inicial que mostrará información de la aplicación. Arriba se verá la pestaña de PanelQR, donde se muestra la información que ve el usuario con el qr antes de escanearlo, y al escanearlo, ve en la siguiente vista la información descrita anteriormente. Para ver el resto, hace falta hacer login, metiendo un nombre de usuario y una contraseña, se puede iniciar sesión como organizador, que a parte de lo anterior, puede entrar a la vista de seguimiento de colas para hacer seguimiento de colas y ver todo lo descrito anteriormente en la explicación del rol de personal de atención. Por último, si se inicia sesión como un usuario administrador, además de todo lo anterior, tendrá acceso a la vista de panel de administración, donde podrá ver, modificar, borrar o crear todo lo que puede hacer un administrador, descrito en los párrafos que explican el papel de este último rol.

## Lista de usuarios en la BBDD

Se facilitan datos de sesión de un trabajador y un administrador, de tal forma que se pueda probar todo lo que pueden hacer.

**• Administrador**: 
Usuario: a
Contraseña : aa
**• Personal de atención**:
Usuario: b
Contraseña : aa

## Estado actual de la aplicación

La aplicación cuenta con todo lo mencionado anteriormente hasta este punto implementado y se encuentra funcional. Para trabajo futuro, se pueden hacer varias cosas, como controlar que un usuario no se va de una cola y vuelve a los 3 minutos antes de que le toque, o un mismo trabajador pueda atender en simultáneo más de una cola.
