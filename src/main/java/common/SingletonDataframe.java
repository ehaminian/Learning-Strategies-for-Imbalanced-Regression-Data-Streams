package common;

import de.unknownreality.dataframe.DataFrame;

import static common.ConstantsKt.*;

public class SingletonDataframe {
    private SingletonDataframe() {
    }
    private static class SingletonClassHolder {
        static final DataFrame SINGLE_INSTANCE_FOR_HISTOGRAM_METHOD = DataFrame.create()
                .addIntegerColumn(ROW_NUMBER)
                .addDoubleColumn(PROB_HIST)
                .addDoubleColumn(K_HIST)
                .addDoubleColumn(TRUE_VALUE)
                .addDoubleColumn(PREDICTION_BASE)
                .addDoubleColumn(PREDICTION_HIST_US)
                .addDoubleColumn(PREDICTION_HIST_OS)
                .addDoubleColumn(RELEVANCY);
        static final DataFrame SINGLE_INSTANCE_FOR_CHEBYSHEV_METHOD = DataFrame.create()
                .addIntegerColumn(ROW_NUMBER)
                .addDoubleColumn(CHEBYSHEV_PROBABILITY)
                .addDoubleColumn(CHEBYSHEV_K)
                .addDoubleColumn(TRUE_VALUE)
                .addDoubleColumn(PREDICTION_BASE)
                .addDoubleColumn(PREDICTION_UNDER)
                .addDoubleColumn(PREDICTION_OVER)
                .addDoubleColumn(RELEVANCY);
        static final DataFrame SINGLE_INSTANCE_FOR_TEST = DataFrame.create()
                .addIntegerColumn(ROW_NUMBER)
                .addDoubleColumn(TRUE_VALUE)
                .addDoubleColumn(PREDICTION_1)
                .addDoubleColumn(PREDICTION_2)
                .addDoubleColumn(PREDICTION_3)
                .addDoubleColumn(PHI);
        static final DataFrame SINGLE_INSTANCE_FOR_DATASETS_STATISTICS = DataFrame.create()
                .addStringColumn(NAME_OF_DATASET)
                .addIntegerColumn(NUMBER_OF_SAMPLES)
                .addDoubleColumn(N_RARE_IN_RIGHT)
                .addDoubleColumn(PERCENT_RARE_IN_RIGHT)
                .addDoubleColumn(N_RARE_IN_LEFT)
                .addDoubleColumn(PERCENT_RARE_IN_LEFT)
                .addDoubleColumn(TOTAL_RARE)
                .addDoubleColumn(PERCENT_TOTAL);
    }
    public static DataFrame getInstanceForHistogramMethod() {
        return SingletonClassHolder.SINGLE_INSTANCE_FOR_HISTOGRAM_METHOD;
    }
    public static DataFrame getInstanceForChebyshevMethod() {
        return SingletonClassHolder.SINGLE_INSTANCE_FOR_CHEBYSHEV_METHOD;
    }
    public static DataFrame getInstanceForTest() {
        return SingletonClassHolder.SINGLE_INSTANCE_FOR_TEST;
    }
    public static DataFrame getInstanceForStatistics() {
        return SingletonClassHolder.SINGLE_INSTANCE_FOR_DATASETS_STATISTICS;
    }
}