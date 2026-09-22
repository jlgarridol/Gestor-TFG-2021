package ubu.digit.ui.views;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Vista de retorno del flujo de SSO de Moodle.
 *
 * Moodle redirige la pestaña abierta para el login a
 * "urlscheme://token=..."; el navegador, mediante un manejador de protocolo
 * registrado por {@link LoginView}, reescribe esa navegación hacia esta
 * vista pasando la URL original como parámetro "url". Esta vista se limita a
 * reenviar ese valor a la pestaña que la abrió (window.opener) mediante
 * postMessage y a cerrarse.
 */
@Route(value = "sso-callback")
@PageTitle("SSO UBUVirtual")
public class SsoCallbackView extends VerticalLayout implements BeforeEnterObserver {

	private static final long serialVersionUID = 1L;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		List<String> values = event.getLocation().getQueryParameters().getParameters()
				.getOrDefault("url", Collections.emptyList());
		String callbackUrl = values.isEmpty() ? "" : values.get(0);

		UI.getCurrent().getPage().executeJs(
				"if (window.opener) {"
				+ "  window.opener.postMessage({ type: 'ubu-sso-token', token: $0 }, window.location.origin);"
				+ "}"
				+ "window.close();",
				callbackUrl);
	}
}
