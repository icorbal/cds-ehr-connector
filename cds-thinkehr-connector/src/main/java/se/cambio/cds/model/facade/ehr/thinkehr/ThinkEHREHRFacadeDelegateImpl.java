package se.cambio.cds.model.facade.ehr.thinkehr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openehr.jaxb.rm.Composition;
import org.openehr.jaxb.rm.PartyIdentified;

import se.cambio.cds.controller.guide.GuideUtil;
import se.cambio.cds.model.facade.ehr.delegate.EHRFacadeDelegate;
import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.AggregationFunctions;
import se.cambio.cds.util.AqlUtil;
import se.cambio.cds.util.CompositionBuilderUtil;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.exceptions.PatientNotFoundException;

import com.marand.thinkehr.facade.AbstractThinkEhrFacadeDelegate;
import com.marand.thinkehr.factory.ThinkEhrConfigEnum;
import com.marand.thinkehr.service.AuditChangeType;
import com.marand.thinkehr.service.ThinkEhrService;
import com.marand.thinkehr.service.VersionLifecycleState;
import com.marand.thinkehr.util.RMConvertUtil;

public class ThinkEHREHRFacadeDelegateImpl extends AbstractThinkEhrFacadeDelegate implements EHRFacadeDelegate
{
  private final String subjectNamespace;

