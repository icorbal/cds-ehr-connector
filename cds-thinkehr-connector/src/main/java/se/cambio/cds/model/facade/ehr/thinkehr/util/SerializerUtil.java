package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.marand.thinkehr.util.JaxbRegistry;

public class SerializerUtil{
    
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize (T t, String name) throws IOException, JAXBException {
	ByteArrayOutputStream oi = new ByteArrayOutputStream();
	Marshaller marshaller = JaxbRegistry.getInstance().getMarshaller();
	marshaller.marshal(new JAXBElement<T>(new QName(name), (Class<T>)t.getClass(), t), oi);
	return oi.toByteArray();
    }
}
