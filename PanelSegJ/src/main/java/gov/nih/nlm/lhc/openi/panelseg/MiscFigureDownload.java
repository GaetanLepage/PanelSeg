package gov.nih.nlm.lhc.openi.panelseg;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Created by jzou on 8/25/2016.
 */
public class MiscFigureDownload
{
    /**
     * @description Module to download the images for a given search query.
     * @param	args
     *          query-string	The server url including the query term and other filters.
     *          output-dir		Directory where the images are downloaded.
     *          imageColor-prefix	Server name to be used as imageColor prefix.
     */
    public static void main(String args[]) throws Exception
    {
        //Stop and print error msg if no arguments passed.
        if(args.length < 3)
        {
            System.out.println("Usage: java -cp PanelSegJ.jar MiscFigureDownload <query-string> <output-dir> <imageColor-prefix>");
            System.out.println("	This is a utility program to download figure images through OpenI API.");
            System.out.println("	Note: The downloaded imageColor is the highest resolution images for OpenI web displaying purpose.");
            System.out.println("	      It is not the original imageColor to be processed by PanelSeg. The original imageColor is on Hadoop server.");
            System.out.println("		  he original imageColor is on Hadoop server, and may be retrieved by MiscCopyOriginalImage program");
            System.exit(0);
        }

//		String queryString = "http://ceb-openi-alpha/iti/search?query=turmeric&it=xg";
//		String outputDirPath  = "D:\\users\\jie\\\query-images";
//		String imagePrefix = "http://ceb-openi-alpha";

        String queryString = args[0];
        String outputDirPath  = args[1];
        String imagePrefix = args[2];

        //Variables to store the JSON pagination params and imageColor urls.
        int m = 0, n = 0, total = 100;
        HashSet<String> imageSet = new HashSet<>();

        System.out.println("Query String: " + queryString);

        //Start getting JSON data
        do{
            m = n + 1;
            n = Math.min(m+99, total);

            System.out.println("Downloading results: " + m + " to " + n + "...");
            URL connectionUrl = new URL(queryString + "&m=" + m + "&n=" + n);

            URLConnection urlConn = connectionUrl.openConnection();
            StringBuilder response = new StringBuilder();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConn.getInputStream()));

            String decodedString;
            while ((decodedString = in.readLine()) != null)
            {
                response.append(decodedString);
            }

            //Close connection after reading.
            in.close();

            //save response to JSON file
//	        String responseString = response.toString();
//	        Files.write(Paths.get("./test.json"), responseString.getBytes());

            JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(response.toString());

            JSONArray imgArr = (JSONArray) jsonObj.get("list");

            for(int i=0; i<imgArr.size(); i++){
                JSONObject imgJson = (JSONObject)imgArr.get(i);
                String imgUrl = imagePrefix + imgJson.get("imgLarge");
                imageSet.add(imgUrl);
            }

            total = (Integer)jsonObj.get("total");

            //Throttling down the requests per second to the server.
            threadWait(200);

        }while(n < total && n < 50000); //We download 50,000 maximum for now.
        System.out.println("Total results: " + imageSet.size());


        System.out.println("Using imageColor prefix: " + imagePrefix);
        System.out.println("Output directory to save images: " + outputDirPath);

        int counter = 0;
        //File outputDir = new File(outputDirPath);

        //Download the images.
        for(String eachQueryImage : imageSet)
        {
            String fileBaseName = FilenameUtils.getBaseName(eachQueryImage);
            URL source = new URL(eachQueryImage);
            File destination = new File(outputDirPath, fileBaseName + ".png");
            if (destination.exists()) {counter++; continue; }//already downloaded

            try
            {
                FileUtils.copyURLToFile(source, destination);
                if(++counter%1000 == 0){
                    System.out.println(counter + " images downloaded...");
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            //Throttling down the requests per second to the server.
            threadWait(200);
        }
        System.out.println(counter + " images downloaded in total");
    }

    private static void threadWait(long millis)
    {
        try{
            Thread.sleep(millis);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
