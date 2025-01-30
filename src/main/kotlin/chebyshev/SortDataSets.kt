package chebyshev

import common.*
import de.unknownreality.dataframe.DataRow


class SortDataSets {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var index=0

            getDatasetPaths(BASE_PATH).forEach {datasetName->
                val dataRow: DataRow = DataRow(dataframeStatistics,index++ )
                println("++++++++++++++++++++++++++++++++++++++++++++++++++++")
                val stat=getStatistics(datasetName,0.8)
                dataRow.set(NAME_OF_DATASET, datasetName)
                dataRow.set(NUMBER_OF_SAMPLES, stat[0])
                dataRow.set(N_RARE_IN_RIGHT, stat[1])
                dataRow.set(PERCENT_RARE_IN_RIGHT, stat[2])
                dataRow.set(N_RARE_IN_LEFT, stat[3])
                dataRow.set(PERCENT_RARE_IN_LEFT, stat[4])
                dataRow.set(TOTAL_RARE, stat[5])
                dataRow.set(PERCENT_TOTAL, stat[6])
                dataframeStatistics.append(dataRow)
            }
            dataframeStatistics.sort(NUMBER_OF_SAMPLES)
            dataframeStatistics.saveAsCSV("./Results/statistics_08.csv")


        }
    }

}
