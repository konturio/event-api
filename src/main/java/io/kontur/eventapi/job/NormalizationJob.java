package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class NormalizationJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(NormalizationJob.class);

    private final List<Normalizer> normalizers;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao normalizedObservationsDao;

    @Value("${scheduler.normalization.providers}")
    private String[] providers;

    public NormalizationJob(List<Normalizer> normalizers, DataLakeDao dataLakeDao,
                            NormalizedObservationsDao normalizedObservationsDao, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.normalizers = normalizers;
        this.dataLakeDao = dataLakeDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Override
    public void execute() {
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(Arrays.asList(providers));
        if (!CollectionUtils.isEmpty(dataLakes)) {
            LOG.info("Normalization processing: {} data lakes", dataLakes.size());

            for (DataLake dataLake : dataLakes) {
                boolean isNormalized = normalize(dataLake);
                if (!isNormalized) {
                    LOG.info("Event wasn't normalized. Provider: {}, observation: {}", dataLake.getProvider(),
                            dataLake.getObservationId());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "normalization";
    }

    @Timed(value = "normalization.observation.timer")
    @Counted(value = "normalization.observation.counter")
    private boolean normalize(DataLake dataLake) {
        try {
            Normalizer normalizer = Applicable.get(normalizers, dataLake);
            Optional<NormalizedObservation> normalizedObservationOpt = normalizer.normalize(dataLake);
            if (normalizedObservationOpt.isPresent()) {
                normalizedObservationsDao.insert(normalizedObservationOpt.get());
            } else {
                dataLakeDao.markAsSkipped(dataLake.getObservationId());
            }
            return true;
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return false;
    }
}
