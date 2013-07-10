package se.cambio.cds.model.facade.ehr.thinkehr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.marand.thinkehr.factory.ThinkEhrConfigEnum;
import com.marand.thinkehr.factory.ThinkEhrServiceFactory;
import com.marand.thinkehr.service.ThinkEhrService;
import com.marand.thinkehr.util.RMConvertUtil;
import se.cambio.cds.controller.guide.GuideUtil;
import se.cambio.cds.model.facade.ehr.delegate.EHRFacadeDelegate;
import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.AqlUtil;
import se.cambio.openehr.util.exceptions.InternalErrorException;
import se.cambio.openehr.util.exceptions.PatientNotFoundException;

public class ThinkEHREHRFacadeDelegateImpl implements EHRFacadeDelegate
{
  private final ThinkEhrService service;
  private final String username;
  private final String password;
  private final String subjectNamespace;

  private volatile String sessionId = null;

  public ThinkEHREHRFacadeDelegateImpl()
  {
    try
    {
      service = ThinkEhrServiceFactory.getThinkEhrService();
      username = ThinkEhrConfigEnum.getThinkEhrUsername();
      password = ThinkEhrConfigEnum.getThinkEhrPassword();
      subjectNamespace = ThinkEhrConfigEnum.getThinkEhrSubjectNamespace();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public ThinkEHREHRFacadeDelegateImpl(ThinkEhrService service, String username, String password, String subjectNamespace)
  {
    this.service = service;
    this.username = username;
    this.password = password;
    this.subjectNamespace = subjectNamespace;
  }

  private String getSessionId() throws Exception
  {
    if (sessionId != null && !service.isOpenSession(sessionId))
    {
      sessionId = null;
    }

    if (sessionId == null)
    {
      sessionId = service.login(username, password);
    }
    return sessionId;
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
          ehrIds.put(externalEHRId, service.findEhr(getSessionId(), externalEHRId, subjectNamespace));
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

      final String sessionId = getSessionId();
      final List<Object[]> resultSet =
          service.queryPopulationContent(
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
                result[i]!=null?RMConvertUtil.convert(result[i]):null,
                clonedArchetypeReference,
                null,
                result[i]!=null?null:GuideUtil.NULL_FLAVOUR_CODE_NO_INFO
            ));
            i++;
          }
        }
      }

      return resultMap;
    }
    catch (Exception e)
    {
      throw new InternalErrorException(e);
    }
  }

  @Override
  public boolean storeEHRElements(
      String ehrId,
      Collection<ArchetypeReference> archetypeReferences)
      throws InternalErrorException, PatientNotFoundException
  {
    // TODO Auto-generated method stub
    return false;
  }
}
