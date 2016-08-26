import java.lang.reflect.Array;
import java.time.OffsetDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import com.api.alm.AlmApiClient;
import com.api.alm.AlmDefect;
import com.api.alm.AlmReleaseBacklogItem;
import com.api.slack.SLChatPostMessageResult;
import com.api.slack.SLMessage;
import com.api.slack.SLWebApiClient;

public class SLLogic
{
	private final String DEFECT_START_LINE = "```Defect: ";
    private final String DEFECT_END_LINE = "```";
    private final String DEFECT_FIELDS_SEPARATOR = "    ";
    private final String STATUS_START_LINE = "Status: ";
    
	private final String COMMENT_START_LINE = DEFECT_START_LINE;
    private final String COMMENT_END_LINE = DEFECT_END_LINE;
    private final String COMMENT_FIELDS_SEPARATOR = DEFECT_FIELDS_SEPARATOR;
    private final String FROM_START_LINE = "From: ";
    
    private String cConfigurationFolderPath;
    private String lastPostedDefectDateTimeFilePath;
    private SLWebApiClient slWebApiClient;
    private AlmApiClient almApiClient;
    private Timer timer;
	
	public SLLogic()
	{
		timer = new Timer(false);
		restartTimer(timer);
	}
	
	private void restartTimer(Timer timer)
	{
		int timerInterval = Configuration.readAgmPullIntervalSeconds() * 1000;
		timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				try
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
					
					updateSlack();
				}
				catch (Exception e)
				{
					
				}
				restartTimer(timer);
			}
		}, timerInterval);
	}
	
	private void updateSlack()
	{
		int timerInterval = Configuration.readAgmPullIntervalSeconds() * 1000;
		
		SimpleEntry<String, String> credentials = Configuration.readAgmUserAndPassword();
		String username = credentials.getKey();
		String password = credentials.getValue();
		
		if (almApiClient.authenticate(username, password))
		{
			performSendingLogic();
		}
		else
		{
			// handle failed ALM authentication
		}
	}
	
	private void performSendingLogic()
	{
		HashMap<String, HashSet<String>> groupIDsForThemes = Configuration.readGroupIDsForThemes();
		HashSet<String> themesToNotify = new HashSet<String>(groupIDsForThemes.keySet());
		HashSet<String> groupIDsToNotify = new HashSet<String>();
		groupIDsForThemes.values().parallelStream().forEach((groupIDs) -> {	groupIDsToNotify.addAll(groupIDs); });
		
		HashMap<String, HashSet<String>> groupIDsForReleases = Configuration.readGroupIDsForReleases();
		groupIDsForReleases.values().parallelStream().forEach((groupIDs) -> { groupIDsToNotify.addAll(groupIDs); });
		
		ArrayList<String> queries = Configuration.readAgmQueries();
		queries = modifyQueries(queries);
		HashSet<AlmDefect> almDefects = getAlmDefectsForQueries(queries);
		HashMap<AlmDefect, String> themesForDefects = getThemesForAlmDefects(almDefects);
		HashMap<String, HashSet<AlmDefect>> almDefectsForThemesToNotify = extractAlmDefectsForThemesToNotify(themesToNotify, themesForDefects);
		
		HashMap<String, ArrayList<SLMessage>> messagesForGroups = getMessagesForGroups(groupIDsToNotify);
		HashMap<String, ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>>> postedEntitesForGroups = new HashMap<>();
		
		groupIDsToNotify.stream().forEach((groupID) ->
		{
			ArrayList<SLMessage> messagesForGroup = messagesForGroups.get(groupID);
			ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>> res = findPostedEntitiesInMessages(almDefects, messagesForGroup);
			postedEntitesForGroups.put(groupID, res);
		});
		
		
		themesForDefects.forEach((defect, theme) ->
		{
			if (groupIDsForReleases.containsKey(defect.target_rel))
			{
				groupIDsForReleases.get(defect.target_rel).forEach((groupID) ->
				{
					SingleDefectSendingLogic(defect, groupID, postedEntitesForGroups);
				});
			}
			else
			{
				if (groupIDsForThemes.containsKey(theme))
				{
					groupIDsForThemes.get(theme).forEach((groupID) ->
					{
						SingleDefectSendingLogic(defect, groupID, postedEntitesForGroups);
					});
				}
			}
		});
	}
	
	private void SingleDefectSendingLogic(AlmDefect defect, String groupID, HashMap<String, ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>>> postedEntitesForGroups)
	{
		ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>> postedEntitiesForGroup = postedEntitesForGroups.get(groupID);
		boolean defectFound = false;
		boolean defectStatusWasUpdated = false;
		SLDefect slackDefect = null;
		SLComment slackComment = null;
		for (int i = 0; i < postedEntitiesForGroup.size(); i++)
		{
			SimpleEntry<AlmDefect, SLPostedEntity> pair = postedEntitiesForGroup.get(i);
			AlmDefect almDefect = pair.getKey();
			slackDefect = pair.getValue().defect;
			slackComment = pair.getValue().lastComment;
		
			if (defect.id.equals(slackDefect.id))
			{
				defectFound = true;
				break;
			}
		}
		
		if (defectFound)
		{
			if (!defect.status.equals(slackDefect.status))
			{
				if (postDefect(groupID, defect))
				{
					defectStatusWasUpdated = true;
					saveLastPostedDefectDateTimeToConfiguration(defect.last_modified);
				}
				else
				{
					// handle
				}
			}
		}
		else
		{
			if (!defect.status.equals("Closed"))
			{
				if (postDefect(groupID, defect))
				{
					defectStatusWasUpdated = true;
					saveLastPostedDefectDateTimeToConfiguration(defect.last_modified);
				}
				else
				{
					// handle
				}
			}
		}
		
		if (!defect.status.equals("Closed"))
		{
			if (! defectStatusWasUpdated)
			{
				if (defect.comments.size() > 0)
				{
					String almLastComment = defect.comments.get(defect.comments.size() - 1);
					if (!almLastComment.equals(slackComment.message))
					{
						if (postComment(groupID, defect))
						{
							saveLastPostedDefectDateTimeToConfiguration(defect.last_modified);
						}
						else
						{
							// handle
						}
					}
				}
			}
		}
	}
	
	private ArrayList<String> modifyQueries(ArrayList<String> queries)
	{
		OffsetDateTime lastSuccessfulUpdateDateTime = Configuration.readLastPostedDefectDateTime();
		for (int i = 0; i < queries.size(); i++)
		{
			String query = queries.get(i);
			StringBuffer modifiedQuery = new StringBuffer();
			modifiedQuery.append(query);
			modifiedQuery.append("last-modified[>'");
			modifiedQuery.append(lastSuccessfulUpdateDateTime.getYear());
			modifiedQuery.append("-");
			modifiedQuery.append(String.format("%2s", lastSuccessfulUpdateDateTime.getMonthValue()).replace(' ', '0'));
			modifiedQuery.append("-");
			modifiedQuery.append(String.format("%2s", lastSuccessfulUpdateDateTime.getDayOfMonth()).replace(' ', '0'));
			modifiedQuery.append(" ");
			modifiedQuery.append(String.format("%2s", lastSuccessfulUpdateDateTime.getHour()).replace(' ', '0'));
			modifiedQuery.append(":");
			modifiedQuery.append(String.format("%2s", lastSuccessfulUpdateDateTime.getMinute()).replace(' ', '0'));
			modifiedQuery.append(":");
			modifiedQuery.append(String.format("%2s", lastSuccessfulUpdateDateTime.getSecond()).replace(' ', '0'));
			modifiedQuery.append("'];");
			queries.set(i, modifiedQuery.toString());
		}
		return queries;
	}
	
	private HashSet<AlmDefect> getAlmDefectsForQueries(ArrayList<String> queries)
	{
		HashSet<AlmDefect> defects = new HashSet<AlmDefect>();
		queries.parallelStream().forEach((q) ->
		{
			ArrayList<AlmDefect> almDefectsForQuery = almApiClient.getDefects(q);
			defects.addAll(almDefectsForQuery);
		});
		return defects;
	}
	
	private HashMap<AlmDefect, String> getThemesForAlmDefects(HashSet<AlmDefect> defects)
	{
		HashMap<AlmDefect, String> themesForDefects = new HashMap<AlmDefect, String>();
		defects.parallelStream().forEach((defect) -> 
		{
			ArrayList<AlmReleaseBacklogItem> releaseBacklogItems = almApiClient.getReleaseBacklogItems("entity-type[defect];entity-id[" + defect.id + "]");
			if (releaseBacklogItems.size() > 0)
			{
				String theme_id = releaseBacklogItems.get(0).theme_id;
				themesForDefects.put(defect, theme_id);
			}
		});
		return themesForDefects;
	}
	
	private HashMap<String, HashSet<AlmDefect>> extractAlmDefectsForThemesToNotify(HashSet<String> themes, HashMap<AlmDefect, String> themesForDefects)
	{
		HashMap<String, HashSet<AlmDefect>> defectsForThemesToNotify = new HashMap<String, HashSet<AlmDefect>>();
		HashSet<AlmDefect> dummyDefect = new HashSet<AlmDefect>();

		themes.forEach((theme) ->
		{
			HashSet<AlmDefect> defects = new HashSet<AlmDefect>();
			defectsForThemesToNotify.put(theme, defects);
		});
		
		themesForDefects.forEach((defect, theme) ->
		{
			if (themes.contains(theme))
			{
				defectsForThemesToNotify.get(theme).add(defect);
			}
		});

		return defectsForThemesToNotify;
	}
	
	private ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>> findPostedEntitiesInMessages(HashSet<AlmDefect> almDefects, ArrayList<SLMessage> messagesForGroup)
	{
		ArrayList<SLDefect> slDefects = new ArrayList<SLDefect>();
		ArrayList<SLComment> slComments = new ArrayList<SLComment>();
		
		messagesForGroup.parallelStream().forEachOrdered((message) ->
		{
			if (message.text.startsWith(DEFECT_START_LINE))
			{
				String[] firstLineElements = message.text.split(DEFECT_FIELDS_SEPARATOR);
                if (firstLineElements.length >= 2)
                {
                	if (firstLineElements[1].startsWith(STATUS_START_LINE))
                	{
                    	SLDefect slackDefect = new SLDefect();
                        slackDefect.id = firstLineElements[0].substring(DEFECT_START_LINE.length(), firstLineElements[0].length());
                        slackDefect.postDate = message.ts;
                        slackDefect.status = firstLineElements[1].substring(STATUS_START_LINE.length(), firstLineElements[1].length());
                        slDefects.add(slackDefect);
                	}
                	else if (firstLineElements[1].startsWith(FROM_START_LINE))
                	{
                		SLComment comment = new SLComment();
                		comment.id = firstLineElements[0].substring(COMMENT_START_LINE.length(), firstLineElements[0].length());
                		comment.message = firstLineElements[1].substring(FROM_START_LINE.length(), firstLineElements[1].length() - COMMENT_END_LINE.length());
                		comment.postDate = message.ts;
                		slComments.add(comment);
                	}
                }
			}
		});
		return getPostedEntitiesForAlmDefects(almDefects, slDefects, slComments);
	}
	
	private ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>> getPostedEntitiesForAlmDefects(HashSet<AlmDefect> almDefects, ArrayList<SLDefect> slDefects, ArrayList<SLComment> slComments)
	{
		ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>> res = new ArrayList<SimpleEntry<AlmDefect, SLPostedEntity>>();
		almDefects.parallelStream().forEach((almDefect) -> 
		{
			SLPostedEntity slackPostedEntity = new SLPostedEntity();
			SLDefect slackDefect = new SLDefect();
			SLComment slackComment = new SLComment();
			Optional<SLDefect> postedSlackDefect = slDefects.parallelStream().filter((d) -> d.id.equals(almDefect.id)).findFirst();
			Optional<SLComment> postedSlackComment = slComments.parallelStream().filter((c) -> c.id.equals(almDefect.id)).findFirst();
			if (postedSlackDefect.isPresent())
			{
				slackDefect = postedSlackDefect.get();
				slackDefect.wasPosted = true;
			}
			if (postedSlackComment.isPresent())
			{
				slackComment = postedSlackComment.get();
			}
			SLPostedEntity postedEntity = new SLPostedEntity();
			postedEntity.defect = slackDefect;
			postedEntity.lastComment = slackComment;
			SimpleEntry<AlmDefect, SLPostedEntity> entity = new SimpleEntry<AlmDefect, SLPostedEntity>(almDefect, postedEntity);
			res.add(entity);
		});
		return res;
	}
	
	private HashMap<String, ArrayList<SLMessage>> getMessagesForGroups(HashSet<String> groupIDs)
	{
		HashMap<String, ArrayList<SLMessage>> messagesForGroups = new HashMap<String, ArrayList<SLMessage>>();
		for (String groupID : groupIDs)
		{
			ArrayList<SLMessage> messagesForGroup = slWebApiClient.groupHistory(groupID);
			messagesForGroups.put(groupID, messagesForGroup);
			ArrayList<SLMessage> messagesForChannel = slWebApiClient.channelHistory(groupID);
			messagesForGroups.put(groupID, messagesForChannel);
		}
		return messagesForGroups;
	}
		
	private boolean postDefect(String group_id, AlmDefect almDefect)
	{
		String qaRole = almDefect.user_02_qalead.equals("") ? almDefect.detected_by_qa : almDefect.user_02_qalead;
        StringBuffer sb = new StringBuffer("");
        sb.append(DEFECT_START_LINE);
        sb.append(almDefect.id.replace("&", "'"));
        sb.append(DEFECT_FIELDS_SEPARATOR);
        sb.append(STATUS_START_LINE);
        sb.append(almDefect.status.replace("&", "'"));
        sb.append(DEFECT_FIELDS_SEPARATOR);
        sb.append("Severity: ");
        sb.append(almDefect.severity.replace("&", "'"));
        sb.append(DEFECT_FIELDS_SEPARATOR);
        sb.append("Devlead: ");
        sb.append(almDefect.owner_dev.replace("&", "'"));
        sb.append(DEFECT_FIELDS_SEPARATOR);
        sb.append("Qalead: ");
        sb.append(qaRole.replace("&", "'"));
        sb.append("\n");
        sb.append(almDefect.name.replace("&", "'"));
        sb.append("\n");
        sb.append(DEFECT_END_LINE);
        SLChatPostMessageResult result = slWebApiClient.chatPostMessage(group_id, sb.toString(), true, false);
        return result.ok ? true : false;
	}
	
	private boolean postComment(String group_id, AlmDefect almDefect)
	{
		if (almDefect.comments.size() > 0)
		{
	        StringBuffer sb = new StringBuffer("");
	        sb.append(COMMENT_START_LINE);
	        sb.append(almDefect.id);
	        sb.append(COMMENT_FIELDS_SEPARATOR);
	        sb.append(FROM_START_LINE);
	        sb.append(almDefect.comments.get(almDefect.comments.size() - 1));
	        sb.append(COMMENT_END_LINE);
	        SLChatPostMessageResult result = slWebApiClient.chatPostMessage(group_id, sb.toString(), true, false);
	        return result.ok ? true : false;
		}
		return false;
	}
	
	private void saveLastPostedDefectDateTimeToConfiguration(OffsetDateTime almDefectLastModified)
	{
		int compareResult = almDefectLastModified.toInstant().compareTo(Configuration.readLastPostedDefectDateTime().toInstant());
		if (compareResult > 0)
		{
			Configuration.writeLastPostedDefectDateTime(almDefectLastModified);
		}
	}
	
	class SLPostedEntity
	{
		public String id;
		public SLDefect defect;
		public SLComment lastComment;
		
		public SLPostedEntity()
		{
			id = "";
			defect = new SLDefect();
			lastComment = new SLComment();
		}
	}
	
	class SLDefect
	{
		public boolean wasPosted;
		public String id;
		public OffsetDateTime postDate;
		public String status;
		
		public SLDefect()
		{
			wasPosted = false;
			id = "";
			postDate = OffsetDateTime.MIN;
			status = "";
		}
	}
	
	class SLComment
	{
		public String id;
		public String message;
		public OffsetDateTime postDate;
		
		public SLComment()
		{
			id = "";
			message = "";
			postDate = OffsetDateTime.MIN;
		}
	}
}
