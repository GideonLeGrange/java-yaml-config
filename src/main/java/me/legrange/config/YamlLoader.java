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
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import org.yaml.snakeyaml.Yaml;

/**
 * Read a configuration from a from YAML config file and return a configuration
 * object. .
 * 
 * @todo Fix the thing 
 * @body Do it properly 
 * @author gideon
 * @param <C> Type of config being processed.
 */
public final class YamlLoader<C extends Configuration> {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final Yaml yaml = new Yaml();
    private final Class<C> clazz;

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
        try {
            YamlLoader<C> loader = new YamlLoader(clazz);
            C conf = loader.load(Files.newInputStream(Paths.get(fileName)));
            if (conf == null) {
                throw new ConfigurationException("Could not load configuration file '%s'. Yaml returned null", fileName);
            }
            loader.validate(conf);
            return conf;
        } catch (IOException ex) {
            throw new ConfigurationException(String.format("Error reading configuraion file '%s': %s", fileName, ex.getMessage()), ex);
        }
    }

    public static <C extends Configuration> C readConfiguration(InputStream in, Class<C> clazz) throws ConfigurationException {
        YamlLoader<C> loader = new YamlLoader(clazz);
        C conf = loader.load(in);
        if (conf == null) {
            throw new ConfigurationException("Could not load configuration from input stream'. Yaml returned null");
        }
        loader.validate(conf);
        return conf;
    }

    private C load(InputStream in) throws ConfigurationException {
        return yaml.loadAs(in, clazz);
    }

    private void validate(Object conf) throws ValidationException {
        try {
            BeanDescriptor constraintsForClass = validator.getConstraintsForClass(conf.getClass());
            Set<ConstraintViolation<Object>> errors = validator.validate(conf);
            if (!errors.isEmpty()) {
                throw new ValidationException(errors.iterator().next().getMessage(), errors);
            }
            Class clazz = conf.getClass();
            if (clazz.isPrimitive() || clazz.isEnum() || (conf instanceof Number) || (conf instanceof String) || (conf instanceof Boolean)) {
                return;
            }
            if (conf instanceof Collection) {
                for (Object item : ((Collection) conf)) {
                    validate(item);
                }
                return;
            }
            if (conf instanceof Map) {
                for (Object item : ((Map) conf).values()) {
                    validate(item);
                }
                return;
            }
            for (Field field : conf.getClass().getDeclaredFields()) {
                if (!field.isEnumConstant() && !field.isSynthetic()) {
                    Object val = get(field, conf);
                    if (val != null) {
                        validate(val);
                    }
                }
            }
        } catch (AnnotationFormatError | IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage(), ex);
        }
    }

    private Object get(Field field, Object inst) throws ValidationException {
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
            if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
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

    private YamlLoader(Class<C> clazz) {
        this.clazz = clazz;

    }
}
