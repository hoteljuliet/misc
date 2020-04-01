package com.comcast.mirs.sixoneone.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfigDrivenUtils {

    @Autowired
    private JsonUtils jsonUtils;

    public <T> List<T> loadListFromFile(String filename, ResourceLoader resourceLoader) {

        List<T> values = new ArrayList<T>();
        Resource fileResource = null;

        try {
            fileResource = resourceLoader.getResource("classpath:" + filename);
            String json = IOUtils.toString(fileResource.getInputStream(), "UTF-8");
            values = jsonUtils.getObjectMapper().readValue(json, new TypeReference<ArrayList<T>>() {});
        }
        catch(IOException ex) {
            ;
        }
        finally {
            closeResource(fileResource);
        }
        return values;
    }

    public <T> List<T> loadListFromURL(String url) {

        List<T> values = new ArrayList<T>();

        try {
            String json = readStringFromURL(url);
            values = jsonUtils.getObjectMapper().readValue(json, new TypeReference<ArrayList<T>>() {});
        }
        catch(IOException ex) {
            ;
        }
        finally {
            ;
        }
        return values;
    }

    public <K,V> Map<K, V> loadMapFromFile(String filename, ResourceLoader resourceLoader) {

        Map<K, V> values = new HashMap<K,V>();
        Resource fileResource = null;

        try {
            fileResource = resourceLoader.getResource("classpath:" + filename);
            String json = IOUtils.toString(fileResource.getInputStream(), "UTF-8");
            values = jsonUtils.getObjectMapper().readValue(json, new TypeReference<HashMap<K, V>>() {});
        }
        catch(IOException ex) {
            ;
        }
        finally {
            closeResource(fileResource);
        }
        return values;
    }

    public <K,V> Map<K, V> loadMapFromURL(String url) {

        Map<K, V> values = new ConcurrentHashMap<>();

        try {
            String json = readStringFromURL(url);
            values = jsonUtils.getObjectMapper().readValue(json, new TypeReference<HashMap<K, V>>() {});
        }
        catch(IOException ex) {
            ;
        }
        finally {
            ;
        }
        return values;
    }

    public <K,V> Map<K, V> loadMapFromURL(String url, ResourceLoader resourceLoader, Class<V> classType) {

        Map<K, V> values = new HashMap<K,V>();

        try {
            String json = readStringFromURL(url);
            Map<K, V> temp = jsonUtils.getObjectMapper().readValue(json, new TypeReference<HashMap<K, V>>() {});

            for (Map.Entry<K,V> entry : temp.entrySet()) {
                V value = jsonUtils.getObjectMapper().convertValue(entry.getValue(), classType);
                values.put(entry.getKey(), value);
            }
        }
        catch(IOException ex) {
            ;
        }
        finally {
            ;
        }
        return values;
    }

    public <K,V> Map<K, V> loadMapFromURL(String url, Class<V> classType) {
        Map<K, V> values = new HashMap<K,V>();

        try {
            String json = readStringFromURL(url);
            Map<K, V> temp = jsonUtils.getObjectMapper().readValue(json, new TypeReference<HashMap<K, V>>() {});

            for (Map.Entry<K,V> entry : temp.entrySet()) {
                V value = jsonUtils.getObjectMapper().convertValue(entry.getValue(), classType);
                values.put(entry.getKey(), value);
            }
        }
        catch(IOException ex) {
            ;
        }
        finally {
            ;
        }
        return values;
    }

    public String readStringFromURL(String requestURL)
    {
        String retVal = null;
        Scanner scanner = null;
        try
        {
            scanner = new Scanner(new java.net.URL(requestURL).openStream(), StandardCharsets.UTF_8.toString());
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                retVal = scanner.next();
            }
        }
        catch (Exception ignored) {
            ;
        }
        finally {
            IOUtils.closeQuietly(scanner);
        }
        return retVal;
    }

    private void closeResource(Resource resource) {
        try {
            IOUtils.closeQuietly(resource.getInputStream());
        }
        catch(IOException ignored) {
            ;
        }
    }
}

