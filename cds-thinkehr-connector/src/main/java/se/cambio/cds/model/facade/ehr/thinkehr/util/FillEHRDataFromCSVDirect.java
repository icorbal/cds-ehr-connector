package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openehr.jaxb.am.Template;
import org.openehr.jaxb.rm.CodePhrase;
import org.openehr.jaxb.rm.Composition;
import org.openehr.jaxb.rm.DataValue;
import org.openehr.jaxb.rm.DvBoolean;
import org.openehr.jaxb.rm.DvCodedText;
import org.openehr.jaxb.rm.DvCount;
import org.openehr.jaxb.rm.DvDate;
import org.openehr.jaxb.rm.DvDateTime;
import org.openehr.jaxb.rm.DvDuration;
import org.openehr.jaxb.rm.DvOrdinal;
import org.openehr.jaxb.rm.DvProportion;
import org.openehr.jaxb.rm.DvQuantity;
import org.openehr.jaxb.rm.DvText;
import org.openehr.jaxb.rm.DvTime;
import org.openehr.jaxb.rm.DvUri;
import org.openehr.jaxb.rm.TerminologyId;

import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.Domains;
import se.cambio.openehr.controller.session.OpenEHRSessionManager;
import se.cambio.openehr.controller.session.data.ArchetypeElements;
import se.cambio.openehr.controller.session.data.Archetypes;
import se.cambio.openehr.controller.session.data.Templates;
import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.util.OpenEHRConst;
import se.cambio.openehr.util.OpenEHRConstUI;
import se.cambio.openehr.util.exceptions.ArchetypeNotFoundException;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.misc.CSVReader;

import com.google.common.collect.Lists;
import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.builder.BuilderContext;
import com.marand.thinkehr.builder.CompositionBuilder;
import com.marand.thinkehr.builder.Pathable;
import com.marand.thinkehr.service.AuditChangeType;
import com.marand.thinkehr.service.ThinkEhrService;
import com.marand.thinkehr.service.VersionLifecycleState;
import com.marand.thinkehr.templates.service.TemplateService;
import com.marand.thinkehr.util.ConversionUtils;

public class FillEHRDataFromCSVDirect extends FillEHRDataFromCSV{
    private TemplateService _templateService = null;
    private int _counter = 0;
    
    protected void fillCases(InputStream is) throws InternalErrorException{
	try{
	    _counter = 0;
	    CSVReader reader = new CSVReader(is, Charset.defaultCharset());
	    reader.readHeaders();
	    String[] headers = reader.getHeaders();
	    while (reader.readRecord()){
		String patientId = null;
		Map<String, LinkedList<ArchetypeReference>> ehrArchetypeReferencesMap = new  HashMap<String, LinkedList<ArchetypeReference>>();
		for (String header : headers) {
		    if(header.equals("idEHR")){
			patientId = reader.get(header);
		    }else if(header.trim().isEmpty()){
			continue;
		    }else{
			String value = reader.get(header);
			if (value!=null && !value.isEmpty()){
			    String idTemplate = null;
			    if (header.contains("|")){
				String[] split = header.split("\\|"); 
				header = split[0];
				idTemplate = split[1];
			    }
			    String rmName = ArchetypeElements.getArchetypeElement(idTemplate, header).getRMType();
			    org.openehr.rm.datatypes.basic.DataValue dv = org.openehr.rm.datatypes.basic.DataValue.parseValue(rmName+","+value);
			    if (dv instanceof org.openehr.rm.datatypes.text.DvCodedText){
				org.openehr.rm.datatypes.text.DvCodedText dvCT = (org.openehr.rm.datatypes.text.DvCodedText)dv;
				if (!OpenEHRConst.LOCAL.equals(dvCT.getTerminologyId())){
				    if (!OpenEHRSessionManager.getTerminologyFacadeDelegate().isValidCodePhrase(dvCT.getDefiningCode())){
					//Ignore invalid codes elements
					Logger.getLogger(
						FillEHRDataFromCSVDirect.class).warn(
							"Coded value '"+value+"' ignored. ");	
					dv = null;
					//Skip the rest
					continue;
				    }
				}
			    }
			    if (dv!=null){
				String idArchetype = header.substring(0, header.indexOf("/"));
				String idElement = header;
				ArchetypeReference ar = getArchetypeReference(idElement, idArchetype, idTemplate, ehrArchetypeReferencesMap);
				new ElementInstance(idElement, dv, ar, null, dv!=null?null:OpenEHRConstUI.NULL_FLAVOUR_CODE_NO_INFO);
			    }
			}
		    }
		}

		if (patientId!=null){
		    _counter++;
		    Collection<ArchetypeReference> ars = new ArrayList<ArchetypeReference>();
		    for (LinkedList<ArchetypeReference> arsAux : ehrArchetypeReferencesMap.values()) {
			ars.addAll(arsAux);
		    }
		    fillArchetypeReferences(_service, _sessionId, patientId, ars);
		}else{
		    Logger.getLogger(FillEHRDataFromCSVDirect.class).warn("Patient id not found!");
		}
	    }
	}catch(Exception e){
	    throw new InternalErrorException(e);
	}
    }

