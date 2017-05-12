package mil.nga.giat.geowave.format.twitter;

import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.index.StringUtils;
import mil.nga.giat.geowave.core.ingest.GeoWaveData;
import mil.nga.giat.geowave.core.store.CloseableIterator;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class TwitterIngestTest
{
	private final static Logger LOGGER = Logger.getLogger(TwitterIngestTest.class);

	private TwitterIngestPlugin ingester;
	private String filePath;
	private int expectedCount;

	@Before
	public void setup() {
		ingester = new TwitterIngestPlugin();
		ingester.init(null);

		filePath = "tweets-20170503p0.json.gz";
		expectedCount = 305;
	}

	@Test
	public void testIngest()
			throws IOException {

		final File toIngest = new File(
				this.getClass().getClassLoader().getResource(
						filePath).getPath());

		assertTrue(TwitterUtils.validate(toIngest));
		final Collection<ByteArrayId> indexIds = new ArrayList<ByteArrayId>();
		indexIds.add(new ByteArrayId(
				"123".getBytes(StringUtils.UTF8_CHAR_SET)));
		final CloseableIterator<GeoWaveData<SimpleFeature>> features = ingester.toGeoWaveData(
				toIngest,
				indexIds,
				"");

		assertTrue((features != null) && features.hasNext());

		int featureCount = 0;
		while (features.hasNext()) {
			final GeoWaveData<SimpleFeature> feature = features.next();

			if (isValidTwitterFeature(feature)) {
				featureCount++;
			}
		}
		features.close();

		final boolean readExpectedCount = (featureCount == expectedCount);
		if (!readExpectedCount) {
			LOGGER.error("Expected " + expectedCount + " features, ingested " + featureCount);
		}
		assertTrue(readExpectedCount);
	}

	private boolean isValidTwitterFeature(
			final GeoWaveData<SimpleFeature> feature ) {
		if ((feature.getValue().getAttribute(
				TwitterUtils.TWITTER_TEXT_ATTRIBUTE) == null) || (feature.getValue().getAttribute(
				TwitterUtils.TWITTER_GEOMETRY_ATTRIBUTE) == null) || (feature.getValue().getAttribute(
				TwitterUtils.TWITTER_DTG_ATTRIBUTE) == null)) {
			return false;
		}
		return true;
	}
}
