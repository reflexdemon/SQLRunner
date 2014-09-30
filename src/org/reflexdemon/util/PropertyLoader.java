/**
 * 
 */
package org.reflexdemon.util;

/**
 * @author vvenkatraman
 *
 */
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Class PropertyLoader.
 */
public class PropertyLoader {

    /**
     * Logging Reference for PropertyLoader
     */
    private static Log log = LogFactory.getLog(PropertyLoader.class);

    /** The Constant THROW_ON_LOAD_FAILURE. */
    private static final boolean THROW_ON_LOAD_FAILURE = true;

    /** The Constant LOAD_AS_RESOURCE_BUNDLE. */
    private static final boolean LOAD_AS_RESOURCE_BUNDLE = true;

    /** The Constant SUFFIX. */
    private static final String SUFFIX = ".properties";

    /**
     * Looks up a resource named 'name' in the classpath. The resource must map
     * to a file with .properties extention. The name is assumed to be absolute
     * and can use either "/" or "." for package segment separation with an
     * optional leading "/" and optional ".properties" suffix. Thus, the
     * following names refer to the same resource:
     * 
     * <pre>
     * some.pkg.Resource
     * some.pkg.Resource.properties
     * some/pkg/Resource
     * some/pkg/Resource.properties
     * /some/pkg/Resource
     * /some/pkg/Resource.properties
     * </pre>
     * 
     * @param name
     *            classpath resource name [may not be null]
     * @param loader
     *            classloader through which to load the resource [null is
     *            equivalent to the application loader]
     * @return resource converted to java.util.Properties [may be null if the
     *         resource was not found and THROW_ON_LOAD_FAILURE is false]
     */
    public static Properties loadProperties(String name, ClassLoader loader) {
        if (name == null)
            throw new IllegalArgumentException("null input: name");

        if (name.startsWith("/"))
            name = name.substring(1);

        if (name.endsWith(SUFFIX))
            name = name.substring(0, name.length() - SUFFIX.length());

        Properties result = null;

        InputStream in = null;
        try {
            if (loader == null)
                loader = ClassLoader.getSystemClassLoader();

            if (LOAD_AS_RESOURCE_BUNDLE) {
                name = name.replace('/', '.');
                // Throws MissingResourceException on lookup failures:
                final ResourceBundle rb = ResourceBundle.getBundle(name,
                        Locale.getDefault(), loader);

                result = new Properties();
                for (String key : rb.keySet()) {
                    final String value = rb.getString(key);
                    if (null != key && key.startsWith("inherit.")) {
                        log.info("Inheriting [" + key + ", " + value + "]");
                        Properties inherited = loadProperties(value);
                        result.putAll(inherited);
                    }
                    result.put(key, value);
                }
            } else {
                name = name.replace('.', '/');

                if (!name.endsWith(SUFFIX))
                    name = name.concat(SUFFIX);

                // Returns null on lookup failures:
                in = loader.getResourceAsStream(name);
                if (in != null) {
                    result = new Properties();
                    result.load(in); // Can throw IOException
                }
            }
        } catch (Exception e) {
            result = null;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
        }

        if (THROW_ON_LOAD_FAILURE && (result == null)) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                    "could not load ["
                            + name
                            + "]"
                            + " as "
                            + (LOAD_AS_RESOURCE_BUNDLE ? "a resource bundle"
                                    : "a classloader resource"));
            log.error(PropertyLoader.class.getName() + " loadProperties",
                    illegalArgumentException);
            throw illegalArgumentException;
        }

        return result;
    }

    /**
     * A convenience overload of {@link #loadProperties(String, ClassLoader)}
     * that uses the current thread's context classloader.
     * 
     * @param name
     *            the name
     * @return the properties
     */
    public static Properties loadProperties(final String name) {
        return loadProperties(name, Thread.currentThread()
                .getContextClassLoader());
    }
} // End of class