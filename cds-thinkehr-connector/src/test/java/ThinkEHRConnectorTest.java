import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;
import se.cambio.cds.model.facade.ehr.thinkehr.ThinkEHREHRFacadeDelegateImpl;
import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.exceptions.PatientNotFoundException;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.service.ThinkEhrService;


public class ThinkEHRConnectorTest extends TestCase {

    private ThinkEhrService service;
    private static String PROPERTY_REMOTING_URL_ID="remoting.url";
    private static String PROPERTY_USERNAME_ID="username";
    private static String PROPERTY_PASSWORD_ID="password";
    private static String DEMOGRAPHIC_REPOSITORY_ID = "IspekEhr";
    private static String MARAND_PLUGIN_FILENAME = "marand.conf";
    private String remoteURL = null;
    private String username = null;
    private String password = null;
    private String sessionId = null;
    private boolean setUpIsDone = false;

    public void setUp() {
	if (!setUpIsDone ){
	    try {
		initConfig();
	    } catch (IOException e) {
		ExceptionHandler.handle(e);
	    }
	    setUpIsDone = true;
	}
    }

    public void testConnect(){
	String externalEHRId = "9044408";
	try {
	    String ehrId = getThinkEhrService().findEhr(getSessionId(), externalEHRId, DEMOGRAPHIC_REPOSITORY_ID);
	    if (ehrId==null){
		throw new PatientNotFoundException(externalEHRId);
	    }
	}catch(Exception e){
	    fail(e.getMessage());
	    ExceptionHandler.handle(e);
	}
    }
    private void initConfig() throws IOException{
	InputStream is = ThinkEHREHRFacadeDelegateImpl.class.getClassLoader().getResourceAsStream(MARAND_PLUGIN_FILENAME);
	Properties properties = new Properties();
	properties.load(is);
	remoteURL = properties.getProperty(PROPERTY_REMOTING_URL_ID);
	username = properties.getProperty(PROPERTY_USERNAME_ID);
	password = properties.getProperty(PROPERTY_PASSWORD_ID);
    }

    private ThinkEhrService getThinkEhrService() throws Exception{
	if (service==null){
	    service = RemotingUtils.getService(remoteURL, ThinkEhrService.class);
	}
	return service;
    }

    private String getSessionId() throws Exception{
	if (sessionId==null){
	    sessionId = getThinkEhrService().login(username, password);
	}
	return sessionId;
    }
}
