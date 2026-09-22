package ubu.digit.persistence;

import java.io.Serializable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import ubu.digit.util.ExternalProperties;


/**
 * Clase abstracta de la cual heredan las fachadas Singleton de acceso a datos.
 * 
 * @author Diana Bringas Ochoa
 */
public abstract class SistInfDataAbstract implements Serializable { 

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -6019587024081762319L;
	
	/**
	 * Select.
	 */
	protected static final String SELECT = "select ";
	
	/**
	 * Select all.
	 */
	protected static final String SELECT_ALL = "select * ";
	
	/**
	 * Select distinct.
	 */
	protected static final String SELECT_DISTINCT = "select distinct ";
	
	/**
	 * Count.
	 */
	protected static final String COUNT = "count";
	
	/**
	 * From.
	 */
	protected static final String FROM = " from ";
	
	/**
	 * Where.
	 */
	protected static final String WHERE = " where ";
	
	/**
	 * Distinto de vacío.
	 */
	protected static final String DISTINTO_DE_VACIO = " != ''";
	
	/**
	 * And.
	 */
	protected static final String AND = " and ";
	
	/**
	 * Like.
	 */
	protected static final String LIKE = " like ";
	
	/**
	 * Order by.
	 */
	protected static final String ORDER_BY = " order by ";
	
	/**
	 * Dirección de los ficheros en la aplicación del servidor.
	 */
	protected String serverPath = "";

	/**
	 * URL donde encontramos el fichero con las propiedades del proyecto.
	 */
	protected static ExternalProperties prop = ExternalProperties.getInstance("/config.properties",false);
	
	/**
	 * Directorio donde se encuentra los datos de entrada, es decir, los
	 * ficheros que contienen los datos que vamos a consultar.
	 */
	public static final String DIRCSV = prop.getSetting("dataIn");
	
	protected abstract Number getResultSetNumber(String sql);

	protected abstract List<Float> obtenerDatos(String columnName, String tableName);

	/**
	 * Calcula la desviación standard.
	 * 
	 * @param list
	 *            Listado de los números de los que calcular la desviación.
	 * @return Desviación standard.
	 */
	protected Double calculateStdev(List<Float> list) {
		double sum = 0;

		for (int i = 0; i < list.size(); i++) {
			sum = sum + list.get(i);
		}
		double mean = sum / list.size();
		double[] deviations = new double[list.size()];

		for (int i = 0; i < deviations.length; i++) {
			deviations[i] = list.get(i) - mean;
		}
		double[] squares = new double[list.size()];

		for (int i = 0; i < squares.length; i++) {
			squares[i] = deviations[i] * deviations[i];
		}
		sum = 0;

		for (int i = 0; i < squares.length; i++) {
			sum = sum + squares[i];
		}
		double result = sum / (list.size() - 1);
		return Math.sqrt(result);
	}
	
	public abstract Number getAvgColumn(String columnName, String tableName);
	
	public abstract List<String> getRankingPercentile();
	
	public abstract List<Integer> getRankingTotal();
	
	public abstract List<Integer> getRankingCurses();
	
	public abstract Number getTotalNumber(String columnName, String tableName);

	protected abstract Number getTotalNumber(String columnName, String tableName, String whereCondition);

	public abstract Number getTotalNumber(String[] columnsName, String tableName);

	public abstract Number getTotalFreeProject();
	
	protected abstract Object getResultSet(String tableName, String columnName);

	protected abstract Object getResultSet(String tableName, String columnName, String whereCondition);

	protected abstract Object getResultSet(String tableName, String columnName, String[] filters, String[] columnsName);

	public abstract LocalDate getYear(String columnName, String tableName, Boolean minimo);

	protected abstract List<List<Object>> getProjectsCurso(String columnName, String columnName2, String columnName3,
			String columnName4, String tableName, Number curso);

	/**
	 * Transforma el string que le llega en un tipo Date.
	 * 
	 * @param date
	 *            Fecha en tipo String
	 * @return Fecha con formato Date
	 */
	protected LocalDate transform(String date) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return LocalDate.parse(date, dateTimeFormatter);
	}
	
	public abstract List<String> getDataModel();
	
	public abstract List<String> getTribunal();
	
	public abstract List<String> getNormas();
	
	public abstract List<String> getDocumentos();
	
	@SuppressWarnings("rawtypes")
	public abstract ArrayList getDataModelHistoric(DateTimeFormatter dateTimeFormatter);
	
	protected abstract List<String> getDates(String columnName, String sheet);

	public abstract Number getQuartilColumn(String columnName, String tableName, double percent);

	public abstract Number getMaxColumn(String columnName, String tableName);
	
	public abstract Number getMinColumn(String columnName, String tableName);
	
	public abstract Number getStdvColumn(String columnName, String tableName);

	protected abstract List<Double> getListNumber(String columnName, String sql);
}
