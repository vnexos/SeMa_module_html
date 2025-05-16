package com.vnexos.sema.module.html.service;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vnexos.sema.loader.Loader;
import com.vnexos.sema.loader.LoaderException;
import com.vnexos.sema.loader.Part;
import com.vnexos.sema.loader.Route;
import com.vnexos.sema.loader.annotations.FromBody;
import com.vnexos.sema.loader.annotations.FromQuery;
import com.vnexos.sema.loader.annotations.FromRoute;
import com.vnexos.sema.loader.annotations.Service;
import com.vnexos.sema.module.html.HTMLMainClass;
import com.vnexos.sema.module.html.annotation.SwaggerIgnore;
import com.vnexos.sema.util.ClassUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Service
public class SwaggerService {
  private List<Route> getRoutes() {
    try {
      ClassLoader classLoader = Loader.class.getClassLoader();
      Class<?> clazz = classLoader.loadClass("com.vnexos.sema.ApiController");
      Field field = clazz.getDeclaredField("apis");
      field.setAccessible(true);

      return (List<Route>) field.get(null);
    } catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException | NoSuchFieldException
        | SecurityException e) {
      HTMLMainClass.context.log(e);
      return new ArrayList<>();
    }
  }

  private Schema<?> convertTypeToSchema(Class<?> clazz) {
    Schema<?> schema = new Schema<>();

    if (clazz == null) {
      schema.setType("object"); // Default to object for null
      return schema;
    }

    // Handle primitive types and their wrapper classes
    if (clazz == java.util.UUID.class) {
      schema.type("string").setFormat("uuid");
    } else if (clazz == String.class) {
      schema.setType("string");
    } else if (clazz == Integer.class || clazz == int.class) {
      schema.type("integer").setFormat("int32");
    } else if (clazz == Long.class || clazz == long.class) {
      schema.type("integer").setFormat("int64");
    } else if (clazz == Double.class || clazz == double.class) {
      schema.type("number").setFormat("double");
    } else if (clazz == Float.class || clazz == float.class) {
      schema.type("number").setFormat("float");
    } else if (clazz == Boolean.class || clazz == boolean.class) {
      schema.type("boolean");
    } else if (clazz == Byte.class || clazz == byte.class) {
      schema.type("string").setFormat("byte");
    } else if (clazz == Character.class || clazz == char.class) {
      schema.type("string");
    } else if (clazz == java.util.Date.class || clazz == java.sql.Date.class) {
      schema.type("string").setFormat("date-time");
    }
    // Handle arrays and collections
    else if (clazz.isArray()) {
      schema.setType("array");
      schema.setItems(convertTypeToSchema(clazz.getComponentType()));
    } else if (List.class.isAssignableFrom(clazz) || Set.class.isAssignableFrom(clazz)) {
      schema.setType("array");
      schema.setItems(new Schema<>().type("object")); // Generic item type; customize if parameterized type is available
    }
    // Handle maps
    else if (Map.class.isAssignableFrom(clazz)) {
      schema.setType("object");
      schema.setAdditionalProperties(new Schema<>().type("object")); // Generic value type
    }
    // Handle custom objects
    else if (ClassUtils.isCustomClass(clazz)) {
      schema.setType("object");
      List<Field> fields = ClassUtils.getAllFields(clazz);
      for (Field field : fields) {
        schema.addProperty(field.getName(), convertTypeToSchema(field.getType()));
      }
      // schema.set$ref("#/components/schemas/" + clazz.getSimpleName());
    } else {
      schema.setType("object");
    }

    return schema;
  }

  private List<io.swagger.v3.oas.models.parameters.Parameter> getParameters(Map<String, Schema<?>> schemaMap,
      Operation op, Route r, String moduleName) {
    List<io.swagger.v3.oas.models.parameters.Parameter> params = new ArrayList<>();
    for (Parameter parameter : r.getMethod().getParameters())
      if (parameter.isAnnotationPresent(FromRoute.class)) {
        params.add(new io.swagger.v3.oas.models.parameters.Parameter()
            .name(parameter.getName().replaceAll("[\\{\\}]", ""))
            .in("path")
            .description("From route")
            .required(true)
            .schema(convertTypeToSchema(parameter.getType())));
      } else if (parameter.isAnnotationPresent(FromQuery.class)) {
        if (ClassUtils.isCustomClass(parameter.getType())) {
          Class<?> paramType = parameter.getType();
          List<Field> fields = ClassUtils.getAllFields(paramType);
          for (Field field : fields) {
            params.add(new io.swagger.v3.oas.models.parameters.Parameter()
                .name(field.getName())
                .in("query")
                .description("From query")
                .required(true)
                .schema(convertTypeToSchema(field.getType())));
          }
        } else {
          params.add(new io.swagger.v3.oas.models.parameters.Parameter()
              .name(parameter.getName())
              .in("path")
              .description("From route")
              .required(true)
              .schema(convertTypeToSchema(parameter.getType())));
        }
      } else if (parameter.isAnnotationPresent(FromBody.class) && parameter.getType() != Part.class) {
        schemaMap.putIfAbsent(parameter.getType().getName(),
            convertTypeToSchema(parameter.getType())
                .description("Module: " + moduleName));

        RequestBody requestBody = new RequestBody()
            .required(true)
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/" + parameter.getType().getName()))));

        op.setRequestBody(requestBody);
      }

    return params;
  }

  private Operation getOperation(Route r, Map<String, Tag> tagMap, Components components) {
    String tag = r.getMethod().getDeclaringClass().getSimpleName();
    String moduleName = "";
    try {
      moduleName = Loader.findModule(r.getMethod().getDeclaringClass().getName()).getModuleName();
    } catch (LoaderException e) {
      HTMLMainClass.context.log(e);
    }

    // Ensure tag is added only once
    tagMap.putIfAbsent(tag, new Tag().name(tag).description("Module: " + moduleName));

    // Prepare operation
    String methodName = r.getMethod().getName();
    Operation op = new Operation()
        .addTagsItem(tag)
        .summary(methodName)
        .description("This route belongs to the function <code>" + methodName + "</code>");

    Map<String, Schema<?>> schemaMap = new LinkedHashMap<>();

    op.parameters(getParameters(schemaMap, op, r, moduleName));

    ApiResponses apiResponses = new ApiResponses()
        .addApiResponse("200", new ApiResponse()
            .description("Success response")
            .content(new Content()));
    op.responses(apiResponses);

    for (Map.Entry<String, Schema<?>> entry : schemaMap.entrySet()) {
      components.addSchemas(entry.getKey(), entry.getValue());
    }
    return op;
  }

  public OpenAPI buildOpenAPI() {
    OpenAPI openAPI = new OpenAPI();
    Components components = new Components();

    openAPI.info(new Info()
        .title("VNExos SeMa API")
        .version("1.x")
        .description("Custom route-based OpenAPI docs"));

    openAPI.setComponents(components);

    components.addSecuritySchemes("bearer", new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("JWT for authentication."));

    openAPI
        .addSecurityItem(new SecurityRequirement().addList("bearer"));

    Paths paths = new Paths();
    Map<String, Tag> tagMap = new LinkedHashMap<>(); // Preserve order

    List<Route> routes = getRoutes();

    for (Route r : routes) {
      if (r.getMethod().isAnnotationPresent(SwaggerIgnore.class) ||
          r.getMethod().getDeclaringClass().isAnnotationPresent(SwaggerIgnore.class))
        continue;

      String path = r.getRoute();

      Operation op = getOperation(r, tagMap, components);

      PathItem pathItem = paths.get(path);
      if (pathItem == null) {
        pathItem = new PathItem();
        paths.addPathItem(path, pathItem);
      }

      // Set operation based on HTTP method
      switch (r.getHttpMethod()) {
        case GET:
          pathItem.get(op);
          break;
        case POST:
          pathItem.post(op);
          break;
        case PUT:
          pathItem.put(op);
          break;
        case PATCH:
          pathItem.patch(op);
          break;
        case DELETE:
          pathItem.delete(op);
          break;
        default:
          break;
      }
    }

    openAPI.setPaths(paths);
    openAPI.setTags(new ArrayList<>(tagMap.values()));

    return openAPI;
  }

}
