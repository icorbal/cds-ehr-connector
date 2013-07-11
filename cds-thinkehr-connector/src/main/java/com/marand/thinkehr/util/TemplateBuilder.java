package com.marand.thinkehr.util;

/*import java.util.Deque;

import com.marand.thinkehr.template.AmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cambio.cds.model.template.dto.TemplateDTO;
import se.cambio.cds.openehr.model.facade.archetype.vo.TemplateObjectBundleCustomVO;*/

/**
 * @author Bostjan Lah
 */
public class TemplateBuilder extends ArchetypeBuilder
{
  /*private static final Logger log = LoggerFactory.getLogger(TemplateBuilder.class);

  private final String templateId;

  public TemplateBuilder(AmNode root, String language, String defaultLanguage, String templateId)
  {
    super(root, language, defaultLanguage);
    this.templateId = templateId;
  }

  public TemplateObjectBundleCustomVO getTemplateVOs(boolean loadAll)
  {
    String name = findText(root, language);

    loadArchetypeObjects(loadAll, templateId);

    return new TemplateObjectBundleCustomVO(
        new TemplateDTO(templateId, root.getArchetypeNodeId(), name, name, root.getRmType(), null, null),
        archetypeElementVOs,
        clusterVOs,
        archetypeSlotVOs,
        codedTextVOs,
        ordinalVOs,
        unitVOs,
        proportionTypeVOs);
  }

  @Override
  protected boolean shouldVisitPathable(AmNode amNode, Deque<String> segments)
  {
    return true;
  }   */
}