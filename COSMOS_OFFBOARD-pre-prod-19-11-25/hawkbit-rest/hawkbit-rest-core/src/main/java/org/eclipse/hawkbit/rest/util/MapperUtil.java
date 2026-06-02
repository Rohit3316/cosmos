package org.eclipse.hawkbit.rest.util;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;

public class MapperUtil {
    private MapperUtil() {
    }

    private static final ModelMapper mapper = new ModelMapper();

    public static <T, S> T convert(S source, Class<T> target) {
        return mapper.map(source, target);
    }

    public static <T, S> List<T> convertToList(List<S> source) {
        return mapper.map(source, new TypeToken<List<T>>() {
        }.getType());
    }
}
