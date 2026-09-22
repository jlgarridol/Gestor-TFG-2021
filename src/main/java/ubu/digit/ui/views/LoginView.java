package ubu.digit.ui.views;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ubu.digit.security.*;
import ubu.digit.util.Constants;
import ubu.digit.util.UtilMethods;
import ubu.digit.webService.CoreCourseGetUserAdministrationOptions;
import ubu.digit.webService.CoreWebserviceGetSiteInfo;
import ubu.digit.ui.entity.Course;
import ubu.digit.ui.entity.MoodleUser;
import ubu.digit.ui.components.Footer;
import ubu.digit.ui.components.NavigationBar;

/**
 * Vista de inicio de sesión.
 *
 * UbuVirtual exige actualmente un inicio de sesión externo (SSO con doble
 * factor / certificado electrónico), por lo que ya no es posible validar al
 * usuario enviando directamente su usuario y contraseña. En su lugar, el
 * login se delega en el navegador: se abre una pestaña nueva con la URL de
 * SSO de Moodle (admin/tool/mobile/launch.php), el usuario se autentica ahí
 * con el mecanismo que tenga configurado UbuVirtual, y Moodle devuelve un
 * token de servicio web mediante un esquema de URL personalizado que esta
 * vista intercepta (ver {@link SsoCallbackView}) para completar el login.
 *
 * @author Diana Bringas Ochoa
 */
