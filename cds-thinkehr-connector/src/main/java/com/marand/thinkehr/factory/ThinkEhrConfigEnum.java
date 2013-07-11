package com.marand.thinkehr.factory;

import se.cambio.util.ThinkEHRConfigurationParametersManager;


/**
 * @author Jure Grom
 */
public enum ThinkEhrConfigEnum
{
  ThinkEhrUsername,
  ThinkEhrPassword,
  ThinkEhrHost,
  ThinkEhrPort,
  ThinkEhrSubjectNamespace;

  public static String getThinkEhrHost() throws Exception
  {
    return ThinkEHRConfigurationParametersManager.getParameter(ThinkEhrHost.name());
  }

  public static String getThinkEhrPassword() throws Exception
  {
    return ThinkEHRConfigurationParametersManager.getParameter(ThinkEhrPassword.name());
  }

  public static String getThinkEhrUsername() throws Exception
  {
    return ThinkEHRConfigurationParametersManager.getParameter(ThinkEhrUsername.name());
  }

  public static int getThinkEhrPort() throws Exception
  {
    return Integer.parseInt(ThinkEHRConfigurationParametersManager.getParameter(ThinkEhrPort.name()));
  }

  public static String getThinkEhrSubjectNamespace() throws Exception
  {
    return ThinkEHRConfigurationParametersManager.getParameter(ThinkEhrSubjectNamespace.name());
  }
}
