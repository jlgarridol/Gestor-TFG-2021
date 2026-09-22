package ubu.digit.ui.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.StartedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.UploadI18N.DropFiles;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import elemental.json.Json;
import ubu.digit.persistence.SistInfDataFactory;
import ubu.digit.security.Controller;
import ubu.digit.ui.components.Footer;
import ubu.digit.ui.components.NavigationBar;
import ubu.digit.util.ExternalProperties;

/**
 * Vista de actualización de ficheros.
 * 
 * @author Diana Bringas Ochoa
 */
@Route(value = "Upload")
@PageTitle("Actualización de ficheros")
public class UploadView extends VerticalLayout implements BeforeEnterObserver {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 8460171059055033456L;

	/**
	 * Logger de la clase.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadView.class.getName());

	/**
	 * Sufijo de ruta usado cuando la aplicación se despliega como WAR clásico.
	 */
	private static final String WEB_INF_CLASSES = "/WEB-INF/classes/";

	/**
	 * Nombre de la vista.
	 */
	public static final String VIEW_NAME = "upload";

	/**
	 * Botón para cerrar sesión.
	 */
	private Button logout;

	/**
	 * Elemento para subida de archivos.
	 */
	private Upload upload;

	/**
	 * Ruta del servidor.
	 */
	private String serverPath;

	/**
	 * Fichero de configuración.
	 */
	private ExternalProperties config;

	/**
	 * Directorio de los archivos csv y xls.
	 */
	private String dir;

	/**
	 * Ruta completa a los archivos csv y xls.
	 */
	private String completeDir;
	
	/**
	 * Buffer que almacenará el fichero actualizado
	 */
	private MemoryBuffer buffer;
	
	/**
	 * Componente output 
	 */
	private Div output;
	
	/**
	 * Controlador del acceso al moodle de UbuVirtual
	 */
	private static Controller CONTROLLER;

	/**
	 * Constructor.
	 */
	public UploadView() {
		
		setMargin(true);
		setSpacing(true);
		
  		String path = this.getClass().getClassLoader().getResource("").getPath();
		config = ExternalProperties.getInstance("/config.properties", false);
		dir = config.getSetting("dataIn");
		if (path.endsWith(WEB_INF_CLASSES)) {
			serverPath = path.substring(0, path.length() - WEB_INF_CLASSES.length());
		} else {
			// Classpath plano (p.ej. mvn spring-boot:run): no existe WEB-INF/classes,
			// así que se resuelve dataIn respecto a la raíz del classpath.
			serverPath = path;
			dir = dir.substring(WEB_INF_CLASSES.length());
		}
		completeDir = serverPath + dir + "/";
		
		NavigationBar bat = new NavigationBar();
		add(bat);
		
		//Se crea la instancia del controlador de acceso al moodle de UbuVirtual
		CONTROLLER = Controller.getInstance();
				
		buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setWidth("1400px");
        upload.setI18n(createSpanishI18n());
        output = new Div();
		ConfigureLogin();
		
		Span span = new Span();
		span.getElement().setProperty("innerHTML", "");
		add(span);
		
		Text infoNameFile = new Text("El nombre de los ficheros csv debe ser alguno de los siguientes:"
				+ " N1_Documento, N1_Tribunal"
				+ ", N1_Norma, N2_Alumno " 
				+ ", N2_Proyecto o N3_Historico");
		
		add(infoNameFile);
		Footer footer = new Footer(null);
		add(footer);
	}
	
	/**
	 * Función que establece los listener para el login.
	 */
	private void ConfigureLogin() {
        upload.addStartedListener(event -> {
        	Boolean isNameValid = checkNameFile(event);
        	if(!isNameValid) {
        		output.removeAll();
        		upload.interruptUpload();
        	}else {
        		upload.getElement().getPropertyNames()
                .forEach(prop -> LOGGER.info(prop + " "
                        + upload.getElement().getProperty(prop)));
        	}
        });
        
        upload.setAcceptedFileTypes(".csv",".xls");
        upload.setAutoUpload(false);

        upload.addSucceededListener(event -> {
        	 try {
                 byte[] buf = new byte[(int)event.getContentLength()];
                 InputStream inputSt = buffer.getInputStream();
                 if(inputSt.read(buf) == 0) {
                	 LOGGER.info("No se ha leido ningún byte ");
                 }
                 
                 String fileName = DeleteFile(event);
                 LOGGER.info("Fichero "+ fileName + " eliminado");
                 
            	 File newFile = new File(completeDir + fileName);
                 OutputStream outStream = new FileOutputStream(newFile);
                 outStream.write(buf);
                 LOGGER.info("Fichero " + fileName +" cargado");
                 outStream.flush();
                 outStream.close();
             } catch (IOException ex) {
            	 LOGGER.error("Error: " + UploadView.class.getName() + " " + ex);
             }
            
            output.removeAll();
            showOutput(event.getFileName(), output);
        });
        
        //Al finalizar la actualización del fichero se cambia el modelo de datos al correspondiente al fichero subido.
        upload.addFinishedListener(event ->{
        	if (!upload.isUploading()) {
	    		 if(event.getFileName().endsWith(".csv")) {
	  				SistInfDataFactory.setInstanceData("CSV");
	  				LOGGER.info("Actualización del fichero "+ event.getFileName() + " finalizada");
	  			}else {
	  				SistInfDataFactory.setInstanceData("XLS");
	  				LOGGER.info("Actualización del fichero "+ event.getFileName() + " finalizada");
	  			}
        	}
        });

        upload.getElement().addEventListener("Actualización fallida", event1 -> {
            String files = upload.getElement().getProperty("files");
            output.removeAll();
            Notification.show(files);
        });
        
      //Cuando se rechaza el fichero debido a: setMaxFileSize, setMaxFiles, setAcceptedFileTypes
  		upload.addFileRejectedListener(event -> {
  		    output.removeAll();
  		    showOutput(event.getErrorMessage(), output);
  		});

        //Fallo al actualizar
        upload.addFailedListener(event -> {
	        Notification.show("Error al cargar el fichero: "
	        +event.getFileName()).addThemeVariants(NotificationVariant.LUMO_ERROR);
	        upload.getElement().setPropertyJson("files", Json.createArray());});

        add(upload, output);
        logout = new Button("Cerrar sesión");
		logout.addClickListener(e ->  {
			LOGGER.info("Cerrando sesión");
			CONTROLLER.setUsername("");
			UI.getCurrent().navigate(InformationView.class);
		});
		add(logout);
	}
	
