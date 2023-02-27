package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventExpirationJob extends AbstractJob {

	private final FeedDao feedDao;

	protected EventExpirationJob(MeterRegistry meterRegistry, FeedDao feedDao) {
		super(meterRegistry);
		this.feedDao = feedDao;
	}

	@Override
	public void execute() throws Exception {
		feedDao.autoExpireEvents();
	}

	@Override
	public String getName() {
		return "eventExpirationJob";
	}
}
