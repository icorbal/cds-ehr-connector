package se.cambio.openehr.model.facade.administration.thinkehr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.marand.thinkehr.facade.AbstractThinkEhrFacadeDelegate;
import com.marand.thinkehr.factory.TemplateServiceFactory;
import com.marand.thinkehr.service.ThinkEhrService;
import com.marand.thinkehr.template.AmTreeBuilder;
import com.marand.thinkehr.templates.dto.TemplateDto;
import com.marand.thinkehr.templates.service.TemplateService;
import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.model.facade.administration.delegate.OpenEHRAdministrationFacadeDelegate;
import se.cambio.openehr.model.template.dto.TemplateDTO;
import se.cambio.openehr.util.UserConfigurationManager;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.exceptions.ModelException;

public class ThinkEHROpenEHRAdministrationFacadeDelegateImpl extends AbstractThinkEhrFacadeDelegate implements OpenEHRAdministrationFacadeDelegate
{
  private static final String DEFAULT_LANGUAGE = "en";
  private final TemplateService templateService;

  public ThinkEHROpenEHRAdministrationFacadeDelegateImpl()
  {
    try
    {
      templateService = TemplateServiceFactory.getTemplateService();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public ThinkEHROpenEHRAdministrationFacadeDelegateImpl(
      final ThinkEhrService service,
      final TemplateService templateService,
      final String username,
      final String password)
  {
    super(service, username, password);
    this.templateService = templateService;
  }

  @Override
  public Collection<ArchetypeDTO> searchAllArchetypes()
      throws InternalErrorException
  {
    try
    {
      String language = UserConfigurationManager.getLanguage();

      Collection<ArchetypeDTO> result = new ArrayList<ArchetypeDTO>();

      List<TemplateDto> templates = templateService.getAllTemplates(getSessionId(), true, false);
      for (TemplateDto templateDto : templates)
      {
        AmTreeBuilder amTreeBuilder = new AmTreeBuilder(templateService.getActiveTemplateByTemplateId(
            getSessionId(),
            templateDto.getTemplateId()));
        /*List<AmNode> roots = new ArrayList<AmNode>();
        ArchetypeBuilder.getArchetypeRoots(roots, amTreeBuilder.build());

        for (AmNode root : roots)
        {
          ArchetypeBuilder builder = new ArchetypeBuilder(root, language, DEFAULT_LANGUAGE);
          if (!containsArchetype(result,root.getArchetypeNodeId()))
          {
            result.add(builder.getArchetypeDTO());
          }
        } */
      }
      return result;
    }
    catch (Exception e)
    {
      throw new InternalErrorException(e);
    }
  }

  private boolean containsArchetype(Collection<ArchetypeDTO> collection, String archetypeId)
  {
    for (ArchetypeDTO archetypeDTO : collection)
    {
      if (archetypeId.equals(archetypeDTO.getIdArchetype()))
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public Collection<TemplateDTO> searchAllTemplates()
      throws InternalErrorException
  {
    try
    {
      String language = UserConfigurationManager.getLanguage();

      Collection<TemplateDTO> result = new ArrayList<TemplateDTO>();

      List<TemplateDto> templates = templateService.getAllTemplates(getSessionId(), true, false);
      for (TemplateDto templateDto : templates)
      {
        AmTreeBuilder amTreeBuilder = new AmTreeBuilder(templateService.getActiveTemplateByTemplateId(
            getSessionId(),
            templateDto.getTemplateId()));
        /*TemplateBuilder builder = new TemplateBuilder(amTreeBuilder.build(), language, DEFAULT_LANGUAGE, templateDto.getTemplateId());
        TemplateDTO templateDTO = builder.getTemplateDTO();
        if (templateDto.getXml()!=null)
        {
          templateDTO.setAom(templateDto.getXml().getBytes("UTF-8"));
        }
        result.add(templateDTO); */
      }
      return result;
    }
    catch (Exception e)
    {
      throw new InternalErrorException(e);
    }
  }

  @Override
  public void addArchetype(ArchetypeDTO archetypeDTO)
      throws InternalErrorException, ModelException
  {
    throw new UnsupportedOperationException("This operation is not supported for EHR server store!");
  }

  @Override
  public void addTemplate(TemplateDTO templateDTO)
      throws InternalErrorException, ModelException
  {
    throw new UnsupportedOperationException("This operation is not supported for EHR server store!");
  }
}
