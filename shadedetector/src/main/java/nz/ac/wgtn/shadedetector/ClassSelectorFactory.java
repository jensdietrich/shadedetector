package nz.ac.wgtn.shadedetector;

import com.google.common.base.Preconditions;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ServiceLoader;

/**
 * Instantiates and sets up a ClassSelector from a configuration string.
 * ClassSelector definitions have the following format:  name (? (key=value)(&key=value)*)?
 * @author jens dietrich
 */
public class ClassSelectorFactory {

    private static Logger LOGGER = LoggerFactory.getLogger(ClassSelectorFactory.class);

    static ClassSelector create(String configuration) {
        String[] parts = configuration.split("\\?");
        String name = parts[0];

        // TODO find name
        ServiceLoader<ClassSelector> loader = ServiceLoader.load(ClassSelector.class);
        ClassSelector selector = loader.stream()
            .map(pm-> pm.get())
            .filter(s -> name.equals(s.name()))
            .findFirst().orElse(null);

        if (selector==null) {
            Preconditions.checkArgument(false,"no class selector found with name " + name);
        }

        LOGGER.info("Instantiated class selector {}",name);

        if (parts.length>1)  {
            LOGGER.info("Configuring class selector",name);
            Preconditions.checkArgument(parts.length==2);
            String configString = parts[1];
            String[] configParts = configString.split("&");
            for (String configDef:configParts) {
                String[] keyValue = configDef.split("=");
                Preconditions.checkArgument(keyValue.length==2,"syntax error in property definition " + configDef);
                String key = keyValue[0];
                String value = keyValue[1];
                LOGGER.info("\tset property {} -> {}",key,value);
                try {
                    BeanUtils.setProperty(selector,key,value);
                } catch (Exception e) {
                    LOGGER.info("Cannot set property {} to value {} for selector {} of type {}",key,value,selector.name(),selector.getClass().getName(),e);
                }
            }
        }

        return selector;
    }
}
