/*
 * Copyright 2016 gideon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.legrange.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import me.legrange.config.annotation.Collection;

import org.yaml.snakeyaml.Yaml;

import me.legrange.config.annotation.NotBlank;
import me.legrange.config.annotation.NotEmpty;
import me.legrange.config.annotation.NotNull;
import me.legrange.config.annotation.Numeric;

/**
 * Read a configuration from a from YAML config file and return a configuration
 * object. .
 *
 * @author gideon
 */
public abstract class YamlLoader {

    /**
     * Read the configuration file and return a configuration object.
     *
     * @param <C>
     * @param fileName The file to read.
     * @param clazz Configuration implementation class to load.
     * @return The configuration object.
     * @throws ConfigurationException Thrown if there is a problem reading or
     * parsing the configuration.
     */
    public static <C extends Configuration> C readConfiguration(String fileName, Class<C> clazz) throws ConfigurationException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(Paths.get(fileName))) {
            C conf = yaml.loadAs(in, clazz);
            if (conf == null) {
                throw new ConfigurationException("Could not load configuration file '%s'. Yaml returned null", fileName);
            }
            validate(conf);
            return conf;
        } catch (IOException ex) {
            throw new ConfigurationException(String.format("Error reading configuraion file '%s': %s", fileName, ex.getMessage()), ex);
        }
    }

    private static void validate(Object conf) throws ValidationException {
        for (Field field : conf.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(NotNull.class)) {
                validateNotNull(field, conf);
            }
            if (field.isAnnotationPresent(NotBlank.class)) {
                validateNotBlank(field, conf);
            }
            if (field.isAnnotationPresent(NotEmpty.class)) {
                validateNotEmpty(field, conf);
            }
            if (field.isAnnotationPresent(Numeric.class)) {
                validateNumber(field.getAnnotation(Numeric.class), field, conf);
            }
            if (field.isAnnotationPresent(Collection.class)) {
                validateCollection(field.getAnnotation(Collection.class), field, conf);
            }
        }
    }

    private static void validateNumber(Numeric ann, Field field, Object inst) throws ValidationException {
        validateNotNull(field, inst);
        Object val = get(field, inst);
        if (!(val instanceof Number)) {
            throw new ValidationException("%s in %s is not a Number as expected", field.getName(), inst.getClass().getSimpleName());
        }
        Number num = (Number) val;
        double nval = num.doubleValue();
        if ((nval < ann.min()) || (nval > ann.max())) {
            throw new ValidationException("%s in %s must be in the range %s...%s ", field.getName(), inst.getClass().getSimpleName(), ann.min(), ann.max());
        }
    }

    private static void validateNotNull(Field field, Object inst) throws ValidationException {
        Object val = get(field, inst);
        if (val == null) {
            throw new ValidationException("%s in %s must not be undefined", field.getName(), inst.getClass().getSimpleName());
        }
        validate(val);
    }

    private static void validateNotBlank(Field field, Object inst) throws ValidationException {
        validateNotNull(field, inst);
        Object val = get(field, inst);
        if (!(val instanceof String)) {
            throw new ValidationException("%s in %s is not a String as expected", field.getName(), inst.getClass().getSimpleName());
        }
        if (((String) val).isEmpty()) {
            throw new ValidationException("%s in %s must not be blank", field.getName(), inst.getClass().getSimpleName());
        }
    }

    private static void validateNotEmpty(Field field, Object inst) throws ValidationException {
        validateNotNull(field, inst);
        Object val = get(field, inst);
        if (!(val instanceof java.util.Collection)) {
            throw new ValidationException("%s in %s is not a collection as expected", field.getName(), inst.getClass().getSimpleName());
        }
        java.util.Collection<?> col = (java.util.Collection<?>) val;
        if (col.isEmpty()) {
            throw new ValidationException("%s in %s must not be empty", field.getName(), inst.getClass().getSimpleName());
        }
        for (Object o : col) {
            validate(o);
        }
    }

    private static void validateCollection(Collection can, Field field, Object inst) throws ValidationException {
        validateNotNull(field, inst);
        Object val = get(field, inst);
        if (!(val instanceof java.util.Collection)) {
          throw new ValidationException("%s in %s is not a collection as expected", field.getName(), inst.getClass().getSimpleName());
        }
        java.util.Collection<?> col = (java.util.Collection<?>) val;
        int size = col.size();
        if ((size < can.min()) || (size > can.max())) {
            throw new ValidationException("%s in %s must contain between %d and %d elements, but has %d", 
                    field.getName(), inst.getClass().getSimpleName(),
                    can.min(), (can.max() == Integer.MAX_VALUE) ? "many" : can.max(), size);
        }
        for (Object o : col) {
            validate(o);
        }
    }

    private static Object get(Field field, Object inst) throws ValidationException {
        if (field.isAccessible()) {
            try {
                return field.get(inst);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Field '%s' is not found on object '%s'", field.getName(), inst.getClass().getSimpleName());

            } catch (IllegalAccessException ex) {
                throw new ValidationException("Field '%s' on '%s' is not accessible", field.getName(), inst.getClass().getSimpleName());
            }
        } else {
            String name = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            if (field.getType().equals(Boolean.class)) {
                name = "is" + name;
            } else {
                name = "get" + name;
            }
            try {
                Method meth = inst.getClass().getDeclaredMethod(name, new Class[]{});
                return meth.invoke(inst, new Object[]{});
            } catch (NoSuchMethodException ex) {
                throw new ValidationException("Field '%s' on '%s' does not have a get-method", field.getName(), inst.getClass().getSimpleName());
            } catch (SecurityException ex) {
                throw new ValidationException("Method '%s' on '%s' is in-accessible", name, inst.getClass().getSimpleName());
            } catch (IllegalAccessException ex) {
                throw new ValidationException("Method '%s' on '%s' is not accessible", name, inst.getClass().getSimpleName());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Method '%s' is not found on object '%s'", name, inst.getClass().getSimpleName());
            } catch (InvocationTargetException ex) {
                throw new ValidationException(String.format("Error calling '%s' on object '%s': %s", name, inst.getClass().getSimpleName(), ex.getMessage()), ex);
            }
        }
    }

}
