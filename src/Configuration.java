import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.AbstractMap.SimpleEntry;

public class Configuration
{
	private static final String rootProjectFolderPath;
	private static final String configurationFolderPath;
	private static final String readmeFilePath;
	private static final String databaseFolderPath;
	private static final String agmProjectsUrlFilePath;  
	private static final String agmAuthenticateUrlFilePath;
	private static final String agmApplicationNameFilePath;
	private static final String agmPullIntervalFilePath;
	private static final String agmQueriesFilePath;
	private static final String agmUserAndPasswordFilePath;
	private static final String groupIDsForThemesFilePath;
	private static final String lastPostedDefectDateTimeFilePath;
	private static final String proxyUrlFilePath;
	private static final String slackWebApiTokenFilePath;
	private static final String slackWebSocketPullInterval;
	
	static
	{
		rootProjectFolderPath = Paths.get("").toAbsolutePath().toString();
		databaseFolderPath = rootProjectFolderPath + "\\database";
		readmeFilePath = rootProjectFolderPath + "\\readme\\readme.txt";
		configurationFolderPath = rootProjectFolderPath + "\\configuration";
		agmProjectsUrlFilePath = configurationFolderPath + "\\agmProjectsUrl.txt";
		agmAuthenticateUrlFilePath = configurationFolderPath + "\\agmAuthenticateUrl.txt";
		agmApplicationNameFilePath = configurationFolderPath + "\\agmApplicationName.txt";
		agmPullIntervalFilePath = configurationFolderPath + "\\agmPullInterval.txt";
		agmQueriesFilePath = configurationFolderPath + "\\agmQueries.txt";
		agmUserAndPasswordFilePath = configurationFolderPath + "\\agmUserAndPassword.txt";
		groupIDsForThemesFilePath = configurationFolderPath + "\\groupIDsForThemes.txt";
		lastPostedDefectDateTimeFilePath = configurationFolderPath + "\\lastPostedDefectDateTime.txt";
		proxyUrlFilePath = configurationFolderPath + "\\proxyUrl.txt";
		slackWebApiTokenFilePath = configurationFolderPath + "\\slackWebApiToken.txt";
		slackWebSocketPullInterval = configurationFolderPath + "\\slackWebSocketPullInterval.txt";
	}
	
	public static String readDatabaseFolderPath()
	{
		return databaseFolderPath;
	}
	
	public static String readReadmeFilePath()
	{
		return readmeFilePath;
	}
	
	public static String readAgmAuthenticateUrl()
	{
		return readLineFromFile(agmAuthenticateUrlFilePath);
	}
	
	public static String readAgmProjectsUrl()
	{
		return readLineFromFile(agmProjectsUrlFilePath);
	}
	
	public static String readAgmApplicationName()
	{
		return readLineFromFile(agmApplicationNameFilePath);
	}
	
	public static int readAgmPullIntervalSeconds()
	{
		String line = readLineFromFile(agmPullIntervalFilePath);
		return Integer.parseInt(line);
	}
	
	public static ArrayList<String> readAgmQueries()
	{
		return readLinesFromFile(agmQueriesFilePath);
	}
	
	public static SimpleEntry<String, String> readAgmUserAndPassword()
	{
		return readKeyAndValueFromFile(agmUserAndPasswordFilePath, " ");
	}
	
	public static HashMap<String, HashSet<String>> readGroupIDsForThemes()
	{
		HashMap<String, HashSet<String>> groupIDsForThemes = new HashMap<String, HashSet<String>>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(groupIDsForThemesFilePath)))
		{
			String partsSeparator = "/";
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				String[] parts = line.split(partsSeparator);
				if (parts.length >= 2)
				{
					String theme = parts[0];
					String groupID = parts[1];
					if (groupIDsForThemes.containsKey(theme))
					{
						groupIDsForThemes.get(theme).add(groupID);
					}
					else
					{
						HashSet<String> groupIDs = new HashSet<String>();
						groupIDs.add(groupID);
						groupIDsForThemes.put(theme, groupIDs);
					}
				}
			}
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return groupIDsForThemes;
	}
	
	public static OffsetDateTime readLastPostedDefectDateTime()
	{
		String line = readLineFromFile(lastPostedDefectDateTimeFilePath);
		OffsetDateTime offsetDateTime = OffsetDateTime.parse(line);
		return offsetDateTime;
	}
	
	public static void writeLastPostedDefectDateTime(OffsetDateTime dateTime)
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(lastPostedDefectDateTimeFilePath)))
		{
			//2016-03-27T13:35:57+3:00
			String line = dateTime.toString();
			writer.write(line);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static SimpleEntry<String, String> readProxyUrl()
	{
		return readKeyAndValueFromFile(proxyUrlFilePath, " ");
	}
	
	public static String readSlackWebApiToken()
	{
		return readLineFromFile(slackWebApiTokenFilePath);
	}
	
	public static int readSlackWebSocketPullIntervalSeconds()
	{
		String line = readLineFromFile(slackWebSocketPullInterval);
		return Integer.parseInt(line);
	}
	
	private static String readLineFromFile(String filePath)
	{
		String line = "";
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			line = reader.readLine();
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}
	
	private static ArrayList<String> readLinesFromFile(String filePath)
	{
		ArrayList<String> lines = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				lines.add(line);
			}
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines;
	}
	
	private static SimpleEntry<String, String> readKeyAndValueFromFile(String filePath, String separator)
	{
		SimpleEntry<String, String> entry = new SimpleEntry<String, String>("",  "");
		String line = "";
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			line = reader.readLine();
			String[] parts = line.split(separator);
			if (parts.length >= 2)
			{
				entry = new SimpleEntry<String, String>(parts[0], parts[1]);
			}
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return entry;
	}
	
}
