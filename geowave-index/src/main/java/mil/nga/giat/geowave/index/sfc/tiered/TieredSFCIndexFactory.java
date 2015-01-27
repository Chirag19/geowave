package mil.nga.giat.geowave.index.sfc.tiered;

import java.util.Arrays;

import mil.nga.giat.geowave.index.dimension.NumericDimensionDefinition;
import mil.nga.giat.geowave.index.sfc.SFCDimensionDefinition;
import mil.nga.giat.geowave.index.sfc.SFCFactory;
import mil.nga.giat.geowave.index.sfc.SFCFactory.SFCType;
import mil.nga.giat.geowave.index.sfc.SpaceFillingCurve;

import com.google.common.collect.ImmutableBiMap;

/**
 * A factory for creating TieredSFCIndexStrategy using various approaches for
 * breaking down the bits of precision per tier
 *
 */
public class TieredSFCIndexFactory
{
	private static int DEFAULT_NUM_TIERS = 11;

	/**
	 * Used to create a Single Tier Index Strategy. For example, this would be
	 * used to generate a strategy that has Point type spatial data.
	 *
	 * @param dimensionDefs
	 *            an array of SFC Dimension Definition objects
	 * @param sfc
	 *            the type of space filling curve (e.g. Hilbert)
	 * @return an Index Strategy object with a single tier
	 */
	static public TieredSFCIndexStrategy createSingleTierStrategy(
			final SFCDimensionDefinition[] dimensionDefs,
			final SFCType sfc ) {
		final SpaceFillingCurve[] orderedSfcs = new SpaceFillingCurve[] {
			SFCFactory.createSpaceFillingCurve(
					dimensionDefs,
					sfc)
		};
		// unwrap SFC dimension definitions
		final NumericDimensionDefinition[] baseDefinitions = new NumericDimensionDefinition[dimensionDefs.length];
		int minBitsOfPrecision = Integer.MAX_VALUE;
		for (int d = 0; d < baseDefinitions.length; d++) {
			baseDefinitions[d] = dimensionDefs[d].getDimensionDefinition();
			minBitsOfPrecision = Math.min(
					dimensionDefs[d].getBitsOfPrecision(),
					minBitsOfPrecision);
		}
		return new TieredSFCIndexStrategy(
				baseDefinitions,
				orderedSfcs,
				ImmutableBiMap.of(
						0,
						(byte) minBitsOfPrecision));
	}

	/**
	 * Used to create a Single Tier Index Strategy. For example, this would be
	 * used to generate a strategy that has Point type spatial data.
	 *
	 * @param dimensionDefs
	 *            an array of SFC Dimension Definition objects
	 * @param sfc
	 *            the type of space filling curve (e.g. Hilbert)
	 * @return an Index Strategy object with a single tier
	 */
	static public TieredSFCIndexStrategy createSingleTierStrategy(
			final NumericDimensionDefinition[] baseDefinitions,
			final int[] maxBitsPerDimension,
			final SFCType sfc ) {
		final SFCDimensionDefinition[] sfcDimensions = new SFCDimensionDefinition[baseDefinitions.length];
		int minBitsOfPrecision = Integer.MAX_VALUE;
		for (int d = 0; d < baseDefinitions.length; d++) {
			sfcDimensions[d] = new SFCDimensionDefinition(
					baseDefinitions[d],
					maxBitsPerDimension[d]);
			minBitsOfPrecision = Math.min(
					maxBitsPerDimension[d],
					minBitsOfPrecision);
		}

		final SpaceFillingCurve[] orderedSfcs = new SpaceFillingCurve[] {
			SFCFactory.createSpaceFillingCurve(
					sfcDimensions,
					sfc)
		};

		return new TieredSFCIndexStrategy(
				sfcDimensions,
				orderedSfcs,
				ImmutableBiMap.of(
						0,
						(byte) minBitsOfPrecision));
	}

	/**
	 *
	 * @param baseDefinitions
	 *            an array of Numeric Dimension Definitions
	 * @param maxBitsPerDimension
	 *            the max cardinality for the Index Strategy
	 * @param sfcType
	 *            the type of space filling curve (e.g. Hilbert)
	 * @return an Index Strategy object with a tier for every incremental
	 *         cardinality between the lowest max bits of precision and 0
	 */
	static public TieredSFCIndexStrategy createFullIncrementalTieredStrategy(
			final NumericDimensionDefinition[] baseDefinitions,
			final int[] maxBitsPerDimension,
			final SFCType sfcType ) {
		if (maxBitsPerDimension.length == 0) {
			final ImmutableBiMap<Integer, Byte> emptyMap = ImmutableBiMap.of();
			return new TieredSFCIndexStrategy(
					baseDefinitions,
					new SpaceFillingCurve[] {},
					emptyMap);
		}
		int numIndices = Integer.MAX_VALUE;
		for (final int element : maxBitsPerDimension) {
			numIndices = Math.min(
					numIndices,
					element + 1);
		}
		final SpaceFillingCurve[] spaceFillingCurves = new SpaceFillingCurve[numIndices];
		final ImmutableBiMap.Builder<Integer, Byte> sfcIndexToTier = ImmutableBiMap.builder();
		for (int sfcIndex = 0; sfcIndex < numIndices; sfcIndex++) {
			final SFCDimensionDefinition[] sfcDimensions = new SFCDimensionDefinition[baseDefinitions.length];
			int minBitsOfPrecision = Integer.MAX_VALUE;
			for (int d = 0; d < baseDefinitions.length; d++) {
				final int bitsOfPrecision = maxBitsPerDimension[d] - (numIndices - sfcIndex - 1);
				minBitsOfPrecision = Math.min(
						bitsOfPrecision,
						minBitsOfPrecision);
				sfcDimensions[d] = new SFCDimensionDefinition(
						baseDefinitions[d],
						bitsOfPrecision);
			}
			sfcIndexToTier.put(
					sfcIndex,
					(byte) minBitsOfPrecision);

			spaceFillingCurves[sfcIndex] = SFCFactory.createSpaceFillingCurve(
					sfcDimensions,
					sfcType);

		}

		return new TieredSFCIndexStrategy(
				baseDefinitions,
				spaceFillingCurves,
				sfcIndexToTier.build());
	}

