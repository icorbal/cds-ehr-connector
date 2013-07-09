package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.xmlbeans.XmlOptions;
import org.openehr.jaxb.rm.Composition;
import org.openehr.rm.composition.content.ContentItem;
import org.openehr.rm.composition.content.entry.Entry;
import org.openehr.schemas.v1.CompositionDocument;

import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.exceptions.InternalErrorException;

import com.marand.thinkehr.service.ThinkEhrService;

public class AQLUtil {
    
    public Collection<Entry> executeAql(String sessionId, ThinkEhrService service, String patientId, String aqlQuery) 
	    throws IOException, JAXBException {
	// execute the population query:
	List<Object[]> rows = service.queryEhrContent(sessionId, aqlQuery);
	//Output result. Just inserted composition must be the first in top 10 results
	Collection<Entry> entries = new ArrayList<Entry>();
	for (Object[] row : rows) {
	    for (Object object : row) {
		if (object instanceof Composition){
		    Composition comp = (Composition)object;
		    byte[] b = MarandUtil.serialize(comp, "composition");
		    //ByteArrayInputStream bais = new ByteArrayInputStream(b);
		    String str = new String(b);
		    try{
			entries.addAll(parseXML(str));
		    }catch(Exception e){
			ExceptionHandler.handle(new InternalErrorException(e));
		    }
		}
	    }
	}
	return entries;
    }
    
    public static Collection<Entry> parseXML(String str) throws Exception {
	XmlOptions xmlOption=new XmlOptions();
	Map<String,String> map=new HashMap<String,String>();
	map.put("", "http://schemas.openehr.org/v1");
	//map.put("", "http://schemas.openehr.org/v1");
	//xmlOption.setSaveAggressiveNamespaces();
	//xmlOption.setUseDefaultNamespace();

	xmlOption.setLoadSubstituteNamespaces(map);
	//Map<String,String> map2=new HashMap<String,String>();
	//map2.put("", "http://schemas.openehr.org/v1");
	//xmlOption.setSaveSuggestedPrefixes(map2);
	//xmlOption.setUseDefaultNamespace();
	//xmlOption.setSaveAggressiveNamespaces();
	CompositionDocument compDoc = CompositionDocument.Factory.parse(str, xmlOption);
	Object obj = XMLBindingDelegate.getXMLBinding().bindToRM(compDoc.getComposition());
	org.openehr.rm.composition.Composition comp = (org.openehr.rm.composition.Composition)obj;
	Collection<Entry> entries = new ArrayList<Entry>();
	if (comp!=null){
	    for (ContentItem contentItem : comp.getContent()) {
		if (contentItem instanceof Entry){
		    entries.add((Entry)contentItem);
		}else{
		    //TODO Sections!
		}
	    }
	}
	return entries;
    }
}
