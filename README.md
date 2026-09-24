[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://gestor-tfg-2021.herokuapp.com)

# GII 20.09 Herramienta web repositorios de TFGII - Gestor TFG/TFM 2021

### Autora
Diana Bringas Ochoa

### Tutores
Álvar Arnaiz González y Carlos López Nozal

### Resumen del proyecto
El propósito del proyecto es evolucionar la aplicación web de Gestor de TFG/TFM del grado de Ingeniería Informática de la Universidad de Burgos. Se llevarán a cabo tanto mejoras funcionales, por ejemplo la incorporación de un nuevo modelo de datos, como mejoras de la interfaz gráfica html. 
Se trata de una aplicación Java, con el uso de componentes gráficos del framework de aplicaciones web Vaaddin https://vaadin.com/home.  

El desarrollo del proyecto será una nueva distribución (fork) del trabajo fin de grado disponible en https://bitbucket.org/ubu_tfg/2014-beatriz.

El proyecto está migrado a Vaadin 14 en la rama **master** y en Vaadin 7 en la rama **Sistinf-V7**.

### Página web
El proyecto está desplegado en https://gestor-tfg-2021.herokuapp.com/ .

### Instalación
* JDK --> https://www.oracle.com/es/java/technologies/javase-jdk11-downloads.html
* Apache Tomcat 9 --> https://tomcat.apache.org/download-90.cgi
* Apache Tomcat como Servidor --> https://vaadin.com/docs/v8/framework/installing/installing-server.html
* IDE Eclipse (Versión 2020-06 R) --> https://vaadin.com/docs/v8/framework/installing/installing-eclipse.html
* Plugin de Vaddin --> Instalar la extensión llamada "Vaadin Plugin for Eclipse" desde el "Eclipse Marketplace".

### Ejecución en local
* Eliminar dependencias --> mvn clean
* Instalar dependencias y compilar --> mvn install
* Instalar en modo producción (despliegue) --> mvn package -Pproduction
* Iniciar app --> mvn spring-boot:run
* Ejecutar los test --> mvn test


