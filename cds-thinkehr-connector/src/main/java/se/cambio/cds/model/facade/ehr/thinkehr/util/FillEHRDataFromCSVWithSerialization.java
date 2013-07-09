package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openehr.binding.XMLBinding;
import org.openehr.binding.XMLBindingException;
import org.openehr.rm.common.archetyped.Archetyped;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.common.generic.PartyIdentified;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.EventContext;
import org.openehr.rm.composition.content.ContentItem;
import org.openehr.rm.composition.content.entry.Entry;
import org.openehr.rm.datatypes.basic.DataValue;
import org.openehr.rm.datatypes.quantity.DvQuantity;
import org.openehr.rm.datatypes.quantity.datetime.DvDateTime;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.datatypes.text.DvCodedText;
import org.openehr.rm.datatypes.text.DvText;
import org.openehr.rm.support.identification.ArchetypeID;
import org.openehr.rm.support.identification.HierObjectID;
import org.openehr.rm.support.identification.PartyRef;
import org.openehr.rm.support.identification.TemplateID;
import org.openehr.rm.support.identification.UIDBasedID;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.terminology.SimpleTerminologyService;

import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.Domains;
import se.cambio.cds.util.LocatableUtil;
import se.cambio.openehr.controller.session.OpenEHRSessionManager;
import se.cambio.openehr.controller.session.data.ArchetypeElements;
import se.cambio.openehr.controller.session.data.Archetypes;
import se.cambio.openehr.controller.session.data.Templates;
import se.cambio.openehr.controller.session.data.Units;
import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.OpenEHRConst;
import se.cambio.openehr.util.OpenEHRConstUI;
import se.cambio.openehr.util.exceptions.ArchetypeNotFoundException;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.misc.CSVReader;

import com.marand.thinkehr.service.AuditChangeType;
import com.marand.thinkehr.service.ThinkEhrService;
import com.marand.thinkehr.service.VersionLifecycleState;
import com.marand.thinkehr.util.ConversionUtils;

public class FillEHRDataFromCSVWithSerialization extends FillEHRDataFromCSV{

    public static final CodePhrase ENGLISH = new CodePhrase("ISO_639-1", "en");
    public static final CodePhrase LATIN_1 = null;//new CodePhrase("test", "iso-8859-1");
    public static DvCodedText event = null;
    private static TerminologyService ts = null;
    private static XMLBinding serializer = null;
    private int _counter = 0;

    private static Composition getComposition(Entry entry) throws Exception{
	DvText name = new DvText("composition");
	UIDBasedID id = new HierObjectID("1.11.2.3.4.5.0");
	List<ContentItem> content = new ArrayList<ContentItem>();
	//content.add(section("section one"));
	//content.add(section("section two", "observation"));
	content.add(entry);
	DvCodedText category = getEvent();
	String archetypeId = entry.getArchetypeDetails().getArchetypeId().getValue();
	String templateId = archetypeId+"_composition";
	Archetyped archetypeDetails = 
		new Archetyped(
			new ArchetypeID("openEHR-EHR-COMPOSITION.encounter.v1"),
			new TemplateID(templateId),
			"1.0");
	return new Composition(id, "openEHR-EHR-COMPOSITION.encounter.v1", name, archetypeDetails, null, 
		null, null, content, ENGLISH, context(), provider(), 
		category, territory(), getTerminologyService());
    }

    private static CodePhrase territory() {
	return new CodePhrase("ISO_3166-1", "SE");
    }

    // test context
    private static EventContext context() throws Exception {
	DvCodedText setting = new DvCodedText("setting", ENGLISH, LATIN_1,
		new CodePhrase("openehr", "229"), getTerminologyService());
	return new EventContext(null, time("2006-02-01T12:00:09.204+01:00"), null, null,
		null, setting, null, getTerminologyService());
    }

    private static PartyIdentified provider() throws Exception {
	PartyRef performer = new PartyRef(new HierObjectID("1.3.3.1.2.42.1"),
		"ORGANISATION");
	return new PartyIdentified(performer, "provider's name", null);
    }

    private static DvDateTime time(String time) throws Exception {
	return new DvDateTime(time);
    }

    private static DvCodedText getEvent() throws Exception{
	if (event==null){
	    event = new DvCodedText("event",
		    ENGLISH, LATIN_1, new CodePhrase("openehr", "433"), getTerminologyService());
	}
	return event;
    }

