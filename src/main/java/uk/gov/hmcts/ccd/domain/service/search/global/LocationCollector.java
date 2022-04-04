package uk.gov.hmcts.ccd.domain.service.search.global;

import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

public class LocationCollector implements Collector<BuildingLocation, LocationLookup, LocationLookup> {

    @Override
    public Supplier<LocationLookup> supplier() {
        return LocationLookup::new;
    }

    @Override
    public BiConsumer<LocationLookup, BuildingLocation> accumulator() {
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
