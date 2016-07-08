package com.api.db;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;

public class DBWorker 
{
	private final int cMaxNoteLength = 4000;
	private String dbFilePath;
	private Connection connection;
	private String tableName;
	
	public DBWorker(String dbFilePath)
	{
		this.dbFilePath = dbFilePath;
		tableName = "notes";
	}
	
	public boolean open()
	{
	    try 
	    {
	    	File file = new File(dbFilePath);
	    	if (!file.exists())
	    	{
	    		file.createNewFile();
	    	}
	    	Class.forName("org.sqlite.JDBC");
	    	connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
	    	connection.setAutoCommit(false);
	    	String sql = "create table if not exists " + tableName + " (id BIGINT PRIMARY KEY ASC, note VARCHAR(" + cMaxNoteLength + "))";
	    	Statement stmt = connection.createStatement();
	    	stmt.executeUpdate(sql);
	    	return true;
	    } 
	    catch ( Exception e ) 
	    {
	    	System.out.println( e.getClass().getName() + ": " + e.getMessage() );
	    	return false;
	    }
	}
	
	public void close()
	{
		if (connection != null)
		{
			try
			{
				connection.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
    public ArrayList<DBNotesEntry> selectNotes()
    {
        ArrayList<DBNotesEntry> notesEntriesList = new ArrayList<DBNotesEntry>();
        if (connection != null)
        {
        	try
        	{
        		String sql = "SELECT id, note FROM " + tableName;    
	            Statement stmt = connection.createStatement();
	            ResultSet rs = stmt.executeQuery(sql);
	            while (rs.next()) 
	            {
                   DBNotesEntry entry = new DBNotesEntry();
                   entry.id = rs.getString("id");
                   entry.note = rs.getString("note");
                   notesEntriesList.add(entry);
	            }
	            rs.close();
	            stmt.close();
        	}
        	catch (Exception ex)
        	{
        		System.out.println("Exception = " + ex.getMessage());
        	}
        }
        return notesEntriesList;
    }
    
    public int insertNote(String note)
    {
        if (connection != null)
        {
            String noteWithEscaping = note.replace("\"", "\"\"");
            if (noteWithEscaping.length() < cMaxNoteLength)
            {
                long id = GetMaxNotesId() + 1;
				try
				{
	                String sql = "INSERT INTO " + tableName + " (id, note) VALUES (" + id + ", " + "\"" + noteWithEscaping + "\")";
					Statement stmt = connection.createStatement();
	                int insertedCount = stmt.executeUpdate(sql);
	                stmt.close();
	                connection.commit();
	                return insertedCount;
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
            }
        }
        return -1;
    }
    
    public int deleteNote(String id)
    {
        if (connection != null)
        {
            String sql = "DELETE FROM " + tableName + " WHERE id = \"" + id + "\"";
			try
			{
				Statement stmt = connection.createStatement();
	            int deletedCount = stmt.executeUpdate(sql);
	            stmt.close();
	            connection.commit();
	            return deletedCount;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
        }
        return -1;
    }
    
    public int deleteAllNotes()
    {
        if (connection != null)
        {
            String sql = "DELETE FROM " + tableName;
			Statement stmt;
			try
			{
				stmt = connection.createStatement();
	            int deletedCount = stmt.executeUpdate(sql);
	            stmt.close();
	            connection.commit();
	            return deletedCount;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
        }
        return -1;
    }
    
    private long GetMaxNotesId()
    {
        if (connection != null)
        {
            String sql = "SELECT (MAX(id)) from " + tableName;
            try 
            {
            	Statement stmt = connection.createStatement();
            	String idStr = stmt.executeQuery(sql).getString(1);
            	if (idStr != null)
            	{
            		long maxId = Long.parseLong(idStr);
            		return maxId;
            	}
            	else
            	{
            		return 0;
            	}
            }
            catch (NumberFormatException e)
            {
            	e.printStackTrace();
            }
			catch (SQLException e)
			{
				e.printStackTrace();
			}
        }
        return -1;
    }
}
