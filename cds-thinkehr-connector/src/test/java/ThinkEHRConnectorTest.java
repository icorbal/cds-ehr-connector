import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;
import se.cambio.cds.model.facade.ehr.thinkehr.ThinkEHREHRFacadeDelegateImpl;
import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.exceptions.MissingConfigurationParameterException;
import se.cambio.openehr.util.exceptions.PatientNotFoundException;
import se.cambio.util.ThinkEHRConfigurationParametersManager;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.factory.ThinkEhrConfigEnum;
import com.marand.thinkehr.factory.ThinkEhrServiceFactory;
import com.marand.thinkehr.service.ThinkEhrService;


public class ThinkEHRConnectorTest extends TestCase {

    private ThinkEhrService service;
    private static String DEMOGRAPHIC_REPOSITORY_ID = "IspekEhr";
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
	try {
	    username = ThinkEhrConfigEnum.getThinkEhrUsername();
	    password = ThinkEhrConfigEnum.getThinkEhrPassword();
	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	}
    }

    private ThinkEhrService getThinkEhrService() throws Exception{
	if (service==null){
	    service = ThinkEhrServiceFactory.getThinkEhrService();
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
