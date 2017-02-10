package gov.nih.nlm.lhc.openi.panelseg;

import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * Refactor from TrainLabelDetect to use Properties to configure the training.
 *
 * Created by jzou on 2/8/2017.
 */
public class ExpTrainLabelDetectHog extends Exp
{
    public enum Task { HogPos, HogSvmFeaExt, HogSvm2SingleVec, HogBootstrap }

    private String posGtFolder, posFolder, negFolder, modelFolder, detectedFolder;
    private String trainFile, svmModelFile, vectorSvmFile;


    public static void main(String args[])
    {
        log.info("Training tasks for HOG-based Label Detection.");

        ExpTrainLabelDetectHog exp = new ExpTrainLabelDetectHog();
        try
        {
            exp.initialize("ExpTrainLabelDetectHog.properties");
            exp.doWork();
            log.info("Completed!");
        }
        catch (Exception ex)
        {
            log.error(ex.getMessage());
        }
    }

    private ExpTrainLabelDetectHog.Task task;

    private ExpTrainLabelDetectHog() {super();}

    /**
     * Load the properties from ExpPanelSeg.properties file.
     * Also, validate all property values, throw exceptions if not valid.
     * Then, do initializations, including static fields, etc.
     * @throws Exception
     */
    @Override
    void initialize(String propertyFile) throws Exception
    {
        super.initialize(propertyFile);

        //Task
        String strTask = setProperty("task");
        switch (strTask)
        {
            case "HogPos":                task = Task.HogPos;                break;
            case "HogSvmFeaExt":          task = Task.HogSvmFeaExt;          break;
            case "HogSvm2SingleVec":      task = Task.HogSvm2SingleVec;      break;
            case "HogBootstrap":          task = Task.HogBootstrap;          break;
            default: throw new Exception("Task " + strTask + " is Unknown");
        }

        switch (task)
        {
            case HogPos:                initializeHogPos();                break;
            case HogSvmFeaExt:          initializeHogSvmFeaExt();          break;
            case HogSvm2SingleVec:            break;
            case HogBootstrap:          initializeHogBootstrap();          break;
        }

//        this.modelFolder = properties.getProperty("modelFolder");
//        if (modelFolder == null) throw new Exception("ERROR: modelFolder property is Missing.");
//        log.info("modelFolder: " + modelFolder);
//
//        this.svmModelFile = properties.getProperty("svmModelFile");
//        if (svmModelFile == null) throw new Exception("ERROR: svmModelFile property is Missing.");
//        log.info("svmModelFile: " + svmModelFile);
//
//        this.vectorSvmFile = properties.getProperty("vectorSvmFile");
//        if (vectorSvmFile == null) throw new Exception("ERROR: vectorSvmFile property is Missing.");
//        log.info("vectorSvmFile: " + vectorSvmFile);
//
//        this.detectedFolder = properties.getProperty("detectedFolder");
//        if (detectedFolder == null) throw new Exception("ERROR: detectedFolder property is Missing.");
//        log.info("detectedFolder: " + detectedFolder);
    }

    private void initializeHogPos() throws Exception
    {
        setProperty("labelSetsHOG");
        setProperty("threading");
        setProperty("listFile");
        setProperty("targetFolder");
        posGtFolder = setProperty("posGtFolder");

        System.out.println();

        for (String name : LabelDetectHog.labelSetsHOG)
        {
            Path folder = this.targetFolder.resolve(name);
            folder = folder.resolve(posGtFolder);
            System.out.println(folder + " is going to be cleaned!");
        }
    }

    private void initializeHogSvmFeaExt() throws Exception
    {
        setProperty("labelSetsHOG");
        posFolder = setProperty("posFolder");
        negFolder = setProperty("negFolder");
        modelFolder = setProperty("modelFolder");
        trainFile = setProperty("trainFile");
    }

    private void initializeHogBootstrap()
    {
        for (String name : LabelDetectHog.labelSetsHOG)
        {
            Path folder = this.targetFolder.resolve(name);
            folder = folder.resolve(detectedFolder);
            AlgMiscEx.createClearFolder(folder);
            log.info("Folder " + folder + " is created or cleaned!");
        }
    }

    @Override
    void doWork() throws Exception
    {
        super.doWork();

        //initialize for certain tasks
        switch (task)
        {
            case HogPos:
                //Clean up all the folders
                for (String name : LabelDetectHog.labelSetsHOG) {
                    Path folder = this.targetFolder.resolve(name);
                    folder = folder.resolve(posGtFolder);
                    AlgMiscEx.createClearFolder(folder);
                    log.info("Folder " + folder + " is created or cleaned!");
                }
                break;
            case HogBootstrap:  initializeHogBootstrap();   break;
        }

        //Do work
        switch (task)
        {
            case HogPos:
            case HogBootstrap:
                if (multiThreading) doWorkMultiThread();
                else doWorkSingleThread();
                break;
            case HogSvmFeaExt:
                doWorkHogSvmFeaExt();     break;
            case HogSvm2SingleVec:
                doWorkHogSvm2SingleVec(); break;
        }
    }

    @Override
    void doWork(int k) throws Exception
    {
        super.doWork(k);
        switch (task)
        {
            case HogPos: doWorkHogPos(k); break;
            case HogBootstrap: doWorkHogBootstrap(k); break;
        }
    }

