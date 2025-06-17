package io.kontur.eventapi.firms.normalization;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.job.Applicable;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.firms.FirmsUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class FirmsNormalizationJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(FirmsNormalizationJob.class);

    private final List<Normalizer> normalizers;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao normalizedObservationsDao;

    public FirmsNormalizationJob(List<Normalizer> normalizers, DataLakeDao dataLakeDao,
                                 NormalizedObservationsDao normalizedObservationsDao, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.normalizers = normalizers;
        this.dataLakeDao = dataLakeDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Override
    public void execute() {
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(FirmsUtil.FIRMS_PROVIDERS);
        if (!CollectionUtils.isEmpty(dataLakes)) {
            LOG.info("Firms Normalization processing: {} data lakes", dataLakes.size());

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
        return "firmsNormalization";
    }

    @Timed(value = "normalization.observation.timer")
    @Counted(value = "normalization.observation.counter")
    protected boolean normalize(DataLake denormalizedEvent) {
        try {
            Normalizer normalizer = Applicable.get(normalizers, denormalizedEvent);
            if (!normalizer.isSkipped()) {
                NormalizedObservation normalizedDto = normalizer.normalize(denormalizedEvent);
                normalizedObservationsDao.insert(checkNotNull(normalizedDto));
            } else {
                dataLakeDao.markAsSkipped(denormalizedEvent.getObservationId());
            }
            return true;
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return false;
    }
}