	/**
	 *
	 * @param baseDefinitions
	 *            an array of Numeric Dimension Definitions
	 * @param maxBitsPerDimension
	 *            the max cardinality for the Index Strategy
	 * @param sfcType
	 *            the type of space filling curve (e.g. Hilbert)
	 * @return an Index Strategy object with a equal interval tiers
	 */
	static public TieredSFCIndexStrategy createEqualIntervalPrecisionTieredStrategy(
			final NumericDimensionDefinition[] baseDefinitions,
			final int[] maxBitsPerDimension,
			final SFCType sfcType ) {
		return createEqualIntervalPrecisionTieredStrategy(
				baseDefinitions,
				maxBitsPerDimension,
				sfcType,
				DEFAULT_NUM_TIERS);
	}

	/**
	 *
	 * @param baseDefinitions
	 *            an array of Numeric Dimension Definitions
	 * @param maxBitsPerDimension
	 *            the max cardinality for the Index Strategy
	 * @param sfcType
	 *            the type of space filling curve (e.g. Hilbert)
	 * @param numTiers
	 *            the number of tiers of the Index Strategy
	 * @return an Index Strategy object with a specified number of tiers
	 */
	static public TieredSFCIndexStrategy createEqualIntervalPrecisionTieredStrategy(
			final NumericDimensionDefinition[] baseDefinitions,
			final int[] maxBitsPerDimension,
			final SFCType sfcType,
			final int numIndices ) {
		// Subtracting one from the number tiers prevents an extra tier. If
		// we decide to create a catch-all, then we can ignore the subtraction.
		final SpaceFillingCurve[] spaceFillingCurves = new SpaceFillingCurve[numIndices];
		final ImmutableBiMap.Builder<Integer, Byte> sfcIndexToTier = ImmutableBiMap.builder();
		for (int sfcIndex = 0; sfcIndex < numIndices; sfcIndex++) {
			final SFCDimensionDefinition[] sfcDimensions = new SFCDimensionDefinition[baseDefinitions.length];
			int minBitsOfPrecision = Integer.MAX_VALUE;
			for (int d = 0; d < baseDefinitions.length; d++) {
				int bitsOfPrecision;
				if (numIndices == 1) {
					bitsOfPrecision = maxBitsPerDimension[d];
				}
				else {
					final double bitPrecisionIncrement = ((double) maxBitsPerDimension[d] / (numIndices - 1));
					bitsOfPrecision = (int) (bitPrecisionIncrement * sfcIndex);
				}
				minBitsOfPrecision = Math.min(
						bitsOfPrecision,
						minBitsOfPrecision);
				sfcDimensions[d] = new SFCDimensionDefinition(
						baseDefinitions[d],
						bitsOfPrecision);
			}
			sfcIndexToTier.put(
					sfcIndex,
					(byte) minBitsOfPrecision);
			spaceFillingCurves[sfcIndex] = SFCFactory.createSpaceFillingCurve(
					sfcDimensions,
					sfcType);

		}

		return new TieredSFCIndexStrategy(
				baseDefinitions,
				spaceFillingCurves,
				sfcIndexToTier.build());
	}

	/**
	 *
	 * @param orderedDimensionDefinitions
	 *            an array of Numeric Dimension Definitions
	 * @param bitsPerDimensionPerLevel
	 * @param sfcType
	 *            the type of space filling curve (e.g. Hilbert)
	 * @return an Index Strategy object with a specified number of tiers
	 */
	static public TieredSFCIndexStrategy createDefinedPrecisionTieredStrategy(
			final NumericDimensionDefinition[] orderedDimensionDefinitions,
			final int[][] bitsPerDimensionPerLevel,
			final SFCType sfcType ) {
		Integer numLevels = null;
		for (final int[] element : bitsPerDimensionPerLevel) {
			if (numLevels == null) {
				numLevels = element.length;
			}
			else {
				numLevels = Math.min(
						numLevels,
						element.length);
			}

			Arrays.sort(element);
		}
		if (numLevels == null) {
			numLevels = 0;
		}

		final SpaceFillingCurve[] orderedSFCTiers = new SpaceFillingCurve[numLevels];
		final int numDimensions = orderedDimensionDefinitions.length;
		final ImmutableBiMap.Builder<Integer, Byte> sfcIndexToTier = ImmutableBiMap.builder();
		for (int l = 0; l < numLevels; l++) {
			final SFCDimensionDefinition[] sfcDimensions = new SFCDimensionDefinition[numDimensions];
			int minBitsOfPrecision = Integer.MAX_VALUE;
			for (int d = 0; d < numDimensions; d++) {
				sfcDimensions[d] = new SFCDimensionDefinition(
						orderedDimensionDefinitions[d],
						bitsPerDimensionPerLevel[d][l]);
				minBitsOfPrecision = Math.min(
						bitsPerDimensionPerLevel[d][l],
						minBitsOfPrecision);
			}
			sfcIndexToTier.put(
					l,
					(byte) minBitsOfPrecision);
			orderedSFCTiers[l] = SFCFactory.createSpaceFillingCurve(
					sfcDimensions,
					sfcType);
		}
		return new TieredSFCIndexStrategy(
				orderedDimensionDefinitions,
				orderedSFCTiers,
				sfcIndexToTier.build());
	}

}
