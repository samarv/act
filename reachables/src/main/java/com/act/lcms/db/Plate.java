package com.act.lcms.db;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Plate {
  public static final String TABLE_NAME = "plates";

  public enum CONTENT_TYPE {
    LCMS,
    STANDARD,
    DELIVERED_STRAIN,
    INDUCTION,
    PREGROWTH,
  }

  // TODO: it might be easier to use parts of Spring-Standalone to do named binding in these queries.
  protected static final List<String> ALL_FIELDS = Collections.unmodifiableList(Arrays.asList(
      "id", // 1
      "name", // 2
      "description", // 3
      "barcode", // 4
      "location", // 5
      "plate_type", // 6
      "solvent", // 7
      "temperature",  // 8
      "content_type" // 9
  ));
  // id is auto-generated on insertion.
  protected static final List<String> INSERT_UPDATE_FIELDS =
      Collections.unmodifiableList(ALL_FIELDS.subList(1, ALL_FIELDS.size()));

  protected static List<Plate> platesFromResultSet(ResultSet resultSet) throws SQLException {
    List<Plate> results = new ArrayList<>();
    while (resultSet.next()) {
      Integer id = resultSet.getInt(1);
      String name = resultSet.getString(2);
      String description = resultSet.getString(3);
      String barcode = resultSet.getString(4);
      String location = resultSet.getString(5);
      String plateType = resultSet.getString(6);
      String solvent = resultSet.getString(7);
      Integer temperature = resultSet.getInt(8);
      if (resultSet.wasNull()) {
        temperature = null;
      }
      CONTENT_TYPE contentType = CONTENT_TYPE.valueOf(resultSet.getString(9));

      results.add(new Plate(id, name, description, barcode, location, plateType, solvent, temperature, contentType));
    }
    return results;
  }

  protected static Plate expectOneResult(ResultSet resultSet, String queryErrStr) throws SQLException{
    List<Plate> results = platesFromResultSet(resultSet);
    if (results.size() > 1) {
      throw new SQLException("Found multiple results where one or zero expected: %s", queryErrStr);
    }
    if (results.size() == 0) {
      return null;
    }
    return results.get(0);
  }


  // Select
  public static final String QUERY_GET_PLATE_BY_ID = StringUtils.join(new String[]{
      "SELECT", StringUtils.join(ALL_FIELDS, ", "),
      "from", TABLE_NAME,
      "where id = ?",
  }, " ");

  public static Plate getPlateById(DB db, Integer id) throws SQLException {
    try (PreparedStatement stmt = db.getConn().prepareStatement(QUERY_GET_PLATE_BY_ID)) {
      stmt.setInt(1, id);
      try (ResultSet resultSet = stmt.executeQuery()) {
        return expectOneResult(resultSet, String.format("id = %d", id));
      }
    }
  }

  public static final String QUERY_GET_PLATE_BY_BARCODE = StringUtils.join(new String[]{
      "SELECT", StringUtils.join(ALL_FIELDS, ", "),
      "from", TABLE_NAME,
      "where barcode = ?",
  }, " ");

  public static Plate getPlateByBarcode(DB db, String barcode) throws SQLException {
    try (PreparedStatement stmt = db.getConn().prepareStatement(QUERY_GET_PLATE_BY_BARCODE)) {
      stmt.setString(1, barcode);
      try (ResultSet resultSet = stmt.executeQuery()) {
        return expectOneResult(resultSet, String.format("barcode = %s", barcode));
      }
    }
  }

  public static final String QUERY_GET_PLATE_BY_NAME = StringUtils.join(new String[]{
      "SELECT", StringUtils.join(ALL_FIELDS, ", "),
      "from", TABLE_NAME,
      "where name = ?",
  }, " ");

  public static Plate getPlateByName(DB db, String name) throws SQLException {
    try (PreparedStatement stmt = db.getConn().prepareStatement(QUERY_GET_PLATE_BY_NAME)) {
      stmt.setString(1, name);
      try (ResultSet resultSet = stmt.executeQuery()) {
        return expectOneResult(resultSet, String.format("name = %s", name));
      }
    }
  }

  // Insert/Update
  public static final String QUERY_INSERT_PLATE = StringUtils.join(new String[] {
      "INSERT INTO", TABLE_NAME, "(", StringUtils.join(INSERT_UPDATE_FIELDS, ", "), ") VALUES (",
      "?,", // 1 = name
      "?,", // 2 = description
      "?,", // 3 = barcode
      "?,", // 4 = location
      "?,", // 5 = plate_type
      "?,", // 6 = solvent
      "?,", // 7 = temperature
      "?",  // 8 = contentType
      ")"
  }, " ");

  protected static void bindInsertOrUpdateParameters(
      PreparedStatement stmt, String name, String description, String barcode, String location,
      String plateType, String solvent, Integer temperature, CONTENT_TYPE contentType) throws SQLException {
    stmt.setString(1, name.trim());
    if (description != null) {
      stmt.setString(2, description.trim());
    } else {
      stmt.setNull(2, Types.VARCHAR);
    }
    if (barcode != null) {
      stmt.setString(3, barcode.trim());
    } else {
      stmt.setNull(3, Types.VARCHAR);
    }
    stmt.setString(4, location.trim());
    stmt.setString(5, plateType.trim());
    if (solvent != null) {
      stmt.setString(6, solvent.trim());
    } else {
      stmt.setNull(6, Types.VARCHAR);
    }
    if (temperature == null) {
      stmt.setNull(7, Types.INTEGER);
    } else {
      stmt.setInt(7, temperature);
    }
    stmt.setString(8, contentType.name());
  }

  protected static void bindInsertOrUpdateParameters(PreparedStatement stmt, Plate plate) throws SQLException {
    bindInsertOrUpdateParameters(stmt, plate.getName(), plate.getDescription(), plate.getBarcode(),
        plate.getLocation(), plate.getPlateType(), plate.getSolvent(), plate.getTemperature(), plate.getContentType());
  }

  public static Plate insertPlate(DB db,
                                  String name, String description, String barcode, String location, String plateType,
                                  String solvent, Integer temperature, CONTENT_TYPE contentType) throws SQLException {
    Connection conn = db.getConn();
    try (PreparedStatement stmt = conn.prepareStatement(QUERY_INSERT_PLATE, Statement.RETURN_GENERATED_KEYS)) {
      bindInsertOrUpdateParameters(stmt, name, description, barcode, location,
          plateType, solvent, temperature, contentType);
      stmt.executeUpdate();
      try (ResultSet resultSet = stmt.getGeneratedKeys()) {
        if (resultSet.next()) {
          // Get auto-generated id.
          int id = resultSet.getInt(1);
          return new Plate(id, name, description, barcode, location, plateType, solvent, temperature, contentType);
        } else {
          System.err.format("ERROR: could not retrieve autogenerated key for plate %s\n", name);
          return null;
        }
      }
    }
  }

  protected static final List<String> UPDATE_STATEMENT_FIELDS_AND_BINDINGS;

  static {
    List<String> fields = new ArrayList<>(INSERT_UPDATE_FIELDS.size());
    for (String field : INSERT_UPDATE_FIELDS) {
      fields.add(String.format("%s = ?", field));
    }
    UPDATE_STATEMENT_FIELDS_AND_BINDINGS = Collections.unmodifiableList(fields);
  }
  public static final String QUERY_UPDATE_PLATE_BY_ID = StringUtils.join(new String[] {
      "UPDATE ", TABLE_NAME, "SET",
      StringUtils.join(UPDATE_STATEMENT_FIELDS_AND_BINDINGS.iterator(), ", "),
      "WHERE",
      "id = ?", // 7
  }, " ");
  public static boolean updatePlate(DB db, Plate plate) throws SQLException {
    Connection conn = db.getConn();
    try (PreparedStatement stmt = conn.prepareStatement(QUERY_UPDATE_PLATE_BY_ID)) {
      bindInsertOrUpdateParameters(stmt, plate);
      stmt.setInt(UPDATE_STATEMENT_FIELDS_AND_BINDINGS.size() + 1, plate.getId());
      return stmt.executeUpdate() > 0;
    }
  }

  public static Plate getOrInsertFromPlateComposition(DB db, PlateCompositionParser parser, CONTENT_TYPE contentType)
      throws SQLException {
    Plate p = Plate.getPlateByName(db, parser.getPlateProperties().get("name"));
    if (p == null) {
      Map<String, String> attrs = parser.getPlateProperties();
      // TODO: check for errors and make these proper constants.
      String tempStr = attrs.get("temperature");
      if (tempStr == null || tempStr.isEmpty()) {
        tempStr = attrs.get("storage");
      }
      Integer temperature = tempStr == null || tempStr.isEmpty() ? null : Integer.parseInt(tempStr.trim());
      p = Plate.insertPlate(db, attrs.get("name"), attrs.get("description"), attrs.get("barcode"),
          attrs.get("location"), attrs.get("plate_type"), attrs.get("solvent"), temperature, contentType);
    }
    return p;
  }

  // Class Definition
  private Integer id;
  private String name;
  private String description;
  private String barcode;
  private String location;
  private String plateType;
  private String solvent;
  private Integer temperature;
  private CONTENT_TYPE contentType;

  protected Plate(Integer id, String name, String description, String barcode, String location,
                  String plateType, String solvent, Integer temperature, CONTENT_TYPE contentType) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.barcode = barcode;
    this.location = location;
    this.plateType = plateType;
    this.solvent = solvent;
    this.temperature = temperature;
    this.contentType = contentType;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getPlateType() {
    return plateType;
  }

  public void setPlateType(String plateType) {
    this.plateType = plateType;
  }

  public String getSolvent() {
    return solvent;
  }

  public void setSolvent(String solvent) {
    this.solvent = solvent;
  }

  public Integer getTemperature() {
    return temperature;
  }

  public void setTemperature(Integer temperature) {
    this.temperature = temperature;
  }

  public CONTENT_TYPE getContentType() {
    return contentType;
  }

  public void setContentType(CONTENT_TYPE contentType) {
    this.contentType = contentType;
  }
}
