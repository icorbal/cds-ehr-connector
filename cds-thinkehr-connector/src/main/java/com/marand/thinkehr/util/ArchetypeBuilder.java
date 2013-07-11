package com.marand.thinkehr.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.marand.thinkehr.template.AmAttribute;
import com.marand.thinkehr.template.AmNode;
import com.marand.thinkehr.template.AmUtils;
import org.apache.commons.lang.StringUtils;
import org.openehr.jaxb.am.ArchetypeSlot;
import org.openehr.jaxb.am.ArchetypeTerm;
import org.openehr.jaxb.am.Assertion;
import org.openehr.jaxb.am.CArchetypeRoot;
import org.openehr.jaxb.am.CCodePhrase;
import org.openehr.jaxb.am.CComplexObject;
import org.openehr.jaxb.am.CDvOrdinal;
import org.openehr.jaxb.am.CDvQuantity;
import org.openehr.jaxb.am.CInteger;
import org.openehr.jaxb.am.CQuantityItem;
import org.openehr.jaxb.rm.DvOrdinal;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.datatypes.text.DvCodedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cambio.openehr.model.archetype.dto.ArchetypeDTO;
import se.cambio.openehr.model.archetype.vo.ArchetypeElementVO;
import se.cambio.openehr.model.archetype.vo.ArchetypeObjectBundleCustomVO;
import se.cambio.openehr.model.archetype.vo.ArchetypeSlotVO;
import se.cambio.openehr.model.archetype.vo.ClusterVO;
import se.cambio.openehr.model.archetype.vo.CodedTextVO;
import se.cambio.openehr.model.archetype.vo.OrdinalVO;
import se.cambio.openehr.model.archetype.vo.ProportionTypeVO;
import se.cambio.openehr.model.archetype.vo.UnitVO;
import se.cambio.openehr.model.facade.terminology.delegate.TerminologyFacadeDelegateFactory;
import se.cambio.openehr.model.facade.terminology.vo.TerminologyNodeVO;
import se.cambio.openehr.util.ExceptionHandler;
import se.cambio.openehr.util.IOUtils;
import se.cambio.openehr.util.OpenEHRConst;
import se.cambio.openehr.util.OpenEHRDataValues;
import se.cambio.openehr.util.OpenEHRDataValuesUI;
import se.cambio.openehr.util.OpenEHRLanguageManager;

/**
 * @author Bostjan Lah
 */
public class ArchetypeBuilder
{
  private static final Logger log = LoggerFactory.getLogger(ArchetypeBuilder.class);
  private static final String DESCRIPTION = "description";

  protected final AmNode root;
  protected final String language;
  protected final Collection<ArchetypeElementVO> archetypeElementVOs = new ArrayList<ArchetypeElementVO>();
  protected final Collection<ClusterVO> clusterVOs = new ArrayList<ClusterVO>();
  protected final Collection<CodedTextVO> codedTextVOs = new ArrayList<CodedTextVO>();
  protected final Collection<OrdinalVO> ordinalVOs = new ArrayList<OrdinalVO>();
  protected final Collection<ArchetypeSlotVO> archetypeSlotVOs = new ArrayList<ArchetypeSlotVO>();
  protected final Collection<UnitVO> unitVOs = new ArrayList<UnitVO>();
  protected final Collection<ProportionTypeVO> proportionTypeVOs = new ArrayList<ProportionTypeVO>();

  public ArchetypeBuilder(AmNode root, String language, String defaultLanguage)
  {
    this.root = root;
    this.language = AmUtils.findText(root, language, root.getNodeId()) == null ? defaultLanguage : language;
  }

  public static void getArchetypeRoots(Collection<AmNode> roots, AmNode amNode)
  {
    if (amNode.getCObject() instanceof CArchetypeRoot)
    {
      roots.add(amNode);
    }
    for (AmAttribute amAttribute : amNode.getAttributes().values())
    {
      for (AmNode childAmNode : amAttribute.getChildren())
      {
        getArchetypeRoots(roots, childAmNode);
      }
    }
  }

