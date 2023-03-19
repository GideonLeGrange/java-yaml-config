package me.legrange.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class TestConfig {

    @Test
    public void loadFromYml( ) throws ConfigurationException {
        SampleConfig conf = YamlLoader.readConfiguration(TestConfig.class.getClassLoader().getResourceAsStream("valid-test.yml"), SampleConfig.class);
        assertThat(conf)
                .as("The loaded configuration must not be null")
                .isNotNull();
        assertThat(conf.getInteger())
                .as("The value of 'integer' must be an int with value 5")
                .isEqualTo(5);
        assertThat(conf.getString())
                .as("The value of 'string' must be 'Some String'")
                .isEqualTo("Some String");
        assertThat(conf.isBool())
                .as("The value of 'bool' must be true")
                .isEqualTo(true);

    }

    @Test
    public void loadInvalidFromYml( ) throws ConfigurationException {
        try {
            SampleConfig conf = YamlLoader.readConfiguration(TestConfig.class.getClassLoader().getResourceAsStream(
                    "invalid-test.yml"), SampleConfig.class);
            throw new ConfigurationException("Validation should have failed!");
        }
        catch (ValidationException e) {
            System.out.println("Validation failed as expected");
        }
    }
}
