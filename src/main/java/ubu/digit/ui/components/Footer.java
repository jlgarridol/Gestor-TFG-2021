package ubu.digit.ui.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import  com.vaadin.flow.component.orderedlayout.VerticalLayout;

import ubu.digit.ui.views.LoginView;
import ubu.digit.util.ExternalProperties;

import static ubu.digit.util.Constants.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pié de página común a todas las vistas.
 * 
 * @author Javier de la Fuente Barrios
 * @author Diana Bringas Ochoa
 */
public class Footer extends VerticalLayout {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 1285443082746553886L;

	/**
	 * Sufijo de ruta usado cuando la aplicación se despliega como WAR clásico.
	 */
	private static final String WEB_INF_CLASSES = "/WEB-INF/classes/";

	/**
	 * Layout que contiene los layout verticales (columnas)
	 * de information y license
	 */
	HorizontalLayout conten;
	
	/**
	 * Layout correspondiente a la columna con la licencia, 
	 * fecha de actualización de los ficheros y acceso al login.
	 */
	VerticalLayout license;
	
	/**
	 * Layout con la información sobre los autores y tutores de la app
	 */
	VerticalLayout information;
	
	/**
	 * Nombre del fichero .csv  o .xls relacionado con la vista
	 */
	private String fileName;
	
	/**
	 * Logger de la clase.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Footer.class.getName());

	/**
	 * Constructor.
	 */
	public Footer(String fileName) {
		this.fileName = fileName;
		conten = new HorizontalLayout();
		conten.addClassName("Footer-grid");
		
		H2 subtitle = new H2(INFORMACION);
		subtitle.addClassName(SUBTITLE_STYLE);
		
		addInformation();
		addLicense();
		conten.add(information, license);
		
		add(subtitle, conten);
	}

	/**
	 * Añade la información de los autores y tutores del proyecto.
	 */
	private void addInformation() {
		information = new VerticalLayout();
		information.setMargin(false);
		information.setSpacing(true);

		Text version1 = new Text("Versión 1.0 creada por Beatriz Zurera Martínez-Acitores");
		Anchor link1 = new Anchor("mailto:bzm0001@alu.ubu.es","bzm0001@alu.ubu.es");
		information.add(new HorizontalLayout(new Icon(VaadinIcon.ENVELOPE),link1));
		information.add(version1);
		
		Text version2 = new Text("Versión 2.0 creada por Javier de la Fuente Barrios");
		Anchor link2 = new Anchor("mailto:jfb0019@alu.ubu.es","jfb0019@alu.ubu.es");
		information.add(new HorizontalLayout(new Icon(VaadinIcon.ENVELOPE),link2));
		information.add(version2);
		
		Text version3 = new Text("Versión 3.0 creada por Diana Bringas Ochoa");
		Anchor link3 = new Anchor("mailto:dbo1001@alu.ubu.es","dbo1001@alu.ubu.es");
		information.add(new HorizontalLayout(new Icon(VaadinIcon.ENVELOPE),link3));
		information.add(version3);
		
		Text tutor1 = new Text("Tutorizado por Carlos López Nozal");
		Anchor linkT1 = new Anchor("mailto:clopezno@alu.ubu.es","clopezno@ubu.es");
		information.add(new HorizontalLayout(new Icon(VaadinIcon.ENVELOPE),linkT1));
		information.add(tutor1);
		
		Text tutor2 = new Text("Tutorizado por Alvar Arnaiz Gonzalez");
		Anchor linkT2 = new Anchor("mailto:alvarag@ubu.es", "alvarag@ubu.es");
		information.add(new HorizontalLayout(new Icon(VaadinIcon.ENVELOPE),linkT2));
		information.add(tutor2);
		
		Text copyright = new Text("Copyright @ LSI");
		information.add(new HorizontalLayout(copyright));
	}
	
	/**
	 * Obtiene la fecha de última modificación del fichero asociado a la vista.
	 * 
	 * @param fileName
	 *            nombre del fichero
	 * @return fecha de última modificación del fichero
	 */
	private String getLastModified(String fileName) {
		String path = this.getClass().getClassLoader().getResource("").getPath();
  		ExternalProperties config = ExternalProperties.getInstance("/config.properties", false);
		String dir = config.getSetting("dataIn");
		String serverPath;
		if (path.endsWith(WEB_INF_CLASSES)) {
			serverPath = path.substring(0, path.length() - WEB_INF_CLASSES.length());
		} else {
			// Classpath plano (p.ej. mvn spring-boot:run): no existe WEB-INF/classes,
			// así que se resuelve dataIn respecto a la raíz del classpath.
			serverPath = path;
			dir = dir.substring(WEB_INF_CLASSES.length());
		}
		String completeDir = serverPath + dir + "/";
		
		TimeZone zoneId = TimeZone.getTimeZone( "Europe/Madrid" );
		Locale locale = new Locale("es","ES");
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale);
		sdf.setTimeZone(zoneId);
		
		String lastModified = null;
		File file = new File(completeDir + fileName);
		LOGGER.info("Comprobar si existe el fichero " + file + " en el path " + completeDir + fileName);
		if (file.exists()) {
			LOGGER.info("Path file" + file);
			Date date = new Date(file.lastModified());
			lastModified = sdf.format(date);
			LOGGER.info("Fecha de última modificación sin formato  " + date);
			LOGGER.info("Fecha empleando la zona horaria Europe/Madrid " + lastModified);
		}
		return lastModified;
	}

	/**
	 * Añade la información de la licencia del proyecto, la fecha de actualización 
	 * de los ficheros de datos y el butón del acceso al Login.
	 */
	private void addLicense() {
		license = new VerticalLayout();
		license.setMargin(false);
		license.setSpacing(true);

		Text licenseText = new Text("This work is licensed under: ");
		Anchor ccLink = new Anchor("https://github.com/dbo1001/Gestor-TFG-2021/blob/master/LICENSE.md", "MIT License.");
		
		license.add(licenseText, ccLink);
		
		if (fileName != null) {
			LOGGER.info("actualización fecha fichero " + fileName);
			String lastModifiedCsv = getLastModified(fileName);
			String lastModifiedXls = getLastModified("BaseDeDatosTFGTFM.xls");
			license.add(new Label("Ultima actualización de " + fileName + " : " + lastModifiedCsv));
			license.add(new Label("Ultima actualización de BaseDeDatosTFGTFM.xls : " + lastModifiedXls));
		}

		Button login = new Button("Actualizar");
		login.addClickListener(e -> {	
			UI.getCurrent().navigate(LoginView.class);
		});
		license.add(login);
	}
}
