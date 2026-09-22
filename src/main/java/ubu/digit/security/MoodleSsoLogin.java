package ubu.digit.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import ubu.digit.webService.WSFunctionEnum;

/**
 * Inicio de sesión mediante el flujo de SSO vía navegador que expone Moodle
 * para aplicaciones que no pueden autenticar directamente con usuario y
 * contraseña (admin/tool/mobile/launch.php), necesario desde que UbuVirtual
 * exige un inicio de sesión externo (SAML/2FA/certificado).
 *
 * El navegador abre esa URL en una pestaña nueva, el usuario se autentica
 * contra el proveedor de identidad configurado en Moodle y, una vez
 * autenticado, Moodle redirige a "urlscheme://token=..." con el token del
 * servicio web codificado en base64.
 */
public class MoodleSsoLogin {

	private static final String LAUNCH_PATH = "/admin/tool/mobile/launch.php";
	private static final String TOKEN_MARKER = "token=";
	private static final String SEGMENT_SEPARATOR = ":::";

	/**
	 * Construye la URL que hay que abrir en una pestaña nueva para delegar el
	 * login en Moodle.
	 *
	 * @param host      host de Moodle (sin barra final), p.ej. https://ubuvirtual.ubu.es
	 * @param passport  identificador aleatorio de un solo uso que permite validar
	 *                  posteriormente que la respuesta corresponde a esta petición
	 * @param urlScheme esquema de URL personalizado con el que Moodle devolverá el token
	 * @return url de lanzamiento del SSO
	 */
	public String buildLaunchUrl(String host, String passport, String urlScheme) {
		return host + LAUNCH_PATH
				+ "?service=" + urlEncode(WSFunctionEnum.MOODLE_MOBILE_APP.toString())
				+ "&passport=" + urlEncode(passport)
				+ "&urlscheme=" + urlEncode(urlScheme);
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Procesa la respuesta del SSO (la URL "urlscheme://token=..." capturada en
	 * el navegador) y construye un {@link WebService} ya autenticado.
	 *
	 * @param host       host de Moodle usado al construir la URL de lanzamiento
	 * @param passport   passport usado al construir la URL de lanzamiento
	 * @param callbackUrl url capturada tras completar el login en Moodle
	 * @return WebService con el token (y, si procede, el token privado) obtenidos
	 * @throws IllegalAccessError si la respuesta no es válida o no corresponde a esta petición
	 */
	public WebService completeSsoLogin(String host, String passport, String callbackUrl) {
		if (callbackUrl == null) {
			throw new IllegalAccessError("Respuesta de SSO vacía");
		}

		int tokenIndex = callbackUrl.indexOf(TOKEN_MARKER);
		if (tokenIndex < 0) {
			throw new IllegalAccessError("Respuesta de SSO inválida");
		}

		String encodedToken = callbackUrl.substring(tokenIndex + TOKEN_MARKER.length());
		String decoded;
		try {
			decoded = new String(Base64.getDecoder().decode(encodedToken), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new IllegalAccessError("No se ha podido decodificar el token de SSO");
		}

		String[] parts = decoded.split(SEGMENT_SEPARATOR);
		if (parts.length < 2) {
			throw new IllegalAccessError("Respuesta de SSO con formato inesperado");
		}

		String siteId = parts[0];
		String token = parts[1];
		String privateToken = parts.length > 2 ? parts[2] : null;

		String expectedSiteId = md5(host + passport);
		if (!expectedSiteId.equalsIgnoreCase(siteId)) {
			throw new IllegalAccessError("El token de SSO no corresponde a esta sesión");
		}

		WebService webService = new WebService();
		webService.setData(host, token, privateToken);
		return webService;
	}

	private String md5(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 no disponible", e);
		}
	}
}
