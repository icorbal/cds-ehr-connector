package se.cambio.cds.model.facade.ehr.thinkehr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.openehr.jaxb.rm.Element;
import org.openehr.rm.datatypes.basic.DataValue;
import org.openehr.rm.datatypes.basic.DvBoolean;
import org.openehr.rm.datatypes.quantity.DvCount;
import org.openehr.rm.datatypes.quantity.DvOrdinal;
import org.openehr.rm.datatypes.quantity.DvProportion;
import org.openehr.rm.datatypes.quantity.DvQuantity;
import org.openehr.rm.datatypes.quantity.ProportionKind;
import org.openehr.rm.datatypes.quantity.datetime.DvDate;
import org.openehr.rm.datatypes.quantity.datetime.DvDateTime;
import org.openehr.rm.datatypes.quantity.datetime.DvDuration;
import org.openehr.rm.datatypes.quantity.datetime.DvTime;
import org.openehr.rm.datatypes.text.DvCodedText;
import org.openehr.rm.datatypes.text.DvText;
import org.openehr.rm.datatypes.uri.DvURI;

import se.cambio.cds.controller.guide.GuideUtil;
import se.cambio.cds.model.facade.ehr.delegate.EHRFacadeDelegate;
import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.AggregationFunctions;
import se.cambio.cds.util.Domains;
import se.cambio.cds.util.GeneratedElementInstanceCollection;
import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.exceptions.PatientNotFoundException;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.service.ThinkEhrService;


public class ThinkEHREHRFacadeDelegateImpl implements EHRFacadeDelegate{

    private static String PROPERTY_REMOTING_URL_ID="remoting.url";
    private static String PROPERTY_USERNAME_ID="username";
    private static String PROPERTY_PASSWORD_ID="password";
    private static String DEMOGRAPHIC_REPOSITORY_ID = "subjectNamespace1";
    private static String MARAND_PLUGIN_FILENAME = "marand.conf";

    //TODO Remove RM dependencies
    private static String OPENEHR_HEADER = "openEHR-EHR-";
    private static String DEFAULT_RM_NAME = "OBSERVATION";
    
    private ThinkEhrService service = null;
    private String remoteURL = null;
    private String username = null;
    private String password = null;
    private String sessionId = null;

