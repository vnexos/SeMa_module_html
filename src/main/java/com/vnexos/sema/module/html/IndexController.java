package com.vnexos.sema.module.html;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vnexos.sema.ApiResponse;
import com.vnexos.sema.loader.Loader;
import com.vnexos.sema.loader.LoaderException;
import com.vnexos.sema.loader.Route;
import com.vnexos.sema.loader.annotations.Controller;
import com.vnexos.sema.loader.annotations.HttpGet;
import com.vnexos.sema.loader.interfaces.ControllerBase;
import com.vnexos.sema.util.html.*;

@Controller("/")
public class IndexController extends ControllerBase {
  private Map<String, List<Route>> analyzeRoutes() throws LoaderException, ClassNotFoundException, NoSuchFieldException,
      SecurityException, IllegalArgumentException, IllegalAccessException {
    ClassLoader classLoader = Loader.class.getClassLoader();
    Class<?> clazz = classLoader.loadClass("com.vnexos.sema.ApiController");
    Field field = clazz.getDeclaredField("apis");
    field.setAccessible(true);

    List<Route> routes = (List<Route>) field.get(null);
    Map<String, List<Route>> result = new HashMap<>();

    for (Route route : routes) {
      String className = route.getMethod().getDeclaringClass().getName();
      String moduleName = Loader.findModule(className).getModuleName();

      String key = className.substring(className.lastIndexOf('.') + 1) + " [Module: " + moduleName + "]";
      if (result.get(key) == null)
        result.put(key, new ArrayList<>());

      result.get(key).add(route);
    }

    return result;
  }

  @HttpGet
  public ApiResponse<?> index() throws LoaderException, ClassNotFoundException, NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Map<String, List<Route>> mappedRoute = analyzeRoutes();
    ApiResponse<?> resp = createOk(
        HTMLFactory.init()
            .addChildren(
                new HeadElement()
                    .addChildren(new TitleElement("VNExos Sema"))
                    .addChildren(new StyleElement("""
                        * {
                          margin: 0;
                          padding: 0;
                          font-family: 'Arial', sans-serif;
                        }
                        """)))
            .addChildren(
                new BodyElement()
                    .addChildren(new RouteBody(mappedRoute))));
    resp.setContentType("text/html; charset=utf-8");
    return resp;
  }
}
