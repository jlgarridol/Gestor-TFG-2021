package ubu.digit.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase Controlador a través de la cual se realizará el inicio de sesión en el moodle de UbuVirtual.
 * Solamente tiene una instancia (Singleton)
 * 
 * @author Diana Bringas Ochoa
 */
public class Controller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	
	/**
	 * Host
	 */
	private URL host;
	
	/**
	 * Usuario (email)
	 */
	private String username;
	
	/**
	 * Contraseña del usuario
	 */
	private String password;
	
	/**
	 * Login con el moodle de UbuVirtual
	 */
	private LoginUbuVirtual login;

	/**
	 * La única instacia de la clase Controller.
	 */
	private static Controller instance;

	/**
	 * Constructor de la clase singleton.
	 */
	private Controller() {
	}

	/**
	 * Devuelve la instancia única de Controller.
	 * 
	 * @return instancia singleton
	 */
	public static Controller getInstance() {
		if (instance == null) {
			instance = new Controller();
		}
		return instance;
	}
	
	/**
	 * Realiza el login en el moodle de UbuVirtual.
	 * @param host
	 * @param username
	 * @param password
	 * @throws IOException si no se ha podido realizar el login con el usuario y contraseña pasados
	 */
	public void loginMoodleUbuVirtual(String host, String username, String password) throws IOException {
		LOGGER.info("Iniciando sesión con el moodle de ubu");
		login = new LoginUbuVirtual(host, username, password);
		String validHost = login.checkUrlServer(host);
		setLogin(login);
		login.setUsername(username);
		login.normalLogin();
		this.host = new URL(validHost);
	}

	/**
	 * Registra en el controlador una sesión iniciada mediante el flujo de SSO
	 * vía navegador de Moodle, para la cual ya se dispone de un
	 * {@link WebService} autenticado (ver {@link MoodleSsoLogin}).
	 *
	 * @param host
	 * @param webService servicio web ya autenticado mediante SSO
	 * @throws MalformedURLException si el host no es una URL válida
	 */
	public void loginMoodleUbuVirtualSso(String host, WebService webService) throws MalformedURLException {
		LOGGER.info("Iniciando sesión SSO con el moodle de ubu");
		login = new LoginUbuVirtual(host, webService);
		setLogin(login);
		this.host = new URL(host);
	}

	/**
	 * Devuelve el host.
	 * 
	 * @return the host
	 */
	public URL getUrlHost() {
		return host;
	}

	/**
	 * Establece el host.
	 * 
	 * @param host
	 */
	public void setURLHost(URL host) {
		this.host = host;
	}

	/**
	 * Obtiene la contraseña.
	 * 
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Establece la contraseña.
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Obtiene el usuario.
	 * 
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Establece el usuario. 
	 * 
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Obtiene el servicio web.
	 * 
	 * @return the webService
	 */
	public WebService getWebService() {
		return login.getWebService();
	}

	/**
	 * Obtiene el login.
	 * 
	 * @return login
	 */
	public LoginUbuVirtual getLogin() {
		return login;
	}

	/***
	 * Establece el login.
	 * 
	 * @param login
	 */
	public void setLogin(LoginUbuVirtual login) {
		this.login = login;
	}
}
