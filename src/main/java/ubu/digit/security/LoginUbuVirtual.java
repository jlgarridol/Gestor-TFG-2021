package ubu.digit.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase donde se realiza el inicio de seción en el moodle de UbuVirtual con 
 * el usuario y contraseña obtenido en la vista del Login (LoginView)
 * 
 * @author Diana Bringas Ochoa
 */
public class LoginUbuVirtual {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LoginUbuVirtual.class);

	/**
	 * Ruta desde la cual se realizará el login al moodle
	 */
	private static final String HOST_LOGIN_PATH = "/login/index.php";
	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";
	
	private WebService webService;
	private String username;
	private String password;
	private String host;

	/**
	 * Constructor donde se inicializan las variables: host, username y password.
	 * 
	 * @param host
	 * @param username
	 * @param password
	 */
	public LoginUbuVirtual(String host, String username, String password) {
		webService = new WebService();
		this.host = host;
		this.username = username;
		this.password = password;
	}

	/**
	 * Constructor usado tras completar un login mediante SSO, donde el
	 * {@link WebService} ya se ha autenticado (token obtenido a través del
	 * flujo de SSO vía navegador de Moodle) sin necesidad de usuario y contraseña.
	 *
	 * @param host
	 * @param webService servicio web ya autenticado
	 */
	public LoginUbuVirtual(String host, WebService webService) {
		this.host = host;
		this.webService = webService;
	}

	/**
	 * Inicia sesión en el moodle de UbuVirtual con el usuario y contraseña especificados.
	 * 
	 * @throws IOException si no ha podido conectarse o la contraseña es erronea
	 */
	public void normalLogin() throws IOException {
		webService = new WebService(host, username, password);
		String hostLogin = host + HOST_LOGIN_PATH;
		LOGGER.info("Realizando login en el moodle");
		try (Response response = Connection.getResponse(hostLogin)) {
			String redirectedUrl = response.request().url().toString();
			Document loginDoc = Jsoup.parse(response.body().byteStream(), null, hostLogin);
			Element e = loginDoc.selectFirst("input[name=logintoken]");
			String logintoken = (e == null) ? "" : e.attr("value");

			RequestBody formBody = new FormBody.Builder().add("username", username)
					.add("password", password)
					.add("logintoken", logintoken)
					.build();
			String html = Connection.getResponse(new Request.Builder().url(redirectedUrl)
					.post(formBody)
					.build())
					.body()
					.string();

			String sesskey = findSesskey(html);
			if (sesskey != null) {
				webService.setSesskey(sesskey);
			}
		}
	}
	
	/**
	 * Comprueba que la Url sea correcta.
	 * 
	 * @param host
	 * @return url
	 * @throws MalformedURLException
	 */
	public String checkUrlServer(String host) throws MalformedURLException {
		String url = convertToHttps(host);
		URL httpsUrl = new URL(url);
		if (checkWebsService(httpsUrl)) {
			return httpsUrl.toString();
		}

		url = url.replaceFirst(HTTPS, HTTP);
		URL httpUrl = new URL(url);
		if (checkWebsService(httpUrl)) {
			return httpUrl.toString();
		}
		throw new IllegalArgumentException("Error en checkUrlServer " + host);
	}
	
	/**
	 * Verifica si la respuesta obtenida al conectar con el Servicio contiene un mensaje de error.
	 * 
	 * @param url
	 * @return boolean 
	 * 			true si el mensaje obtenido contiene un error, falso si no.	
	 */
	private boolean checkWebsService(URL url) {
		try (Response response = Connection.getResponse(url + "/login/token.php")) {
			JSONObject jsonObject = new JSONObject(response.body()
					.string());
			return jsonObject.has("error");

		} catch (IOException e) {
			LOGGER.info("Error checkWebsService al intentar obtener una respuesta del Servicio Web", e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error en checkWebsService", e);
		}

		return false;
	}
	
	/**
	 * Convertir la ruta en https.
	 * 
	 * @param host
	 * @return url con el host en un determinado formato
	 */
	private String convertToHttps(String host) {
		String url;

		if (!host.matches("^(?i)https?://.*$")) {
			url = HTTPS + host;
		} else if (host.matches("^(?i)http://.*$")) {
			url = host.replaceFirst("(?i)http://", HTTPS);
		} else {
			url = host;
		}
		return url;
	}
	
	/**
	 * Obtiene la Sesskey.
	 * 
	 * @param html
	 * @return Sesskey
	 */
	public String findSesskey(String html) {
		Pattern pattern = Pattern.compile("sesskey=(\\w+)");
		Matcher m = pattern.matcher(html);
		if (m.find()) {
			LOGGER.info("Obtenida Sesskey");
			return m.group(1);
		}
		LOGGER.warn("No se pudo encontrar la clave Sesskey: ", html);
		return null;
	}
	
	/**
	 * Obtiene el servicio web.
	 * 
	 * @return WebService
	 */
	public WebService getWebService() {
		return webService;
	}

	/**
	 * Establece el servicio web.
	 * 
	 * @param webService
	 */
	public void setWebService(WebService webService) {
		this.webService = webService;
	}

	/**
	 * Obtiene el username.
	 * 
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Establece el username.
	 * 
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Obtiene el password.
	 * 
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Establece el password.
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Obtiene el host.
	 * 
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Establece el host.
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}
}