	/**
	 * Función que muestra un mensaje cuando el fichero se ha actualizado correctamente.
	 * @param text
	 * @param outputContainer
	 */
	private void showOutput(String text ,HasComponents outputContainer) {
        HtmlComponent p = new HtmlComponent(Tag.P);
        p.getElement().setText(text);
        outputContainer.add(p);
    }
	
	/**
	 * Método que traduce los mensajes del Upload a Español.
	 * @return UploadI18N
	 */
	private UploadI18N createSpanishI18n() {
		final UploadI18N uploadI18N = new UploadI18N();

		uploadI18N.setDropFiles(new DropFiles()
				.setOne("Arrastre el archivo aquí")
				.setMany("Arrastre los archivos aquí"));
		
		//Error
		uploadI18N.setError( new UploadI18N.Error()
				.setFileIsTooBig("El tamaño del archivo es demasiado grande")
				.setIncorrectFileType("La extensión debe coincidir con .csv o .xls")
				.setTooManyFiles("Adjunte unicamente un archivo"));
		
		//Añadir files
		uploadI18N.setAddFiles( new UploadI18N.AddFiles()
				.setOne("Suba un archivo")
				.setMany("Suba los archivos"));

		//Actualización
		uploadI18N.setUploading( new UploadI18N.Uploading()
				 .setStatus(new UploadI18N.Uploading.Status()
						 .setConnecting("Conectando...")
						 .setStalled("Descarga bloqueada.")
						 .setProcessing("Cargando..."))
				 .setRemainingTime(
	                        new UploadI18N.Uploading.RemainingTime()
	                        .setPrefix("Tiempo restante: ")
	                        .setUnknown("Tiempo restante desconocido"))
				 .setError(new UploadI18N.Uploading.Error()
	                        .setServerUnavailable("Servidor no disponible")
	                        .setUnexpectedServerError("Error inesperado del servidor")
	                        .setForbidden("No es posible realizar la descarga")));

		uploadI18N.setCancel("Cancelado");
		
		return uploadI18N;
	}
	
	/**
	 * Comprueba que el nombre del fichero csv corresponda con alguno de los siguientes: 
	 * 	N1_Documento, N1_Tribunal, N1_Norma, N2_Alumno, N2_Proyecto o N3_Historico
	 * @param event
	 * @return
	 */
	private Boolean checkNameFile(StartedEvent event) {
		if(event.getFileName().endsWith(".csv") && event.getFileName().equals("N1_Documento.csv") == false
				&& event.getFileName().equals("N1_Tribunal.csv")  == false && event.getFileName().equals("N1_Norma.csv")  == false 
				&& event.getFileName().equals("N2_Alumno.csv") == false && event.getFileName().equals("N2_Proyecto.csv") == false 
				&& event.getFileName().equals("N3_Historico.csv") == false) {
			Notification.show("Nombre de fichero inválido - Debe ser alguno de los siguientes: "
					+ "N1_Documento, N1_Tribunal"
					+ ", N1_Norma, N2_Alumno"
					+ ", N2_Proyecto, N3_Historico").addThemeVariants(NotificationVariant.LUMO_ERROR);
			return false;
			
		}
		return true;
	}
	
	/**
	 * Se elimina el fichero actual para reemplazarlo con el nuevo.
	 * Si el fichero es de tipo xls se sobreescribe el nombre por "BaseDeDatosTFGTFM.xls".
	 * 
	 * @param event
	 * @return nombre fichero 
	 */
	private String DeleteFile(SucceededEvent event) {
		String fileName="";
		if(event.getFileName().endsWith(".xls")) {
			fileName = "BaseDeDatosTFGTFM.xls";
		}else {
			fileName = event.getFileName();
		}
		
		try {
			File fileDeleted = new File(completeDir + fileName);
	        if (fileDeleted.exists()) {
				boolean deleted = fileDeleted.delete();
				LOGGER.info("Fichero " + fileDeleted + " eliminado");
				if (!deleted) {
					LOGGER.error("Fichero " + fileDeleted.getName() + " no se ha borrado correctamente");
				}
	        }
		}catch(Exception e) {
			LOGGER.error("Error al intentar eliminar el fichero " + fileName , e);
		}
        return fileName;
	}
	
	/** 
	 * Controla el evento antes de acceder al mismo. 
	 * Redirige al usuario al login para que inicie sesión.
	 * 
	 * @param event
	 *            before navigation event with event details
	 */
	public void beforeEnter(BeforeEnterEvent event) {
		LOGGER.info("Inicio de la vista de actualización de archivos");
		if (CONTROLLER.getUsername() == "") {
			LOGGER.info("Inicio de sesión no verificado curr");
			event.rerouteTo(LoginView.class);
		}
	}
}
