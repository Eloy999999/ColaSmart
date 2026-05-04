Feature: Prueba base de datos lista de salas (colas)

Background:
    * configure driver = { type: 'chrome' }

#
# Admin crea una cola nueva
#

    @login_a
    Scenario: login correcto como admin
    Given driver baseUrl + '/login'
    And input('#username', 'a')
    And input('#password', 'aa')
    When click(".form-signin button")
    Then waitForUrl(baseUrl + '/panelAdmin')

    @login_b
    Scenario: login correcto como personal
        Given driver baseUrl + '/login'
        And input('#username', 'b')
        And input('#password', 'aa')
        When click(".form-signin button")
        Then waitForUrl(baseUrl + '/seguimientoCola')
    
    Scenario: login malo en plantilla
        Given driver baseUrl + '/login'
        And input('#username', 'dummy')
        And input('#password', 'world')
        When click(".form-signin button")
        Then match html('.error') contains 'Error en nombre de usuario o contraseña'

    Scenario: logout after login
        Given driver baseUrl + '/login'
        And input('#username', 'a')
        And input('#password', 'aa')
        When click(".form-signin button")
        Then waitForUrl(baseUrl + '/panelAdmin')
        When click("{button}logout")
        Then waitForUrl(baseUrl + '/login')
