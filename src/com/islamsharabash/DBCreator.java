package com.islamsharabash;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.sql.*;

import org.json.*;

/**
 * 
 * @author ibash
 * @ref: http://www.zentus.com/sqlitejdbc/
 */
public class DBCreator {
	private static Statement stat = null;
	private static String dbName = "cumtdDB";
	
  // http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
  private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
  
	private static final String MTD_API_KEY = "9353bd1b2c2d4f7f814b698d5006be92";
  	private static JSONObject getStops() throws IOException, JSONException { 
	    String sURL = "http://developer.cumtd.com/api/v1.0/json/stops.getList?key=" + MTD_API_KEY;
	    return readJsonFromUrl(sURL);
  	}
  	
  	private static void createTables() throws SQLException {
	    stat.executeUpdate("CREATE TABLE android_metadata (\"locale\" TEXT DEFAULT 'en_US');");

	    String createStopTable = "CREATE TABLE stopTable (" +
	    	"_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
	    	"_favorites BOOLEAN," +
	    	"_stop VARCHAR(15) UNIQUE," +
	    	"_name VARCHAR(50)," +
	    	"_latitude INTEGER," +
	    	"_longitude INTEGER);";
	    stat.executeUpdate(createStopTable);
  	}
	
	public static void main(String args[]) throws Exception{
		// make a new db
		File dbFile = new File(dbName);
		dbFile.delete();

		
		// connect to db
		Class.forName("org.sqlite.JDBC");
	    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
	    stat = conn.createStatement();
	    
	    
		// create tables
	    createTables();
	    
		
		// getJson
	    JSONArray stops = getStops().getJSONArray("stops");
	    
	    
		// insert data
	    double million = 1000000; // lat/lon are stored in db as E6
	    String insertString = "REPLACE INTO stopTable " +
	    	"(_stop, _name, _latitude, _longitude, _favorites) VALUES " +
	    	"(?, ?, ?, ?, ?)";
	    
	    PreparedStatement insertStop = conn.prepareStatement(insertString);
	    
	    for (int i = 0; i < stops.length(); i++) {
	    	JSONObject stop = stops.getJSONObject(i);
	    	double lon = million * stop.getDouble("stop_lon");
	    	double lat = million * stop.getDouble("stop_lat");
	    	String name = stop.getString("stop_name");
	    	
	    	String stop_id = stop.getString("stop_id");
	    	// clean up stop id
	    	System.out.println(stop_id);
	    	int colonIndex = stop_id.lastIndexOf(':');
	    	stop_id = stop_id.substring(0, colonIndex);
	    	System.out.println(stop_id);
	    	
	    	insertStop.setString(1, stop_id);
	    	insertStop.setString(2, name);
	    	insertStop.setDouble(3, lat);
	    	insertStop.setDouble(4, lon);
	    	insertStop.setBoolean(5, false);
	    	insertStop.executeUpdate();
	    }
	    
		
		
		// close connection
      conn.close();
	}
}