    private void doWorkHogPos(int i)
    {
        Path imagePath = imagePaths.get(i);
        String imageFile = imagePath.toString();
        //System.out.println(Integer.toString(i) +  ": processing " + imageFile);

        //Load annotation
        String xmlFile = FilenameUtils.removeExtension(imageFile) + "_data.xml";
        File annotationFile = new File(xmlFile);
        ArrayList<Panel> panels = null;
        try {
            panels = iPhotoDraw.loadPanelSegGt(annotationFile);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Figure figure = new Figure(imagePath, panels);
        figure.cropLabelGrayNormPatches(64, 64);

        for (Panel panel : figure.panels)
        {
            if (panel.labelRect == null) continue; //No label rect
            if (panel.panelLabel.length() != 1) continue; //Label has more than 1 char, we ignore for now.

            String name = LabelDetectHog.getLabelSetName(panel.panelLabel.charAt(0));
//                if (name == null)
//                {
//                    System.out.println(panel.panelLabel); continue;
//                }
            Path folder = targetFolder.resolve(name).resolve(posGtFolder);
            Path file = folder.resolve(getLabelPatchFilename(imageFile, panel));
            opencv_imgcodecs.imwrite(file.toString(), panel.labelGrayNormPatch);
        }
    }

    private void doWorkHogSvmFeaExt()
    {
        for (int i = 0; i <  LabelDetectHog.labelSetsHOG.length; i++) {
            String name = LabelDetectHog.labelSetsHOG[i];

            Path folder = targetFolder.resolve(name);
            Path folderPos = folder.resolve(posFolder);
            Path folderNeg = folder.resolve(negFolder);
            Path folderModel = folder.resolve(modelFolder);

            List<Path> posPatches = AlgMiscEx.collectImageFiles(folderPos);
            List<Path> negPatches = AlgMiscEx.collectImageFiles(folderNeg);

            Path file = folderModel.resolve(trainFile);
            double[] targets = new double[posPatches.size() + negPatches.size()];
            float[][] features = new float[posPatches.size() + negPatches.size()][];

            int k = 0;
            for (Path path : posPatches) {
                opencv_core.Mat gray = imread(path.toString(), CV_LOAD_IMAGE_GRAYSCALE);
                float[] feature = LabelDetectHog.featureExtraction(gray);
                features[k] = feature;
                targets[k] = 1.0;
                k++;
            }
            for (Path path : negPatches) {
                opencv_core.Mat gray = imread(path.toString(), CV_LOAD_IMAGE_GRAYSCALE);
                float[] feature = LabelDetectHog.featureExtraction(gray);
                features[k] = feature;
                targets[k] = 0.0;
                k++;
            }

            LibSvmEx.SaveInLibSVMFormat(file.toString(), targets, features);
        }
    }

    private void doWorkHogBootstrap(int k)
    {
        Path imagePath = imagePaths.get(k);
        String imageFile = imagePath.toString();

        Figure figure = new Figure(imageFile);
        LabelDetectHog hog = new LabelDetectHog(figure);
        hog.hoGDetect();

        //Save detected patches
        for (int i = 0; i < hog.hogDetectionResult.size(); i++)
        {
            ArrayList<Panel> segmentationResult = hog.hogDetectionResult.get(i);
            if (segmentationResult == null) continue;

            for (int j = 0; j < segmentationResult.size(); j++)
            {
                if (j == 3) break; //We just save the top 3 patches for training, in order to avoiding collecting a very large training set at the beginning.

                Panel panel = segmentationResult.get(j);
                Rectangle rectangle = panel.labelRect;

                opencv_core.Mat patch = AlgOpenCVEx.cropImage(hog.figure.imageGray, rectangle);
                panel.labelGrayNormPatch = new opencv_core.Mat();
                resize(patch, panel.labelGrayNormPatch, new opencv_core.Size(64, 64)); //Resize to 64x64 for easy browsing the results

                Path folder = targetFolder.resolve(panel.panelLabel).resolve(detectedFolder);
                Path file = folder.resolve(getLabelPatchFilename(imageFile, panel));
                opencv_imgcodecs.imwrite(file.toString(), panel.labelGrayNormPatch);
            }
        }
    }

    private void doWorkHogSvm2SingleVec()
    {
        Path vectorPath = targetFolder.resolve(vectorSvmFile);

        //Save to a java file
        try (PrintWriter pw = new PrintWriter(vectorPath.toString()))
        {
            pw.println("package gov.nih.nlm.lhc.openi.panelseg;");
            pw.println();

            for (String name : LabelDetectHog.labelSetsHOG)
            {
                Path folder = targetFolder.resolve(name);
                Path folderModel = folder.resolve(modelFolder);
                Path modelPath = folderModel.resolve(svmModelFile);

                float[] singleVector = LibSvmEx.ToSingleVector(modelPath.toString());

                String classname =	"PanelSegLabelRegHoGModel_" + name;

                pw.println("final class " + classname);
                pw.println("{");

                pw.println("	static float[] svmModel = ");

                pw.println("    	{");
                for (int k = 0; k < singleVector.length; k++)
                {
                    pw.print(singleVector[k] + "f,");
                }
                pw.println();
                pw.println("    };");
                pw.println("}");
            }
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
