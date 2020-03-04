package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.collections4.CollectionUtils;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CommonUtils {

    /**
     * For use with java Stream.collect to collect a list of one element down to one single object
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list->{
                    return (CollectionUtils.isNotEmpty(list))?list.get(0):null;
                }
        );
    }
}