    protected void fillCases(InputStream is) throws InternalErrorException{
	try{
	    CSVReader reader = new CSVReader(is, Charset.defaultCharset());
	    reader.readHeaders();
	    String[] headers = reader.getHeaders();
	    _counter = 0;
	    while (reader.readRecord()){
		Collection<Entry> entries = new ArrayList<Entry>();
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
			    DataValue dv = null;
			    try{
				dv = DataValue.parseValue(rmName+","+value);
			    }catch(Exception e){
				ExceptionHandler.handle(e);
			    }
			    if (dv instanceof DvCodedText){
				DvCodedText dvCT = (DvCodedText)dv;
				if (!OpenEHRConst.LOCAL.equals(dvCT.getTerminologyId())){
				    if (!OpenEHRSessionManager.getTerminologyFacadeDelegate().isValidCodePhrase(dvCT.getDefiningCode())){
					//Ignore invalid codes elements
					Logger.getLogger(
						FillEHRDataFromCSVWithSerialization.class).warn(
							"Coded value '"+value+"' ignored. ");
					//Skip this dv
					dv = null;
				    }
				}
			    }else if (dv instanceof DvQuantity){
				DvQuantity dvQ = (DvQuantity)dv;
				Collection<String> units = Units.getUnits(idTemplate, header);
				if (units!=null && !units.contains(dvQ.getUnits())){
				    //skip this dv
				    Logger.getLogger(
					    FillEHRDataFromCSVWithSerialization.class).warn(
						    "Value '"+value+"' ignored. Unknown units.");	
				    dv=null;
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
		for (String ehrId : ehrArchetypeReferencesMap.keySet()) {
		    for (ArchetypeReference ar : ehrArchetypeReferencesMap.get(ehrId)) {
			Locatable loc = LocatableUtil.createLocatable(ar);
			if (loc instanceof Entry){
			    entries.add((Entry)loc);
			}else{
			    Logger.getLogger(FillEHRDataFromCSVWithSerialization.class).warn("Locatable not an Entry! \n"+loc.toString());
			}
		    }
		}
		if (patientId!=null){
		    fillEntries(_service, _sessionId, patientId, entries);
		}else{
		    Logger.getLogger(FillEHRDataFromCSVWithSerialization.class).warn("Patient id not found!");
		}
	    }
	}catch(Exception e){
	    throw new InternalErrorException(e);
	}
    }



    public static org.openehr.jaxb.rm.Composition transform(Composition comp) throws Exception {
	String xmlStr = getXMLSerializer().bindToXML(comp).toString();
	//CHANGE DATE FORMAT (TODO FIX)
	/*
	    private static Pattern setLinePattern = Pattern.compile("\D{4}\\-\D{2}\\-\D{2}T\D{2}\\\.createDV\\([^\\,]+,[\\s]*\"(.*)\"\\)$");
	    Matcher m = setLinePattern.matcher(xmlStr);
		while(m.find()){
		    String aux = m.group(1);
		}*/
	return (org.openehr.jaxb.rm.Composition)MarandUtil.deserialize(xmlStr.getBytes());
    }

    private static XMLBinding getXMLSerializer() throws XMLBindingException{
	if (serializer==null){
	    serializer = new XMLBinding();
	}
	return serializer;
    }

    private static TerminologyService getTerminologyService() throws Exception{
	if (ts==null){
	    ts = SimpleTerminologyService.getInstance();
	}
	return ts;
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

    private void fillEntries(ThinkEhrService service, String sessionId, String patientId, Collection<Entry> entries) throws Exception{
	String ehrId = service.findEhr(sessionId, patientId, DEMOGRAPHIC_REPOSITORY_ID);
	if (ehrId==null){
	    ehrId = service.createSubjectEhr(sessionId, patientId, DEMOGRAPHIC_REPOSITORY_ID, "Administrator");
	}
	service.useEhr(sessionId, ehrId);
	service.createContribution(sessionId);
	for (Entry entry : entries) {
	    Composition comp = getComposition(entry);
	    //comp.getContent().add(entry);
	    org.openehr.jaxb.rm.Composition marandComp = transform(comp);
	    service.createComposition(sessionId, VersionLifecycleState.COMPLETE, marandComp);
	}
	service.commitContribution(sessionId, ConversionUtils.getPartyIdentified("Jane Nurse"), "Comment", AuditChangeType.CREATION);
	//service.useEhr(sessionId, ehrId);
	_counter++;
	Logger.getLogger(FillEHRDataFromCSVWithSerialization.class).info("Patient '"+patientId+"' loaded correctly. ("+_counter+")");
    }

    public static void main(String[] args){
	try {
	    Archetypes.loadArchetypes();
	    Templates.loadTemplates();
	    FillEHRDataFromCSVWithSerialization filler = new FillEHRDataFromCSVWithSerialization();
	    if (args.length>0){
		for (String fileName : args) {
		    InputStream csvIS = new FileInputStream(new File(fileName));
		    filler.fillCSVEHRCases(csvIS);
		}
	    }else{
		InputStream csvIS = new FileInputStream(new File("ehr_basic.csv"));
		filler.fillCSVEHRCases(csvIS);
	    }
	} catch (InternalErrorException e) {
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
    }
}