@Route(value = "Login")
@PageTitle("Login ")
public class LoginView extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	/**
	 * Logger de la clase.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(LoginView.class.getName());

	/**
	 * Nombre de la vista.
	 */
	public static final String VIEW_NAME = "login";

	/**
	 * Controlador
	 */
	private static Controller CONTROLLER;

	/**
	 * Url del host de UbuVirtual
	 */
	private static final String HOST = "https://ubuvirtual.ubu.es";

	/**
	 * Esquema de URL personalizado con el que Moodle devuelve el token de SSO.
	 * Debe empezar por "web+" para que los navegadores permitan registrarlo
	 * como manejador de protocolo desde una página web.
	 */
	private static final String SSO_URL_SCHEME = "web+ubutfg";

	/**
	 * Mensaje de error que se mostrará en el login
	 * en caso de no poder acceder.
	 */
	public static ErrorMessage errorMessage;

	/**
	 * Componente donde se muestra el estado/errores del login.
	 */
	private final Span statusLabel = new Span();

	/**
	 * Passport de un solo uso generado para la petición de SSO en curso, usado
	 * para validar que la respuesta recibida corresponde a esa petición.
	 */
	private String pendingPassport;

	/**
	 * Constructor donde se crea el login
	 */
	public LoginView() {

		addClassName("login-view");
		setMargin(true);
		setSpacing(true);
		setSizeFull();

		NavigationBar bat = new NavigationBar();
		add(bat);

		//Se crea la instancia del controlador
		CONTROLLER = Controller.getInstance();

		H3 title = new H3("Gestor-TFG-2021");
		Paragraph description = new Paragraph(
				"Inicie sesión con su cuenta de UBUVirtual. Se abrirá una pestaña nueva para autenticarse.");

		Button ssoButton = new Button("Iniciar sesión con UBUVirtual", e -> startSsoLogin());

		statusLabel.getStyle().set("color", "var(--lumo-error-text-color)");

		add(title, description, ssoButton, statusLabel);

		Footer footer = new Footer(null);
		add(footer);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		// Se (re)registra el listener de mensajes del callback de SSO apuntando
		// a esta instancia, sustituyendo cualquier listener de una vista anterior.
		getElement().executeJs(
				"const el = this;"
				+ "if (window.__ubuSsoListener) { window.removeEventListener('message', window.__ubuSsoListener); }"
				+ "window.__ubuSsoListener = function(event) {"
				+ "  if (event.origin !== window.location.origin) { return; }"
				+ "  if (!event.data || event.data.type !== 'ubu-sso-token') { return; }"
				+ "  el.$server.onSsoToken(event.data.token);"
				+ "};"
				+ "window.addEventListener('message', window.__ubuSsoListener);");
	}

	/**
	 * Genera un passport de un solo uso, abre una pestaña nueva con la URL de
	 * SSO de Moodle y registra (si el navegador lo permite) un manejador para
	 * el esquema de URL personalizado con el que Moodle devolverá el token.
	 */
	private void startSsoLogin() {
		statusLabel.setText("");
		pendingPassport = UUID.randomUUID().toString();
		String ssoUrl = new MoodleSsoLogin().buildLaunchUrl(HOST, pendingPassport, SSO_URL_SCHEME);

		getElement().executeJs(
				"try {"
				+ "  navigator.registerProtocolHandler($0, window.location.origin + '/sso-callback?url=%s');"
				+ "} catch (err) {"
				+ "  console.warn('No se pudo registrar el gestor de protocolo para el SSO', err);"
				+ "}"
				+ "window.open($1, '_blank');",
				SSO_URL_SCHEME, ssoUrl);

		statusLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
		statusLabel.setText("Complete el inicio de sesión en la pestaña nueva. Puede que su navegador le pida "
				+ "permiso para \"abrir enlaces de este tipo\" la primera vez; acéptelo para poder continuar.");
	}

	/**
	 * Invocado desde el navegador cuando se ha capturado la respuesta del SSO
	 * de Moodle (ver {@link SsoCallbackView}). Completa el login con el token
	 * recibido y, si el usuario tiene acceso a la asignatura de TFG con
	 * permisos de actualización, navega a {@link UploadView}.
	 *
	 * @param callbackUrl url "urlscheme://token=..." devuelta por Moodle
	 */
	@ClientCallable
	public void onSsoToken(String callbackUrl) {
		if (pendingPassport == null) {
			return;
		}
		String passport = pendingPassport;
		pendingPassport = null;

		try {
			WebService webService = new MoodleSsoLogin().completeSsoLogin(HOST, passport, callbackUrl);
			CONTROLLER.loginMoodleUbuVirtualSso(HOST, webService);
		} catch (IllegalAccessError | MalformedURLException e) {
			LOGGER.error("Error al completar el login SSO", e);
			showError("No se ha podido iniciar sesión",
					"El proceso de autenticación con UBUVirtual no se ha completado correctamente. Vuelva a intentarlo.");
			return;
		}

		if (Boolean.TRUE.equals(completeLogin())) {
			LOGGER.info("Usuario validado mediante SSO");
			UI.getCurrent().navigate(UploadView.class);
		} else {
			showError(errorMessage != null ? errorMessage.getTitle() : "No se ha podido iniciar sesión",
					errorMessage != null ? errorMessage.getMessage() : "Verifique que su usuario tiene acceso.");
		}
	}

	/**
	 * Comprueba si el usuario autenticado (ya con un {@link WebService} válido
	 * en {@link Controller}) tiene la asignatura correspondiente al TFG y
	 * permisos de actualización (update) en dicha asignatura.
	 *
	 * Si todos estos casos se cumplen entonces se retorna true, con lo que se
	 * autentificará el usuario y se permitirá ir a la vista de UploadView.
	 *
	 * @return boolean
	 * 			True, si el usuario posee la asignatura de TFG y tiene permisos de actualización
	 * 			False en caso contrario.
	 */
	private Boolean completeLogin() {
		try {
			String validUsername = UtilMethods.getJSONObjectResponse(CONTROLLER.getWebService(), new CoreWebserviceGetSiteInfo())
					.getString(Constants.USERNAME);
			PopulateMoodleUser populateMoodleUser = new PopulateMoodleUser(CONTROLLER.getWebService());
			MoodleUser moodleUser = populateMoodleUser.populateMoodleUser(validUsername, CONTROLLER.getUrlHost().toString());

			LOGGER.info("Obteniendo información del usuario: " + moodleUser.getFullName());

			//Creacion instancia de CreateCourse desde la cual se accedera a los metodos de obtención de los cursos y permisos
			CreateUserCourses createUserCourses = new CreateUserCourses(CONTROLLER.getWebService());

			//Se obtienen los cursos del usuario y se guardan en el usuario (MoodleUser)
			List<Course> userCourses = createUserCourses.getUserCourses(moodleUser.getId());
			moodleUser.setCourses(userCourses);

			//Se comprueba si el usuario tiene la asignatura correspondiente al Trabajo de Fin de Grado.
			//Si no la tiene, se impide inciar seción.
			Course courseTFG = createUserCourses.checkCourseTFG(userCourses);
			if (courseTFG == null) {
				createErrorLogin("Usuario sin acceso", "El usuario no cuenta con la asignatura de Trabajos de Final de Grado");
				return false;
			}

			//Se obtiene los id de los cursos del moodleUser con los que se buscará los permisos del usuario en la asignatura
			Collection<Integer> courseids = moodleUser.getCourses().stream().map(Course::getId).collect(Collectors.toList());
			LOGGER.info("Cursos del usuario: " + courseids);
			Collection<Integer> idTFG = new ArrayList<Integer>();
			Iterator<Integer> it = courseids.iterator();
			while (it.hasNext()) {
				int id = it.next();
				if (id == 2204 || id == 11707) {
					idTFG.add(id);
				}
			}
			LOGGER.info("Curso TFG del usuario: ID--> " + idTFG);

			JSONArray jsonArray;
			jsonArray = UtilMethods.getJSONObjectResponse(CONTROLLER.getWebService(), new CoreCourseGetUserAdministrationOptions(idTFG))
					.getJSONArray(Constants.COURSES);
			boolean hasPermission = createUserCourses.findPermission(jsonArray, courseTFG, "update");
			if (hasPermission) {
				CONTROLLER.setUsername(validUsername);
			}
			return hasPermission;

		} catch (Exception e) {
			LOGGER.error("Error al recuperar los datos del usuario ", e);
		}
		return false;
	}

	/**
	 * Muestra un error tanto en el propio formulario como en una notificación.
	 *
	 * @param title
	 * @param message
	 */
	private void showError(String title, String message) {
		createErrorLogin(title, message);
		statusLabel.getStyle().set("color", "var(--lumo-error-text-color)");
		statusLabel.setText(title + ": " + message);

		Notification notification = Notification.show(title + ". " + message, 5000, Notification.Position.MIDDLE);
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
	}

	/**
	 * Se crea un mensaje de error en función de los parámetros pasados
	 * @param titleError titulo del error que se ha producido
	 * @param notification mensaje correspondiente al error
	 * @return errorMessage mensaje de error
	 */
	public static ErrorMessage createErrorLogin(String titleError, String notification) {
		errorMessage = new ErrorMessage(titleError, notification);
		return errorMessage;
	}

	/**
	 * Mensaje de error simple mostrado en el login, en sustitución del que
	 * antes ofrecía {@code LoginI18n.ErrorMessage} del componente LoginForm
	 * (retirado al pasar a autenticación por SSO).
	 */
	public static class ErrorMessage {
		private final String title;
		private final String message;

		public ErrorMessage(String title, String message) {
			this.title = title;
			this.message = message;
		}

		public String getTitle() {
			return title;
		}

		public String getMessage() {
			return message;
		}
	}
}
