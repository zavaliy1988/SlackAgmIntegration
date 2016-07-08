import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import com.api.alm.AlmApiClient;
import com.api.alm.AlmDefect;
import com.api.alm.AlmRequirement;
import com.api.db.DBNotesEntry;
import com.api.db.DBWorker;
import com.api.slack.SLExceptionEventArgs;
import com.api.slack.SLNormalizer;
import com.api.slack.SLRuntimeApiClient;
import com.api.slack.SLRuntimeEventArgs;
import com.api.slack.SLWebApiClient;
import com.api.slack.SLWebSocketListener;
import com.neovisionaries.ws.client.*;

public class SLRuntimeLogic implements SLWebSocketListener
{
    private final String cReplyCommand = "!reply";
    private final String cDefCommand = "!def";
    private final String cReqCommand = "!req";
    private final String cNoteCommand = "!note";
    private final String cWhereCommand = "!where";
    private final String cReadmeCommand = "!readme";
	private SLWebApiClient slWebApiClient;
	private SLRuntimeApiClient slRuntimeApiClient;
	private AlmApiClient almApiClient;
	private WebSocketAdapter webSocketAdapter;
	private Timer timer;
	private boolean webSocketConnected;
	
	public SLRuntimeLogic()
	{
		SimpleEntry<String, String> proxyHostAndPort = Configuration.readProxyUrl();
		String proxyHost = proxyHostAndPort.getKey();
		int proxyPort = Integer.parseInt(proxyHostAndPort.getValue());
		
		System.out.println("Current path = " + Configuration.readAgmProjectsUrl());
		String agmAuthenticateUrl = Configuration.readAgmAuthenticateUrl();
		String agmProjectsUrl = Configuration.readAgmProjectsUrl();
		String agmApplicationName = Configuration.readAgmApplicationName();
		almApiClient = new AlmApiClient(agmAuthenticateUrl, agmProjectsUrl, agmApplicationName, proxyHost, proxyPort);

		String slWebApiUrl = "https://www.slack.com/api";
		String slWebApiToken = Configuration.readSlackWebApiToken();
		slWebApiClient = new SLWebApiClient(slWebApiUrl, slWebApiToken, proxyHost, proxyPort);
		slRuntimeApiClient = new SLRuntimeApiClient(slWebApiUrl, slWebApiToken, proxyHost, proxyPort);
		
		slRuntimeApiClient.addListener(this);
		webSocketConnected = false;
		
		timer = new Timer(false);
		restartTimer(timer);
	}

