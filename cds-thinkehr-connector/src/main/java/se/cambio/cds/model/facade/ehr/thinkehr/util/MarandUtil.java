package se.cambio.cds.model.facade.ehr.thinkehr.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.openehr.jaxb.rm.Composition;

import com.marand.thinkehr.util.JaxbRegistry;

public class MarandUtil{

    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize (T t, String name) throws IOException, JAXBException {
	ByteArrayOutputStream oi = new ByteArrayOutputStream();
	Marshaller marshaller = JaxbRegistry.getInstance().getMarshaller();
	marshaller.marshal(new JAXBElement<T>(new QName(name), (Class<T>)t.getClass(), t), oi);
	return oi.toByteArray();
    }

    public static Composition deserialize(byte[] xmlStr) throws IOException, JAXBException {
	InputStream is = new ByteArrayInputStream(xmlStr);
	Unmarshaller unmarshaller = JaxbRegistry.getInstance().getUnmarshaller();
	unmarshaller.setSchema(null);
	JAXBElement<Composition> element = unmarshaller.unmarshal(new StreamSource(is), Composition.class);
	return element.getValue();
    }
}