  public ArchetypeDTO getArchetypeDTO(boolean loadAll)
  {
    String name = findText(root, language);
    String desc = findDescription(root, language);

    loadArchetypeObjects(loadAll, null);

    ArchetypeObjectBundleCustomVO vo = new ArchetypeObjectBundleCustomVO(
        archetypeElementVOs,
        clusterVOs,
        archetypeSlotVOs,
        codedTextVOs,
        ordinalVOs,
        unitVOs,
        proportionTypeVOs);

    return new ArchetypeDTO(root.getArchetypeNodeId(), name, desc, root.getRmType(), null, null, IOUtils.getBytes(vo));
  }

  protected void loadArchetypeObjects(boolean loadAll, String idTemplate)
  {
    String archetypeId = root.getArchetypeNodeId();

    final Map<String, AmNode> pathObjectMap = Maps.filterValues(
        getPathables(root), new Predicate<AmNode>()
    {
      @Override
      public boolean apply(final AmNode node)
      {
        return OpenEHRConst.PARSABLE_OPENEHR_RM_NAMES.contains(node.getRmType());
      }
    });

    List<String> paths = new ArrayList<String>(pathObjectMap.keySet());
    //Shortest path first (to populate clusters before children)
    Collections.sort(paths);

    for (final String path : paths)
    {
      AmNode node = pathObjectMap.get(path);
      if (node.getCObject() instanceof CComplexObject)
      {
        String text = findText(node, language);
        String desc = findDescription(node, language);

        if ("@ internal @".equals(desc) || text == null || text.startsWith("*"))
        {
          int firstIndex = path.lastIndexOf('/') + 1;
          int finalIndex = path.lastIndexOf('[');
          if (finalIndex > firstIndex)
          {
            text = path.substring(firstIndex, finalIndex);
          }
        }

        AmNode child = AmUtils.resolvePath(node, "value");
        String type = child == null ? node.getRmType() : child.getRmType();

        if (OpenEHRDataValuesUI.isManaged(type))
        {
          ArchetypeElementVO archetypeElementVO =
              new ArchetypeElementVO(text, desc, type, getIdParentCluster(path), archetypeId, idTemplate, path);
          archetypeElementVO.setLowerCardinality(node.getOccurrences().getLower());
          archetypeElementVO.setUpperCardinality(node.getOccurrences().getUpper());
          archetypeElementVOs.add(archetypeElementVO);

          if (loadAll)
          {
            if (OpenEHRDataValues.DV_CODED_TEXT.equals(type))
            {
              loadCodedTexts(child, idTemplate, path, archetypeElementVO.getId());
            }
            else if (OpenEHRDataValues.DV_ORDINAL.equals(type))
            {
              loadOrdinals(child, idTemplate, path, archetypeElementVO.getId());
            }
            else if (OpenEHRDataValues.DV_QUANTITY.equals(type))
            {
              loadUnits(child, idTemplate, archetypeElementVO.getId());
            }
            else if (OpenEHRDataValues.DV_PROPORTION.equals(type))
            {
              loadProportionTypes(child, idTemplate, archetypeElementVO.getId());
            }
          }
        }
        else
        {
          ClusterVO clusterVO = new ClusterVO(
              node.getName(),
              desc,
              type,
              getIdParentCluster(path),
              archetypeId,
              idTemplate,
              path);
          clusterVO.setLowerCardinality(node.getOccurrences().getLower());
          clusterVO.setUpperCardinality(node.getOccurrences().getUpper());
          clusterVOs.add(clusterVO);
        }
      }
      else if (node.getCObject() instanceof ArchetypeSlot)
      {
        if (loadAll)
        {
          ArchetypeSlot archetypeSlot = (ArchetypeSlot)node.getCObject();
          String text = findText(node, language);
          if (text == null)
          {
            text = OpenEHRLanguageManager.getMessage("UnnamedSlot");
          }
          String desc = findDescription(node, language);
          if (desc == null)
          {
            desc = OpenEHRLanguageManager.getMessage("UnnamedSlot");
          }
          archetypeSlotVOs.add(
              new ArchetypeSlotVO(
                  text,
                  desc,
                  node.getRmType(),
                  getIdParentCluster(path),
                  archetypeId,
                  idTemplate,
                  path,
                  processSlotAssertions(archetypeSlot.getIncludes()),
                  processSlotAssertions(archetypeSlot.getExcludes())));
        }
      }
      else
      {
        log.warn("Unknown CObject '{}': Skipped", node.getCObject());
      }
    }
    loadRMElements(archetypeId, idTemplate, root.getRmType(), archetypeElementVOs);
  }

