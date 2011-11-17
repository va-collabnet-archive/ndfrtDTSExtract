package com.apelon.akcds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.JOptionPane;

import com.apelon.apelonserver.client.ApelonException;
import com.apelon.apelonserver.client.ServerConnection;
import com.apelon.apelonserver.client.ServerConnectionJDBC;
import com.apelon.apelonserver.client.ServerConnectionSecureSocket;
import com.apelon.apelonserver.client.ServerConnectionSocket;
import com.apelon.dts.client.association.AssociationQuery;
import com.apelon.dts.client.concept.BaseConceptQuery;
import com.apelon.dts.client.concept.DTSConceptQuery;
import com.apelon.dts.client.concept.NavQuery;
import com.apelon.dts.client.concept.OntylogConceptQuery;
import com.apelon.dts.client.concept.SearchQuery;
import com.apelon.dts.client.concept.ThesaurusConceptQuery;
import com.apelon.dts.client.term.TermQuery;

/**
 * Read DTS Server connection parameters from a text file, and create a DTS connection, along with various query objects.
 * 
 * @author clim Created on Jun 6, 2005 Apelon, Inc.
 *
 * @author Daniel Armbrust
 * 
 * Modified to add support for 3 different connection types:
 * 
 * # Local DB JDBC Connection:
 * # DB|namespace|host|port|user|password|instance
 *
 * # DTS ServerConnectionSecureSocket
 * # SCSS|namespace||host|port|user|password
 *
 * # DTS ServerConnectionSocket
 * # SCS|namespace|host|port
 */
public class DbConn
{
	// DTS
	public SearchQuery searchQuery;
	public ThesaurusConceptQuery thesQuery;
	public AssociationQuery associationQuery;
	public com.apelon.dts.client.namespace.NamespaceQuery nameQuery;
	public BaseConceptQuery bsq;
	public TermQuery termQry;
	public NavQuery navQry;
	public DTSConceptQuery conQry;
	public OntylogConceptQuery ontQry;

	private int namespace_;
	private String connectionInfo_;
	
	public int getNamespace()
	{
		return namespace_;
	}
	
	public String toString()
	{
		return (connectionInfo_ == null ? "No DTS connection" : connectionInfo_);
	}

	private ServerConnection getConnection(File parametersFile) throws ApelonException, IOException
	{
		String connParams = null;
		if (parametersFile.exists())
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(parametersFile)));

			while (connParams == null)
			{
				String line = in.readLine();
				if (line == null)
				{
					break;
				}
				if (!line.startsWith("#") && line.length() > 0)
				{
					connParams = line;
					break;
				}
			}
			in.close();
		}
		else
		{
			BufferedWriter fo = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(parametersFile)));
			connParams = JOptionPane.showInputDialog(null, "Enter connection parameters (DB|namespace|host|port|user|password|instance|)" + 
					" OR (SCSS|namespace||host|port|user|password) OR (SCS|namespace|host|port)");
			fo.write(connParams);
			fo.close();
		}

		String[] params = connParams.split("\\|");
		
		namespace_ = Integer.parseInt(params[1]);
		String host = params[2];
		int port = Integer.parseInt(params[3]);
		
		ServerConnection sc = null;
		if (params[0].equals("DB"))
		{
			String user = params[4];
			String password = params[5];
			String instance = params[6];
			sc = new ServerConnectionJDBC(user, password, host, port, instance);
			connectionInfo_ = "ServerConnectionJDBC " + user + "@" + host + ":" + port + " instance: " + instance + " namespace: " + namespace_;	
		}
		else if (params[0].equals("SCSS"))
		{
			String user = params[4];
			String password = params[5];
			sc = new ServerConnectionSecureSocket(host, port, user, password);
			connectionInfo_ = "ServerConnectionSecureSocket " + user + "@" + host + ":" + port + " namespace: " + namespace_;
		}
		else if (params[0].equals("SCS"))
		{
			sc = new ServerConnectionSocket(host, port);
			connectionInfo_ = "ServerConnectionSocket " + host + ":" + port + " namespace: " + namespace_;
		}
		else
		{
			throw new IOException("Invalid Server connection file format");
		}
		return sc;
	}

	public void connectDTS(File connectionParametersFile) throws ApelonException, IOException, ClassNotFoundException
	{
		ServerConnection connection = getConnection(connectionParametersFile);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.SearchQueryServer"),
				com.apelon.dts.client.common.DTSHeader.SEARCHSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.AssociationServer"),
				com.apelon.dts.client.common.DTSHeader.ASSOCIATIONSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.TermServer"),
				com.apelon.dts.client.common.DTSHeader.TERMSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.ThesaurusConceptServer"),
				com.apelon.dts.client.common.DTSHeader.THESAURUSCONCEPTSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.OntylogConceptServer"),
				com.apelon.dts.client.common.DTSHeader.ONTYLOGCONCEPTSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.NamespaceServer"),
				com.apelon.dts.client.common.DTSHeader.NAMESPACESERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.DTSConceptServer"),
				com.apelon.dts.client.common.DTSHeader.DTSCONCEPTSERVER_HEADER);
		connection.setQueryServer(Class.forName("com.apelon.dts.server.NavQueryServer"),
				com.apelon.dts.client.common.DTSHeader.NAVSERVER_HEADER);

		termQry = TermQuery.createInstance(connection);
		associationQuery = (AssociationQuery) AssociationQuery.createInstance(connection);
		searchQuery = (SearchQuery) SearchQuery.createInstance(connection);
		thesQuery = (ThesaurusConceptQuery) ThesaurusConceptQuery.createInstance(connection);
		nameQuery = com.apelon.dts.client.namespace.NamespaceQuery.createInstance(connection);
		navQry = NavQuery.createInstance(connection);
		conQry = DTSConceptQuery.createInstance(connection);
		ontQry = OntylogConceptQuery.createInstance(connection);
	}
}