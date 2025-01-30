package common

import com.yahoo.labs.samoa.instances.Instance
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter
import common.ConsoleColors.*
import common.RUtils.getPhi
import common.StreamHelper.TOTAL_NUMBER_OF_SAMPLES
import common.StreamHelper.TARGET_MAX
import common.StreamHelper.TARGET_MEAN
import common.StreamHelper.TARGET_MEDIAN
import common.StreamHelper.TARGET_MIN
import common.StreamHelper.TARGET_STDDEV
import common.StreamHelper.TRAINING_PHASE_LENGTH
import common.StreamHelper.WARMING_PHASE_LENGTH
import common.StreamHelper.extremeSides
import common.StreamHelper.relevancyIndex
import common.StreamHelper.streamHeader

import common.Utils.replicateInstances
import de.unknownreality.dataframe.DataFrame
import de.unknownreality.dataframe.csv.CSVWriter
import de.unknownreality.dataframe.csv.CSVWriterBuilder
import histogram.Main
import moa.streams.ArffFileStream
import moa.streams.CachedInstancesStream
import weka.core.Instances
import weka.core.Instance as wekaInstance
import weka.core.converters.ConverterUtils.DataSource
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Add
import java.io.File
import java.util.*
import weka.core.converters.ArffSaver
import java.io.FileWriter
import java.io.IOException


import kotlin.math.round
import kotlin.system.exitProcess

fun <T: Enum<T>> T.next(): T {
    val values = declaringClass.enumConstants
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}
fun getStream(): ArffFileStream {
    val stream = ArffFileStream(tmpPath, classindex)
    stream.prepareForUse()
    return stream
}

fun getDatasetPaths(path: String): MutableList<String> {
    val fileList = mutableListOf<String>()
    File(path).walk().forEach {
        if (it.isFile)
            fileList.add(it.absolutePath)
    }
    return fileList
}

fun DataFrame.saveAsCSV(filePath: String) {
    val file = File(filePath)
    val csvWriter: CSVWriter = CSVWriterBuilder.create()
        .withHeader(true)
        .withSeparator(',')
        .build()
    this.write(file, csvWriter);
    file.setWritable(true)
}

//fun DataFrame.saveAsCSV(filePath: String) {
//    try {
//        val file = File(filePath)
//        val csvWriter: CSVWriter = CSVWriterBuilder.create()
//            .withHeader(true)
//            .withSeparator(',')
//            .build()
//        this.write(file, csvWriter);
//    } catch (e: IOException) {
//        println("$RED saveAsCSV: Error: Unable to save DataFrame to CSV. Reason: ${e.message}. Exiting with non-zero code")
//        exitProcess(1)
//    }
//}




fun Instances.shuffle() {
    this.randomize(Random((1..1000).random().toLong()))
}

fun Instances.save(fileName: String) {
    val s = ArffSaver()
    s.instances = this
    s.setFile(File(fileName))
    s.writeBatch()
}

fun Instance.shouldBeConsidered():Boolean = when(extremeSides){
    StreamHelper.ExtremeType.RIGHT->{
        this.classValue()>=TARGET_MEDIAN
    }
    StreamHelper.ExtremeType.LEFT->{
        this.classValue()<=TARGET_MEDIAN
    }
    else->{
        true
    }
}

fun wekaInstance.shouldBeConsidered():Boolean = when(extremeSides){
    StreamHelper.ExtremeType.RIGHT->{
        this.classValue()>=TARGET_MEDIAN
    }
    StreamHelper.ExtremeType.LEFT->{
        this.classValue()<=TARGET_MEDIAN
    }
    else->{
        true
    }
}