    private static ArchetypeReference getArchetypeReference(String idElement, String idArchetype, String idTemplate, Map<String, LinkedList<ArchetypeReference>> ehrArchetypeReferencesMap)
	    throws ArchetypeNotFoundException{
	LinkedList<ArchetypeReference> ehrInstances = ehrArchetypeReferencesMap.get(idArchetype);
	if (ehrInstances==null){
	    ehrInstances = new LinkedList<ArchetypeReference>();
	    ehrArchetypeReferencesMap.put(idArchetype, ehrInstances);
	}
	ArchetypeDTO archetypeVO = Archetypes.getArchetypeVO(idArchetype);
	ArchetypeReference ar = null;
	if (ehrInstances.isEmpty() || ehrInstances.getLast().getElementInstancesMap().containsKey(idElement)){
	    if (archetypeVO==null){
		throw new ArchetypeNotFoundException(idArchetype);
	    }
	    ar = new ArchetypeReference(Domains.EHR_ID, idArchetype, idTemplate, archetypeVO.getRMName());
	    ehrInstances.add(ar);
	}else{
	    ar = ehrInstances.getLast();
	}
	return ar;
    }

    private void fillArchetypeReferences(
	    ThinkEhrService service, 
	    String sessionId, 
	    String patientId, 
	    Collection<ArchetypeReference> archetypeReferences) throws Exception{
	String ehrId = service.findEhr(sessionId, patientId, DEMOGRAPHIC_REPOSITORY_ID);
	if (ehrId==null){
	    ehrId = service.createSubjectEhr(sessionId, patientId, DEMOGRAPHIC_REPOSITORY_ID, "Administrator");
	}
	service.useEhr(sessionId, ehrId);
	service.createContribution(sessionId);
	for (ArchetypeReference archetypeReference : archetypeReferences) {
	    Composition marandComp = buildComposition(sessionId, archetypeReference);
	    service.createComposition(sessionId, VersionLifecycleState.COMPLETE, marandComp);
	}
	//service.rollbackContribution(sessionId);
	service.commitContribution(sessionId, ConversionUtils.getPartyIdentified("Jane Nurse"), "Comment", AuditChangeType.CREATION);
	Logger.getLogger(FillEHRDataFromCSVDirect.class).info("Patient '"+patientId+"' loaded correctly. ("+_counter+")");
    }

    private Composition buildComposition(String sessionId, ArchetypeReference archetypeReference) throws Exception {
	// get template
	String idArchetype = archetypeReference.getIdArchetype();
	String idTemplate = idArchetype+"_composition";
	Template template = getTemplateService().getActiveTemplateByTemplateId(sessionId, idTemplate);

	List<Pathable> pathables = new ArrayList<Pathable>();
	int i = 0;
	for (String idElement : archetypeReference.getElementInstancesMap().keySet()) {
	    ElementInstance ei = archetypeReference.getElementInstancesMap().get(idElement);
	    if (ei.getDataValue()!=null){
		String path = idElement.substring(idElement.indexOf("/"));
		pathables.add(new Pathable(
			""+i++, //field unique id
			"/content["+idArchetype+"]"+path+"/value",
			Lists.newArrayList(convertDV(ei.getDataValue()))));
	    }
	}
	/*
	Archetyped archetyped = new Archetyped();
	ArchetypeId archetypeId  = new ArchetypeId();
	archetypeId.setValue(idArchetype);
	archetyped.setArchetypeId(archetypeId);
	archetyped.setRmVersion("1.0.1");
	pathables.add(new Pathable(
		""+i++, //field unique id
		"/content["+idArchetype+"]/archetype_details/rm_version",
		Lists.newArrayList(archetyped)));
	*/
	BuilderContext context = createContext();
	CompositionBuilder compositionBuilder = new CompositionBuilder(template);
	//create new composition with composition builder
	return compositionBuilder.build(pathables, context);
    }

    private static BuilderContext createContext() {
	BuilderContext context = new BuilderContext();
	context.setLanguage("en");
	context.setTerritory("SI");
	context.setComposerName("Jane Nurse");
	context.setEncoding("UTF-8");
	context.setEntryProvider(ConversionUtils.getPartyIdentified("Dr. James Surgeon"));
	context.setHistoryOrigin(new DateTime());
	return context;
    }

    private TemplateService getTemplateService() throws Exception{
	if (_templateService==null){
	    _templateService = RemotingUtils.getService(_remoteURL, TemplateService.class);
	}
	return _templateService;
    }

