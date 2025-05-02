package com.vnexos.sema.module.html;

import com.vnexos.sema.context.ModuleServerContext;
import com.vnexos.sema.loader.annotations.MainClass;
import com.vnexos.sema.loader.interfaces.AModule;

@MainClass("HTML")
public class HTMLMainClass extends AModule {
  public static ModuleServerContext context;

  @Override
  public void onEnabled(ModuleServerContext context) {
    HTMLMainClass.context = context;
    context.log("HTML enabled!");
  }

  @Override
  public void onDisabled() {
    context.log("HTML disabled!");
  }

}
