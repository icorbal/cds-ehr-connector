import java.util.Collection;

import com.marand.thinkehr.factory.TemplateServiceFactory;
import com.marand.thinkehr.factory.ThinkEhrServiceFactory;
import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.model.facade.administration.delegate.OpenEHRAdministrationFacadeDelegate;
import se.cambio.openehr.model.facade.administration.thinkehr.ThinkEHROpenEHRAdministrationFacadeDelegateImpl;
import se.cambio.openehr.model.template.dto.TemplateDTO;

/**
 * @author Jure Grom
 */
public class TestOpenEHRAdministrationFacadeDelegate
{
  public static void main(String[] args) throws Exception
  {
    String thinkEhrHost = args[0];
    String thinkEhrPort = args[1];
    String thinkEhrUser = args[2];
    String thinkEhrPass = args[3];

    OpenEHRAdministrationFacadeDelegate delegate = new ThinkEHROpenEHRAdministrationFacadeDelegateImpl(
        ThinkEhrServiceFactory.getThinkEhrService(thinkEhrHost,Integer.parseInt(thinkEhrPort)),
        TemplateServiceFactory.getTemplateService(thinkEhrHost,Integer.parseInt(thinkEhrPort)),
        thinkEhrUser,
        thinkEhrPass
    );

    Collection<TemplateDTO> templates = delegate.searchAllTemplates();
    Collection<ArchetypeDTO> archetypes = delegate.searchAllArchetypes();

    System.out.println("archetypes.size() = " + archetypes.size());
    System.out.println("templates.size()  = " + templates.size());
  }
}
