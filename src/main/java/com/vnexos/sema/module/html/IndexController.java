package com.vnexos.sema.module.html;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vnexos.sema.ApiResponse;
import com.vnexos.sema.loader.annotations.AutoWired;
import com.vnexos.sema.loader.annotations.Controller;
import com.vnexos.sema.loader.annotations.FromRoute;
import com.vnexos.sema.loader.annotations.HttpGet;
import com.vnexos.sema.loader.interfaces.ControllerBase;
import com.vnexos.sema.loader.json.HiddenExclusionStrategy;
import com.vnexos.sema.loader.json.LocalDateAdapter;
import com.vnexos.sema.loader.json.LocalDateTimeAdapter;
import com.vnexos.sema.loader.json.LocalTimeAdapter;
import com.vnexos.sema.loader.json.ReorderFactory;
import com.vnexos.sema.module.html.annotation.SwaggerIgnore;
import com.vnexos.sema.module.html.service.HTMLService;
import com.vnexos.sema.module.html.service.SwaggerService;
import com.vnexos.sema.util.html.*;

import io.swagger.v3.core.util.Json;

@Controller("/")
@SwaggerIgnore
public class IndexController extends ControllerBase {

  @AutoWired
  private HTMLService htmlService;
  @AutoWired
  private SwaggerService swaggerService;

  @HttpGet
  public ApiResponse<?> index() {

    ApiResponse<?> resp = createOk(
        HTMLFactory.init().setProperty("lang", "vi-vn")
            .addChildren(
                new HeadElement().addChildren(
                    new MetaElement().setProperty("charset", "UTF-8"),
                    new TitleElement("VNExos SeMa - Swagger"),
                    new LinkElement("stylesheet", "text/css")
                        .setProperty("href",
                            "./static/index.css"),
                    new LinkElement("stylesheet", "text/css")
                        .setProperty("href",
                            "./static/swagger-ui.css"),
                    new LinkElement("icon", "image/png")
                        .setProperty("href", "./static/vnexos-16x16.png")
                        .setProperty("sizes", "16x16"),
                    new LinkElement("icon", "image/png")
                        .setProperty("href", "./static/vnexos-32x32.png")
                        .setProperty("sizes", "32x32")),
                new BodyElement().addChildren(
                    new DivElement().setProperty("id", "swagger-ui"),
                    new ScriptElement(
                        "./static/swagger-ui-bundle.js")
                        .setProperty("charset", "UTF-8"),
                    new ScriptElement(
                        "./static/swagger-ui-standalone-preset.js")
                        .setProperty("charset", "UTF-8"),
                    new ScriptElement().addChildren(
                        """
                            window.onload = function() {
                              //<editor-fold desc="Changeable Configuration Block">

                              // the following lines will be replaced by docker/configurator, when it runs in a docker-container
                              window.ui = SwaggerUIBundle({
                                url: "/vnexos-swagger",
                                dom_id: '#swagger-ui',
                                deepLinking: true,
                                presets: [
                                  SwaggerUIBundle.presets.apis,
                                  SwaggerUIStandalonePreset
                                ]
                              });

                              //</editor-fold>
                            };"""))));
    resp.setContentType("text/html; charset=utf-8");
    return resp;
  }

  public ApiResponse<?> getFileResponse(String fileName) {
    try {
      htmlService.initResourceFile(fileName);
      ApiResponse<byte[]> apiResponse = createOk(htmlService.readFromFile(fileName));
      apiResponse.setContentType(htmlService.getContentType(fileName));
      System.out.println(apiResponse.getContentType());
      return apiResponse;
    } catch (IOException e) {
      return createInternalRequest("Cannot read file!");
    }
  }

  @SwaggerIgnore
  @HttpGet("static/{file}")
  public ApiResponse<?> favicon(@FromRoute String file) {
    return getFileResponse("/" + file);
  }

  @SwaggerIgnore
  @HttpGet("vnexos-swagger")
  public ApiResponse<?> swagger() throws JsonProcessingException {
    ApiResponse<?> response = new ApiResponse<>(swaggerService.buildOpenAPI(), 200) {
      Gson gson = new GsonBuilder()
          .setExclusionStrategies(new HiddenExclusionStrategy())
          .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
          .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
          .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
          .registerTypeAdapterFactory(new ReorderFactory())
          .create();

      @Override
      public String getJsonData() {
        try {
          return Json.mapper().writeValueAsString(getData());
        } catch (JsonProcessingException e) {
          return gson.toJson(getData());
        }
      }
    };
    return response;
  }
}
