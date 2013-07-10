package com.marand.thinkehr.factory;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.service.ThinkEhrService;

/**
 * @author Jure Grom
 */
public class ThinkEhrServiceFactory
{
  public static ThinkEhrService getThinkEhrService() throws Exception
  {
    return getThinkEhrService(
        ThinkEhrConfigEnum.getThinkEhrHost(),
        ThinkEhrConfigEnum.getThinkEhrPort()
    );
  }

  public static ThinkEhrService getThinkEhrService(final String host, final int port) throws Exception
  {
    return RemotingUtils.getService(ThinkEhrService.class, host, port);
  }
}