    public ThinkEHREHRFacadeDelegateImpl(){
	try {
	    initConfig();
	} catch (IOException e) {
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

    @Override
    public Collection<ElementInstance> queryEHRElements(
	    String idPatient,
	    Collection<ArchetypeReference> archetypeReferences) 
		    throws InternalErrorException, PatientNotFoundException {
	Collection<ElementInstance> allElementInstances = new ArrayList<ElementInstance>();
	Calendar timeStart = Calendar.getInstance();
	try {
	    String ehrId = getThinkEhrService().findEhr(getSessionId(), idPatient, DEMOGRAPHIC_REPOSITORY_ID);
	    if (ehrId==null){
		throw new PatientNotFoundException(idPatient);
	    }
	    //Use the GeneratedElementInstanceCollection to avoid repeated references to archetypes and elements
	    GeneratedElementInstanceCollection geic = new GeneratedElementInstanceCollection();
	    geic.addAll(archetypeReferences, null);
	    if (geic!=null){
		Map<String, Set<String>> refMap = new HashMap<String, Set<String>>();
		for (ArchetypeReference ar : geic.getAllArchetypeReferences()) {
		    Set<String> idsElements = refMap.get(ar.getIdArchetype());
		    if (idsElements==null){
			idsElements = new HashSet<String>();
			refMap.put(ar.getIdArchetype(), idsElements);
		    }
		    idsElements.addAll(ar.getElementInstancesMap().keySet());
		}
		for (String archetypeId : refMap.keySet()) {
		    List<String> idElements = new ArrayList<String>(refMap.get(archetypeId));
		    String aql = getAql(archetypeId, idElements, ehrId);
		    Collection<ElementInstance> elementInstances = executeAql(sessionId, service, aql, archetypeId, idElements);
		    if (elementInstances!=null){
			allElementInstances.addAll(elementInstances);
		    }
		}
	    }
	}catch(Exception e){
	    throw new InternalErrorException(e);
	}
	long remaining = Calendar.getInstance().getTimeInMillis()-timeStart.getTimeInMillis();
	Logger.getLogger(ThinkEHREHRFacadeDelegateImpl.class).info("EHRQuery for "+idPatient+" in "+remaining+" ms.");
	return allElementInstances;
    }
    
    private static Collection<ElementInstance> executeAql(String sessionId, ThinkEhrService service, String aqlQuery, String idArchetype, List<String> idElements) 
	    throws IOException, JAXBException {
	// execute the population query:
	List<Object[]> rows = service.queryPopulationContent(sessionId, aqlQuery);
	Collection<ElementInstance> elementInstances = new ArrayList<ElementInstance>();
	//Output result
	for (Object[] row : rows) {
	    int i = 0;
	    for (Object object : row) {
		ArchetypeReference arAux = 
			new ArchetypeReference(Domains.EHR_ID, idArchetype, null, AggregationFunctions.ID_AGGREGATION_FUNCTION_LAST/*TODO*/);
		if (object instanceof Element){
		    Element e = ((Element)object);
		    if (e.getValue()!=null){
			DataValue dv = convertDV(e.getValue());
			ElementInstance ei = 
				new ElementInstance(idElements.get(i), dv, arAux, null, dv!=null?null:GuideUtil.NULL_FLAVOUR_CODE_NO_INFO);
			elementInstances.add(ei);
		    }
		    i++;
		}
	    }
	}
	return elementInstances;
    }

    private static String getAql(final String archetypeId, Collection<String> idElements, String ehrId){
	final StringBuilder sb = new StringBuilder();
	//TODO RM Should be resolved with another approach
	String rmName = DEFAULT_RM_NAME;
	if (archetypeId.startsWith(OPENEHR_HEADER) && archetypeId.contains(".")){
	    rmName = archetypeId.substring(OPENEHR_HEADER.length(), archetypeId.indexOf("."));
	}
	sb.append("SELECT\n");
	int count =0;
	for (String idElement : idElements) {
	    String path = idElement.substring(idElement.indexOf("/"));
	    sb.append("o").append(path);
	    count ++;
	    if (count<idElements.size()){
		sb.append(",\n");
	    }
	}
	sb.append("\nFROM\n");
	sb.append("EHR[ehr_id='").append(ehrId).append("']\n");
	sb.append("CONTAINS ").append(rmName).append(" o [").append(archetypeId).append("]\n");
	return sb.toString();
    }

    private static DataValue convertDV(org.openehr.jaxb.rm.DataValue dv){
	if(dv==null){
	    return null;
	}else if (dv instanceof org.openehr.jaxb.rm.DvCodedText){
	    org.openehr.jaxb.rm.DvCodedText dvAux = (org.openehr.jaxb.rm.DvCodedText)dv; 
	    return new DvCodedText(dvAux.getValue(), dvAux.getDefiningCode().getTerminologyId().getValue(), dvAux.getDefiningCode().getCodeString());
	}else if (dv instanceof org.openehr.jaxb.rm.DvOrdinal){
	    org.openehr.jaxb.rm.DvOrdinal dvAux = (org.openehr.jaxb.rm.DvOrdinal)dv; 
	    return new DvOrdinal(dvAux.getValue(), dvAux.getSymbol().getValue(), dvAux.getSymbol().getDefiningCode().getTerminologyId().getValue(), dvAux.getSymbol().getDefiningCode().getCodeString());
	}else if (dv instanceof org.openehr.jaxb.rm.DvQuantity){
	    org.openehr.jaxb.rm.DvQuantity dvAux = (org.openehr.jaxb.rm.DvQuantity)dv; 
	    return new DvQuantity(dvAux.getUnits(), dvAux.getMagnitude(), dvAux.getPrecision());
	}else if (dv instanceof org.openehr.jaxb.rm.DvCount){
	    org.openehr.jaxb.rm.DvCount dvAux = (org.openehr.jaxb.rm.DvCount)dv; 
	    return new DvCount((int)dvAux.getMagnitude());
	}else if (dv instanceof org.openehr.jaxb.rm.DvBoolean){
	    org.openehr.jaxb.rm.DvBoolean dvAux = (org.openehr.jaxb.rm.DvBoolean)dv; 
	    return new DvBoolean(dvAux.isValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvDateTime){
	    org.openehr.jaxb.rm.DvDateTime dvAux = (org.openehr.jaxb.rm.DvDateTime)dv; 
	    return new DvDateTime(dvAux.getValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvDate){
	    org.openehr.jaxb.rm.DvDate dvAux = (org.openehr.jaxb.rm.DvDate)dv; 
	    return new DvDate(dvAux.getValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvTime){
	    org.openehr.jaxb.rm.DvTime dvAux = (org.openehr.jaxb.rm.DvTime)dv; 
	    return new DvTime(dvAux.getValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvDuration){
	    org.openehr.jaxb.rm.DvDuration dvAux = (org.openehr.jaxb.rm.DvDuration)dv; 
	    return new DvDuration(dvAux.getValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvProportion){
	    org.openehr.jaxb.rm.DvProportion dvAux = (org.openehr.jaxb.rm.DvProportion)dv;
	    ProportionKind pk = ProportionKind.valueOf(dvAux.getType().intValue());
	    return new DvProportion(dvAux.getNumerator(), dvAux.getDenominator(), pk, dvAux.getPrecision());
	}else if (dv instanceof org.openehr.jaxb.rm.DvText){
	    org.openehr.jaxb.rm.DvText dvAux = (org.openehr.jaxb.rm.DvText)dv;
	    return new DvText(dvAux.getValue());
	}else if (dv instanceof org.openehr.jaxb.rm.DvUri){
	    org.openehr.jaxb.rm.DvUri dvAux = (org.openehr.jaxb.rm.DvUri)dv;
	    return new DvURI(dvAux.getValue());
	}else{
	    Logger.getLogger(ThinkEHREHRFacadeDelegateImpl.class).warn("DV Type unknown = '"+dv.getClass().getName()+"'");
	    return null;
	}
    }
    
    @Override
    public Collection<String> getAllEHRIds() throws InternalErrorException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Map<String, Collection<ElementInstance>> queryEHRElements(
	    Collection<String> ehrIds,
	    Collection<ArchetypeReference> archetypeReferences)
	    throws InternalErrorException, PatientNotFoundException {
	//TODO Use one AQL Query
	Map<String, Collection<ElementInstance>> elementInstancesMap = 
		new HashMap<String, Collection<ElementInstance>>();
	for (String ehrId : ehrIds) {
	    elementInstancesMap.put(ehrId, queryEHRElements(ehrId, archetypeReferences));
	}
	return elementInstancesMap;
    }

    @Override
    public boolean storeEHRElements(String ehrId,
	    Collection<ArchetypeReference> archetypeReferences)
	    throws InternalErrorException, PatientNotFoundException {
	// TODO Auto-generated method stub
	return false;
    }
}