	private void restartTimer(Timer timer)
	{
		int timerInterval = Configuration.readSlackWebSocketPullIntervalSeconds() * 1000;
		timer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				if (!webSocketConnected)
				{
					slRuntimeApiClient.authenticate();
				}
			}
		}, 0, timerInterval);
	}
	
	@Override
	public void onConnected(WebSocket webSocket)
	{
		webSocketConnected = true;
	}

	@Override
	public void onTextMessage(WebSocket webSocket, SLRuntimeEventArgs e)
	{
		webSocketConnected = true;
		// TODO Auto-generated method stub
	     if (e.text != null)
	        {
	            String[] parameters = e.text.split(" ");
	            String commandName = parameters[0];

	            switch (commandName)
	            {
	                case cReplyCommand:
	                    HandleReplyCommand(parameters, e);
	                    break;

	                case cWhereCommand:
	                    HandleWhereCommand(parameters, e);
	                    break;

	                case cReadmeCommand:
	                    HandleReadmeCommand(parameters, e);
	                    break;

	                case cDefCommand:
	                    HandleDefCommand(parameters, e);
	                    break;

	                case cReqCommand:
	                    HandleReqCommand(parameters, e);
	                    break;

	                case cNoteCommand:
	                    HandleNoteCommand(parameters, e);
	                    break;

	                default: break;
	            }
	        }
	}

	@Override
	public void onDisconnected(WebSocket webSocket)
	{
		webSocketConnected = false;
	}

	@Override
	public void onError(WebSocket webSocket, SLExceptionEventArgs err)
	{
		webSocketConnected = false;
	}
	
	private void HandleReplyCommand(String[] parameters, SLRuntimeEventArgs e)
	{
        StringBuffer text = new StringBuffer();
        if (parameters.length > 1)
        {
            for (int i = 1; i < parameters.length; i++)
            {
                text.append(parameters[i]);
                text.append(" ");
            }
        }
        slWebApiClient.chatPostMessage(e.channel, text.toString(), true, false);
	}
	
	private void HandleWhereCommand(String[] parameters, SLRuntimeEventArgs e)
	{
        try
        {
        	String hostName = InetAddress.getLocalHost().getHostName();
        	String path = Paths.get("").toAbsolutePath().toString();
        	String message = "hostname: " + hostName + "; path: " + path;
        	slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
        }
        catch (Exception ex)
        {

        }
	}
	
	private void HandleReadmeCommand(String[] parameters, SLRuntimeEventArgs e)
	{
		String readmeFilePath = Configuration.readReadmeFilePath();
		File readmeFile = new File(readmeFilePath);
		try 
		{
	        if (readmeFile.exists())
	        {
	            StringBuffer readmeContent = new StringBuffer();
	        	List<String> lines = Files.readAllLines(Paths.get(readmeFilePath));
	        	for (String line : lines)
	        	{
	        		readmeContent.append(line);
	        		readmeContent.append("\n");
	        	}
	            slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(readmeContent.toString()), true, false);
	        }
	        else
	        {
	        	Files.createFile(Paths.get(readmeFilePath));
	        }
		} 
		catch (IOException e1) 
		{
			e1.printStackTrace();
		}
	}
	
	private void HandleDefCommand(String[] parameters, SLRuntimeEventArgs e)
	{
		SimpleEntry<String, String> userAndPassword = Configuration.readAgmUserAndPassword();
				
        if (almApiClient.authenticate(userAndPassword.getKey(), userAndPassword.getValue()))
        {
            if (parameters.length == 2)
            {
                String defectId = parameters[1];

                List<AlmDefect> defects = almApiClient.getDefects("id[" + defectId + "];");
                StringBuffer text = new StringBuffer();
                if (defects.size() > 0)
                {
                    text.append("Title: " + defects.get(0).name + "\n");
                    text.append("State: " + defects.get(0).status + "\n");
                    text.append("Dev: " + defects.get(0).owner_dev + "\n");
                    text.append("QA Lead: " + defects.get(0).user_02_qalead + "\n");
                    slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                }
                else
                {
                    text.append("Either such defect doesn't exist, either connection to ALM server is broken");
                    slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                }
            }
            else if (parameters.length == 3)
            {
                String userType = parameters[1];
                if (userType.equals("-d"))
                {
                    String developerEmail = parameters[2];
                    developerEmail = tryParseEmail(developerEmail);
                    List<AlmDefect> defects = almApiClient.getDefects("planned-closing-ver[Not \"Obsolete\"];owner[" + developerEmail + "];status[Not \"Closed\"];");
                    StringBuilder text = new StringBuilder("");
                    if (defects.size() > 0)
                    {
                        for (AlmDefect defect : defects)
                        {
                            text.append(defect.id + " - ");
                            text.append(defect.name + "\n");
                            slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                            text.setLength(0);
                        }
                    }
                    else
                    {
                        text.append("There is no 'Not Closed' defect on developer + " + developerEmail + ", or developer name specified incorrectly");
                        slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                    }
                }
                else if (userType.equals("-ql"))
                {
                    String qaleadEmail = parameters[2];
                    qaleadEmail = tryParseEmail(qaleadEmail);
                    ArrayList<AlmDefect> defects = almApiClient.getDefects("planned-closing-ver[Not \"Obsolete\"];user-40[" + qaleadEmail + "];status[Not \"Closed\"];");
                    StringBuilder text = new StringBuilder("");
                    if (defects.size() > 0)
                    {
                        for (AlmDefect defect : defects)
                        {
                            text.append(defect.id + " - ");
                            text.append(defect.name + "\n");
                            slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                            text.setLength(0);
                        }
                    }
                    else
                    {
                        text.append("There is no 'Not Closed' defect on qalead " + qaleadEmail + ", or qalead name specified incorrectly");
                        slWebApiClient.chatPostMessage(e.channel,  text.toString(), true, true);
                    }
                }
            }
        }
        else
        {
        	slWebApiClient.chatPostMessage(e.channel, "Connection to ALM server is broken", true, true);
        }
	}
	
	private void HandleReqCommand(String[] parameters, SLRuntimeEventArgs e)
	{
		SimpleEntry<String, String> userAndPassword = Configuration.readAgmUserAndPassword();
		
        if (almApiClient.authenticate(userAndPassword.getKey(), userAndPassword.getValue()))
        {
            if (parameters.length == 2)
            {
                String reqId = parameters[1];
                ArrayList<AlmRequirement> requirements = almApiClient.getRequirements("id[" + reqId + "];");
                StringBuffer text = new StringBuffer();
                if (requirements.size() > 0)
                {
                    AlmRequirement requirement = requirements.get(0);
                    text.append("Title: " + requirement.name + "\n");
                    text.append("State: " + requirement.cover_status + "\n");
                    text.append("Dev: " + requirement.owner_author + "\n");
                    text.append("QA Lead: " + requirement.user_03_qalead + "\n");
                    // text.append("Sprint: " + requirement.user_90_sprint + "\n");
                    slWebApiClient.chatPostMessage(e.channel,  text.toString(),  true, true);
                }
                else
                {
                    text.append("Either such requirement doesn't exist, either connection to ALM server is broken");
                    slWebApiClient.chatPostMessage(e.channel,  text.toString(),  true, true);
                }
            }
            else if (parameters.length == 5)
            {
                String userType = parameters[1];
                String sprintOption = parameters[3];
                if ((userType.equals("-d")) && sprintOption.equals("-s"))
                {
                    if (almApiClient.authenticate(userAndPassword.getKey(), userAndPassword.getValue()))
                    {
                        String developerEmail = parameters[2];
                        developerEmail = tryParseEmail(developerEmail);
                        String sprintNumber = parameters[4];
                        List<AlmRequirement> requirements;
						try 
						{
							requirements = almApiClient.getRequirements("user-17[" +  URLEncoder.encode(developerEmail, "UTF-8") + "];user-90[" + URLEncoder.encode(sprintNumber, "UTF-8") + "];user-06[(Not \"5-Done\") And (Not \"8-Done\")];");
	                        StringBuffer text = new StringBuffer();
	                        if (requirements.size() > 0)
	                        {
	                            for (AlmRequirement requirement : requirements)
	                            {
	                                text.append(requirement.id);
	                                text.append(" - ");
	                                text.append(requirement.name);
	                                text.append("\n");
	                                slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
	                                text.setLength(0);
	                            }
	                        }
	                        else
	                        {
	                            text.append("There is no 'Not Done' requirements on this developer, or developer name specified incorrectly");
	                            slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
	                        }
						} 
						catch (UnsupportedEncodingException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

                    }
                }
                else if (userType.equals("-ql") && sprintOption.equals("-s"))
                {
                    String qaleadEmail = parameters[2];
                    qaleadEmail = tryParseEmail(qaleadEmail);
                    String sprintNumber = parameters[4];
                    List<AlmRequirement> requirements = almApiClient.getRequirements("user-27[" + qaleadEmail + "];user-90[" + sprintNumber + "];user-06[(Not \"5-Done\") And (Not \"8-Done\")];");
                    StringBuilder text = new StringBuilder("");
                    if (requirements.size() > 0)
                    {
                        for (AlmRequirement requirement : requirements)
                        {
                            text.append(requirement.id);
                            text.append(" - ");
                            text.append(requirement.name);
                            text.append("\n");
                            slWebApiClient.chatPostMessage(e.channel, text.toString(), true, true);
                            text.setLength(0);
                        }
                    }
                    else
                    {
                        text.append("There is no 'Not Done' requirements on this qalead, or qalead name specified incorrectly");
                        slWebApiClient.chatPostMessage(e.channel, "Connection to ALM server is broken", true, true);
                    }
                }
            }
        }
        else
        {
        	slWebApiClient.chatPostMessage(e.channel, "Connection to ALM server is broken", true, true);
        }
	}
	
	private void HandleNoteCommand(String[] parameters, SLRuntimeEventArgs e)
	{
        if (parameters.length >= 2)
        {
            String option = parameters[1];
            String databaseFolderPath = Configuration.readDatabaseFolderPath();
            DBWorker dbWorker = new DBWorker(databaseFolderPath + "\\" + e.user + ".sqlite");
            dbWorker.open();
            if (option.equals("-l"))
            {
                ArrayList<DBNotesEntry> dbNotesEntriesList = dbWorker.selectNotes();
                if (dbNotesEntriesList.size() > 0)
                {
                    for (DBNotesEntry dbNoteEntry : dbNotesEntriesList)
                    {
                        StringBuilder message = new StringBuilder("");
                        message.append("Id: ");
                        message.append(dbNoteEntry.id);
                        message.append("\n");
                        message.append("Note: ");
                        message.append(dbNoteEntry.note);
                        message.append("\n");
                        slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message.toString()), true, true);
                    }
                }
                else
                {
                    String message = "There is 0 notes found in database";
                    slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                }
            }
            else if (option.equals("-d"))
            {
                if (parameters.length >= 3)
                {
                    String id = parameters[2];
                    if (dbWorker.deleteNote(id) > 0)
                    {
                        String message = "Note " + id + " was deleted";
                        slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                    }
                    else
                    {
                        String message = "Note was not deleted or there is no note to delete";
                        slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                    }
                }
            }
            else if (option.equals("-da"))
            { 
                int deletedNotesCount = dbWorker.deleteAllNotes();
                if (deletedNotesCount > 0)
                {
                    String message = deletedNotesCount + " notes was deleted";
                    slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                }
                else
                {
                    String message = "Notes was not deleted or there is no notes to delete";
                    slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                }
            }
            else
            {
                String note = e.text.substring((cNoteCommand + " ").length());
                if ((dbWorker.insertNote(SLNormalizer.denormalize(note))) > 0)
                {
                    String message = "Note was added";
                    slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message.toString()), true, true);
                }
                else
                {
                    String message = "For some reason note was not inserted";
                    slWebApiClient.chatPostMessage(e.channel, slWebApiClient.wrapWithConsoles(message), true, true);
                }
            }
            dbWorker.close();
        }
	}
	
	private String tryParseEmail(String email)
	{
        int lastIndexOfOrBar = email.lastIndexOf("|");
        if (email.length() > (lastIndexOfOrBar + 2))
        {
        	email = email.substring(lastIndexOfOrBar + 1, email.length() - 1);
        }
        return email;
	}

}
