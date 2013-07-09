package se.cambio.openehr.model.facade.administration.thinkehr;

import java.util.Collection;

import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.model.facade.administration.delegate.OpenEHRAdministrationFacadeDelegate;
import se.cambio.openehr.model.template.dto.TemplateDTO;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.exceptions.ModelException;

public class ThinkEHROpenEHRAdministrationFacadeDelegateImpl implements OpenEHRAdministrationFacadeDelegate{

    @Override
    public Collection<ArchetypeDTO> searchAllArchetypes()
	    throws InternalErrorException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Collection<TemplateDTO> searchAllTemplates()
	    throws InternalErrorException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void addArchetype(ArchetypeDTO archetypeDTO)
	    throws InternalErrorException, ModelException {
	// TODO Auto-generated method stub
	
    }

    @Override
    public void addTemplate(TemplateDTO templateDTO)
	    throws InternalErrorException, ModelException {
	// TODO Auto-generated method stub
	
    }
}
