package com.marand.thinkehr.facade;

import com.marand.thinkehr.factory.ThinkEhrConfigEnum;
import com.marand.thinkehr.factory.ThinkEhrServiceFactory;
import com.marand.thinkehr.service.ThinkEhrService;

/**
 * @author Jure Grom
 */
public abstract class AbstractThinkEhrFacadeDelegate
{
  private final ThinkEhrService service;
  private final String username;
  private final String password;

  private volatile String sessionId = null;

  public AbstractThinkEhrFacadeDelegate()
  {
    try
    {
      service = ThinkEhrServiceFactory.getThinkEhrService();
      username = ThinkEhrConfigEnum.getThinkEhrUsername();
      password = ThinkEhrConfigEnum.getThinkEhrPassword();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public AbstractThinkEhrFacadeDelegate(ThinkEhrService service, String username, String password)
  {
    this.service = service;
    this.username = username;
    this.password = password;
  }

  protected String getSessionId() throws Exception
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

  protected ThinkEhrService getService()
  {
    return service;
  }
}
