package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;

import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.exceptions.InternalErrorException;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.service.ThinkEhrService;

public abstract class FillEHRDataFromCSV {

    private static String PROPERTY_REMOTING_URL_ID="remoting.url";
    private static String PROPERTY_USERNAME_ID="username";
    private static String PROPERTY_PASSWORD_ID="password";
    protected static String DEMOGRAPHIC_REPOSITORY_ID = "subjectNamespace1";
    private static String CONFIG_FILENAME = "marand.plugin";
    protected String _remoteURL = null;
    protected ThinkEhrService _service = null;
    protected String _sessionId = null;
    protected String _username = null;
    protected String _password = null;

    public FillEHRDataFromCSV(){
	try {
	    InputStream is = FillEHRDataFromCSVDirect.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME);
	    Properties properties = new Properties();
	    properties.load(is);
	    _remoteURL = properties.getProperty(PROPERTY_REMOTING_URL_ID);
	    _username = properties.getProperty(PROPERTY_USERNAME_ID);
	    _password = properties.getProperty(PROPERTY_PASSWORD_ID);
	}catch(IOException e){
	    ExceptionHandler.handle(e);
	}
    }

    public void fillCSVEHRCases(InputStream is) throws InternalErrorException{
	try{
	    // establish connection to ThinkEhr server
	    _service = RemotingUtils.getService(_remoteURL, ThinkEhrService.class);
	    _sessionId = _service.login(_username, _password);
	    Calendar timeStart = Calendar.getInstance();
	    fillCases(is);
	    double totalTime = Calendar.getInstance().getTimeInMillis()-timeStart.getTimeInMillis();
	    Logger.getLogger(FillEHRDataFromCSV.class).info("Fill operation finished ("+new DecimalFormat("#.##").format(totalTime/1000)+" s)");
	} catch (Exception e) {
	    throw new InternalErrorException(e);
	}
	finally{
	    if (_service!=null && _sessionId!=null){
		_service.closeSession(_sessionId);
	    }
	}
    }

    protected abstract void fillCases(InputStream is) throws InternalErrorException;
}