  private Collection<String> processSlotAssertions(final List<Assertion> assertions)
  {
    Collection<String> assertionExpressions = new ArrayList<String>();
    for (Assertion assertion : assertions)
    {
      String exp = assertion.getStringExpression();
      if (exp != null)
      {
        int indexS = exp.indexOf('{');
        int indexE = exp.indexOf('}');
        exp = exp.substring(indexS + 2, indexE - 1);
        assertionExpressions.add(exp);
      }
    }
    return assertionExpressions;
  }

  protected Map<String, AmNode> getPathables(final AmNode amNode)
  {
    Map<String, AmNode> pathables = new HashMap<String, AmNode>();
    Deque<String> segments = new ArrayDeque<String>();
    findPathables(pathables, amNode, segments);
    return pathables;
  }

  private void findPathables(final Map<String, AmNode> pathables, final AmNode amNode, final Deque<String> segments)
  {
    if (shouldVisitPathable(amNode, segments))
    {
      pathables.put(buildPath(segments), amNode);
      if (!amNode.getRmType().startsWith("DV_"))
      {
        for (Map.Entry<String, AmAttribute> entry : amNode.getAttributes().entrySet())
        {
          for (AmNode node : entry.getValue().getChildren())
          {
            segments.push(entry.getKey() + (StringUtils.isBlank(node.getArchetypeNodeId()) ? "" : '[' + node.getArchetypeNodeId() + ']'));
            findPathables(pathables, node, segments);
            segments.pop();
          }
        }
      }
    }
  }

  protected boolean shouldVisitPathable(final AmNode amNode, final Deque<String> segments)
  {
    return segments.isEmpty() || !(amNode.getCObject() instanceof CArchetypeRoot);
  }

  private String buildPath(final Deque<String> segments)
  {
    return '/' + Joiner.on('/').join(segments.descendingIterator());
  }

  private String getIdParentCluster(String path)
  {
    List<ClusterVO> parentClusters = new ArrayList<ClusterVO>();
    for (ClusterVO clusterVO : clusterVOs)
    {
      if (path.startsWith(clusterVO.getPath()))
      {
        parentClusters.add(clusterVO);
      }
    }
    String idParentCluster = null;
    int length = 0;
    for (ClusterVO clusterVO : parentClusters)
    {
      if (clusterVO.getPath().length() > length)
      {
        idParentCluster = clusterVO.getId();
        length = clusterVO.getPath().length();
      }
    }
    return idParentCluster;
  }

  private void loadCodedTexts(AmNode node, String idTemplate, String path, String idElement)
  {
    CCodePhrase codePhrase = AmUtils.getCObjectItem(node, CCodePhrase.class, "defining_code");
    if (codePhrase != null)
    {
      for (String code : codePhrase.getCodeList())
      {
        CodedTextVO codedText = new CodedTextVO(
            code,
            code,
            codePhrase.getRmTypeName(),
            idElement,
            root.getArchetypeNodeId(),
            idTemplate,
            path,
            codePhrase.getTerminologyId().getValue(),
            code,
            null);
        if (codePhrase.getTerminologyId().getValue().equals(OpenEHRConst.LOCAL))
        {
          codedText.setName(findText(node, code, language));
          codedText.setDescription(findDescription(node, code, language));
        }
        else
        {
          addSubclassCodedTexts(codedText);
        }
        codedTextVOs.add(codedText);
      }
    }
  }

