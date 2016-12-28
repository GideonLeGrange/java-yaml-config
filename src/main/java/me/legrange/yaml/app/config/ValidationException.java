package me.legrange.yaml.app.config;

/**
 *
 * @author gideon
 */
public class ValidationException extends ConfigurationException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ValidationException(String message, Object... args) {
        super(message, args);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