    private static DataValue convertDV(org.openehr.rm.datatypes.basic.DataValue dv){
	if(dv==null){
	    return null;
	}else if (dv instanceof org.openehr.rm.datatypes.text.DvCodedText){
	    org.openehr.rm.datatypes.text.DvCodedText dvAux = (org.openehr.rm.datatypes.text.DvCodedText)dv;
	    DvCodedText result = new DvCodedText();
	    result.setValue(dvAux.getValue());
	    CodePhrase cp = new CodePhrase();
	    cp.setCodeString(dvAux.getDefiningCode().getCodeString());
	    TerminologyId terminologyId = new TerminologyId();
	    terminologyId.setValue(dvAux.getDefiningCode().getTerminologyId().getValue());
	    cp.setTerminologyId(terminologyId);
	    result.setDefiningCode(cp);
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.DvOrdinal){
	    org.openehr.rm.datatypes.quantity.DvOrdinal dvAux = (org.openehr.rm.datatypes.quantity.DvOrdinal)dv; 
	    DvOrdinal result = new DvOrdinal();
	    result.setValue(dvAux.getValue());
	    DvCodedText symbol = new DvCodedText();
	    CodePhrase cp = new CodePhrase();
	    cp.setCodeString(dvAux.getSymbol().getDefiningCode().getCodeString());
	    TerminologyId terminologyId = new TerminologyId();
	    terminologyId.setValue(dvAux.getSymbol().getDefiningCode().getTerminologyId().getValue());
	    cp.setTerminologyId(terminologyId);
	    symbol.setDefiningCode(cp);
	    result.setSymbol(symbol);
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.DvQuantity){
	    org.openehr.rm.datatypes.quantity.DvQuantity dvAux = (org.openehr.rm.datatypes.quantity.DvQuantity)dv;
	    DvQuantity result = new DvQuantity();
	    result.setMagnitude(dvAux.getMagnitude());
	    result.setPrecision(dvAux.getPrecision());
	    result.setUnits(dvAux.getUnits());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.DvCount){
	    org.openehr.rm.datatypes.quantity.DvCount dvAux = (org.openehr.rm.datatypes.quantity.DvCount)dv;
	    DvCount result = new DvCount();
	    result.setMagnitude(dvAux.getMagnitude());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.basic.DvBoolean){
	    org.openehr.rm.datatypes.basic.DvBoolean dvAux = (org.openehr.rm.datatypes.basic.DvBoolean)dv;
	    DvBoolean result = new DvBoolean();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.datetime.DvDateTime){
	    org.openehr.rm.datatypes.quantity.datetime.DvDateTime dvAux = (org.openehr.rm.datatypes.quantity.datetime.DvDateTime)dv;
	    DvDateTime result = new DvDateTime();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.datetime.DvDate){
	    org.openehr.rm.datatypes.quantity.datetime.DvDate dvAux = (org.openehr.rm.datatypes.quantity.datetime.DvDate)dv; 
	    DvDate result = new DvDate();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.datetime.DvTime){
	    org.openehr.rm.datatypes.quantity.datetime.DvTime dvAux = (org.openehr.rm.datatypes.quantity.datetime.DvTime)dv; 
	    DvTime result = new DvTime();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.datetime.DvDuration){
	    org.openehr.rm.datatypes.quantity.datetime.DvDuration dvAux = (org.openehr.rm.datatypes.quantity.datetime.DvDuration)dv;
	    DvDuration result = new DvDuration();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.quantity.DvProportion){
	    org.openehr.rm.datatypes.quantity.DvProportion dvAux = (org.openehr.rm.datatypes.quantity.DvProportion)dv;
	    DvProportion result = new DvProportion();
	    result.setNumerator((float)dvAux.getNumerator());
	    result.setDenominator((float)dvAux.getDenominator());
	    result.setPrecision(dvAux.getPrecision());
	    result.setType(new BigInteger(""+dvAux.getType().getValue()));
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.text.DvText){
	    org.openehr.rm.datatypes.text.DvText dvAux = (org.openehr.rm.datatypes.text.DvText)dv;
	    DvText result = new DvText();
	    result.setValue(dvAux.getValue());
	    return result;
	}else if (dv instanceof org.openehr.rm.datatypes.uri.DvURI){
	    org.openehr.rm.datatypes.uri.DvURI dvAux = (org.openehr.rm.datatypes.uri.DvURI)dv;
	    DvUri result = new DvUri();
	    result.setValue(dvAux.getValue());
	    return result;
	}else{
	    Logger.getLogger(FillEHRDataFromCSVDirect.class).warn("DV Type unknown = '"+dv.getClass().getName()+"'");
	    return null;
	}
    }
    
    public static void main(String[] args){
	try {
	    Archetypes.loadArchetypes();
	    Templates.loadTemplates();
	    InputStream csvIS = FillEHRDataFromCSVDirect.class.getClassLoader().getResourceAsStream("ehr_drugs.csv");
	    FillEHRDataFromCSVDirect filler = new FillEHRDataFromCSVDirect();
	    filler.fillCSVEHRCases(csvIS);
	} catch (InternalErrorException e) {
	    e.printStackTrace();
	}
    }
}