  private void addSubclassCodedTexts(CodedTextVO codedTextVO)
  {
    if (!OpenEHRConst.LOCAL.equals(codedTextVO.getTerminology()))
    {
      try
      {
        TerminologyNodeVO node = TerminologyFacadeDelegateFactory.getDelegate().retrieveAllSubclasses(
            new CodePhrase(codedTextVO.getTerminology(), codedTextVO.getCode()),
            OpenEHRDataValuesUI.getLanguageCodePhrase()
        );
        DvCodedText ct = node.getValue();
        codedTextVO.setName(getValidCodedTextName(ct.getValue()));
        codedTextVO.setDescription(getValidCodedTextName(ct.getValue()));
        addCodedTextVOs(node, codedTextVO);
      }
      catch (Exception e)
      {
        ExceptionHandler.handle(e);
      }
    }
  }

  private void addCodedTextVOs(TerminologyNodeVO root, CodedTextVO rootCodedTextVO)
  {
    for (TerminologyNodeVO node : root.getChildren())
    {
      DvCodedText ct = node.getValue();
      CodedTextVO codedTextVO = new CodedTextVO(
          getValidCodedTextName(ct.getValue()),
          getValidCodedTextName(ct.getValue()),
          OpenEHRDataValues.DV_CODED_TEXT,
          rootCodedTextVO.getIdParent(),
          rootCodedTextVO.getIdArchetype(),
          rootCodedTextVO.getIdTemplate(),
          rootCodedTextVO.getPath(),
          ct.getDefiningCode().getTerminologyId().getValue(),
          ct.getDefiningCode().getCodeString(),
          rootCodedTextVO);
      codedTextVOs.add(codedTextVO);
      addCodedTextVOs(node, codedTextVO);
    }
  }

  /* Remove all parenthesis to avoid parsing problems */
  private static String getValidCodedTextName(String string)
  {
    return string.replaceAll("\\(", "[").replaceAll("\\)", "\\]");
  }

  private void loadOrdinals(AmNode node, String idTemplate, String path, String idElement)
  {
    CDvOrdinal cDvOrdinal = (CDvOrdinal)node.getCObject();
    if (cDvOrdinal != null && cDvOrdinal.getList() != null)
    {
      for (DvOrdinal ordinal : cDvOrdinal.getList())
      {
        String codedStr = ordinal.getSymbol().getDefiningCode().getCodeString();
        String text = codedStr;
        String desc = codedStr;
        if ("local".equals(ordinal.getSymbol().getDefiningCode().getTerminologyId().getValue()))
        {
          text = findText(node, codedStr, language);
          desc = findDescription(node, codedStr, language);
        }
        else
        {
          log.error(
              "Unknown terminology: '{}', skipping...",
              ordinal.getSymbol().getDefiningCode().getTerminologyId().getValue());
          //TODO TERMINOLOGY SERVICE
        }
        ordinalVOs.add(
            new OrdinalVO(
                text,
                desc,
                cDvOrdinal.getRmTypeName(),
                idElement,
                root.getArchetypeNodeId(),
                idTemplate,
                path,
                ordinal.getValue(),
                ordinal.getSymbol().getDefiningCode().getTerminologyId().getValue(),
                ordinal.getSymbol().getDefiningCode().getCodeString()));
      }
    }
  }

