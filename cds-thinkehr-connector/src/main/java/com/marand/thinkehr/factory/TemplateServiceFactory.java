package com.marand.thinkehr.factory;

import com.marand.maf.jboss.remoting.RemotingUtils;
import com.marand.thinkehr.templates.service.TemplateService;

/**
 * @author Jure Grom
 */
public class TemplateServiceFactory
{
  public static TemplateService getTemplateService() throws Exception
  {
    return getTemplateService(
        ThinkEhrConfigEnum.getThinkEhrHost(),
        ThinkEhrConfigEnum.getThinkEhrPort()
    );
  }

  public static TemplateService getTemplateService(final String host, final int port) throws Exception
  {
    return RemotingUtils.getService(TemplateService.class, host, port);
  }
}
