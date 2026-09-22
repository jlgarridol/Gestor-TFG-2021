package ubu.digit.persistence;

import java.io.Serializable;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ubu.digit.util.Constants.*;


/**
 * Fachada Singleton de acceso a datos a través de un recurso jdbc:relique:csv:dir (directorio donde se encuentra). 
 * Se proporciona una hoja de datos con la definición de la estructura para probar sus funciones.
 * 
 * @author Carlos López Nozal
 * @author Beatriz Zurera Martínez-Acitores
 * @author Javier de la Fuente Barrios
 * @author Diana Bringas Ochoa
 * @since 0.5
 */
public class SistInfDataCsv extends SistInfDataAbstract implements Serializable {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -6019587024081762319L;

	/**
	 * Logger de la clase.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SistInfDataCsv.class.getName());

	/**
	 * Sufijo de ruta usado cuando la aplicación se despliega como WAR clásico.
	 */
	private static final String WEB_INF_CLASSES = "/WEB-INF/classes/";

	/**
	 * Conexión que se produce entre la base de datos(csv) y la aplicación.
	 */
	private transient Connection connection;

	/**
	 * Instancia con los datos.
	 */
	private static SistInfDataCsv instance;
	
	/**
	 * Constructor que inicializa la conexión con el CsvDriver
	 */
	private SistInfDataCsv() {
		super();
		this.connection = this.getConection();
	}

	/**
	 * Método singleton para obtener la instancia de la clase fachada.
	 */
	public static SistInfDataCsv getInstance(){
		if (instance == null) {
			instance = new SistInfDataCsv();
		}
		return instance;
	}

	/**
	 * Inicializa la conexión odbc al almacen de datos.
	 * 
	 * @return con
	 * 				conexión con el fichero .csv
	 */
	private Connection getConection(){
		Connection con = null;
		String url = "jdbc:relique:csv:";
		try {
			Class.forName("org.relique.jdbc.csv.CsvDriver");
			String dataDir = DIRCSV;
			if (DIRCSV.startsWith("/")) {
				String path = this.getClass().getClassLoader().getResource("").getPath();
				if (path.endsWith(WEB_INF_CLASSES)) {
					serverPath = path.substring(0, path.length() - WEB_INF_CLASSES.length());
				} else {
					// Classpath plano (p.ej. mvn spring-boot:run): no existe WEB-INF/classes,
					// así que se resuelve dataIn respecto a la raíz del classpath.
					serverPath = path;
					dataDir = DIRCSV.substring(WEB_INF_CLASSES.length());
				}
			}

			new BOMRemoveUTF().bomRemoveUTFDirectory(serverPath + dataDir);

			Properties props = new java.util.Properties();
			props.put("ignoreNonParseableLines", true);
			props.put("separator",  prop.getSetting("csvSeparator"));
			props.put("charset", "UTF-8");
			con = DriverManager.getConnection(url + serverPath + dataDir, props);
			LOGGER.info("Ruta Fachada CSV " + url + serverPath + dataDir);
		} catch (ClassNotFoundException | SQLException e) {
			LOGGER.error("Error al obtener la conexión con el Csv " + e.getMessage());
		}
		return con;
	}
	
