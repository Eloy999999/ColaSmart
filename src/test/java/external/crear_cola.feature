Feature: Prueba base de datos lista de salas (colas)

Background:
    * configure driver = { type: 'chrome' }

#
# Admin crea una cola nueva
#
Scenario: crear cola desde panel de administración

    # login como admin
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/admin')

    # ir a manejar colas
    When click("a[href='/admin/colas']")
    Then waitForText('Gestión de Colas')

    # crear nueva cola
    When click("{button}Crear Cola")
    Then waitForText('Configuración Cola')

    And input('#nombreServicio', 'Cola Test')
    And input('#tiempoEstimado', '10')
    And input('#aforoMaximo', '5')
    And input('#ubicacion', 'Sala 1')
    And input('#horaInicio', '09:00')
    And input('#horaFin', '18:00')

    When submit().click("{button}Crear Cola")

    # verificar que aparece en la lista (confirmación BD)
    Then waitForText('Cola Test')


#
# Comprobación de error si faltan campos
#
Scenario: crear cola con campos vacíos

    Given driver baseUrl + '/admin/colas/nueva'

    And input('#nombreServicio', '')
    And input('#tiempoEstimado', '')
    And input('#aforoMaximo', '')
    And input('#ubicacion', '')
    And input('#horaInicio', '')
    And input('#horaFin', '')

    When submit().click("{button}Crear Cola")

    Then match html('.error') contains 'Falta campo'

# Horario de fin es anterior al de inicio
Scenario: error horario incorrecto

    Given driver baseUrl + '/admin/colas/nueva'

    And input('#nombreServicio', 'Cola Test')
    And input('#horaInicio', '18:00')
    And input('#horaFin', '09:00')

    When submit().click("{button}Crear Cola")

    Then match html('.error') contains 'Horario inválido'

#
# Dos clientes entran en la cola mediante URL
#
Scenario: clientes entran en cola

    Given driver baseUrl + '/cola/1/entrar?usuario=cliente1'
    Then waitForText('Te has unido a la cola')

    Given driver baseUrl + '/cola/1/entrar?usuario=cliente2'
    Then waitForText('Te has unido a la cola')

#
# Personal accede a cola y llama siguiente
#
Scenario: personal atiende cola

    # login personal
    Given driver baseUrl + '/login'
    And input('#username', 'personal')
    And input('#password', 'aa')
    When submit().click(".form-signin button")

    Then waitForUrl(baseUrl + '/colas')

    # seleccionar la cola creada
    When click("{text}Cola Test")

    Then waitForText('Seguimiento de Cola')

    # llamar siguiente usuario
    When click("{button}Llamar siguiente")

    # comprobar que se actualiza persona atendida
    Then waitForText('Persona actualmente atendida')


#
# Llamar siguiente cuando la cola está vacía
#
Scenario: llamar siguiente con cola vacía

    Given driver baseUrl + '/colas/1'

    When click("{button}Llamar siguiente")

    Then match html('.estado-cola') contains 'Cola vacía'