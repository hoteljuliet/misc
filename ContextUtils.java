package com.comcast.mirs.sixoneone.utils;

import com.comcast.mirs.sixoneone.model.ContextContents;
import org.apache.commons.chain.Context;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ContextUtils {

    /**
     * Utility method for safely getting values out of the Context
     * @param content - the enumeration for the value
     * @param context - the map of values
     * @param classType - the type of the value
     * @param <T> - the type of the value
     * @return An optional with the value if the key exists, otherwise an empty Optional
     */
    public <T> Optional<T> get(ContextContents content, Context context, Class<T> classType) {
        Optional<T> retVal = Optional.empty();
        if (content != null) {
            if (context.containsKey(content)) {
                Object object = context.get(content);

                try {
                    if (classType.isInstance(object)) {
                        retVal = Optional.of(classType.cast(object));
                    } else {
                        ;
                    }
                }
                catch(Exception ignored) {
                    ;
                }
                finally {
                    ;
                }
            }
            else {
                ;
            }
        }
        return retVal;
    }
}
