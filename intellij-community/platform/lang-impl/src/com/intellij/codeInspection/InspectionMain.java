// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection;

import com.intellij.openapi.application.ApplicationStarter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class InspectionMain implements ApplicationStarter {
  private InspectionApplication myApplication;

  @Override
  public String getCommandName() {
    return "inspect";
  }

  @Override
  @SuppressWarnings({"HardCodedStringLiteral"})
  public void premain(String[] args) {
    if (args.length < 4) {
      System.err.println("invalid args:" + Arrays.toString(args));
      printHelp();
    }

    //System.setProperty("idea.load.plugins.category", "inspection");
    myApplication = new InspectionApplication();

    myApplication.myHelpProvider = new InspectionToolCmdlineOptionHelpProvider() {
      @Override
      public void printHelpAndExit() {
        printHelp();
      }
    };
    myApplication.myProjectPath = args[1];
    myApplication.myStubProfile = args[2];
    myApplication.myOutPath = args[3];

    if (myApplication.myProjectPath == null
        || myApplication.myOutPath == null
        || myApplication.myStubProfile == null) {
      System.err.println(myApplication.myProjectPath + myApplication.myOutPath + myApplication.myStubProfile);
      printHelp();
    }

    try {
      for (int i = 4; i < args.length; i++) {
        String arg = args[i];
        if ("-profileName".equals(arg)) {
          myApplication.myProfileName = args[++i];
        }
        else if ("-profilePath".equals(arg)) {
          myApplication.myProfilePath = args[++i];
        }
        else if ("-d".equals(arg)) {
          myApplication.mySourceDirectory = args[++i];
        }
        else if ("-v0".equals(arg)) {
          myApplication.setVerboseLevel(0);
        }
        else if ("-v1".equals(arg)) {
          myApplication.setVerboseLevel(1);
        }
        else if ("-v2".equals(arg)) {
          myApplication.setVerboseLevel(2);
        }
        else if ("-v3".equals(arg)) {
          myApplication.setVerboseLevel(3);
        }
        else if ("-e".equals(arg)) {
          myApplication.myRunWithEditorSettings = true;
        }
        else if ("-t".equals(arg)) {
          myApplication.myErrorCodeRequired = false;
        }
        else {
          System.err.println("unexpected argument: " + arg);
          printHelp();
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
      printHelp();
    }

    myApplication.myRunGlobalToolsOnly = System.getProperty("idea.no.local.inspections") != null;
  }

  @Override
  public void main(@NotNull String[] args) {
    myApplication.startup();
  }

  public static void printHelp() {
    System.out.println(InspectionsBundle.message("inspection.command.line.explanation"));
    System.exit(1);
  }
}

