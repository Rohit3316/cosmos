package org.eclipse.hawkbit.utils;

import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;

public class MapperUtil {
    private MapperUtil(){}

    private static final ModelMapper mapper = new ModelMapper();

    public static <T, S> T convert(S source, Class<T> target) {
        return mapper.map(source, target);
    }

    public static <T, S> List<T> convertToList(List<S> source, Class<T> target) {
        if (Objects.isNull(target)) {
            throw new IllegalArgumentException("Target class must not be null");
        }
        return mapper.map(source, new TypeToken<List<T>>(){}.getType());
    }
}
