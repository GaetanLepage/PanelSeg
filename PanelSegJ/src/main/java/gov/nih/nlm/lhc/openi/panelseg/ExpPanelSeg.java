package gov.nih.nlm.lhc.openi.panelseg;

import org.bytedeco.javacpp.opencv_core;

import java.nio.file.Path;
import java.util.List;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

/**
 * Experiments of various methods for Panel Segmentation
 *
 * Created by jzou on 11/4/2016.
 */
public class ExpPanelSeg extends Exp
{
    public static void main(String args[]) throws Exception {
        //Stop and print error msg if no arguments passed.
        if (args.length != 1 && args.length != 3) {
            System.out.println();

            System.out.println("Usage: java -cp PanelSegJ.jar ExpPanelSeg <Method> <Sample List File> <Target Folder>");
            System.out.println("Panel Segmentation with various methods.");
            System.out.println("Results (iPhotoDraw XML file) and preview images are saved in target folder.");

            System.out.println();

            System.out.println("Method:");
            System.out.println("LabelDetHog                 HoG method for Label Detection");
            System.out.println("LabelRegHogSvm              HoG+SVM method for Label Recognition");
            System.out.println("LabelRegHogSvmThreshold     HoG+SVM then followed by simple threshold for Label Recognition");
            System.out.println("LabelRegHogSvmBeam	        HoG+SVM then followed by beam search for Label Recognition");

            System.out.println("LabelDetHogLeNet5	        HoG+LeNet5 method for Label Detection");
            System.out.println("LabelRegHogLeNet5Svm	    HoG+LeNet5+SVM method for Label Recognition");

            System.out.println();

            System.out.println("Default Sample List File: \\Users\\jie\\projects\\PanelSeg\\Exp\\eval.txt");
            System.out.println("Default target folder: \\Users\\jie\\projects\\PanelSeg\\Exp\\PanelSeg\\eval");

            System.out.println();

            System.exit(0);
        }

        PanelSeg.SegMethod method = null;
        switch (args[0]) {
            case "LabelDetHog":  method = PanelSeg.SegMethod.LabelDetHog; break;
            case "LabelRegHogSvm": method = PanelSeg.SegMethod.LabelRegHogSvm; break;
            case "LabelRegHogSvmThreshold": method = PanelSeg.SegMethod.LabelRegHogSvmThreshold; break;
            case "LabelRegHogSvmBeam": method = PanelSeg.SegMethod.LabelRegHogSvmBeam; break;

            case "LabelDetHogLeNet5": method = PanelSeg.SegMethod.LabelDetHogLeNet5; break;
            case "LabelRegHogLeNet5Svm": method = PanelSeg.SegMethod.LabelRegHogLeNet5Svm; break;
            default:
                System.out.println("Unknown method!!");
                System.exit(0);
        }

        String trainListFile, targetFolder;
        if (args.length == 1)
        {
            trainListFile = "\\Users\\jie\\projects\\PanelSeg\\Exp\\eval.txt";
            targetFolder = "\\Users\\jie\\projects\\PanelSeg\\Exp\\PanelSeg\\eval";
        }
        else
        {
            trainListFile = args[1];
            targetFolder = args[2];
        }

        PanelSeg.initialize(method);

        ExpPanelSeg exp = new ExpPanelSeg(trainListFile, targetFolder, method);
        exp.segmentSingle();
        //exp.segmentMulti();
        System.out.println("Completed!");
    }

    private PanelSeg.SegMethod method;

    /**
     * Ctor, set targetFolder and then collect all imagePaths
     * It also clean the targetFolder
     *
     * @param trainListFile
     * @param targetFolder
     */
    private ExpPanelSeg(String trainListFile, String targetFolder, PanelSeg.SegMethod method) {
        super(trainListFile, targetFolder, true);
        this.method = method;
    }

    void doExp(int k) throws Exception
    {
        Path imagePath = imagePaths.get(k);
        System.out.println(Integer.toString(k) +  ": processing " + imagePath.toString());

        //if (!imagePath.toString().endsWith("PMC1397864_1472-6882-6-3-8.jpg")) return;

        opencv_core.Mat image = imread(imagePath.toString(), CV_LOAD_IMAGE_COLOR);
        List<Panel> panels = PanelSeg.segment(image, method);

        saveSegResult(imagePath.toFile().getName(), image, panels);
    }

}
