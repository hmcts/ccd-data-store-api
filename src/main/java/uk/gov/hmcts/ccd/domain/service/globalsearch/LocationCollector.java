package uk.gov.hmcts.ccd.domain.service.globalsearch;

import uk.gov.hmcts.ccd.domain.model.search.global.LocationLookup;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

public class LocationCollector implements Collector<LocationRefData, LocationLookup, LocationLookup> {

    @Override
    public Supplier<LocationLookup> supplier() {
        return LocationLookup::new;
    }

    @Override
    public BiConsumer<LocationLookup, LocationRefData> accumulator() {
        return LocationLookup::add;
    }

    @Override
    public BinaryOperator<LocationLookup> combiner() {
        return (LocationLookup::combine);
    }

    @Override
    public Function<LocationLookup, LocationLookup> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(IDENTITY_FINISH, UNORDERED);
    }

    public static LocationCollector toLocationLookup() {
        return new LocationCollector();
    }
}
