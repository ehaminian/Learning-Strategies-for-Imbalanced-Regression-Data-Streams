package common

import com.yahoo.labs.samoa.instances.InstancesHeader
import moa.classifiers.AbstractClassifier
import moa.classifiers.rules.AMRulesRegressor
import moa.classifiers.rules.functions.Perceptron
import moa.classifiers.rules.functions.TargetMean
import moa.classifiers.trees.FIMTDD

class Learner(type: LearnerType, streamHeader: InstancesHeader) {
    private val learnerModel: AbstractClassifier = when (type) {
        LearnerType.PERCEPTRON -> {
            Perceptron()
        }
        LearnerType.FIMTDD -> {
            FIMTDD()
        }
        LearnerType.TargetMean -> {
            TargetMean()
        }
        LearnerType.AMRulesRegressor -> {
            AMRulesRegressor()
        }
    }

    init {
        learnerModel.modelContext = streamHeader
        learnerModel.prepareForUse()
    }

    val learner get() = learnerModel
}