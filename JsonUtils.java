package com.comcast.mirs.sixoneone.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public <T> T convert(Object object, Class<T> classType) {
        T value = null;

        try {
            value = objectMapper.convertValue(object, classType);
        } catch (IllegalArgumentException ex) {
            ;
        }
        return value;
    }

    public <T> T convert(Map<String,Object> objectMap, Class<T> classType) {
        T value = null;

        try {
            value = objectMapper.convertValue(objectMap, classType);
        } catch (IllegalArgumentException ex) {
            ;
        }
        return value;
    }

    public <T> T deserialize(String json, Class<T> classType) {
        T value = null;

        try {
            value = objectMapper.readValue(json, classType);
        } catch (IOException ex) {
            logger.error("Something went wrong during deserialize " + classType.getSimpleName() + ": ", ex);
        }
        return value;
    }

    public <T> T deserialize(Resource resource, Class<T> classType) {
        T value = null;

        String json = getJsonFromResource(resource);

        if (resource != null) {
            try {
                value = objectMapper.readValue(json, classType);
            } catch (IOException ex) {
                logger.error("Something went wrong during deserialize " + classType.getSimpleName() + ": ", ex);
            }
        }
        return value;
    }

    public String getJsonFromResource(Resource resource) {

        String retVal = null;
        BufferedReader br = null;
        try {
            InputStream is = resource.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }

            String json = stringBuilder.toString();

            if (isJSONValid(json)) {
                retVal = json;
            }
        } catch (IOException ex) {
            IOUtils.closeQuietly(br);
        }
        return retVal;
    }

    public String serialize(Throwable throwable) {
        String value = null;

        try {
            Map<String, Object> exceptionMap = new HashMap<>();

            exceptionMap.put("message", throwable.getMessage());
            exceptionMap.put("cause", throwable.getCause());

            List<StackTraceElement> stackTraceElements = new ArrayList<>();
            for (int i = 0; i < 8 && i < throwable.getStackTrace().length; i++) {
                stackTraceElements.add(throwable.getStackTrace()[i]);
            }
            exceptionMap.put("stacktrace", stackTraceElements);

            value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exceptionMap);

        } catch (Exception ex) {
            logger.error("", ex);
        }
        return value;
    }

    public String serialize(Object object, Boolean pretty) {
        String value = null;

        try {
            if (pretty) {
                value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } else {
                value = objectMapper.writeValueAsString(object);
            }
        } catch (IOException ex) {
            logger.error("", ex);
        }
        return value;
    }

    public void writeResponseData(String json, HttpServletResponse response) {

        try {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(json);
        } catch (IOException ex) {
            ;
        }
    }

    public boolean isJSONValid(String jsonInString) {
        try {
            JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(jsonInString);
            while (parser.nextToken() != null) {
                ;
            }
            return jsonInString.contains("{");
        } catch (IOException e) {
            return false;
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}