	/**
	 * Destructor elimina la conexión al sistema de acceso a datos.
	 * 
	 **/
	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			connection.close();
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
		super.finalize();
	}
	
	/**
	 * Ejecuta una sentencia SQL sumando todos los datos Float contenidos en la
	 * primera columna.
	 * 
	 * @param SQL
	 *            sentencia sql a ejecutar
	 * @return suma todas la filas de la primera columna
	 */
	@Override
	protected Number getResultSetNumber(String sql) { 
		Float number = 0.0f;
		boolean hasResults;

		try (Statement statement = connection.createStatement()) {
			hasResults = statement.execute(sql);

			if (hasResults) {
				try (ResultSet result = statement.getResultSet()) {
					result.next();
					number = result.getFloat(1);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return (Number) number; 
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo la media aritmética de la columna de
	 * la tabla ambas pasadas como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return media aritmética
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Number getAvgColumn(String columnName, String tableName){
		List<Float> media = obtenerDatos(columnName, tableName);
		Float resultadoMedia = new Float(0);
		for (Float numero : media) {
			resultadoMedia += numero;
		}
		return resultadoMedia / media.size();
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el valor máximo de la columna de una
	 * tabla, ambas pasadas como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return valor máximo de la columna
	 */
	@Override
	public Number getMaxColumn(String columnName, String tableName){
		return Collections.max(obtenerDatos(columnName, tableName));
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el valor mínimo de la columna de una
	 * tabla, ambas pasadas como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return valor mínimo de la columna
	 */
	@Override
	public Number getMinColumn(String columnName, String tableName){
		return Collections.min(obtenerDatos(columnName, tableName));
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo la desviación estandart de la
	 * columna de una tabla, ambas pasadas como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return desviación estandar
	 * @throws SQLException
	 */
	@Override
	public Number getStdvColumn(String columnName, String tableName){
		return calculateStdev(obtenerDatos(columnName, tableName));
	}

	/**
	 * Obtiene los datos de una columna determinada de una tabla determinada.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return Listado con los datos de dicha columna.
	 */
	@Override
	protected List<Float> obtenerDatos(String columnName, String tableName){
		String sql = SELECT + columnName + FROM + tableName + ";";
		List<Float> media = new ArrayList<>();
		try (Statement statement = connection.createStatement(); 
				ResultSet result = statement.executeQuery(sql)) {
			ResultSetMetaData rmeta = result.getMetaData();
			int numColumns = rmeta.getColumnCount();
			while (result.next()) {
				for (int i = 1; i <= numColumns; ++i) {
					media.add(result.getFloat(i));
				}
			}
		}catch(SQLException e ) {
			LOGGER.error(e.getMessage());
		}
		return media;
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo la mediana de la columna de una
	 * tabla, ambas pasadas como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @param percent
	 * @return mediana
	 */
	@Override
	public Number getQuartilColumn(String columnName, String tableName, double percent){
		Number nTotalValue = getTotalNumber(columnName, tableName);
		String sql = SELECT + columnName + FROM + tableName + WHERE + columnName + DISTINTO_DE_VACIO + ORDER_BY
				+ columnName;
		List<Double> listValues = getListNumber(columnName, sql);
		int indexMedian = (int) (nTotalValue.intValue() * percent);
		return listValues.get(indexMedian);
	}
	
	/**
	 * Ejecuta una sentencia SQL obteniendo la nota de los proyectos.
	 * Devolverá los percentiles de las notas obtenidas.
	 * 
	 * @return Lista con los percentiles 
	 */
	@Override
	public List<String> getRankingPercentile(){ 
		List<Double> scoreList = new ArrayList<Double>();
		List<Integer> percentileList = new ArrayList<Integer>();
		List<String> rankingList = new ArrayList<String>();
		int i, count;
		
		String sql = SELECT + NOTA + FROM + HISTORICO + WHERE + NOTA + DISTINTO_DE_VACIO;
		try (Statement statement = connection.createStatement()) {
			Boolean hasResults = statement.execute(sql);
			if (hasResults) {
				try (ResultSet result = statement.getResultSet()) {
					addNumbersToList(NOTA, scoreList, result);
				}
			}
		}catch(SQLException ex) { 
			LOGGER.error("Error al obtener el ranking total de las notas", ex);
		}
		if(scoreList.size() > 0) {
	        for (i = 0; i < scoreList.size(); i++) { 
	            count = 0; 
	            for (int j = 0; j < scoreList.size(); j++){ 
	                if (scoreList.get(i) > scoreList.get(j)){
	                    count++; 
	                } 
	            } 
	            percentileList.add((count * 100) / (scoreList.size() - 1)); 
	        }
	        for (i = 0; i < percentileList.size(); i++) { 
	        	if(percentileList.get(i)>=0 && percentileList.get(i)<=20) {
	        		rankingList.add("E");
	        	}else if(percentileList.get(i)>=21 && percentileList.get(i)<=40) {
	        		rankingList.add("D");
	        	}else if(percentileList.get(i)>=41 && percentileList.get(i)<=60) {
	        		rankingList.add("C");
	        	}else if(percentileList.get(i)>=61 && percentileList.get(i)<=80) {
	        		rankingList.add("B");
	        	}else if(percentileList.get(i)>=81 && percentileList.get(i)<=100) {
	        		rankingList.add("A");
	        	}
	        }
		}
        
        return rankingList;
	}
	
	/**
	 * Calcula el ranking por notas total de los proyectos
	 * 
	 * @return Lista con los rankings por notas total
	 */
	@Override
	public List<Integer> getRankingTotal(){
		List<Double> scoreList = new ArrayList<Double>();
		List<Integer> rankingTotalList = new ArrayList<Integer>();
		List<Double> duplicatedList;
		int i,j,cont=1;

		String sql = SELECT + NOTA + FROM + HISTORICO + WHERE + NOTA + DISTINTO_DE_VACIO;
		try (Statement statement = connection.createStatement()) {
			Boolean hasResults = statement.execute(sql);
			if (hasResults) {
				try (ResultSet result = statement.getResultSet()) {
					addNumbersToList(NOTA, scoreList, result);
					if(scoreList.size() > 0){
						//Pasamos la lista a un stream que tiene el método distinct (elimina duplicados) 
						//y después lo volvemos a pasar a una lista nueva
						duplicatedList = scoreList.stream().distinct().collect(Collectors.toList());
				        
				        for (i = 0; i < scoreList.size(); i++) {
				        	for (j = 0; j < duplicatedList.size(); j++) {
				        		if(scoreList.get(i) < duplicatedList.get(j)) {
				        			cont++;
				        		}
				        	}
				        	rankingTotalList.add(cont);
				        	cont=1;
				        }
							}
				}
			}
		}catch(SQLException ex) { 
			LOGGER.error("Error al obtener el ranking total de las notas", ex);
		}catch(Exception e) { 
			LOGGER.error("Error al obtener el ranking total de las notas", e);
		}
		return rankingTotalList;
	}
	
	/**
	 * Método que devuelve una lista con el curso al que pertenece (yyyy/yyyy).
	 * 
	 * @return lista el curso (yyyy/yyyy)
	 */
	protected List<String> addYearsCurseToList(){
		List<String> curses = new ArrayList<String>();
		String sql_CurseDate = SELECT + FECHA_ASIGNACION + "," + FECHA_PRESENTACION + FROM + 
				HISTORICO + WHERE + FECHA_ASIGNACION + DISTINTO_DE_VACIO + AND + FECHA_PRESENTACION + DISTINTO_DE_VACIO;
		String dateIni = "";
		String dateEnd = "";
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql_CurseDate)) {
				while (result.next()) {
					dateIni = result.getString(FECHA_ASIGNACION);
					dateEnd = result.getString(FECHA_PRESENTACION);
					curses.add(dateIni.toString().substring(6) + dateEnd.toString().substring(5));
				}
		}catch(SQLException ex) { 
			LOGGER.error("Error al obtener las fechas de presentación y asignación", ex);
		}
		return curses;
	}
	
	/**
	 * Calcula el ranking de notas según el curso academico(cursos)
	 * 
	 * @return Lista con los rankings de notas por cursos
	 */
	@Override
	public List<Integer> getRankingCurses(){
		List<Double> scoreList = new ArrayList<Double>();
		List<Integer> rankingTotalList = new ArrayList<Integer>();
		List<String> datesCurse = new ArrayList<String>();
		int i,j,cont=1;
		
		String sql_Historico = SELECT + NOTA + FROM + HISTORICO + WHERE + NOTA + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement()) {
			Boolean hasResults = statement.execute(sql_Historico);
			if (hasResults) {
				try (ResultSet result = statement.getResultSet()) {
					addNumbersToList(NOTA, scoreList, result);
					datesCurse = addYearsCurseToList();
					
					 for (i=0;i<datesCurse.size();i++){
				        	for(j=0;j<datesCurse.size();j++){
				        		if(i!=j ) {
				        			String dateInic = datesCurse.get(i).substring(5);
				        			if(datesCurse.get(i).equals(datesCurse.get(j)) || ( dateInic.equals(datesCurse.get(j).substring(5))
				        				&&  dateInic.equals(datesCurse.get(j).substring(0,4)))){
					        			if(scoreList.get(i) < scoreList.get(j)){
					        				cont++;
					        			}
				        			}
				        		}
				        	}
				        	rankingTotalList.add(cont);
				        	cont=1;
				        }
				}
			}
		}catch(SQLException ex) { 
			LOGGER.error("Error al obtener el ranking de notas por cursos", ex);
		}catch(Exception e) { 
			LOGGER.error("Error al obtener el ranking de notas por cursos", e);
		}
		
		return rankingTotalList;
	}

	/**
	 * Obtiene la lista de valores de una columna de una consulta SQL.
	 * 
	 * @param columnName
	 *            Nombre de la columna.
	 * @param SQL
	 *            Sentencia a ejecutar.
	 * @return listado con los números.
	 */
	@Override
	protected List<Double> getListNumber(String columnName, String sql){
		List<Double> listValues = new ArrayList<>(100);
		try (Statement statement = connection.createStatement()) {
			Boolean hasResults = statement.execute(sql);
			if (hasResults) {
				try (ResultSet result = statement.getResultSet()) {
					addNumbersToList(columnName, listValues, result);
				}
			}
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return listValues;
	}

	/**
	 * Añade los valores de una columna a una lista.
	 * 
	 * @param columnName
	 *            Nombre de la columna.
	 * @param listValues
	 *            Lista que guarda los valores.
	 * @param result
	 *            ResultSet a partir del cual obtener los valores.
	 */
	protected void addNumbersToList(String columnName, List<Double> listValues, ResultSet result) {
		try {
			while (result.next()) {
				listValues.add(result.getDouble(columnName));
			}
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el número total de filas diferentes,
	 * distintas de null y cumplen la claúsula where de la columna de una tabla.
	 * 
	 * @param columnName
	 *            nombre de la columna.
	 * @param tableName
	 *            nombre de la tabla de datos.
	 * @return número total de filas distintas
	 * @throws SQLException
	 */
	@Override
	public Number getTotalNumber(String columnName, String tableName){
		String sql = SELECT_DISTINCT + COUNT + "(" + columnName + ")" + FROM + tableName + WHERE + columnName
				+ DISTINTO_DE_VACIO;
		return getResultSetNumber(sql);
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el número total de filas diferentes,
	 * distintas de null y que cumplan el filtro de la columna de una tabla.
	 * 
	 * @param columnName
	 *            nombre de la columna
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @param whereCondition
	 *            filtro con la condición
	 * @return número total de filas distintas
	 */
	@Override
	public Number getTotalNumber(String columnName, String tableName, String whereCondition){
		String sql = SELECT_DISTINCT + COUNT + "(" + columnName + ")" + FROM + tableName + WHERE + columnName
				+ DISTINTO_DE_VACIO + AND + whereCondition + " ;";
		return getResultSetNumber(sql);
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el número total de filas diferentes
	 * y distintas de null de las columnas de una tabla pasadas como parámetro.
	 * 
	 * @param columnsName
	 *            nombre de las columnas
	 * @param tableName
	 *            nombre de la tabla de datos
	 * @return número total de filas distintas
	 */
	@Override
	public Number getTotalNumber(String[] columnsName, String tableName){
		String sql;
		Set<String> noDups = new HashSet<>();
		if (columnsName != null) {
			for (int i = 0; i < columnsName.length; i++) {
				sql = SELECT + columnsName[i] + FROM + tableName + WHERE + columnsName[i] + DISTINTO_DE_VACIO;
				try (Statement statement = connection.createStatement();
						ResultSet resultSet = statement.executeQuery(sql)) {
					ResultSetMetaData rmeta = resultSet.getMetaData();
					int numColumns = rmeta.getColumnCount();
					addUniqueStrings(noDups, resultSet, numColumns);
				}catch(SQLException e) {
					LOGGER.error(e.getMessage());
				}
			}
			return (float) noDups.size();
		} else {
			return 0;
		}
	}

	/**
	 * Añade a una colección sin duplicados los valores de las columnas.
	 * 
	 * @param resultSet
	 *            resultado de ejecutar la sentencia sql correspondiente
	 * @param noDups
	 *            colección sin duplicados donde almacenar los valores
	 * @param numColumns
	 *            número de columnas que tiene el resultset
	 * @throws SQLException
	 */
	protected void addUniqueStrings(Set<String> noDups, ResultSet resultSet, int numColumns) throws SQLException {
		while (resultSet.next()) {
			for (int j = 1; j <= numColumns; ++j) {
				noDups.add(resultSet.getString(j));
			}
		}
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo el número total de proyectos sin
	 * asignar. Se busca una cadena que contenga la subcadena "Aal".
	 * 
	 * @return número total de proyectos sin asignar
	 */
	@Override
	public Number getTotalFreeProject(){
		String sql = SELECT + COUNT + "(*)" + FROM + "Proyecto" + WHERE + ALUMNO1 + LIKE + "'%Aal%'";
		return getResultSetNumber(sql);
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo un conjunto de filas distintas de
	 * nulo de una columna de una tabla, ambas pasadas como parámetros.
	 * 
	 * @param columnName
	 *            nombre de la columna a discriminar el contador.
	 * @param tableName
	 *            nombre de la tabla de datos.
	 * @return conjunto de filas distintas de null.
	 */
	public ResultSet getResultSet(String tableName, String columnName){
		String sql = SELECT_ALL + FROM + tableName + WHERE + columnName + DISTINTO_DE_VACIO;
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			return result;
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo un conjunto de filas distintas de
	 * nulo y cumplen la condición pasada como parámetro.
	 * 
	 * @param columnName
	 *            nombre de la columna a discriminar con nulos.
	 * @param tableName
	 *            nombre de la tabla de datos.
	 * @param whereCondition
	 *            condición de la claúsula where.
	 * @return conjunto de filas distintas de null y condición de la claúsula where.
	 */
	public ResultSet getResultSet(String tableName, String columnName, String whereCondition){
		String sql = SELECT_ALL + FROM + tableName + WHERE + whereCondition + ";";
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
			return statement.getResultSet();
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Ejecuta una sentencia SQL obteniendo un conjunto de filas distintas de
	 * nulo y que contienen las cadenas pasadas como filtros de una columna de
	 * una tabla, ambas pasadas como parámetro.
	 * 
	 * @param tableName
	 *            nombre de la tabla de datos.
	 * @param columnName
	 *            nombre de la columna a discriminar el contador.
	 * @param filters
	 *            valores de las columnas que continen las cadenas filters.
	 * @param columnsName
	 *            nombres de las columnas a seleccionar
	 * @return conjunto de filas distintas de null.
	 */
	public ResultSet getResultSet(String tableName, String columnName, String[] filters, String[] columnsName){
		StringBuilder sql = new StringBuilder();
		ResultSet result = null;
		try (Statement statement = connection.createStatement()){
			if (columnsName == null) {
				sql.append(SELECT_ALL);
			} else {
				sql.append(SELECT);
				int index = 0;
				for (String selectedColumn : columnsName) {
					if (index == 0) {
						sql.append(" " + selectedColumn);
						index++;
					} else {
						sql.append(", " + selectedColumn);
					}
				}
			}
			sql.append(" \n" + FROM + tableName + " \n" + WHERE + " (" + columnName + DISTINTO_DE_VACIO + ")");
			if (filters != null) {
				sql.append(AND + "(");
				int index = 0;
				for (String filter : filters) {
					if (index == 0) {
						sql.append(" \n" + columnName + " = '" + filter + "'");
						index++;
					} else {
						sql.append(" \nOR " + columnName + " = '" + filter + "'");
					}
				}
				sql.append(");");
			}
			statement.execute(sql.toString());
			result = statement.getResultSet();
			
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}
	
	/**
	 * Método que obtiene el curso más bajo o más alto, según el booleano, que
	 * tiene la base de datos.
	 * 
	 * @param columnName
	 *            Nombre de la columna.
	 * @param tableName
	 *            Nombre de la tabla.
	 * @param minimo
	 *            True si queremos el curso mínimo, false si queremos el curso
	 *            máximo.
	 * @return Curso más bajo que ha encontrado.
	 */
	@Override
	public LocalDate getYear(String columnName, String tableName, Boolean minimo) {
		String sql = SELECT + columnName + FROM + tableName + ";";
		List<LocalDate> listadoFechas = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				listadoFechas.add(transform(result.getString(columnName)));
			}
		}catch(SQLException e) {
			LOGGER.error(e.getMessage());
		}
		if (minimo) {
			return Collections.min(listadoFechas);
		} else {
			return Collections.max(listadoFechas);
		}
	}
	
	/**
	 * Método que devuleve una lista con las fechas.
	 * 
	 * @param columnName
	 *            Nombre de la columna.
	 * @param tableName
	 *            nombre de la hoja o del csv donde se encuentran los datos
	 * @return Lista de fechas
	 * @throws SQLException
	 */
	public List<String> getDates(String columnName, String tableName) {
		List<String> dates = new ArrayList<String>();
		String sql = SELECT + columnName + FROM + tableName + WHERE + columnName + DISTINTO_DE_VACIO +";";
			
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				dates.add(result.getString(columnName));
			}
		}catch(SQLException ex) { 
			LOGGER.error("Error al obtener el ranking de notas por cursos", ex);
		}
		return dates;
	}

	/**
	 * Método que obtiene la fecha de asignación, la fecha de presentación, el
	 * total de días y la nota respectiva del proyecto.
	 * 
	 * @param columnName
	 *            Nombre de la columna de la fecha de asignación.
	 * @param columnName2
	 *            Nombre de la columna de la fecha de presentación.
	 * @param columnName3
	 *            Nombre de la columna del total de días.
	 * @param columnName4
	 *            Nombre de la columna de la nota del proyecto.
	 * @param tableName
	 *            Nombre de la tabla.
	 * @param curso
	 *            Curso del que queremos los datos.
	 * @return Un lista con todos los datos que hemos solicitado.
	 * @throws SQLException
	 */
	@Override
	protected List<List<Object>> getProjectsCurso(String columnName, String columnName2, String columnName3,
			String columnName4, String tableName, Number curso){
		List<Object> lista;
		List<List<Object>> resultados = new ArrayList<>();
		String sql = SELECT + columnName + "," + columnName2 + "," + columnName3 + "," + columnName4 + ", " 
				+ ALUMNO1 + ", " + ALUMNO2 + ", " + ALUMNO3 + ", " 
				+ TUTOR1 + ", " + TUTOR2 + ", " + TUTOR3 + FROM + tableName
				+ WHERE + columnName + LIKE + "'%" + curso + "';";
		try (Statement statement = connection.createStatement(); 
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				lista = new ArrayList<>();
				// Fecha asignación
				lista.add(transform(result.getString(columnName)));
				// Fecha presentación
				lista.add(transform(result.getString(columnName2)));
				// Dias
				lista.add(result.getInt(columnName3));
				// Nota
				lista.add(result.getDouble(columnName4));
				lista.add(result.getString(ALUMNO1));
				lista.add(result.getString(ALUMNO2));
				lista.add(result.getString(ALUMNO3));
				lista.add(result.getString(TUTOR1));
				lista.add(result.getString(TUTOR2));
				lista.add(result.getString(TUTOR3));
				resultados.add(lista);
			}
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos del actuales", e);
		}
		return resultados;
	}
	
	/**
	 * Obtener los datos del modelo de datos de los proyectos activos.
	 */
	public List<String> getDataModel() { 
		List<String> listaDataModel = new ArrayList<String>();
		String sql = SELECT_ALL + FROM + PROYECTO + WHERE + TITULO + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				String title = result.getString(TITULO);
				String description = result.getString(DESCRIPCION);
				String tutor1 = result.getString(TUTOR1);
				String tutor2 = result.getString(TUTOR2);
				if (tutor2 == null) {
					tutor2 = "";
				}
				String tutor3 = result.getString(TUTOR3);
				if (tutor3 == null) {
					tutor3 = "";
				}
				String student1 = result.getString(ALUMNO1);
				String student2 = result.getString(ALUMNO2);
				if (student2 == null) {
					student2 = "";
				}
				String student3 = result.getString(ALUMNO3);
				if (student3 == null) {
					student3 = "";
				}
				String courseAssignment = result.getString(CURSO_ASIGNACION);
				
				listaDataModel.add(title);
				listaDataModel.add(description);
				listaDataModel.add(tutor1);
				listaDataModel.add(tutor2);
				listaDataModel.add(tutor3);
				listaDataModel.add(student1);
				listaDataModel.add(student2);
				listaDataModel.add(student3);
				listaDataModel.add(courseAssignment);
			}	
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos del actuales", e);
		}
		return listaDataModel;
}
	
	/**
	 * Obtener nombre y apellidos del tribunal
	 */
	public List<String> getTribunal(){
		List<String> listaTribunal = new ArrayList<String>();
		String sql = SELECT_ALL + FROM + TRIBUNAL + WHERE + NOMBRE_APELLIDOS + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				String cargo = result.getString(CARGO);
				String nombre = result.getString(NOMBRE_APELLIDOS);
				String filaTribunal = cargo + ": " + nombre;
				listaTribunal.add(filaTribunal);
			}
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos del tribunal", e);
		}
		return listaTribunal;
	}	
	
	/**
	 * Obtener la descripción de las normas
	 */
	public List<String> getNormas(){
		List<String> listaNormas = new ArrayList<String>();
		String sql = SELECT_ALL + FROM + NORMA + WHERE + DESCRIPCION + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				String descripcion = result.getString(DESCRIPCION);
				listaNormas.add(descripcion);
			}
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos del normas", e);
		}
		return listaNormas;
	}
	
	/**
	 * Obtener la descripción y la url de los documentos
	 */
	public List<String> getDocumentos(){
		List<String> listaDocumentos = new ArrayList<String>();
		String sql = SELECT_ALL + FROM + DOCUMENTO + WHERE + DESCRIPCION + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				String descripcion = result.getString(DESCRIPCION);
				String url = result.getString("Url"); 
				listaDocumentos.add(descripcion);
				listaDocumentos.add(url);
			}
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos del documentos", e);
		}
		return listaDocumentos;
	}
	
	/**
	 * Obtener los datos del modelo de datos de los históricos
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ArrayList getDataModelHistoric(DateTimeFormatter dateTimeFormatter) { 
		int contador=0;
		String rankingPercentile="";
		int rankingTotal=0;
		int rankingCurse=0;
		List<String> rankingsPercentile = getRankingPercentile();
		List<Integer> rankingsTotal = getRankingTotal();
		List<Integer> rankingsCurse = getRankingCurses();
		ArrayList listaDataModel=new ArrayList();
		
		String sql = SELECT_ALL + FROM + HISTORICO + WHERE + TITULO + DISTINTO_DE_VACIO;
		
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				int numStudents = 0;
				int numTutors = 0;
				String title = result.getString(TITULO_CORTO);
				String description = result.getString(DESCRIPCION);
				String tutor1 = result.getString(TUTOR1);
				if (tutor1 == null || "".equals(tutor1)) {
					tutor1 = "";
				} else {
					numTutors++;
				}
				String tutor2 = result.getString(TUTOR2);
				if (tutor2 == null || "".equals(tutor2)) {
					tutor2 = "";
				} else {
					numTutors++;
				}
				String tutor3 = result.getString(TUTOR3);
				if (tutor3 == null || "".equals(tutor3)) {
					tutor3 = "";
				} else {
					numTutors++;
				}
				String student1 = result.getString(ALUMNO1);
				if (student1 == null || "".equals(student1)) {
					student1 = "";
				} else {
					numStudents++;
				}
				String student2 = result.getString(ALUMNO2);
				if (student2 == null || "".equals(student2)) {
					student2 = "";
				} else {
					numStudents++;
				}
				String student3 = result.getString(ALUMNO3);
				if (student3 == null || "".equals(student3)) {
					student3 = "";
				} else {
					numStudents++;
				}
				LocalDate assignmentDate =  LocalDate.parse(result.getString(FECHA_ASIGNACION), dateTimeFormatter);
				LocalDate presentationDate = LocalDate.parse(result.getString(FECHA_PRESENTACION), dateTimeFormatter);
				rankingPercentile = rankingsPercentile.get(contador);
				rankingTotal = rankingsTotal.get(contador);
				rankingCurse = rankingsCurse.get(contador);
				contador++;
				Double score = result.getDouble(NOTA);
				int totalDays = result.getShort(TOTAL_DIAS);
				String repoLink = result.getString(ENLACE_REPOSITORIO);
				if (repoLink == null) {
					repoLink = "";
				}

				listaDataModel.add(title);
				listaDataModel.add(description);
				listaDataModel.add(tutor1);
				listaDataModel.add(tutor2);
				listaDataModel.add(tutor3);
				listaDataModel.add(student1);
				listaDataModel.add(student2);
				listaDataModel.add(student3);
				listaDataModel.add(numStudents);				
				listaDataModel.add(numTutors);
				listaDataModel.add(assignmentDate);
				listaDataModel.add(presentationDate);
				listaDataModel.add(score);
				listaDataModel.add(totalDays);
				listaDataModel.add(repoLink);
				listaDataModel.add(rankingPercentile);
				listaDataModel.add(rankingTotal);
				listaDataModel.add(rankingCurse);
				
			}
		} catch (SQLException e) {
			LOGGER.error("Error al obtener los datos de los históricos", e);
		}
		return listaDataModel;
	}
}
