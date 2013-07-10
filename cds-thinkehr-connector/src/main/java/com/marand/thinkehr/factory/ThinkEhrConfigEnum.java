package com.marand.thinkehr.factory;

import se.cambio.openehr.util.UserConfigurationManager;

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
    return UserConfigurationManager.getParameter(ThinkEhrHost.name());
  }

  public static String getThinkEhrPassword() throws Exception
  {
    return UserConfigurationManager.getParameter(ThinkEhrPassword.name());
  }

  public static String getThinkEhrUsername() throws Exception
  {
    return UserConfigurationManager.getParameter(ThinkEhrUsername.name());
  }

  public static int getThinkEhrPort() throws Exception
  {
    return Integer.parseInt(UserConfigurationManager.getParameter(ThinkEhrPort.name()));
  }

  public static String getThinkEhrSubjectNamespace() throws Exception
  {
    return UserConfigurationManager.getParameter(ThinkEhrSubjectNamespace.name());
  }
}
