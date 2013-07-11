package se.cambio.cds.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.openehr.jaxb.rm.Observation;
import org.openehr.jaxb.rm.PointEvent;
import org.openehr.jaxb.rm.TerminologyId;

import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;

import com.google.common.collect.Lists;
import com.marand.thinkehr.builder.BuilderContext;
import com.marand.thinkehr.builder.CompositionBuilder;
import com.marand.thinkehr.builder.Pathable;
import com.marand.thinkehr.util.ConversionUtils;

public class CompositionBuilderUtil {

    public static Composition buildComposition(Collection<ArchetypeReference> archetypeReferences) throws Exception {
	List<Pathable> pathables = new ArrayList<Pathable>();
	for (ArchetypeReference archetypeReference : archetypeReferences) {
	    String idArchetype = archetypeReference.getIdArchetype();
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
	Template template = new Template();
	Composition comp = new Composition();
	/*
	Observation o = new Observation();
	PointEvent e = new PointEvent();
	e.getData().
	o.getData().getEvents()CompositionBuilderUtil.add(e);
	comp.getContent().add(o);
	*/
	return comp;
	//CompositionBuilder compositionBuilder = new CompositionBuilder(template);
	//create new composition with composition builder
	//return compositionBuilder.build(pathables, context);
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
	    Logger.getLogger(CompositionBuilderUtil.class).warn("DV Type unknown = '"+dv.getClass().getName()+"'");
	    return null;
	}
    }

    private static BuilderContext createContext() {
	BuilderContext context = new BuilderContext();
	context.setLanguage("en");
	context.setTerritory("SI");
	context.setComposerName("CDS");
	context.setEncoding("UTF-8");
	context.setEntryProvider(ConversionUtils.getPartyIdentified("CDS"));
	context.setHistoryOrigin(new DateTime());
	return context;
    }
}
