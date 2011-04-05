/*
 * Created on Jun 6, 2005
 *  
 */

package com.apelon.akcds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import com.apelon.apelonserver.client.ServerConnectionJDBC;
import com.apelon.dts.client.association.AssociationQuery;
import com.apelon.dts.client.concept.BaseConceptQuery;
import com.apelon.dts.client.concept.DTSConceptQuery;
import com.apelon.dts.client.concept.OntylogConceptQuery;
import com.apelon.dts.client.concept.SearchQuery;
import com.apelon.dts.client.concept.ThesaurusConceptQuery;
import com.apelon.dts.client.term.TermQuery;
import com.apelon.dts.client.concept.NavQuery;


/**
 * Created by
 * 
 * @author clim Created on Jun 6, 2005 Apelon, Inc.
 */
public class DbConn {
	//DTS
	public SearchQuery searchQuery;
	public ThesaurusConceptQuery thesQuery;
	public AssociationQuery associationQuery;
	public com.apelon.dts.client.namespace.NamespaceQuery nameQuery;
	public BaseConceptQuery bsq;
	public TermQuery termQry;
	public NavQuery navQry;
	public DTSConceptQuery conQry;
	public OntylogConceptQuery ontQry;

	public Connection con = null;
	private String connParams = null;
	public String host;
	public int port;
	public int namespace_ID;
	public String user;
	public String pass;
	public String instance;
	
	private void getConnParams(File f) throws FileNotFoundException, IOException, SQLException {
	    if (f.exists()) {
	     StringBuffer rv = new StringBuffer();
	     BufferedReader in =
	      new BufferedReader(
	       new InputStreamReader(new FileInputStream(f)));
	     connParams = in.readLine();
	     in.close();
	    } else {
	     BufferedWriter fo =
	      new BufferedWriter(
	       new OutputStreamWriter(new FileOutputStream(f)));
	     String connParams =
	      JOptionPane.showInputDialog(null, "Enter connection parameters (host|port|instance|user|pass|:");
	     fo.write(connParams);
	     fo.close();
	    }
	    String [] params = connParams.split("\\|");
	    host = params[0];
	    port = Integer.valueOf(params[1]).intValue();
	    instance = params[2];
	    user = params[3];
	    pass = params[4];
	    con = com.apelon.common.sql.SQL.getConnection(user, pass, host, port, instance);
	    namespace_ID = Integer.valueOf(params[5]).intValue();
	}
	
	public void connectDTS() throws IOException, SQLException {
		String curDir = System.getProperty("user.dir");
		File DTSConn = new File( curDir + "\\conn\\dts_conn_params.txt");
		getConnParams(DTSConn);
		try {
			ServerConnectionJDBC jdbcConnection1 = new ServerConnectionJDBC(
					user, pass, host, port, instance);
			jdbcConnection1.setQueryServer(Class.forName("com.apelon.dts.server.SearchQueryServer"),
					com.apelon.dts.client.common.DTSHeader.SEARCHSERVER_HEADER);
			jdbcConnection1.setQueryServer(Class.forName("com.apelon.dts.server.AssociationServer"),
					com.apelon.dts.client.common.DTSHeader.ASSOCIATIONSERVER_HEADER);
			jdbcConnection1.setQueryServer(Class.forName("com.apelon.dts.server.TermServer"),
				    com.apelon.dts.client.common.DTSHeader.TERMSERVER_HEADER);
			
			ServerConnectionJDBC jdbcConnection2 = new ServerConnectionJDBC(
					user, pass, host, port, instance);
			jdbcConnection2.setQueryServer(Class.forName("com.apelon.dts.server.ThesaurusConceptServer"),
					com.apelon.dts.client.common.DTSHeader.THESAURUSCONCEPTSERVER_HEADER);
			jdbcConnection2.setQueryServer(Class.forName("com.apelon.dts.server.OntylogConceptServer"),
					com.apelon.dts.client.common.DTSHeader.ONTYLOGCONCEPTSERVER_HEADER);
			jdbcConnection2.setQueryServer(Class.forName("com.apelon.dts.server.NamespaceServer"),
				    com.apelon.dts.client.common.DTSHeader.NAMESPACESERVER_HEADER);
			jdbcConnection2.setQueryServer(Class.forName("com.apelon.dts.server.DTSConceptServer"),
				    com.apelon.dts.client.common.DTSHeader.DTSCONCEPTSERVER_HEADER);
			jdbcConnection2.setQueryServer(Class.forName("com.apelon.dts.server.NavQueryServer"),
					com.apelon.dts.client.common.DTSHeader.NAVSERVER_HEADER);
			
			termQry = TermQuery.createInstance(jdbcConnection1);
			associationQuery = (AssociationQuery) AssociationQuery.createInstance(jdbcConnection1);
			searchQuery = (SearchQuery) SearchQuery.createInstance(jdbcConnection1);
			thesQuery = (ThesaurusConceptQuery) ThesaurusConceptQuery.createInstance(jdbcConnection2);
			nameQuery = com.apelon.dts.client.namespace.NamespaceQuery.createInstance(jdbcConnection2);
			navQry = NavQuery.createInstance(jdbcConnection2);
			conQry = DTSConceptQuery.createInstance(jdbcConnection2);
			ontQry = OntylogConceptQuery.createInstance(jdbcConnection2);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	public Connection initializeConnection(String dbName) {
        String dataSourceName = (dbName);
        String dbURL = "jdbc:odbc:" + dataSourceName;
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            con = DriverManager.getConnection(dbURL, "", "");
        } catch (Exception err) {
        }
        return con;
    }
}