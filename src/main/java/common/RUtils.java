package common;

import com.github.rcaller.graphics.SkyTheme;
import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCallerOptions;
import com.github.rcaller.rstuff.RCode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class RUtils {

    public static String getRFunction(String funName) throws IOException, URISyntaxException {
        URI rScriptUri = Objects.requireNonNull(RUtils.class.getClassLoader().getResource(funName)).toURI();
        Path inputScript = Paths.get(rScriptUri);
        return Files.lines(inputScript).collect(Collectors.joining());
    }
    public static void savePicture() throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("savePicture.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addRCode("savePicture()");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runOnly();
    }
    public static void savePictureHistogram() throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("savePictureHistogram.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addRCode("savePicture()");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runOnly();
    }

    public static double rmseIRon(double[] values1, double[] values2) throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("rmseIRon.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("input1", values1);
        code.addDoubleArray("input2", values2);
        code.addRCode("result <- rmseIRon(input1,input2)");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runAndReturnResult("result");
        return caller.getParser().getAsDoubleArray("result")[0];
    }

    public static double[] getPhi(double[] values1) throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("phiRon.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("input1", values1);
        code.addRCode("result <- phiIRon(input1)");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runAndReturnResult("result");
        return caller.getParser().getAsDoubleArray("result");
    }

    public static void histogram(double[] vector, int numberOfBreaks, String title) throws IOException, URISyntaxException {
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("hist.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("vector", vector);
        code.addInt("numberOfBreaks", numberOfBreaks);
        code.addString("title", title);
        File plt = code.startPlot();
        code.addRCode("result<-histogram(vector,numberOfBreaks,title)");
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
        code.showPlot(plt);
    }

    public static void histogram(double[] vector) throws IOException, URISyntaxException {
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("hist.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("vector", vector);
        File plt = code.startPlot();
        code.addRCode("result<-simpleHistogram(vector)");
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
        code.showPlot(plt);
    }

    public static void barPlot(double[] vector1, double[] vector2) throws IOException, URISyntaxException {
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("barPlot.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("vector1", vector1);
        code.addDoubleArray("vector2", vector2);
        File plt = code.startPlot();
        code.addRCode("result<-barPlot(vector1,vector2)");
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
        code.showPlot(plt);
    }

    public static void histFromFrequencies(double[] frequencies) throws IOException, URISyntaxException {
        int l = frequencies.length;
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("barPlot.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("vector1", frequencies);
        File plt = code.startPlot();
        code.addRCode(new StringBuilder().append("result<-barPlot(vector1,1:").append(l).append(")").toString());
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
        code.showPlot(plt);
    }

    public static void phiGraph(double[] vectorPhi, double[] vectorY) throws IOException, URISyntaxException {
        int l = vectorPhi.length;
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("lineGraph.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("vectorPhi", vectorPhi);
        code.addDoubleArray("vectorY", vectorY);
        File plt = code.startPlot();
        code.addRCode("result<-lineGraph(vectorPhi,vectorY)");
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
        code.showPlot(plt);
    }

    public static double[] analyseHistogram() throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("analyseHistogram.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addRCode("result <- analyseHistogram()");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runAndReturnResult("result");
        return caller.getParser().getAsDoubleArray("result");
    }


    public static double[] analyseCheby() throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("analyseCheby.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addRCode("result <- analyseCheby()");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runAndReturnResult("result");
        return caller.getParser().getAsDoubleArray("result");
    }

    public static double[] analyse(String type,double median) throws IOException, URISyntaxException {
        String fileContent = RUtils.getRFunction("analyseCheby.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addString("type", type);
        code.addDouble("median", median);
        code.addRCode("result <- analyse(type,median)");
        RCaller caller = RCaller.create(code, RCallerOptions.create());
        caller.runAndReturnResult("result");
        return caller.getParser().getAsDoubleArray("result");
    }
    public static void histogram(double[] counts, double[] breaks) throws IOException, URISyntaxException {
        RCaller caller = RCaller.create();
        caller.setGraphicsTheme(new SkyTheme());
        String fileContent = RUtils.getRFunction("drawHistFromCountBreaks.R");
        RCode code = RCode.create();
        code.addRCode(fileContent);
        code.addDoubleArray("counts", counts);
        code.addDoubleArray("breaks", breaks);
        File plt = code.startPlot();
        code.addRCode("result<-drawHistFromCountBreaks(counts,breaks)");
        code.endPlot();
        caller.setRCode(code);
        caller.runAndReturnResult("result");
    }

}