  public ThinkEHREHRFacadeDelegateImpl()
  {
    try
    {
      subjectNamespace = ThinkEhrConfigEnum.getThinkEhrSubjectNamespace();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public ThinkEHREHRFacadeDelegateImpl(
      final ThinkEhrService service,
      final String username,
      final String password,
      final String subjectNamespace)
  {
    super(service, username, password);
    this.subjectNamespace = subjectNamespace;
  }

  @Override
  public Collection<String> getEHRIds(final Collection<String> externalEHRIds)
      throws InternalErrorException, PatientNotFoundException
  {
    try
    {
      Map<String, String> ehrIds = new HashMap<String, String>();
      for (String externalEHRId : externalEHRIds)
      {
        if (!ehrIds.containsKey(externalEHRId))
        {
          ehrIds.put(externalEHRId, getService().findEhr(getSessionId(), externalEHRId, subjectNamespace));
        }
      }
      return ehrIds.values();
    }
    catch (Exception e)
    {
      throw new InternalErrorException(e);
    }
  }

  @Override
  public Collection<ElementInstance> queryEHRElements(
      final String ehrId, final Collection<ArchetypeReference> archetypeReferences)
      throws InternalErrorException, PatientNotFoundException
  {
    Map<String, Collection<ElementInstance>> map = queryEHRElements(
        Collections.singleton(ehrId),
        archetypeReferences);

    return map.get(ehrId);
  }

  @Override
  public Map<String, Collection<ElementInstance>> queryEHRElements(
      Collection<String> ehrIds,
      Collection<ArchetypeReference> archetypeReferences)
      throws InternalErrorException, PatientNotFoundException
  {
    try
    {
      Map<String, Collection<ElementInstance>> resultMap =
          new HashMap<String, Collection<ElementInstance>>();

      Map<String,Collection<ArchetypeReference>> arByAF = splitArchetypeReferences(archetypeReferences);

      if (arByAF.containsKey(null))
      {
        if (ehrIds==null || ehrIds.isEmpty())
        {
          throw new IllegalArgumentException("Can not perform population query for archetype references with empty aggregation function.");
        }
        for (ArchetypeReference archetypeReference : arByAF.remove(null))
        {
          appendResults(ehrIds,Collections.singleton(archetypeReference),resultMap);
        }
      }

      for (String aggregationFunction : arByAF.keySet())
      {
        Map<String, Collection<ElementInstance>> partialResultMap =
            new HashMap<String, Collection<ElementInstance>>();
        appendResults(ehrIds,arByAF.get(aggregationFunction),partialResultMap);

        mergeResultMap(aggregationFunction,partialResultMap,resultMap);
      }

      return resultMap;
    }
    catch (Exception e)
    {
      throw new InternalErrorException(e);
    }
  }

  private void mergeResultMap(
      final String aggregationFunction,
      final Map<String, Collection<ElementInstance>> partialResultMap,
      final Map<String, Collection<ElementInstance>> resultMap)
  {
    if (AggregationFunctions.ID_AGGREGATION_FUNCTION_LAST.equals(aggregationFunction))
    {
      mergeResultMapWithLastResults(partialResultMap,resultMap);
    }
    else
    {
      throw new UnsupportedOperationException("Unsupported aggregation function: "+aggregationFunction+".");
    }
  }

  private void mergeResultMapWithLastResults(
      final Map<String, Collection<ElementInstance>> partialResultMap,
      final Map<String, Collection<ElementInstance>> resultMap)
  {
    for (String ehrId : partialResultMap.keySet())
    {
      if (resultMap.containsKey(ehrId))
      {
        e:for (ElementInstance elementInstance : partialResultMap.get(ehrId))
        {
          for (ElementInstance ei : resultMap.get(ehrId))
          {
            if (elementInstance.getArchetypeReference().getIdArchetype().equals(ei.getArchetypeReference().getIdArchetype()))
            {
              ei.getArchetypeReference().setAggregationFunction(AggregationFunctions.ID_AGGREGATION_FUNCTION_LAST);
              continue e;
            }
          }
          resultMap.get(ehrId).add(elementInstance);
        }
      }
      else
      {
        resultMap.put(ehrId,partialResultMap.get(ehrId));
      }
    }
  }

  private void appendResults(
      final Collection<String> ehrIds,
      final Collection<ArchetypeReference> archetypeReferences,
      final Map<String, Collection<ElementInstance>> resultMap) throws Exception
  {
    final String sessionId = getSessionId();
    final List<Object[]> resultSet =
        getService().queryPopulationContent(
            sessionId,
            AqlUtil.getAql(ehrIds, archetypeReferences)
        );

    for (final Object[] result : resultSet)
    {
      final String ehrId = (String)result[0];
      if (!resultMap.containsKey(ehrId))
      {
        resultMap.put(ehrId, new ArrayList<ElementInstance>());
      }
      int i = 1;
      for (ArchetypeReference archetypeReference : archetypeReferences)
      {
        ArchetypeReference clonedArchetypeReference = archetypeReference.clone();
        for (ElementInstance elementInstance : archetypeReference.getElementInstancesMap().values())
        {
          resultMap.get(ehrId).add(
              new ElementInstance(
              elementInstance.getId(),
              result[i]!=null? RMConvertUtil.convert(result[i]):null,
              clonedArchetypeReference,
              null,
              result[i]!=null?null: GuideUtil.NULL_FLAVOUR_CODE_NO_INFO
          ));
          i++;
        }
      }
    }
  }

  private Map<String, Collection<ArchetypeReference>> splitArchetypeReferences(final Collection<ArchetypeReference> archetypeReferences)
  {
    Map<String, Collection<ArchetypeReference>> map = new HashMap<String, Collection<ArchetypeReference>>();
    for (ArchetypeReference archetypeReference : archetypeReferences)
    {
      if (!map.containsKey(archetypeReference.getAggregationFunction()))
      {
        map.put(archetypeReference.getAggregationFunction(), new ArrayList<ArchetypeReference>());
      }
      map.get(archetypeReference.getAggregationFunction()).add(archetypeReference);
    }
    return map;
  }

  @Override
  public boolean storeEHRElements(
	    String ehrId,
	    Collection<String> guideIds,
	    Collection<ArchetypeReference> archetypeReferences)
		    throws InternalErrorException, PatientNotFoundException {
	try{
	    final String sessionId = getSessionId();
	    getService().useEhr(sessionId, ehrId);
	    PartyIdentified pi = new PartyIdentified();
	    pi.setName("CDS");
	    String comment = "CDS generated composition";
	    String topic = "CDS";
	    Composition composition = CompositionBuilderUtil.buildComposition(archetypeReferences);
	    String result =
		    getService().commitGeneratedComposition(
			    sessionId, pi, 
			    comment, AuditChangeType.CREATION,
			    VersionLifecycleState.COMPLETE,
			    null,
			    topic,
			    composition);
	}catch (Exception e){
	    throw new InternalErrorException(e);
	}
	return false;
  }
}
