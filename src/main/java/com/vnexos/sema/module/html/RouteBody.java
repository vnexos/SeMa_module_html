package com.vnexos.sema.module.html;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.vnexos.sema.loader.HttpMethod;
import com.vnexos.sema.loader.Route;
import com.vnexos.sema.util.html.DOM;
import com.vnexos.sema.util.html.DivElement;

public class RouteBody extends DOM {
  private static final List<String> METHOD_ORDER = List.of("GET", "POST", "PUT", "PATCH", "DELETE");

  private String getBorderColor(HttpMethod method) {
    switch (method) {
      case GET:
        return "#61AFFE";
      case POST:
        return "#49CC90";
      case PUT:
        return "#FCA130";
      case PATCH:
        return "#50E3C2";
      case DELETE:
        return "#F93E3E";
    }
    return "#ccc";
  }

  private String getBackgroundColor(HttpMethod method) {
    switch (method) {
      case GET:
        return "#EBF3FB";
      case POST:
        return "#E8F6F0";
      case PUT:
        return "#FBF1E6";
      case PATCH:
        return "#E9F8F5";
      case DELETE:
        return "#FBE7E7";
    }
    return "#EFEFEF";
  }

  public RouteBody(Map<String, List<Route>> mappedRoute) {
    super("div");
    for (Map.Entry<String, List<Route>> entry : mappedRoute.entrySet()) {
      DOM elementDiv = new DivElement();
      DOM controllerContainer = new DivElement();

      elementDiv.addChildren(new DivElement().addChildren(entry.getKey())
          .setProperty("style", "border-bottom: 2px solid #888;"));
      elementDiv.addChildren(controllerContainer);
      elementDiv.setProperty("style",
          "width: 80%; min-width: 1000px; margin: auto; margin-top: 20px;");

      controllerContainer.setProperty("style",
          "width: 100%; padding-left: 20px; margin-top: 10px;");

      List<Route> sortedRoutes = entry.getValue().stream()
          .sorted(
              Comparator
                  .comparingInt(route -> ((Route) route).getRoute().split("/").length)
                  .thenComparingInt(
                      route -> METHOD_ORDER.indexOf(((Route) route).getHttpMethod().getValue().toUpperCase())))
          .toList();
      for (Route route : sortedRoutes) {
        DOM methodDiv = new DivElement().addChildren(route.getHttpMethod().getValue());
        DOM routeDiv = new DivElement().addChildren(route.getRoute());

        controllerContainer.addChildren(new DivElement()
            .setProperty("style",
                "display: flex; margin-top: 10px; padding: 5px; border-radius: 5px; border: 2px solid "
                    + getBorderColor(route.getHttpMethod()) + "; background-color: "
                    + getBackgroundColor(route.getHttpMethod()))
            .addChildren(methodDiv.setProperty("style",
                "color: white; width: 100px; border-radius: 5px; font-weight: bold; padding: 5px; text-align: center; background-color: "
                    + getBorderColor(route.getHttpMethod())))
            .addChildren(routeDiv.setProperty("style", "padding: 5px; font-family: 'Consolas';")));
      }
      addChildren(elementDiv);
    }
  }
}