fun getCachedInstancesStream(currentDataset: String,numberOfReplication:Int=1): CachedInstancesStream {
    if (DEBUG) println("$BLUE_BRIGHT currentDataset $currentDataset, numberOfReplication=$numberOfReplication $RESET")
    val source = DataSource(currentDataset)
    val data = source.dataSet

    if (DEBUG) println("Original size of dataset: ${data.size}")
    data.shuffle()




    try {
        streamHeader?.let {} ?: run {
            CachedInstancesStream(WekaToSamoaInstanceConverter().samoaInstances(data)).let { streamHeader = it.header}
        }
    }catch (e:Exception){
        println("streamHeader not set. Last value will be used!")
    }

    if (data!!.classIndex() == -1) {
        with(data) {
            relevancyIndex?.let {
                setClassIndex(numAttributes() - 2)
            }?: run {
                // if relevancyIndex is null.
                setClassIndex(numAttributes() - 1)
            }
        }
    }

    val classValues = data.attributeToDoubleArray(data.classIndex())

    // Calculate some statistics from class values
    val stats = data.attributeStats(data.classIndex()).numericStats
    TOTAL_NUMBER_OF_SAMPLES = stats.count
//    WARMING_PHASE_LENGTH = (TOTAL_NUMBER_OF_SAMPLES*(15/100)).toInt()
//    TRAINING_PHASE_LENGTH = (TOTAL_NUMBER_OF_SAMPLES*(65/100)).toInt()
    TARGET_MIN = stats.min
    TARGET_MAX = stats.max
    TARGET_MEAN = stats.mean
    TARGET_STDDEV = stats.stdDev
    TARGET_MEDIAN = data.kthSmallestValue(data.classAttribute(), (data.size/2))
    if (DEBUG) println("TARGET_COUNT=$TOTAL_NUMBER_OF_SAMPLES, TARGET_MEAN:$TARGET_MEAN, TARGET_STDDEV=$TARGET_STDDEV, TARGET_MEDIAN=$TARGET_MEDIAN, TARGET_MIN=$TARGET_MIN, TARGET_MAX=$TARGET_MAX")


    var newInstances: Instances? = null
    relevancyIndex?.let{
        //NOTE: Relevancy index in the data MUST be the last index
        newInstances = data
    } ?: run {
        val phi = getPhi(classValues)
        val filter = Add()
        filter.attributeIndex = LAST_INDEX
        filter.attributeName = PHI
        filter.setInputFormat(data)
        newInstances = Filter.useFilter(data, filter)
        val phiIndex = newInstances!!.numAttributes() - 1
        (0 until newInstances!!.size).forEach { i ->
            newInstances!!.instance(i).setValue(phiIndex, phi[i])
        }
    }
    val replicatedInstances = replicateInstances(newInstances!!, numberOfReplication)
    if (DEBUG) println("Size of dataset after replication:" + replicatedInstances.size)
    val instances = WekaToSamoaInstanceConverter().samoaInstances(replicatedInstances)
    return CachedInstancesStream(instances)
}

fun extractExtremes(data:Instances, phiAttributeName:String, phiThreshold: Double =0.8): Instances {
    val extremes=Instances(data,0)
    val phiIndex: Int = data.attribute(phiAttributeName).index()
    (0 until data.size).forEach { i->
        if(data[i].shouldBeConsidered() && data[i].value(phiIndex)>=phiThreshold){
            extremes.add(data[i])
        }
    }
    extremes.save("Datasets/00Extremes.arff")
    return extremes
}

fun subSamples(data:Instances, n:Int) {
    val tmpInstances=Instances(data,0)
    data.shuffle()
    (0 until n).forEach { i->
        tmpInstances.add(data[i])
    }
    tmpInstances.save("Datasets/00sample.arff")
}

fun getStatistics(datasetName: String,threshold:Double): List<Double> {
    if (DEBUG) println("currentDataset $datasetName")
    val source = DataSource(datasetName)
    val data = source.dataSet
    if (data!!.classIndex() == -1) {
        with(data) { setClassIndex(numAttributes() - 1) }
    }
    val classValues = data.attributeToDoubleArray(data.classIndex())
    val phi = getPhi(classValues)
    val median = data.kthSmallestValue(data.classAttribute(), (data.size/2))
    var rights=0
    var lefts=0
    (phi.indices).forEach { i ->
        if(phi[i]>=threshold && classValues[i]>median) rights++
        else if(phi[i]>=threshold && classValues[i]<median) lefts++
    }
    val size=data.size
    val rightPercent=((1.0*rights/size)*100).round(2)
    val leftPercent=((1.0*lefts/size)*100).round(2)
    val total=rights+lefts
    val tolalPercent=leftPercent+rightPercent
    return listOf(data.size.toDouble(),rights.toDouble(),rightPercent,lefts.toDouble(),leftPercent,total.toDouble(),tolalPercent.round(2))

}
fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
