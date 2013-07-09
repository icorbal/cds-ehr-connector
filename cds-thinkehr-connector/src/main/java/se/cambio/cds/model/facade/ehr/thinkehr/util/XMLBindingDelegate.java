package se.cambio.cds.model.facade.ehr.thinkehr.util;

import org.openehr.binding.XMLBinding;
import org.openehr.binding.XMLBindingException;

public class XMLBindingDelegate {

    private static XMLBindingDelegate _delegate = null;
    private XMLBinding _xmlBinding = null;
    
    private XMLBindingDelegate(){
    }
    
    public static XMLBinding getXMLBinding() throws XMLBindingException{
	if(getDelegate()._xmlBinding==null){
	    getDelegate()._xmlBinding = new XMLBinding();
	}
	return getDelegate()._xmlBinding;
    }
    
    
    public static XMLBindingDelegate getDelegate(){
	if(_delegate==null){
	    _delegate = new XMLBindingDelegate();
	}
	return _delegate;
    }
}
