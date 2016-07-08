import com.api.agm.AgmApiClient;
import com.api.alm.*;
import com.api.slack.SLChannel;
import com.api.slack.SLChatPostMessageResult;
import com.api.slack.SLMessage;
import com.api.slack.SLRuntimeApiClient;
import com.api.slack.SLUser;
import com.api.slack.SLWebApiClient;

public class SlackAgmIntegration 
{
	public static void main(String[] argc)
	{
//		SimpleEntry<String, String> proxyHostAndPort = Configuration.readProxyUrl();
//		String proxyHost = proxyHostAndPort.getKey();
//		int proxyPort = Integer.parseInt(proxyHostAndPort.getValue());
//		
//		System.out.println("Current path = " + Configuration.readAgmProjectsUrl());
//		String agmAuthenticateUrl = Configuration.readAgmAuthenticateUrl();
//		String agmProjectsUrl = Configuration.readAgmProjectsUrl();
//		String agmApplicationName = Configuration.readAgmApplicationName();
//		AlmApiClient almApiClient = new AlmApiClient(agmAuthenticateUrl, agmProjectsUrl, agmApplicationName, proxyHost, proxyPort);
//
//		String slWebApiUrl = "https://www.slack.com/api";
//		String slWebApiToken = Configuration.readSlackWebApiToken();
//		SLWebApiClient slWebApiClient = new SLWebApiClient(slWebApiUrl, slWebApiToken, proxyHost, proxyPort);
//		SLRuntimeApiClient slRuntimeApiClient = new SLRuntimeApiClient(slWebApiUrl, slWebApiToken, proxyHost, proxyPort);
		System.out.println("Gogo");
		SLLogic slLogic = new SLLogic();
		SLRuntimeLogic slRuntimeLogic = new SLRuntimeLogic();

		System.out.println("Main ended");
	}
}