  private static void loadRMElements(
      String idArchetype,
      String idTemplate,
      String entryType,
      Collection<ArchetypeElementVO> archetypeElementVOs)
  {
    if (OpenEHRConst.OBSERVATION.equals(entryType))
    {
      /*Origin (Use EventTime instead)
      archetypeElementVOs.add(
		    new ArchetypeElementVO(
			    LanguageManager.getMessage("Origin"),
			    LanguageManager.getMessage("OriginDesc"),
			    OpenEHRDataValues.DV_DATE_TIME, null,
			    idArchetype, "/time"));
	     */
      //EventTime
      archetypeElementVOs.add(
          new ArchetypeElementVO(
              OpenEHRLanguageManager.getMessage("EventTime"),
              OpenEHRLanguageManager.getMessage("EventTimeDesc"),
              OpenEHRDataValues.DV_DATE_TIME, null,
              idArchetype, idTemplate, "/event/time"));
    }
    else if (OpenEHRConst.INSTRUCTION.equals(entryType))
    {
      //Expiry Time
      archetypeElementVOs.add(
          new ArchetypeElementVO(
              OpenEHRLanguageManager.getMessage("ExpireTime"),
              OpenEHRLanguageManager.getMessage("ExpireTimeDesc"),
              OpenEHRDataValues.DV_DATE_TIME, null,
              idArchetype, idTemplate, "/expiry_time"));
      //Expiry Time
      archetypeElementVOs.add(
          new ArchetypeElementVO(
              OpenEHRLanguageManager.getMessage("NarrativeDescription"),
              OpenEHRLanguageManager.getMessage("NarrativeDescriptionDesc"),
              OpenEHRDataValues.DV_TEXT, null,
              idArchetype, idTemplate, "/narrative"));
    }
    else if (OpenEHRConst.ACTION.equals(entryType))
    {
      //Date and time Action step performed
      archetypeElementVOs.add(
          new ArchetypeElementVO(
              OpenEHRLanguageManager.getMessage("DateTimeActionPerformed"),
              OpenEHRLanguageManager.getMessage("DateTimeActionPerformedDesc"),
              OpenEHRDataValues.DV_DATE_TIME, null,
              idArchetype, idTemplate, "/time"));
      //Current Action State
      archetypeElementVOs.add(
          new ArchetypeElementVO(
              OpenEHRLanguageManager.getMessage("CurrentActionState"),
              OpenEHRLanguageManager.getMessage("CurrentActionStateDesc"),
              OpenEHRDataValues.DV_DATE_TIME, null,
              idArchetype, idTemplate, "/ism_transition/current_state"));
    }
    else if (OpenEHRConst.EVALUATION.equals(entryType))
    {

    }
    //Template Id
    archetypeElementVOs.add(
        new ArchetypeElementVO(
            OpenEHRLanguageManager.getMessage("TemplateId"),
            OpenEHRLanguageManager.getMessage("TemplateIdDesc"),
            OpenEHRDataValues.DV_TEXT, null,
            idArchetype, idTemplate, "/archetype_details/template_id"));
  }

  private void loadUnits(AmNode childCObject, String idTemplate, String idElement)
  {
    if (childCObject.getCObject() instanceof CDvQuantity)
    {
      CDvQuantity cDvQuantity = (CDvQuantity)childCObject.getCObject();
      for (CQuantityItem quantityItem : cDvQuantity.getList())
      {
        unitVOs.add(new UnitVO(idTemplate, idElement, quantityItem.getUnits()));
      }
    }
  }

  private void loadProportionTypes(AmNode childCObject, String idTemplate, String idElement)
  {
    if (childCObject.getCObject() instanceof CComplexObject)
    {
      CInteger cInteger = AmUtils.getPrimitiveItem(childCObject, CInteger.class, "type");
      if (cInteger != null)
      {
        for (Integer proportionType : cInteger.getList())
        {
          proportionTypeVOs.add(new ProportionTypeVO(idTemplate, idElement, proportionType));
        }
      }
    }
  }

  protected String findText(final AmNode node, final String language)
  {
    return findText(node, node.getNodeId(), language);
  }

  private String findText(final AmNode node, final String code, final String language)
  {
    Collection<ArchetypeTerm> terms = node.getTermDefinitions().get(language);
    if (terms != null) {
      return AmUtils.findTerm(terms, code, AmUtils.TEXT_ID);
    }
    return AmUtils.findTerm(node.getTerms(), code, AmUtils.TEXT_ID);
  }

  private String findDescription(final AmNode node, final String language)
  {
    return findDescription(node, node.getName(), language);
  }

  private String findDescription(final AmNode node, final String code, final String language)
  {
    Collection<ArchetypeTerm> terms = node.getTermDefinitions().get(language);
    if (terms != null) {
      return AmUtils.findTerm(terms, code, DESCRIPTION);
    }
    return AmUtils.findTerm(node.getTerms(), code, DESCRIPTION);
  }
}
