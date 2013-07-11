package com.marand.thinkehr.util;

import java.util.Deque;

import com.marand.thinkehr.template.AmNode;
import se.cambio.openehr.model.archetype.vo.TemplateObjectBundleCustomVO;
import se.cambio.openehr.model.template.dto.TemplateDTO;
import se.cambio.openehr.util.IOUtils;

/**
 * @author Bostjan Lah
 */
public class TemplateBuilder extends ArchetypeBuilder
{
  private final String templateId;

  public TemplateBuilder(AmNode root, String language, String defaultLanguage, String templateId)
  {
    super(root, language, defaultLanguage);
    this.templateId = templateId;
  }

  public TemplateDTO getTemplateDTO(boolean loadAll)
  {
    loadArchetypeObjects(loadAll, templateId);

    TemplateObjectBundleCustomVO vo = new TemplateObjectBundleCustomVO(
        archetypeElementVOs,
        clusterVOs,
        archetypeSlotVOs,
        codedTextVOs,
        ordinalVOs,
        unitVOs,
        proportionTypeVOs);



    return new TemplateDTO(templateId, root.getArchetypeNodeId(), root.getRmType(), null, null, IOUtils.getBytes(vo));
  }

  @Override
  protected boolean shouldVisitPathable(AmNode amNode, Deque<String> segments)
  {
    return true;
  }
}